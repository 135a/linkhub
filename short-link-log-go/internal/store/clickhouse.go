package store

import (
	"context"
	"errors"
	"fmt"
	"log"
	"shortlink-log-go/internal/model"
	"strings"
	"sync/atomic"
	"time"

	clickhouse "github.com/ClickHouse/clickhouse-go/v2"
	"github.com/ClickHouse/clickhouse-go/v2/lib/driver"
)

type ClickHouseStore struct {
	conn         driver.Conn
	ch           chan model.LogEntry
	wal          *WAL
	warnInterval time.Duration
	lastWarnAt   int64
	writeSuccess atomic.Int64
	writeFail    atomic.Int64
	droppedCount atomic.Int64
	walPending   atomic.Int64
	channelUsage atomic.Int64 // percentage [0,100]
}

func NewClickHouseStore(host, port, user, password string) (*ClickHouseStore, error) {
	conn, err := clickhouse.Open(&clickhouse.Options{
		Addr: []string{fmt.Sprintf("%s:%s", host, port)},
		Auth: clickhouse.Auth{
			Database: "default",
			Username: user,
			Password: password,
		},
		Settings: map[string]interface{}{
			"async_insert":          1,
			"wait_for_async_insert": 0,
		},
	})
	if err != nil {
		return nil, fmt.Errorf("clickhouse connect: %w", err)
	}

	s := &ClickHouseStore{
		conn:         conn,
		ch:           make(chan model.LogEntry, 10000),
		wal:          NewWAL("./data/wal.log", 100*1024*1024),
		warnInterval: 5 * time.Second,
	}
	if err := s.recoverFromWAL(); err != nil {
		log.Printf("[WARN] recover wal failed during startup: %v", err)
	}
	go s.batchLoop()
	go s.channelMonitorLoop()
	return s, nil
}

func (s *ClickHouseStore) Ping() error {
	return s.conn.Ping(context.Background())
}

func (s *ClickHouseStore) Buffer(entry model.LogEntry) {
	capacity := cap(s.ch)
	if capacity == 0 {
		return
	}
	usagePct := float64(len(s.ch)) / float64(capacity) * 100
	if usagePct >= 95 {
		s.droppedCount.Add(1)
		return
	}

	select {
	case s.ch <- entry:
	default:
		s.droppedCount.Add(1)
	}
}

func (s *ClickHouseStore) batchLoop() {
	ticker := time.NewTicker(2 * time.Second)
	defer ticker.Stop()

	buf := make([]model.LogEntry, 0, 1000)
	for {
		select {
		case entry := <-s.ch:
			buf = append(buf, entry)
			if len(buf) >= 1000 {
				s.flush(buf)
				buf = make([]model.LogEntry, 0, 1000)
			}
		case <-ticker.C:
			if len(buf) > 0 {
				s.flush(buf)
				buf = make([]model.LogEntry, 0, 1000)
			}
		}
	}
}

func (s *ClickHouseStore) flush(batch []model.LogEntry) {
	if len(batch) == 0 {
		return
	}
	if err := s.recoverFromWAL(); err != nil {
		log.Printf("[WARN] recover wal before flush failed: %v", err)
	}

	b, err := s.conn.PrepareBatch(context.Background(), "INSERT INTO logs (timestamp, level, service, trace_id, thread, message, fields)")
	if err != nil {
		s.writeFail.Add(int64(len(batch)))
		s.appendWAL(batch, err)
		return
	}
	for _, e := range batch {
		b.Append(e.Timestamp, e.Level, e.Service, e.TraceID, e.Thread, e.Message, e.Fields)
	}
	if err := b.Send(); err != nil {
		s.writeFail.Add(int64(len(batch)))
		s.appendWAL(batch, err)
		return
	}
	s.writeSuccess.Add(int64(len(batch)))
}

func (s *ClickHouseStore) Query(req model.LogQueryRequest) (*model.LogQueryResponse, error) {
	var where []string
	var args []any

	if req.StartTime != nil {
		where = append(where, "timestamp >= ?")
		args = append(args, *req.StartTime)
	}
	if req.EndTime != nil {
		where = append(where, "timestamp <= ?")
		args = append(args, *req.EndTime)
	}
	appendLogServiceBucket(&where, &args, req.Service)
	if req.Level != "" {
		where = append(where, "level = ?")
		args = append(args, req.Level)
	}
	if req.Keyword != "" {
		where = append(where, "message LIKE ?")
		args = append(args, "%"+req.Keyword+"%")
	}
	if req.TraceID != "" {
		where = append(where, "trace_id = ?")
		args = append(args, req.TraceID)
	}

	whereClause := ""
	if len(where) > 0 {
		whereClause = " WHERE " + strings.Join(where, " AND ")
	}

	var total uint64
	countSQL := "SELECT count() FROM logs" + whereClause
	err := s.conn.QueryRow(context.Background(), countSQL, args...).Scan(&total)
	if err != nil {
		log.Printf("[ERROR] clickhouse count query failed: %v", err)
		return nil, fmt.Errorf("count query failed: %w", err)
	}

	page := req.Page
	if page < 1 {
		page = 1
	}
	pageSize := req.PageSize
	if pageSize < 1 || pageSize > 100 {
		pageSize = 50
	}
	offset := (page - 1) * pageSize

	querySQL := fmt.Sprintf(
		"SELECT timestamp, level, service, trace_id, thread, message, fields FROM logs%s ORDER BY timestamp DESC LIMIT %d OFFSET %d",
		whereClause, pageSize, offset,
	)

	rows, err := s.conn.Query(context.Background(), querySQL, args...)
	if err != nil {
		log.Printf("[ERROR] clickhouse data query failed: %v", err)
		return nil, fmt.Errorf("data query failed: %w", err)
	}
	defer rows.Close()

	var logs []model.LogEntry
	for rows.Next() {
		var e model.LogEntry
		rows.Scan(&e.Timestamp, &e.Level, &e.Service, &e.TraceID, &e.Thread, &e.Message, &e.Fields)
		logs = append(logs, e)
	}

	return &model.LogQueryResponse{
		Total: int64(total),
		Logs:  logs,
		Page:  page,
	}, nil
}

func (s *ClickHouseStore) ListServices() ([]string, error) {
	rows, err := s.conn.Query(context.Background(),
		"SELECT DISTINCT service FROM logs WHERE service != '' ORDER BY service")
	if err != nil {
		return nil, fmt.Errorf("list services failed: %w", err)
	}
	defer rows.Close()
	var services []string
	for rows.Next() {
		var svc string
		rows.Scan(&svc)
		services = append(services, svc)
	}
	return services, nil
}

func (s *ClickHouseStore) appendWAL(batch []model.LogEntry, reason error) {
	written, err := s.wal.AppendBatch(batch)
	if err != nil {
		if errors.Is(err, ErrWALLimitReached) {
			s.droppedCount.Add(int64(len(batch)))
			log.Printf("[ERROR] clickhouse flush failed and wal limit reached, dropped=%d reason=%v", len(batch), reason)
			return
		}
		s.droppedCount.Add(int64(len(batch)))
		log.Printf("[ERROR] clickhouse flush failed and wal append failed, dropped=%d reason=%v wal_err=%v", len(batch), reason, err)
		return
	}
	s.walPending.Add(int64(written))
	log.Printf("[WARN] clickhouse flush failed, appended wal entries=%d reason=%v", written, reason)
}

func (s *ClickHouseStore) recoverFromWAL() error {
	entries, err := s.wal.ReadAll()
	if err != nil {
		return err
	}
	if len(entries) == 0 {
		return nil
	}

	if err := s.insertBatch(entries); err != nil {
		return fmt.Errorf("insert wal entries: %w", err)
	}
	if err := s.wal.Delete(); err != nil {
		return fmt.Errorf("delete wal file: %w", err)
	}
	s.walPending.Store(0)
	log.Printf("[INFO] recovered wal entries=%d", len(entries))
	return nil
}

func (s *ClickHouseStore) insertBatch(batch []model.LogEntry) error {
	b, err := s.conn.PrepareBatch(context.Background(), "INSERT INTO logs (timestamp, level, service, trace_id, thread, message, fields)")
	if err != nil {
		return err
	}
	for _, e := range batch {
		b.Append(e.Timestamp, e.Level, e.Service, e.TraceID, e.Thread, e.Message, e.Fields)
	}
	return b.Send()
}

func (s *ClickHouseStore) channelMonitorLoop() {
	ticker := time.NewTicker(1 * time.Second)
	defer ticker.Stop()

	for range ticker.C {
		capacity := cap(s.ch)
		if capacity == 0 {
			s.channelUsage.Store(0)
			continue
		}
		usagePct := float64(len(s.ch)) / float64(capacity) * 100
		usageInt := int64(usagePct)
		s.channelUsage.Store(usageInt)
		if usagePct >= 80 {
			now := time.Now().UnixNano()
			last := atomic.LoadInt64(&s.lastWarnAt)
			if now-last >= s.warnInterval.Nanoseconds() && atomic.CompareAndSwapInt64(&s.lastWarnAt, last, now) {
				log.Printf("[WARN] channel usage high usage_pct=%.2f len=%d cap=%d", usagePct, len(s.ch), capacity)
			}
		}
	}
}

type WriteMetrics struct {
	WriteSuccessTotal int64 `json:"write_success_total"`
	WriteFailTotal    int64 `json:"write_fail_total"`
	WALPending        int64 `json:"wal_pending"`
	ChannelUsagePct   int64 `json:"channel_usage_pct"`
	DroppedTotal      int64 `json:"dropped_count"`
}

func (s *ClickHouseStore) Metrics() WriteMetrics {
	return WriteMetrics{
		WriteSuccessTotal: s.writeSuccess.Load(),
		WriteFailTotal:    s.writeFail.Load(),
		WALPending:        s.walPending.Load(),
		ChannelUsagePct:   s.channelUsage.Load(),
		DroppedTotal:      s.droppedCount.Load(),
	}
}

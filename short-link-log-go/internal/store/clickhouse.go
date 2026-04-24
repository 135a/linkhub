package store

import (
	"context"
	"fmt"
	"log"
	"shortlink-log-go/internal/model"
	"strings"
	"time"

	clickhouse "github.com/ClickHouse/clickhouse-go/v2"
	"github.com/ClickHouse/clickhouse-go/v2/lib/driver"
)

type ClickHouseStore struct {
	conn driver.Conn
	ch   chan model.LogEntry
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
		conn: conn,
		ch:   make(chan model.LogEntry, 10000),
	}
	go s.batchLoop()
	return s, nil
}

func (s *ClickHouseStore) Ping() error {
	return s.conn.Ping(context.Background())
}

func (s *ClickHouseStore) Buffer(entry model.LogEntry) {
	select {
	case s.ch <- entry:
	default:
		// buffer full, drop silently to avoid blocking hot path
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
	b, err := s.conn.PrepareBatch(context.Background(), "INSERT INTO logs (timestamp, level, service, trace_id, thread, message, fields)")
	if err != nil {
		log.Printf("[WARN] clickhouse prepare batch failed: %v", err)
		return
	}
	for _, e := range batch {
		b.Append(e.Timestamp, e.Level, e.Service, e.TraceID, e.Thread, e.Message, e.Fields)
	}
	if err := b.Send(); err != nil {
		log.Printf("[WARN] clickhouse batch send failed: %v", err)
	}
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


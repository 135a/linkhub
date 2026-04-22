package store

import (
	"context"
	"fmt"
	"shortlink-log-go/internal/model"
	"strings"
	"sync"
	"time"

	clickhouse "github.com/ClickHouse/clickhouse-go/v2"
	"github.com/ClickHouse/clickhouse-go/v2/lib/driver"
)

type ClickHouseStore struct {
	conn  driver.Conn
	mu    sync.Mutex
	buf   []model.LogEntry
	ch    chan struct{}
	maxBuf int
}

func NewClickHouseStore(host, port string) (*ClickHouseStore, error) {
	conn, err := clickhouse.Open(&clickhouse.Options{
		Addr: []string{fmt.Sprintf("%s:%s", host, port)},
		Auth: clickhouse.Auth{
			Database: "default",
			Username: "default",
			Password: "",
		},
	})
	if err != nil {
		return nil, fmt.Errorf("clickhouse connect: %w", err)
	}

	s := &ClickHouseStore{
		conn:   conn,
		buf:    make([]model.LogEntry, 0, 1000),
		ch:     make(chan struct{}, 1),
		maxBuf: 1000,
	}
	go s.flushLoop()
	return s, nil
}

func (s *ClickHouseStore) Ping() error {
	return s.conn.Ping(context.Background())
}

func (s *ClickHouseStore) Buffer(entry model.LogEntry) {
	s.mu.Lock()
	s.buf = append(s.buf, entry)
	s.mu.Unlock()

	if len(s.buf) >= s.maxBuf {
		s.Flush()
	}
}

func (s *ClickHouseStore) Flush() {
	s.mu.Lock()
	if len(s.buf) == 0 {
		s.mu.Unlock()
		return
	}
	batch := s.buf
	s.buf = make([]model.LogEntry, 0, s.maxBuf)
	s.mu.Unlock()

	go func() {
		batch, err := s.conn.PrepareBatch(context.Background(), "INSERT INTO logs (timestamp, level, service, trace_id, thread, message, fields)")
		if err != nil {
			return
		}
		for _, e := range batch {
			batch.Append(e.Timestamp, e.Level, e.Service, e.TraceID, e.Thread, e.Message, e.Fields)
		}
		batch.Send()
	}()
}

func (s *ClickHouseStore) flushLoop() {
	ticker := time.NewTicker(2 * time.Second)
	defer ticker.Stop()
	for range ticker.C {
		s.Flush()
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
	if req.Service != "" {
		where = append(where, "service = ?")
		args = append(args, req.Service)
	}
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

	// Count
	var total int64
	countSQL := "SELECT count() FROM logs" + whereClause
	s.conn.QueryRow(context.Background(), countSQL, args...).Scan(&total)

	// Pagination
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
		return nil, err
	}
	defer rows.Close()

	var logs []model.LogEntry
	for rows.Next() {
		var e model.LogEntry
		rows.Scan(&e.Timestamp, &e.Level, &e.Service, &e.TraceID, &e.Thread, &e.Message, &e.Fields)
		logs = append(logs, e)
	}

	return &model.LogQueryResponse{
		Total: total,
		Logs:  logs,
		Page:  page,
	}, nil
}

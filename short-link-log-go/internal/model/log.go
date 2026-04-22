package model

import "time"

type LogEntry struct {
	Timestamp time.Time `json:"timestamp" ch:"timestamp"`
	Level     string    `json:"level" ch:"level"`
	Service   string    `json:"service" ch:"service"`
	TraceID   string    `json:"trace_id" ch:"trace_id"`
	Thread    string    `json:"thread" ch:"thread"`
	Message   string    `json:"message" ch:"message"`
	Fields    string    `json:"fields" ch:"fields"`
}

type LogQueryRequest struct {
	StartTime  *time.Time `form:"start_time"`
	EndTime    *time.Time `form:"end_time"`
	Service    string     `form:"service"`
	Level      string     `form:"level"`
	Keyword    string     `form:"keyword"`
	TraceID    string     `form:"trace_id"`
	Page       int        `form:"page"`
	PageSize   int        `form:"page_size"`
}

type LogQueryResponse struct {
	Total int64      `json:"total"`
	Logs  []LogEntry `json:"logs"`
	Page  int        `json:"page"`
}

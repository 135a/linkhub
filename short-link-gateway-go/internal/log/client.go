package log

import (
	"bytes"
	"encoding/json"
	"log"
	"net/http"
	"shortlink-gateway-go/internal/config"
	"time"
)

type LogEntry struct {
	Level     string    `json:"level"`
	Timestamp time.Time `json:"timestamp"`
	Message   string    `json:"message"`
	Service   string    `json:"service"`
	TraceID   string    `json:"trace_id"`
	Method    string    `json:"method"`
	Path      string    `json:"path"`
	Status    int       `json:"status"`
	Latency   int64     `json:"latency"`
}

type LogClient struct {
	endpoint string
	service  string
}

func NewLogClient(cfg *config.Config) *LogClient {
	return &LogClient{
		endpoint: cfg.LogCollector.Endpoint,
		service:  "gateway",
	}
}

func (c *LogClient) Send(entry LogEntry) {
	if c.endpoint == "" {
		return
	}

	entries := []LogEntry{entry}
	data, err := json.Marshal(entries)
	if err != nil {
		log.Printf("failed to marshal log entry: %v", err)
		return
	}

	resp, err := http.Post(c.endpoint, "application/json", bytes.NewBuffer(data))
	if err != nil {
		log.Printf("failed to send log to collector: %v", err)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusAccepted {
		log.Printf("log collector returned non-202 status: %d", resp.StatusCode)
	}
}

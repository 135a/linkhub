package handler

import (
	"net/http"
	"shortlink-log-go/internal/model"
	"shortlink-log-go/internal/service"

	"github.com/gin-gonic/gin"
)

type LogHandler struct {
	svc *service.LogService
}

func NewLogHandler(svc *service.LogService) *LogHandler {
	return &LogHandler{svc: svc}
}

func (h *LogHandler) Ingest(c *gin.Context) {
	var entries []model.LogEntry
	if err := c.ShouldBindJSON(&entries); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid json"})
		return
	}
	if len(entries) == 0 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "empty log entries"})
		return
	}
	if len(entries) > 1000 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "too many entries, max 1000"})
		return
	}
	for _, e := range entries {
		if e.Level == "" || e.Timestamp.IsZero() || e.Message == "" {
			c.JSON(http.StatusBadRequest, gin.H{"error": "missing required fields: level, timestamp, message"})
			return
		}
	}
	h.svc.Ingest(entries)
	c.JSON(http.StatusAccepted, gin.H{"accepted": len(entries)})
}

func (h *LogHandler) Query(c *gin.Context) {
	var req model.LogQueryRequest
	if err := c.ShouldBindQuery(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "invalid query params"})
		return
	}
	resp, err := h.svc.Query(req)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "query failed"})
		return
	}
	c.JSON(http.StatusOK, resp)
}

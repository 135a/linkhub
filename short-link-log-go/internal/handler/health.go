package handler

import (
	"net/http"
	"shortlink-log-go/internal/store"

	"github.com/gin-gonic/gin"
)

type HealthHandler struct {
	chStore *store.ClickHouseStore
}

func NewHealthHandler(chStore *store.ClickHouseStore) *HealthHandler {
	return &HealthHandler{chStore: chStore}
}

func (h *HealthHandler) Health(c *gin.Context) {
	if err := h.chStore.Ping(); err != nil {
		c.JSON(http.StatusServiceUnavailable, gin.H{"status": "degraded", "clickhouse": "disconnected"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"status": "ok", "clickhouse": "connected"})
}

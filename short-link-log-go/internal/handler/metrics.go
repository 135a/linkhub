package handler

import (
	"net/http"
	"shortlink-log-go/internal/store"

	"github.com/gin-gonic/gin"
)

// MetricsHandler exposes internal write metrics for the log-collector service.
type MetricsHandler struct {
	chStore *store.ClickHouseStore
}

// NewMetricsHandler creates a MetricsHandler backed by the given ClickHouseStore.
func NewMetricsHandler(chStore *store.ClickHouseStore) *MetricsHandler {
	return &MetricsHandler{chStore: chStore}
}

// Metrics handles GET /metrics and returns real-time write statistics in JSON.
func (h *MetricsHandler) Metrics(c *gin.Context) {
	c.JSON(http.StatusOK, h.chStore.Metrics())
}

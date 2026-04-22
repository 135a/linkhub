package handler

import (
	"net/http"
	"shortlink-gateway-go/internal/metrics"

	"github.com/gin-gonic/gin"
)

type MetricsHandler struct {
	collector *metrics.MetricsCollector
}

func NewMetricsHandler(collector *metrics.MetricsCollector) *MetricsHandler {
	return &MetricsHandler{collector: collector}
}

func (h *MetricsHandler) GetMetrics(c *gin.Context) {
	clientIP := c.ClientIP()
	if clientIP != "127.0.0.1" && clientIP != "::1" && clientIP != "localhost" {
		c.JSON(http.StatusForbidden, gin.H{
			"error": "forbidden",
			"message": "metrics endpoint only accessible from localhost",
		})
		return
	}

	m := h.collector.GetMetrics()
	c.JSON(http.StatusOK, m)
}
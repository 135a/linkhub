package service

import (
	"shortlink-log-go/internal/model"
	"shortlink-log-go/internal/store"
)

type LogService struct {
	chStore *store.ClickHouseStore
}

func NewLogService(chStore *store.ClickHouseStore) *LogService {
	return &LogService{chStore: chStore}
}

func (s *LogService) Ingest(entries []model.LogEntry) {
	for _, e := range entries {
		s.chStore.Buffer(e)
	}
}

func (s *LogService) Query(req model.LogQueryRequest) (*model.LogQueryResponse, error) {
	return s.chStore.Query(req)
}

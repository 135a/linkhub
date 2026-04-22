-- ClickHouse logs table initialization
-- Partitioned by day, 30-day TTL

CREATE TABLE IF NOT EXISTS logs
(
    `timestamp`   DateTime64(3) CODEC(Delta, ZSTD(1)),
    `level`       LowCardinality(String) CODEC(ZSTD(1)),
    `service`     LowCardinality(String) CODEC(ZSTD(1)),
    `trace_id`    String CODEC(ZSTD(1)),
    `thread`      String CODEC(ZSTD(1)),
    `message`     String CODEC(ZSTD(3)),
    `fields`      String CODEC(ZSTD(3))
)
ENGINE = MergeTree()
PARTITION BY toYYYYMMDD(timestamp)
ORDER BY (service, level, timestamp)
TTL timestamp + INTERVAL 30 DAY
SETTINGS index_granularity = 8192;

## Why

Currently, the log observability system (`log-collector` and ClickHouse) only collects logs from our application services (`shortlink-admin`, `shortlink-project`, and `gateway`). Critical infrastructure components like MySQL, Redis, Nginx Proxy, and RocketMQ are missing from the centralized dashboard. Centralizing infrastructure logs is necessary for end-to-end troubleshooting and root-cause analysis.

## What Changes

- Add a lightweight log forwarder (Fluent Bit) to the Docker Compose stack.
- Configure Fluent Bit to receive logs from Docker containers and HTTP POST them to the `log-collector` API (`/api/v1/logs/ingest`).
- Update `docker-compose.yml` and `docker-compose.dev.yml` to change the `logging.driver` for `mysql`, `redis`, `nginx-proxy`, `rocketmq-broker`, and `rocketmq-namesrv` to `fluentd`.
- Map infrastructure logs directly without requiring a Trace ID (since they do not participate in HTTP request chains).

## Capabilities

### New Capabilities
- `infra-log-collection`: Infrastructure container log collection and forwarding to centralized log storage.

### Modified Capabilities
- (None)

## Impact

- **Docker Compose**: Minor changes to the infrastructure services to define their logging driver. Added a Fluent Bit service.
- **Log Collector**: No changes required. The existing `/api/v1/logs/ingest` endpoint accepts logs without strict Trace ID validation.
- **Storage**: ClickHouse `logs` table will receive higher write volume.

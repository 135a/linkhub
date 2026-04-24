## Context

The current log observability system (`log-collector` and ClickHouse) only collects logs from application services (`shortlink-admin`, `shortlink-project`, and `gateway`). Critical infrastructure components like MySQL, Redis, Nginx Proxy, and RocketMQ are not included in the centralized dashboard, which hinders end-to-end troubleshooting and root-cause analysis.

The existing log collection architecture uses a custom `log-collector` service that receives logs via HTTP POST to `/api/v1/logs/ingest`. This endpoint already accepts logs without strict Trace ID validation, making it suitable for infrastructure logs that don't participate in HTTP request chains.

## Goals / Non-Goals

**Goals:**
- Implement centralized collection of infrastructure logs from MySQL, Redis, Nginx Proxy, and RocketMQ
- Use a lightweight log forwarder (Fluent Bit) to collect Docker container logs
- Configure infrastructure services to send logs to the existing `log-collector` API
- Ensure logs are properly mapped and stored in ClickHouse for analysis
- Maintain compatibility with existing log collection infrastructure

**Non-Goals:**
- Modifying the `log-collector` service or its API
- Implementing log aggregation or analysis beyond what's already provided
- Adding Trace ID support to infrastructure logs
- Changing the existing log storage schema

## Decisions

### 1. Log Forwarder Selection

**Decision:** Use Fluent Bit as the log forwarder

**Rationale:**
- **Lightweight:** Fluent Bit has a small footprint (~45MB) compared to alternatives like Fluentd (~100MB+)
- **Docker Integration:** Built-in support for Docker logging driver
- **HTTP Output:** Native support for sending logs via HTTP POST
- **Performance:** High throughput with low resource consumption
- **Maturity:** Widely used in production environments

**Alternatives Considered:**
- **Fluentd:** More feature-rich but heavier resource footprint
- **Logstash:** More complex configuration and higher resource requirements
- **Filebeat:** Less flexible for our use case

### 2. Logging Driver Configuration

**Decision:** Use `fluentd` logging driver for infrastructure containers

**Rationale:**
- **Native Integration:** Docker's fluentd driver directly sends logs to Fluent Bit
- **Reliability:** Built-in buffering and retry mechanisms
- **Standardization:** Consistent approach across all infrastructure services

**Alternatives Considered:**
- **json-file + filebeat:** More complex setup requiring additional configuration
- **syslog:** Less flexible for structured logging

### 3. Log Mapping Strategy

**Decision:** Map infrastructure logs directly without Trace ID

**Rationale:**
- **Simplicity:** Infrastructure services don't participate in HTTP request chains
- **Compatibility:** Existing `log-collector` API already accepts logs without Trace ID
- **Clarity:** Infrastructure logs have their own context and don't require correlation with application requests

**Alternatives Considered:**
- **Generate synthetic Trace IDs:** Would add unnecessary complexity without practical benefit
- **Use service-specific identifiers:** Would require changes to the log schema

### 4. Deployment Approach

**Decision:** Add Fluent Bit as a service in Docker Compose

**Rationale:**
- **Simplified Deployment:** Integrated into existing Docker Compose stack
- **Scalability:** Can be scaled independently if needed
- **Isolation:** Runs as a separate service with its own resources

**Alternatives Considered:**
- **Host-level installation:** Less portable and harder to manage
- **Sidecar containers:** More complex and resource-intensive

## Risks / Trade-offs

**Risk:** Increased log volume may impact ClickHouse performance

**Mitigation:**
- Monitor ClickHouse resource utilization and adjust configuration as needed
- Consider implementing log rotation and retention policies
- Evaluate the possibility of log sampling for high-volume services

**Risk:** Fluent Bit failure could cause log loss

**Mitigation:**
- Configure Fluent Bit with appropriate buffering settings
- Set up health checks for the Fluent Bit service
- Consider implementing a secondary log storage mechanism as backup

**Risk:** Configuration errors could break existing log collection

**Mitigation:**
- Test changes in a development environment first
- Implement a rollback plan
- Maintain separate configuration files for different environments

**Trade-off:** Additional resource consumption by Fluent Bit

**Mitigation:**
- Configure Fluent Bit with minimal resource allocation
- Monitor resource usage and adjust as needed

## Migration Plan

### Deployment Steps

1. **Update Docker Compose Files:**
   - Add Fluent Bit service to `docker-compose.yml` and `docker-compose.dev.yml`
   - Update logging configuration for infrastructure services

2. **Configure Fluent Bit:**
   - Create Fluent Bit configuration file
   - Set up input for Docker logs
   - Configure output to `log-collector` API

3. **Testing:**
   - Deploy changes in development environment
   - Verify logs are being collected from all infrastructure services
   - Check log-collector and ClickHouse for proper processing

4. **Production Deployment:**
   - Deploy changes during maintenance window
   - Monitor system performance and log collection

### Rollback Strategy

1. **Revert Docker Compose Changes:**
   - Remove Fluent Bit service
   - Revert logging driver configuration for infrastructure services

2. **Restart Services:**
   - Restart affected infrastructure services
   - Verify application logs continue to be collected

## Open Questions

1. **Log Retention Policy:** What is the appropriate retention period for infrastructure logs?

2. **Log Filtering:** Should we implement any filtering for infrastructure logs to reduce volume?

3. **Alerting:** Should we add specific alerting rules for infrastructure log patterns?

4. **Performance Monitoring:** What are the expected performance impacts on the overall system?

5. **Scaling Considerations:** How will this solution scale as the system grows?
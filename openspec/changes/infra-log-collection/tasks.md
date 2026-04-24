## 1. Fluent Bit Configuration

- [x] 1.1 Create Fluent Bit configuration file with Docker input and HTTP output
- [x] 1.2 Configure Fluent Bit to send logs to log-collector API endpoint `/api/v1/logs/ingest`
- [x] 1.3 Set up appropriate buffering and retry settings for Fluent Bit

## 2. Docker Compose Updates

- [x] 2.1 Add Fluent Bit service to `docker-compose.yml`
- [x] 2.2 Add Fluent Bit service to `docker-compose.dev.yml`
- [x] 2.3 Update MySQL service in `docker-compose.yml` to use fluentd logging driver
- [x] 2.4 Update Redis service in `docker-compose.yml` to use fluentd logging driver
- [x] 2.5 Update Nginx Proxy service in `docker-compose.yml` to use fluentd logging driver
- [x] 2.6 Update RocketMQ Broker service in `docker-compose.yml` to use fluentd logging driver
- [x] 2.7 Update RocketMQ Namesrv service in `docker-compose.yml` to use fluentd logging driver
- [x] 2.8 Repeat logging driver updates for `docker-compose.dev.yml`

## 3. Testing and Verification

- [ ] 3.1 Start Docker Compose stack with new configuration
- [ ] 3.2 Verify Fluent Bit service is running correctly
- [ ] 3.3 Check that infrastructure services are sending logs to Fluent Bit
- [ ] 3.4 Verify log-collector is receiving and processing infrastructure logs
- [ ] 3.5 Confirm logs are being stored in ClickHouse
- [ ] 3.6 Test log collection for each infrastructure service (MySQL, Redis, Nginx, RocketMQ)

## 4. Deployment and Monitoring

- [ ] 4.1 Deploy changes to production environment
- [ ] 4.2 Monitor system performance and log collection
- [ ] 4.3 Verify all infrastructure logs are being collected properly
- [ ] 4.4 Document the new log collection setup

## 5. Rollback Preparation

- [ ] 5.1 Create rollback plan for Docker Compose configuration
- [ ] 5.2 Test rollback procedure in development environment
- [ ] 5.3 Document rollback steps for production deployment
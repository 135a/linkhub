## ADDED Requirements

### Requirement: Infrastructure logs collection
The system SHALL collect logs from MySQL, Redis, Nginx Proxy, and RocketMQ containers and forward them to the centralized log storage.

#### Scenario: MySQL logs collection
- **WHEN** MySQL container generates logs
- **THEN** Fluent Bit SHALL collect the logs and forward them to the log-collector API
- **THEN** log-collector SHALL process the logs and store them in ClickHouse

#### Scenario: Redis logs collection
- **WHEN** Redis container generates logs
- **THEN** Fluent Bit SHALL collect the logs and forward them to the log-collector API
- **THEN** log-collector SHALL process the logs and store them in ClickHouse

#### Scenario: Nginx Proxy logs collection
- **WHEN** Nginx Proxy container generates logs
- **THEN** Fluent Bit SHALL collect the logs and forward them to the log-collector API
- **THEN** log-collector SHALL process the logs and store them in ClickHouse

#### Scenario: RocketMQ logs collection
- **WHEN** RocketMQ Broker and Namesrv containers generate logs
- **THEN** Fluent Bit SHALL collect the logs and forward them to the log-collector API
- **THEN** log-collector SHALL process the logs and store them in ClickHouse

### Requirement: Fluent Bit configuration
The system SHALL configure Fluent Bit to receive logs from Docker containers and forward them to the log-collector API.

#### Scenario: Fluent Bit startup
- **WHEN** Fluent Bit service starts
- **THEN** it SHALL load the configuration file
- **THEN** it SHALL listen for logs from Docker containers
- **THEN** it SHALL establish connection to log-collector API

#### Scenario: Log forwarding
- **WHEN** Fluent Bit receives logs from Docker containers
- **THEN** it SHALL format the logs appropriately
- **THEN** it SHALL send the logs to the log-collector API endpoint `/api/v1/logs/ingest`

### Requirement: Docker Compose configuration
The system SHALL update Docker Compose files to use the fluentd logging driver for infrastructure services and add the Fluent Bit service.

#### Scenario: Docker Compose startup
- **WHEN** Docker Compose is started
- **THEN** it SHALL create the Fluent Bit service
- **THEN** it SHALL configure infrastructure services to use the fluentd logging driver
- **THEN** it SHALL establish communication between infrastructure services and Fluent Bit

#### Scenario: Log driver configuration
- **WHEN** infrastructure services start
- **THEN** they SHALL send logs to the fluentd logging driver
- **THEN** the fluentd driver SHALL forward logs to Fluent Bit

### Requirement: Log mapping
The system SHALL map infrastructure logs directly without requiring a Trace ID.

#### Scenario: Log processing without Trace ID
- **WHEN** log-collector receives infrastructure logs without Trace ID
- **THEN** it SHALL accept and process the logs
- **THEN** it SHALL store the logs in ClickHouse

#### Scenario: Log storage
- **WHEN** log-collector processes infrastructure logs
- **THEN** it SHALL store them in the ClickHouse `logs` table
- **THEN** the logs SHALL be available for querying and analysis
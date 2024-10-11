# SS7 Client Module

The `ss7-client-module` is a crucial component within the Short Message Service Center (SMSC) environment, specifically designed for handling SS7 (Signaling System No. 7) communications. This module facilitates the signaling and transport of messages between the SMSC and telecommunication networks, ensuring efficient and reliable message delivery.

## Key Responsibilities
- **SS7 Signaling**: Manages SS7 signaling messages for SMS delivery, providing connectivity and communication with telecommunication networks (MNO/MVNO).
- **Message Processing**: Consumes incoming SS7 messages and processes them according to defined routing and handling rules.
- **WebSocket Communication**: Enables real-time communication via WebSocket for status updates or control messages.
- **Concurrency Management**: Utilizes a configurable thread pool to handle high volumes of SS7 messages concurrently.
- **JMX Monitoring**: Provides JMX capabilities for monitoring performance and operational metrics of the SS7 client module.

## Key Configurable Variables

### JVM Settings
- **`JVM_XMS`**: Sets the initial heap size for the JVM (default: `-Xms512m`).
- **`JVM_XMX`**: Sets the maximum heap size for the JVM (default: `-Xmx1024m`).

### Server Settings
- **`SERVER_PORT`**: Port on which the SS7 client module runs (default: `9999`).

### Redis Cluster Settings
- **`CLUSTER_NODES`**: Specifies the list of Redis nodes in the cluster (e.g., `localhost:7000,localhost:7001,...,localhost:7009`).

### Thread Pool Settings
- **`THREAD_POOL_MAX_TOTAL`**: Maximum number of threads available for the service (default: `60`).
- **`THREAD_POOL_MAX_IDLE`**: Maximum number of idle threads (default: `50`).
- **`THREAD_POOL_MIN_IDLE`**: Minimum number of idle threads (default: `10`).
- **`THREAD_POOL_BLOCK_WHEN_EXHAUSTED`**: Blocks when no threads are available (default: `true`).

### SS7 Configuration
- **`SS7_KEY_GATEWAYS`**: Redis key for storing SS7 gateway configurations (default: `ss7_gateways`).
- **`SS7_KEY_ERROR_CODE_MAPPING`**: Redis key for mapping SS7 error codes (default: `error_code_mapping`).

### Message Consumer Settings
- **`SS7_WORKER_PER_GATEWAY`**: Number of worker threads per SS7 gateway (default: `10`).
- **`SS7_GATEWAY_WORK_EXECUTE`**: Interval in milliseconds for executing work at the gateway (default: `1000`).
- **`SS7_TPS_PER_GW`**: Transactions per second allowed per gateway (default: `10000`).

### WebSocket Settings
- **`WEBSOCKET_SERVER_ENABLED`**: Enables the WebSocket server (default: `true`).
- **`WEBSOCKET_SERVER_HOST`**: Host IP for the WebSocket server (default: `{WEBSOCKET_SERVER_HOST}`).
- **`WEBSOCKET_SERVER_PORT`**: Port for WebSocket communication (default: `9087`).
- **`WEBSOCKET_SERVER_PATH`**: Path for WebSocket communication (default: `/ws`).
- **`WEBSOCKET_SERVER_RETRY_INTERVAL`**: Retry interval for WebSocket reconnections (default: `10` seconds).
- **`WEBSOCKET_HEADER_NAME`**: WebSocket header used for authentication (default: `Authorization`).
- **`WEBSOCKET_HEADER_VALUE`**: Authorization token for WebSocket communication.

### Configuration Directory
- **`SS7_CONFIG_DIRECTORY`**: Directory path for SS7 configuration files (default: `/opt/paic/ss7_module/conf/`).

### Virtual Threads
- **`THREADS_VIRTUAL_ENABLED`**: Enables virtual threads for concurrency optimization (default: `true`).

### Redis Lists
- **`PRE_MESSAGE_LIST`**: Redis list for pre-message processing (default: `preMessage`).
- **`RETRY_MESSAGES_QUEUE`**: Redis queue for retrying undelivered messages (default: `sms_retry`).

### JMX Monitoring
- **`ENABLE_JMX`**: Enables JMX for service monitoring and management (default: `true`).
- **`IP_JMX`**: IP address for JMX communication (default: `127.0.0.1`).
- **`JMX_PORT`**: Port for JMX access (default: `9010`).

## Docker Compose Example

```yaml
version: '3.8'

services:
  ss7-client-module:
    image: paic/ss7-module:latest
    ulimits:
      nofile:
        soft: 1000000
        hard: 1000000
    environment:
      JVM_XMS: "-Xms512m"
      JVM_XMX: "-Xmx1024m"
      SERVER_PORT: "9999"
      CLUSTER_NODES: "localhost:7000,localhost:7001,localhost:7002,localhost:7003,localhost:7004,localhost:7005,localhost:7006,localhost:7007,localhost:7008,localhost:7009"
      THREAD_POOL_MAX_TOTAL: 60
      THREAD_POOL_MAX_IDLE: 50
      THREAD_POOL_MIN_IDLE: 10
      THREAD_POOL_BLOCK_WHEN_EXHAUSTED: true
      SS7_KEY_GATEWAYS: "ss7_gateways"
      SS7_KEY_ERROR_CODE_MAPPING: "error_code_mapping"
      SS7_WORKER_PER_GATEWAY: 10
      SS7_GATEWAY_WORK_EXECUTE: 1000
      SS7_TPS_PER_GW: 10000
      WEBSOCKET_SERVER_ENABLED: true
      WEBSOCKET_SERVER_HOST: "{WEBSOCKET_SERVER_HOST}"
      WEBSOCKET_SERVER_PORT: 9087
      WEBSOCKET_SERVER_PATH: "/ws"
      WEBSOCKET_SERVER_RETRY_INTERVAL: 10
      WEBSOCKET_HEADER_NAME: "Authorization"
      WEBSOCKET_HEADER_VALUE: "{WEBSOCKET_HEADER_VALUE}"
      SS7_CONFIG_DIRECTORY: "/opt/paic/ss7_module/conf/"
      THREADS_VIRTUAL_ENABLED: true
      PRE_MESSAGE_LIST: "preMessage"
      RETRY_MESSAGES_QUEUE: "sms_retry"
      ENABLE_JMX: "true"
      IP_JMX: "127.0.0.1"
      JMX_PORT: "9010"
    volumes:
      - /opt/paic/smsc-docker/ss7/ss7-client-module-docker/resources/conf/logback.xml:/opt/paic/SS7_MODULE/conf/logback.xml
      - /opt/paic/smsc-docker/ss7/ss7-client-module-docker/resources/conf/log4j.xml:/opt/paic/SS7_MODULE/conf/log4j.xml
    network_mode: host

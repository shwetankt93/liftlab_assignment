# Real-Time Analytics Platform

A real-time analytics service that processes user events and provides insights through a modern dashboard. Built with Spring Boot WebFlux, React, Redis, and Kafka.

## Table of Contents

- [Quick Startup Steps](#quick-startup-steps)
- [Setup Instructions](#setup-instructions)
- [API Documentation](#api-documentation)
- [Architecture Overview](#architecture-overview)
- [Future Improvements](#future-improvements)

---

## Quick Startup Steps

Get the Real-time Analytics Platform running in minutes!

### Prerequisites

- **Docker** (version 20.10+)
- **Docker Compose** (version 2.0+)
- **Available Ports**: 3000, 8080, 6379, 9092, 2181

### Step 1: Clone and Navigate

```bash
cd liftlab_assignment
```

### Step 2: Build and Start All Services

```bash
docker-compose up -d --build
```

This command will:
- Build backend and frontend Docker images
- Start Zookeeper, Kafka, Redis, Backend, and Frontend services
- Set up networking between containers

### Step 3: Verify Services Are Running

```bash
# Check all services status
docker-compose ps

# Expected output: All services should show "Up" status
```

### Step 4: Access the Application

- **Frontend Dashboard**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health

### Step 5: Test the System

```bash
# Send a test event
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "2024-03-15T14:30:00Z",
    "user_id": "usr_789",
    "event_type": "page_view",
    "page_url": "/products/electronics",
    "session_id": "sess_456"
  }'

# Check metrics
curl http://localhost:8080/api/metrics
```

### Assumptions and Important Notes
```
1. Timestamp passed in event is considered to capture the metrics data. Please make sure that the latest timestamp is passed in the events. 

2. Definition of active Session: If there is no event from a session in last 5 minutes, that session is considered as inactive.

```

### Stop Services

```bash
docker-compose down
```

To remove volumes (Redis/Kafka data):

```bash
docker-compose down -v
```

---

## Setup Instructions

### Development Setup

#### Backend Setup

**Prerequisites:**
- Java 11 or higher
- Maven 3.6+
- Docker (for Redis and Kafka)

**Local Development:**

```bash
cd realtime_analytic_backend

# Install dependencies and build
mvn clean install

# Run application (requires Redis and Kafka running)
mvn spring-boot:run
```

**Configuration:**
- Default configuration: `src/main/resources/application.yml`
- Override with environment variables (see Environment Variables section)

#### Frontend Setup

**Prerequisites:**
- Node.js 16+ and npm

**Local Development:**

```bash
cd realtime_analytic_frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will be available at `http://localhost:5173`

**Configuration:**
- API URL can be configured via environment variable:
  ```bash
  VITE_API_BASE_URL=http://localhost:8080/api
  ```

### Docker Setup

The application uses Docker Compose to orchestrate all services:

**Services:**
1. **Zookeeper** (Port 2181) - Coordination service for Kafka
2. **Kafka** (Port 9092) - Message broker for async event ingestion
3. **Redis** (Port 6379) - In-memory store for real-time metrics
4. **Backend** (Port 8080) - Spring Boot WebFlux API
5. **Frontend** (Port 3000) - React dashboard served via Nginx

### Environment Variables

#### Backend Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `REDIS_HOST` | `redis` | Redis server hostname |
| `REDIS_PORT` | `6379` | Redis server port |
| `KAFKA_BOOTSTRAP_SERVERS` | `kafka:29092` | Kafka bootstrap servers (internal Docker network) |
| `KAFKA_CONSUMER_GROUP_ID` | `analytics-consumer-group` | Kafka consumer group ID |
| `SERVER_PORT` | `8080` | Server port number |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,...` | Comma-separated list of allowed origins |
| `RATE_LIMIT_PER_SECOND` | `100` | Maximum number of requests per second |
| `JAVA_OPTS` | `-Xms512m -Xmx1024m...` | JVM options |

**Kafka Connection:**
- **From Docker containers**: `kafka:29092`
- **From host machine**: `localhost:9092`

#### Frontend Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | Backend API base URL |


### Mock Data Generator

A Python script can be used to generate mock events for testing:

```bash
# Install dependencies
pip install kafka-python requests

# Run generator (100 events/second)
python mock_data_generator.py

# Or with custom rate
python mock_data_generator.py --rate 50
```


### Rate Limiting

- **Limit**: 100 requests per second (configurable)
- **Window**: 1 second
- **Timeout**: 100ms

When rate limit is exceeded, the API returns HTTP 429 (Too Many Requests).

---

### API Documentation

#### 1. Ingest Event

**POST** `/api/events`

Ingest a user analytics event into the system. Events can be processed via REST API or Kafka consumer.

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "timestamp": "2024-03-15T14:30:00Z",
  "user_id": "usr_789",
  "event_type": "page_view",
  "page_url": "/products/electronics",
  "session_id": "sess_456"
}
```

**Field Descriptions:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `timestamp` | String (ISO-8601) | Yes | Event timestamp in UTC (e.g., "2024-03-15T14:30:00Z") |
| `user_id` | String | Yes | Unique identifier for the user |
| `event_type` | String | Yes | Type of event (e.g., "page_view") |
| `page_url` | String | Yes | URL of the page (e.g., "/products/electronics") |
| `session_id` | String | Yes | Unique identifier for the user session |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "message": "Event processed successfully",
  "processedAt": "2024-03-15T14:30:01.234Z"
}
```

**Error Response:** `200 OK` (with success: false)
```json
{
  "success": false,
  "message": "Validation error: User ID is required",
  "processedAt": "2024-03-15T14:30:01.234Z"
}
```

**Rate Limited Response:** `429 Too Many Requests`
```json
{
  "timestamp": "2024-03-15T14:30:01.234Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded"
}
```

**Example using cURL:**
```bash
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "timestamp": "2024-03-15T14:30:00Z",
    "user_id": "usr_123",
    "event_type": "page_view",
    "page_url": "/home",
    "session_id": "sess_456"
  }'
```

**Example using Kafka:**
```bash
# Publish to Kafka topic
docker exec -it kafka kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic analytics-events

# Then paste JSON event
{"timestamp":"2024-03-15T14:30:00Z","user_id":"usr_123","event_type":"page_view","page_url":"/home","session_id":"sess_456"}
```

---

#### 2. Get Metrics

**GET** `/api/metrics`

Retrieve current real-time analytics metrics. Returns active users, top pages, and active sessions per user.

**Success Response:** `200 OK`
```json
{
  "activeUsersCount": 42,
  "topPages": [
    {
      "url": "/products/electronics",
      "viewCount": 150
    },
    {
      "url": "/home",
      "viewCount": 89
    },
    {
      "url": "/products/books",
      "viewCount": 67
    },
    {
      "url": "/cart",
      "viewCount": 45
    },
    {
      "url": "/checkout",
      "viewCount": 32
    }
  ],
  "activeSessionsByUser": {
    "usr_123": 3,
    "usr_456": 2,
    "usr_789": 1
  },
  "timestamp": "2024-03-15T14:30:01.234Z"
}
```

**Response Field Descriptions:**

| Field | Type | Description |
|-------|------|-------------|
| `activeUsersCount` | Integer | Number of unique users with events in the last 5 minutes |
| `topPages` | Array | Top 5 pages by view count (last 15 minutes), sorted descending |
| `topPages[].url` | String | Normalized page URL |
| `topPages[].viewCount` | Long | Number of page views |
| `activeSessionsByUser` | Object | Map of user IDs to their active session counts (last 5 minutes) |
| `timestamp` | String (ISO-8601) | Timestamp when metrics were calculated |

**Metrics Time Windows:**
- **Active Users**: Last 5 minutes
- **Top Pages**: Last 15 minutes
- **Active Sessions**: Last 5 minutes

**Example using cURL:**
```bash
curl http://localhost:8080/api/metrics
```

**Example Response (No Data):**
```json
{
  "activeUsersCount": 0,
  "topPages": [],
  "activeSessionsByUser": {},
  "timestamp": "2024-03-15T14:30:01.234Z"
}
```

---

#### 3. Health Check

**GET** `/actuator/health`

Check the health status of the backend service.

**Success Response:** `200 OK`
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    },
    "rateLimiter": {
      "status": "UP"
    }
  }
}
```

---

### Error Responses

**Validation Error:** `400 Bad Request`
```json
{
  "timestamp": "2024-03-15T14:30:01.234Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "userId",
      "message": "User ID is required"
    }
  ]
}
```

**Internal Server Error:** `500 Internal Server Error`
```json
{
  "timestamp": "2024-03-15T14:30:01.234Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred"
}
```

---

## Architecture Overview

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Real-Time Analytics Platform                    │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────┐
│   Event Sources     │
│                     │
│  • Web Application  │
│  • Mobile App       │
│  • Mock Generator   │
└──────────┬──────────┘
           │
           │ HTTP POST /api/events
           │ (Rate Limited: 100 req/s)
           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Backend Service (Spring Boot WebFlux)              │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  EventController (REST API)                                  │   │
│  │  • Validates events                                          │   │
│  │  • Rate limiting (Resilience4j)                             │   │
│  └───────────────────┬──────────────────────────────────────────┘   │
│                      │                                                │
│  ┌───────────────────▼──────────────────────────────────────────┐   │
│  │  EventProcessingService                                      │   │
│  │  • Validation (Strategy Pattern)                            │   │
│  │  • URL Normalization                                         │   │
│  │  • Event Processing                                          │   │
│  └───────────────────┬──────────────────────────────────────────┘   │
│                      │                                                │
│  ┌───────────────────▼──────────────────────────────────────────┐   │
│  │  MetricsStorageService (Interface)                           │   │
│  │  • RedisMetricsStorageService (Implementation)               │   │
│  │  • Strategy Pattern - Extensible storage layer               │   │
│  └───────────────────┬──────────────────────────────────────────┘   │
└──────────────────────┼────────────────────────────────────────────────┘
                       │
                       │ Reactive Redis Operations
                       │ (Non-blocking, SCAN for keys)
                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Redis (In-Memory Store)                       │
│                                                                       │
│  Data Structures:                                                    │
│  • Sorted Sets (ZSET) - Time-windowed metrics with timestamps       │
│  • Sets - Active user sessions                                      │
│  • Master Rankings - Top pages aggregation                          │
│                                                                       │
│  Metrics Stored:                                                     │
│  • Active Users (5 min window)                                      │
│  • Page Views by URL (15 min window)                                │
│  • Active Sessions per User (5 min window)                          │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────┐
│   Kafka Consumer    │
│  (Async Ingestion)  │
└──────────┬──────────┘
           │
           │ Consumes from 'analytics-events' topic
           │
           └───────────► EventProcessingService (Same flow as REST)
                       │
                       ▼

┌─────────────────────────────────────────────────────────────────────┐
│                    Metrics Service (On-Demand)                        │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  MetricsController                                           │   │
│  │  GET /api/metrics                                            │   │
│  └───────────────────┬──────────────────────────────────────────┘   │
│                      │                                                │
│  ┌───────────────────▼──────────────────────────────────────────┐   │
│  │  MetricsService                                               │   │
│  │  • Triggers cleanup (lazy, on metrics retrieval)            │   │
│  │  • Coordinates metric calculations                           │   │
│  └───────────────────┬──────────────────────────────────────────┘   │
│                      │                                                │
│  ┌───────────────────▼──────────────────────────────────────────┐   │
│  │  MetricsCollector                                            │   │
│  │  • ActiveUsersMetric (5 min)                                │   │
│  │  • TopPagesMetric (15 min)                                  │   │
│  │  • ActiveSessionsMetric (5 min)                             │   │
│  │  • IMetric interface (Strategy Pattern)                     │   │
│  └──────────────────────────────────────────────────────────────┘   │
└──────────────────────┬────────────────────────────────────────────────┘
                       │
                       │ HTTP GET /api/metrics
                       │ (Polled every 30 seconds)
                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   Frontend Dashboard (React)                          │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Components:                                                  │   │
│  │  • ActiveUsers - Displays active user count                  │   │
│  │  • TopPages - Bar chart (Chart.js)                           │   │
│  │  • ActiveSessions - Table of sessions per user               │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                       │
│  • Auto-refresh: 30 seconds                                          │
│  • Responsive design (TailwindCSS)                                  │
│  • Real-time updates                                                 │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                      Infrastructure (Docker)                          │
│                                                                       │
│  • Zookeeper (Port 2181) - Kafka coordination                        │
│  • Kafka (Port 9092 external, 29092 internal) - Message broker       │
│  • Redis (Port 6379) - Metrics storage                               │
│  • Backend (Port 8080) - Spring Boot application                     │
│  • Frontend (Port 3000) - React app (Nginx)                          │
└─────────────────────────────────────────────────────────────────────┘
```

### Component Details

#### 1. Event Ingestion Layer
- **REST API**: Synchronous event ingestion via WebFlux endpoint
- **Kafka Consumer**: Asynchronous event ingestion from Kafka topic
- **Rate Limiting**: Configurable global rate limit (default: 100 req/s)
- **Validation**: Strategy pattern with multiple validators (UserId, SessionId, Timestamp, PageUrl)

#### 2. Event Processing Layer
- **Validation Manager**: Orchestrates validation using Strategy pattern
- **URL Normalizer**: Normalizes URLs for consistent metric aggregation
- **Event Processing Service**: Processes validated events and stores metrics

#### 3. Storage Layer
- **Strategy Pattern**: `MetricsStorageService` interface allows swapping storage implementations
- **Redis Implementation**: Uses reactive Redis operations
  - Sorted Sets (ZSET) for time-windowed data
  - Sets for tracking active users and sessions
  - Non-blocking SCAN for key iteration
- **Cleanup Strategy**: Lazy cleanup on metrics retrieval (not on every event)

#### 4. Metrics Calculation Layer
- **Metrics Interface**: `IMetric` interface for extensibility
- **Metrics Collector**: Coordinates calculation of all metrics
- **Time Windows**: Configurable windows (currently hardcoded: 5min, 15min)

#### 5. API Layer
- **Reactive**: Built on Spring WebFlux for non-blocking I/O
- **Error Handling**: Global exception handler with proper error responses
- **CORS**: Configurable CORS support for frontend

#### 6. Frontend Layer
- **React**: Modern React with hooks
- **Auto-refresh**: Pull-based mechanism (30-second intervals)
- **Visualization**: Chart.js for data visualization

### Data Flow

1. **Event Ingestion**:
   - Event arrives via REST API or Kafka
   - Validated by ValidationManager
   - Processed by EventProcessingService
   - Metrics stored in Redis (non-blocking)

2. **Metrics Retrieval**:
   - Frontend polls `/api/metrics` every 30 seconds
   - MetricsService triggers cleanup (removes expired data)
   - MetricsCollector calculates all metrics
   - Results returned to frontend

3. **Cleanup Strategy**:
   - Cleanup happens **only** when metrics are retrieved
   - Separates concerns: storing vs. serving metrics
   - Prevents blocking event processing with cleanup operations

### Design Patterns Used

1. **Strategy Pattern**: 
   - Storage layer (`MetricsStorageService` interface)
   - Validation layer (`IValidation` interface)
   - Metrics calculation (`IMetric` interface)

2. **Reactive Programming**: 
   - Full reactive stack (WebFlux, Reactive Redis)
   - Non-blocking I/O throughout

3. **Separation of Concerns**: 
   - Clean separation between ingestion, storage, and serving
   - Cleanup happens separately from event processing

---

## Future Improvements

### Improvements

1. **Configurable Time Windows**
   - Make metric time windows configurable via application properties
   - Currently hardcoded (5 min, 15 min)
   - Add environment variables for easy configuration

2. **Enhanced Rate Limiting**
   - Per-user rate limiting in addition to global rate limiting
   - Dynamic rate limit adjustment based on system load
   - Rate limit metrics and monitoring

4. **Improved Error Handling**
   - Retry mechanism for Kafka consumers
   - Dead letter queue (DLQ) for failed events
   - Better error messages and error codes


5. **Data Persistence**
   - Store historical metrics in a time-series database
   - Long-term retention for trend analysis
   - Data archival strategy
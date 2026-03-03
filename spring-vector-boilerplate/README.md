# Spring Vector Boilerplate

Sample demo project showcasing **IoT** and **Vector Search** use cases with Spring Boot and MongoDB.

📐 [Architecture diagram & flows](docs/ARCHITECTURE.md)

## Prerequisites

- Java 21+
- MongoDB Atlas cluster or local MongoDB
- [Model API key](https://www.mongodb.com/docs/voyageai/management/api-keys/) for Vector Search (Voyage AI via Atlas API)

## Setup

1. Copy the environment file and fill in your values:

   ```bash
   cp .env.example .env
   ```

2. Edit `.env` with your MongoDB connection string and Atlas API key.

3. Build and run:

   ```bash
   mvn spring-boot:run
   ```

4. Open the UI at [http://localhost:8080](http://localhost:8080)

## Use Cases

### 1. IoT – Time Series Sensor Readings

- **GET** `/api/iot/readings/{deviceId}` – get readings by device (optional `start`/`end` query params)
- **GET** `/api/iot/latest-readings?limit=10` – Last 10 readings (for live dashboard)
- **GET** `/api/iot/stats` – collection stats (documents, buckets, storage size, index size)

Example:

```bash
# Start 5-minute ingestion (1000 readings spread over 5 minutes)
curl -X POST http://localhost:8080/api/iot/readings/ingest-demo

curl "http://localhost:8080/api/iot/readings/sensor-001"
```

### 2. Vector Search – Semantic & Hybrid Search

- **POST** `/api/vector/add` – add documents for semantic search
- **GET** `/api/vector/search?query=...&topK=5&mode=semantic` – semantic search (default)
- **GET** `/api/vector/search?query=...&topK=5&mode=hybrid&vectorWeight=0.5` – hybrid search (semantic + keyword, RRF fusion)

Example:

```bash
curl -X POST http://localhost:8080/api/vector/add \
  -H "Content-Type: application/json" \
  -d '{"documents": [{"content": "Spring Boot simplifies Java development", "metadata": {"topic": "java"}}]}'

curl "http://localhost:8080/api/vector/search?query=Java framework&topK=5"
```

## Environment Variables

All config is in `.env` (see `.env.example`). Shared for both use cases:

| Variable | Description |
|----------|-------------|
| `MONGODB_URI` | MongoDB connection string |
| `MONGODB_DATABASE` | Database name |
| `ATLAS_API_KEY` | Model API key (Voyage AI via Atlas API) |
| `VECTOR_COLLECTION` | Vector store collection name |
| `VECTOR_INDEX` | Vector search index name |
| `VECTOR_EMBEDDING_MODEL` | Model name (e.g. `voyage-4-large`) |

# .NET IoT + MongoDB Time Series Boilerplate

A .NET console application boilerplate demonstrating **MongoDB time series collections**, **aggregation pipelines**, and **simulated IoT sensor data**.

## Features

- **MongoDB Time Series Collection** – Creates and uses a time series collection optimized for sensor data
- **IoT Sensor Simulation** – Simulates multiple sensors (temperature, humidity, pressure) sending readings
- **Interactive CLI** – Run commands: generate data, read, query, stats
- **Aggregation Examples** – Three aggregation pipelines:
  1. **$group** – Average temperature/humidity per sensor
  2. **$dateToString + $group** – Readings grouped by time window (per minute)
  3. **$facet** – Multi-facet statistics (totals, per-sensor, global stats)

## Prerequisites

- [.NET 9 SDK](https://dotnet.microsoft.com/download)
- [MongoDB Atlas](https://www.mongodb.com/cloud/atlas) account (free tier available)

## Quick Start

1. **Create a MongoDB Atlas cluster** (free tier):
   - Sign up at [mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas)
   - Create a cluster (M0 free tier is sufficient)
   - Create a database user and get your connection string
   - Add your IP to the network access list 

2. **Configure the connection**:
   ```bash
   cp appsettings.example.json appsettings.json
   ```
   Edit `appsettings.json` and replace the placeholder with your Atlas connection string:
   ```json
   {
     "MongoDb": {
       "ConnectionString": "mongodb+srv://YOUR_USER:YOUR_PASSWORD@YOUR_CLUSTER.mongodb.net/?retryWrites=true&w=majority",
       "DatabaseName": "iot_db"
     }
   }
   ```

3. **Run the application** (interactive CLI):
   ```bash
   dotnet restore
   dotnet run
   ```

4. **Use the commands**:
   | Command | Description |
   |--------|-------------|
   | `init` | Ensure time series collection exists |
   | `generate [count] [interval]` | Generate sensor data (default: 20 batches, 30s interval) |
   | `read [limit]` | Read recent readings (default: 10) |
   | `query [type]` | Run aggregation: avg, time, stats, or all |
   | `stats` | Show document count |
   | `clear` | Drop collection (reset) |
   | `help` | Show commands |
   | `exit` | Exit |

## Project Structure

```
├── Models/
│   ├── SensorReading.cs    # IoT reading (timestamp, metadata, temperature, humidity, pressure)
│   └── SensorMetadata.cs   # Sensor identity (sensorId, type, location)
├── Services/
│   ├── SensorTimeSeriesService.cs  # Time series setup, inserts, aggregations
│   └── IotSimulator.cs             # Simulates sensors sending data
├── Program.cs
└── appsettings.json
```

## Time Series Collection

The boilerplate creates a time series collection with:

- **timeField**: `timestamp` – When the measurement was taken
- **metaField**: `metadata` – Sensor identity (sensorId, sensorType, location)
- **granularity**: `seconds` – Optimized for sub-minute readings

## Aggregation Pipelines

| Pipeline | Stages | Purpose |
|----------|--------|---------|
| Averages by sensor | `$group`, `$sort` | Per-sensor avg temp/humidity |
| Time window | `$match`, `$group` ($dateToString), `$sort`, `$limit` | Bucket readings by minute |
| Multi-facet stats | `$facet` | Total count, per-sensor, global min/max/avg in one pass |

## License

MIT

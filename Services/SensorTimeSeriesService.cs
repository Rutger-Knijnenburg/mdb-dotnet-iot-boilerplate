using Microsoft.Extensions.Configuration;
using MongoDB.Bson;
using MongoDB.Driver;
using DotNetMongoBoilerplate.Models;

namespace DotNetMongoBoilerplate.Services;

/// <summary>
/// Service for MongoDB time series operations with IoT sensor data.
/// Demonstrates: time series collection setup, inserts, and aggregation pipelines.
/// </summary>
public class SensorTimeSeriesService
{
    private const string CollectionName = "sensor_readings";
    private readonly IMongoDatabase _database;
    private readonly IMongoCollection<SensorReading> _collection;

    public SensorTimeSeriesService(IConfiguration configuration)
    {
        var connectionString = configuration["MongoDb:ConnectionString"]
            ?? throw new InvalidOperationException("MongoDb:ConnectionString is required. Copy appsettings.example.json to appsettings.json and add your Atlas connection string.");
        var databaseName = configuration["MongoDb:DatabaseName"] ?? "iot_db";

        var client = new MongoClient(connectionString);
        _database = client.GetDatabase(databaseName);
        _collection = _database.GetCollection<SensorReading>(CollectionName);
    }

    /// <summary>
    /// Ensures the time series collection exists. Creates it if not.
    /// Requires MongoDB 5.0+.
    /// </summary>
    public async Task EnsureTimeSeriesCollectionAsync(CancellationToken cancellationToken = default)
    {
        var collections = await _database.ListCollectionNamesAsync(cancellationToken: cancellationToken);
        var names = await collections.ToListAsync(cancellationToken);

        if (names.Contains(CollectionName))
        {
            Console.WriteLine($"  Time series collection '{CollectionName}' already exists.");
            return;
        }

        var timeSeriesOptions = new TimeSeriesOptions(
            timeField: "timestamp",
            metaField: "metadata",
            granularity: TimeSeriesGranularity.Seconds);

        var createOptions = new CreateCollectionOptions { TimeSeriesOptions = timeSeriesOptions };
        await _database.CreateCollectionAsync(CollectionName, createOptions, cancellationToken);

        Console.WriteLine($"  Created time series collection '{CollectionName}' (timeField: timestamp, metaField: metadata).");
    }

    /// <summary>
    /// Inserts sensor readings. Supports bulk insert for efficiency.
    /// </summary>
    public async Task InsertManyAsync(
        IEnumerable<SensorReading> readings,
        CancellationToken cancellationToken = default)
    {
        var list = readings.ToList();
        if (list.Count == 0) return;

        await _collection.InsertManyAsync(list, cancellationToken: cancellationToken);
    }

    /// <summary>
    /// Inserts a single sensor reading.
    /// </summary>
    public async Task InsertOneAsync(SensorReading reading, CancellationToken cancellationToken = default)
    {
        await _collection.InsertOneAsync(reading, cancellationToken: cancellationToken);
    }

    /// <summary>
    /// Aggregation 1: Average temperature and humidity per sensor.
    /// </summary>
    public async Task<List<BsonDocument>> GetAveragesBySensorAsync(CancellationToken cancellationToken = default)
    {
        var pipeline = new[]
        {
            BsonDocument.Parse("{ $group: { _id: '$metadata.sensorId', avgTemp: { $avg: '$temperature' }, avgHumidity: { $avg: '$humidity' }, count: { $sum: 1 } } }"),
            BsonDocument.Parse("{ $sort: { _id: 1 } }")
        };

        var pipelineDef = PipelineDefinition<SensorReading, BsonDocument>.Create(pipeline);
        return await _collection
            .Aggregate(pipelineDef, cancellationToken: cancellationToken)
            .ToListAsync(cancellationToken);
    }

    /// <summary>
    /// Aggregation 2: Readings grouped by time window (e.g. per minute).
    /// </summary>
    public async Task<List<BsonDocument>> GetReadingsByTimeWindowAsync(
        DateTime? from = null,
        DateTime? to = null,
        CancellationToken cancellationToken = default)
    {
        var stages = new List<BsonDocument>();

        if (from.HasValue || to.HasValue)
        {
            var timestampFilter = new BsonDocument();
            if (from.HasValue) timestampFilter["$gte"] = from.Value;
            if (to.HasValue) timestampFilter["$lte"] = to.Value;
            stages.Add(new BsonDocument("$match", new BsonDocument("timestamp", timestampFilter)));
        }

        stages.Add(BsonDocument.Parse(@"
            {
                $group: {
                    _id: {
                        $dateToString: { format: '%Y-%m-%d %H:%M', date: '$timestamp' }
                    },
                    avgTemp: { $avg: '$temperature' },
                    avgHumidity: { $avg: '$humidity' },
                    minTemp: { $min: '$temperature' },
                    maxTemp: { $max: '$temperature' },
                    count: { $sum: 1 }
                }
            }"));
        stages.Add(BsonDocument.Parse("{ $sort: { _id: 1 } }"));
        stages.Add(BsonDocument.Parse("{ $limit: 20 }"));

        var pipelineDef = PipelineDefinition<SensorReading, BsonDocument>.Create(stages);
        return await _collection
            .Aggregate(pipelineDef, cancellationToken: cancellationToken)
            .ToListAsync(cancellationToken);
    }

    /// <summary>
    /// Aggregation 3: Sensor statistics with $facet for multiple metrics in one pass.
    /// </summary>
    public async Task<BsonDocument?> GetSensorStatisticsAsync(CancellationToken cancellationToken = default)
    {
        var pipeline = new[]
        {
            BsonDocument.Parse(@"
                {
                    $facet: {
                        totalReadings: [{ $count: 'count' }],
                        bySensor: [
                            { $group: { _id: '$metadata.sensorId', count: { $sum: 1 }, avgTemp: { $avg: '$temperature' } } },
                            { $sort: { count: -1 } }
                        ],
                        globalStats: [
                            {
                                $group: {
                                    _id: null,
                                    avgTemp: { $avg: '$temperature' },
                                    avgHumidity: { $avg: '$humidity' },
                                    minTemp: { $min: '$temperature' },
                                    maxTemp: { $max: '$temperature' }
                                }
                            }
                        ]
                    }
                }")
        };

        var pipelineDef = PipelineDefinition<SensorReading, BsonDocument>.Create(pipeline);
        var result = await _collection
            .Aggregate(pipelineDef, cancellationToken: cancellationToken)
            .FirstOrDefaultAsync(cancellationToken);

        return result;
    }

    /// <summary>
    /// Returns the most recent sensor readings.
    /// </summary>
    public async Task<List<SensorReading>> GetRecentReadingsAsync(int limit = 10, CancellationToken cancellationToken = default)
    {
        return await _collection
            .Find(FilterDefinition<SensorReading>.Empty)
            .SortByDescending(r => r.Timestamp)
            .Limit(limit)
            .ToListAsync(cancellationToken);
    }

    /// <summary>
    /// Returns total document count in the collection.
    /// </summary>
    public async Task<long> GetCountAsync(CancellationToken cancellationToken = default)
    {
        return await _collection.CountDocumentsAsync(FilterDefinition<SensorReading>.Empty, cancellationToken: cancellationToken);
    }

    /// <summary>
    /// Drops the collection (useful for demo reset).
    /// </summary>
    public async Task DropCollectionAsync(CancellationToken cancellationToken = default)
    {
        await _collection.Database.DropCollectionAsync(CollectionName, cancellationToken);
    }
}

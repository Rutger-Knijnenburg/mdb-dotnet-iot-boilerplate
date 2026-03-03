using DotNetMongoBoilerplate.Models;

namespace DotNetMongoBoilerplate.Services;

/// <summary>
/// Simulates IoT sensors sending readings to MongoDB.
/// Generates realistic temperature, humidity, and pressure values.
/// </summary>
public class IotSimulator
{
    private readonly Random _random = Random.Shared;

    /// <summary>
    /// Sensor definitions for simulation.
    /// </summary>
    public static IReadOnlyList<SensorMetadata> DefaultSensors { get; } = new[]
    {
        new SensorMetadata { SensorId = "sensor-001", SensorType = "environment", Location = "warehouse-a" },
        new SensorMetadata { SensorId = "sensor-002", SensorType = "environment", Location = "warehouse-b" },
        new SensorMetadata { SensorId = "sensor-003", SensorType = "environment", Location = "server-room" },
        new SensorMetadata { SensorId = "sensor-004", SensorType = "outdoor", Location = "rooftop" },
        new SensorMetadata { SensorId = "sensor-005", SensorType = "environment", Location = "cold-storage" }
    };

    /// <summary>
    /// Generates a single random sensor reading.
    /// </summary>
    public SensorReading GenerateReading(SensorMetadata metadata, DateTime? timestamp = null)
    {
        var baseTemp = metadata.Location switch
        {
            "cold-storage" => 2.0,
            "server-room" => 22.0,
            "rooftop" => 15.0,
            _ => 20.0
        };

        var tempVariation = (_random.NextDouble() - 0.5) * 4;
        var humidityBase = 45 + _random.NextDouble() * 30;
        var pressureBase = 1013 + (_random.NextDouble() - 0.5) * 20;

        return new SensorReading
        {
            Timestamp = timestamp ?? DateTime.UtcNow,
            Metadata = metadata,
            Temperature = Math.Round(baseTemp + tempVariation, 2),
            Humidity = Math.Round(humidityBase, 2),
            Pressure = Math.Round(pressureBase, 1)
        };
    }

    /// <summary>
    /// Generates a batch of readings for all default sensors.
    /// </summary>
    public IReadOnlyList<SensorReading> GenerateBatch(DateTime? baseTime = null)
    {
        var time = baseTime ?? DateTime.UtcNow;
        var readings = new List<SensorReading>();

        foreach (var sensor in DefaultSensors)
        {
            readings.Add(GenerateReading(sensor, time));
        }

        return readings;
    }

    /// <summary>
    /// Generates multiple batches over a time range, simulating periodic sensor reports.
    /// </summary>
    /// <param name="count">Number of batches to generate.</param>
    /// <param name="intervalSeconds">Seconds between each batch.</param>
    /// <param name="startTime">Start time for the first batch.</param>
    public IReadOnlyList<SensorReading> GenerateTimeSeries(
        int count = 20,
        int intervalSeconds = 30,
        DateTime? startTime = null)
    {
        var start = startTime ?? DateTime.UtcNow.AddMinutes(-count * intervalSeconds / 60.0);
        var all = new List<SensorReading>();

        for (var i = 0; i < count; i++)
        {
            var batchTime = start.AddSeconds(i * intervalSeconds);
            var batch = GenerateBatch(batchTime);
            all.AddRange(batch);
        }

        return all;
    }
}

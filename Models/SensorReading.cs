using MongoDB.Bson.Serialization.Attributes;

namespace DotNetMongoBoilerplate.Models;

/// <summary>
/// IoT sensor reading for MongoDB time series collection.
/// Time series documents require: timeField (timestamp) and optional metaField (metadata).
/// </summary>
public class SensorReading
{
    /// <summary>
    /// Required time field for time series collections.
    /// </summary>
    [BsonElement("timestamp")]
    public DateTime Timestamp { get; set; }

    /// <summary>
    /// Metadata identifying the sensor (sensorId, type, location).
    /// Used as metaField for efficient querying and indexing.
    /// </summary>
    [BsonElement("metadata")]
    public SensorMetadata Metadata { get; set; } = new();

    /// <summary>
    /// Temperature reading in Celsius.
    /// </summary>
    [BsonElement("temperature")]
    public double Temperature { get; set; }

    /// <summary>
    /// Humidity reading as percentage (0-100).
    /// </summary>
    [BsonElement("humidity")]
    public double Humidity { get; set; }

    /// <summary>
    /// Optional pressure reading in hPa.
    /// </summary>
    [BsonElement("pressure")]
    public double? Pressure { get; set; }
}

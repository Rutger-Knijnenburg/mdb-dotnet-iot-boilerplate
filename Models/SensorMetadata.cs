using MongoDB.Bson.Serialization.Attributes;

namespace DotNetMongoBoilerplate.Models;

/// <summary>
/// Metadata for an IoT sensor. Used as the metaField in time series collections.
/// Metadata should rarely change and identifies a unique series of measurements.
/// </summary>
public class SensorMetadata
{
    [BsonElement("sensorId")]
    public string SensorId { get; set; } = string.Empty;

    [BsonElement("sensorType")]
    public string SensorType { get; set; } = "environment";

    [BsonElement("location")]
    public string Location { get; set; } = string.Empty;
}

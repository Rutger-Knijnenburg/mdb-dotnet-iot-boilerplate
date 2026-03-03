package com.sample.springvector.iot;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.TimeSeries;
import org.springframework.data.mongodb.core.timeseries.Granularity;

import java.time.Instant;

/**
 * IoT use case: Sensor reading stored in a MongoDB time series collection.
 */
@TimeSeries(
    collection = "sensor_readings",
    timeField = "timestamp",
    metaField = "metadata",
    granularity = Granularity.SECONDS
)
@Document(collection = "sensor_readings")
public class SensorReading {

    @Id
    private String id;

    private Instant timestamp;
    private double temperature;
    private double humidity;
    private double pressure;

    private Metadata metadata;

    public record Metadata(String deviceId, String location) {}

    public SensorReading() {}

    public SensorReading(Instant timestamp, double temperature, double humidity, double pressure, String deviceId, String location) {
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.humidity = humidity;
        this.pressure = pressure;
        this.metadata = new Metadata(deviceId, location);
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }
    public double getPressure() { return pressure; }
    public void setPressure(double pressure) { this.pressure = pressure; }
    public Metadata getMetadata() { return metadata; }
    public void setMetadata(Metadata metadata) { this.metadata = metadata; }
}

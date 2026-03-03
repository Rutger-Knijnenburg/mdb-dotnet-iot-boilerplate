package com.sample.springvector.iot;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface SensorReadingRepository extends MongoRepository<SensorReading, String> {

    List<SensorReading> findByMetadata_DeviceId(String deviceId);

    List<SensorReading> findByMetadata_DeviceIdAndTimestampBetween(String deviceId, Instant start, Instant end);
}

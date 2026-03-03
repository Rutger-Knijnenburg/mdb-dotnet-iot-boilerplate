package com.sample.springvector.iot;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Ensures the time series collection exists before first use.
 */
@Configuration
public class TimeSeriesCollectionInitializer {

    @Bean
    public ApplicationRunner initTimeSeriesCollection(MongoTemplate mongoTemplate) {
        return args -> {
            var collectionName = "sensor_readings";
            if (!mongoTemplate.collectionExists(collectionName)) {
                mongoTemplate.createCollection(SensorReading.class);
            }
        };
    }
}

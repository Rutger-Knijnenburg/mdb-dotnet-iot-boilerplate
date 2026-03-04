package com.sample.springvector.iot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IoT use case: REST API for sensor readings (time series data).
 */
@RestController
@RequestMapping("/api/iot")
public class IotController {

    private static final Logger log = LoggerFactory.getLogger(IotController.class);
    private final SensorReadingRepository repository;
    private final MongoTemplate mongoTemplate;
    private final IngestionDemoService ingestionDemoService;
    private final IotStatsService statsService;
    private final IotAggregationService aggregationService;

    public IotController(SensorReadingRepository repository, MongoTemplate mongoTemplate,
                         IngestionDemoService ingestionDemoService, IotStatsService statsService,
                         IotAggregationService aggregationService) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.ingestionDemoService = ingestionDemoService;
        this.statsService = statsService;
        this.aggregationService = aggregationService;
    }

    @GetMapping("/stats")
    public IotStatsService.IotStats getStats() {
        return statsService.getStats();
    }

    @PostMapping("/readings/batch")
    public ResponseEntity<BatchResponse> createReadingsBatch(@RequestBody List<SensorReadingRequest> requests) {
        var baseTime = Instant.now();
        var index = new AtomicInteger(0);
        var saved = requests.stream()
            .map(r -> new SensorReading(
                baseTime.plusSeconds(index.getAndIncrement()),
                r.temperature(), r.humidity(), r.pressure(),
                r.deviceId(), r.location()))
            .map(repository::save)
            .toList();
        return ResponseEntity.ok(new BatchResponse(saved.size()));
    }

    @PostMapping("/readings/ingest-demo")
    public ResponseEntity<IngestionDemoResponse> startIngestionDemo() {
        ingestionDemoService.startIngestion()
            .thenAccept(r -> log.info("Ingestion demo completed: {} readings", r.count()))
            .exceptionally(e -> { log.error("Ingestion demo failed", e); return null; });
        return ResponseEntity.accepted()
            .body(new IngestionDemoResponse(1000, 5, "Ingestion started. 1000 readings will be sent over 5 minutes."));
    }

    @GetMapping("/latest-readings")
    public List<SensorReading> getLatestReadings(@RequestParam(defaultValue = "10") int limit) {
        int maxLimit = Math.min(limit, 50);
        Aggregation agg = Aggregation.newAggregation(
            Aggregation.sort(Sort.by(Sort.Direction.DESC, "timestamp")),
            Aggregation.limit(maxLimit)
        );
        AggregationResults<SensorReading> results = mongoTemplate.aggregate(
            agg, "sensor_readings", SensorReading.class);
        return results.getMappedResults();
    }

    @GetMapping("/readings/{deviceId}")
    public List<SensorReading> getReadings(
            @PathVariable String deviceId,
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end) {
        if (start != null && end != null) {
            return repository.findByMetadata_DeviceIdAndTimestampBetween(deviceId, start, end);
        }
        return repository.findByMetadata_DeviceId(deviceId);
    }

    @GetMapping("/aggregations")
    public IotAggregationService.AggregationResult runAggregation(
            @RequestParam String op,
            @RequestParam(required = false, defaultValue = "temperature") String field,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) Instant start,
            @RequestParam(required = false) Instant end) {
        return aggregationService.run(op, field, deviceId, start, end);
    }
}

record SensorReadingRequest(double temperature, double humidity, double pressure, String deviceId, String location) {}
record BatchResponse(int count) {}
record IngestionDemoResponse(int count, int durationMinutes, String message) {}

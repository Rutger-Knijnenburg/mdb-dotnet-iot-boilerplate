package com.sample.springvector.iot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spreads 1000 sensor readings over 5 minutes to demonstrate ingestion over time.
 */
@Service
public class IngestionDemoService {

    private static final Logger log = LoggerFactory.getLogger(IngestionDemoService.class);
    private static final int TOTAL_READINGS = 1000;
    private static final long DURATION_MS = 5 * 60 * 1000; // 5 minutes
    private static final long INTERVAL_MS = DURATION_MS / TOTAL_READINGS; // ~300ms per reading

    private static final String[] DEVICES = {"sensor-001", "sensor-002", "sensor-003", "sensor-004", "sensor-005",
            "sensor-006", "sensor-007", "sensor-008", "sensor-009", "sensor-010"};
    private static final String[] LOCATIONS = {"warehouse-a", "warehouse-b", "factory-floor", "cold-storage", "loading-dock"};

    private final SensorReadingRepository repository;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        var t = new Thread(r, "ingestion-demo");
        t.setDaemon(true);
        return t;
    });

    public IngestionDemoService(SensorReadingRepository repository) {
        this.repository = repository;
    }

    public CompletableFuture<IngestionResult> startIngestion() {
        var result = new CompletableFuture<IngestionResult>();
        var baseTime = Instant.now();
        var index = new AtomicInteger(0);

        log.info("Starting ingestion demo: {} readings over {} minutes", TOTAL_READINGS, DURATION_MS / 60000);

        for (int i = 0; i < TOTAL_READINGS; i++) {
            final int idx = i;
            scheduler.schedule(() -> {
                try {
                    var r = generateReading(baseTime.plusSeconds(idx), idx);
                    repository.save(r);
                    if (index.incrementAndGet() == TOTAL_READINGS) {
                        result.complete(new IngestionResult(TOTAL_READINGS, DURATION_MS));
                    }
                } catch (Exception e) {
                    log.error("Ingestion error", e);
                    if (!result.isDone()) result.completeExceptionally(e);
                }
            }, i * INTERVAL_MS, TimeUnit.MILLISECONDS);
        }

        return result;
    }

    private SensorReading generateReading(Instant timestamp, int index) {
        return new SensorReading(
            timestamp,
            18 + Math.random() * 10,
            40 + Math.random() * 40,
            1000 + Math.random() * 25,
            DEVICES[index % DEVICES.length],
            LOCATIONS[index % LOCATIONS.length]
        );
    }

    record IngestionResult(int count, long durationMs) {}
}

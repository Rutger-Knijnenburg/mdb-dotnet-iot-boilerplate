package com.sample.springvector.iot;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

/**
 * Provides statistics for the IoT time series collection: document count,
 * bucket count, storage size, and index size.
 */
@Service
public class IotStatsService {

    private static final Logger log = LoggerFactory.getLogger(IotStatsService.class);
    private static final String COLLECTION = "sensor_readings";

    private final MongoTemplate mongoTemplate;

    public IotStatsService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public IotStats getStats() {
        long documentCount = 0;
        long bucketCount = 0;
        long storageSizeBytes = 0;
        long indexSizeBytes = 0;

        try {
            documentCount = mongoTemplate.getCollection(COLLECTION).countDocuments();
        } catch (Exception e) {
            log.debug("Document count failed: {}", e.getMessage());
        }

        try {
            var result = mongoTemplate.getCollection(COLLECTION).aggregate(
                java.util.List.of(
                    new Document("$collStats",
                        new Document("storageStats", new Document("scale", 1))
                            .append("count", new Document()))
                )
            ).first();

            if (result != null) {
                // Do NOT use collStats count for time series - it returns bucket count, not measurement count
                if (result.containsKey("storageStats")) {
                    var storageStats = (Document) result.get("storageStats");
                    if (storageStats != null) {
                        storageSizeBytes = getLong(storageStats, "storageSize");
                        indexSizeBytes = getLong(storageStats, "totalIndexSize");
                        if (storageStats.containsKey("timeseries")) {
                            var ts = (Document) storageStats.get("timeseries");
                            if (ts != null && ts.containsKey("bucketCount")) {
                                bucketCount = getInt(ts, "bucketCount");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("$collStats failed, trying collStats command: {}", e.getMessage());
            try {
                var cmdResult = mongoTemplate.getDb().runCommand(
                    new Document("collStats", COLLECTION).append("scale", 1));
                // Do NOT use collStats count for time series - it returns bucket count, not measurement count
                storageSizeBytes = getLong(cmdResult, "storageSize");
                if (storageSizeBytes == 0) storageSizeBytes = getLong(cmdResult, "size");
                indexSizeBytes = getLong(cmdResult, "totalIndexSize");
                if (cmdResult.containsKey("timeseries")) {
                    var ts = (Document) cmdResult.get("timeseries");
                    if (ts != null && ts.containsKey("bucketCount")) {
                        bucketCount = getInt(ts, "bucketCount");
                    }
                }
            } catch (Exception e2) {
                log.warn("collStats command failed: {}", e2.getMessage());
            }
        }

        return new IotStats(
            documentCount,
            bucketCount,
            storageSizeBytes,
            indexSizeBytes,
            bucketingInfo()
        );
    }

    private static long getLong(Document doc, String key) {
        Object v = doc.get(key);
        if (v == null) return 0;
        if (v instanceof Number n) return n.longValue();
        return 0;
    }

    private static int getInt(Document doc, String key) {
        Object v = doc.get(key);
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        return 0;
    }

    private static String bucketingInfo() {
        return "Buckets group measurements by metadata (deviceId+location) and time. " +
            "Granularity: seconds (max 1h per bucket). " +
            "Buckets close at ~1000 docs or ~125KB.";
    }

    public record IotStats(
        long documentCount,
        long bucketCount,
        long storageSizeBytes,
        long indexSizeBytes,
        String bucketingInfo
    ) {}
}

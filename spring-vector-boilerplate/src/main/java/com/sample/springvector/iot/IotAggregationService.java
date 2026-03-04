package com.sample.springvector.iot;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Runs aggregations on the IoT time series collection (avg, max, min, count).
 */
@Service
public class IotAggregationService {

    private static final String COLLECTION = "sensor_readings";
    private static final List<String> NUMERIC_FIELDS = List.of("temperature", "humidity", "pressure");

    private final MongoTemplate mongoTemplate;

    public IotAggregationService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public AggregationResult run(String op, String field, String deviceId, Instant start, Instant end) {
        if (!NUMERIC_FIELDS.contains(field) && !"count".equals(op)) {
            throw new IllegalArgumentException("Field must be one of: " + NUMERIC_FIELDS);
        }

        var matchCriteria = new Criteria();
        boolean hasFilter = false;
        if (deviceId != null && !deviceId.isBlank()) {
            matchCriteria = matchCriteria.and("metadata.deviceId").is(deviceId);
            hasFilter = true;
        }
        if (start != null) {
            matchCriteria = matchCriteria.and("timestamp").gte(start);
            hasFilter = true;
        }
        if (end != null) {
            matchCriteria = matchCriteria.and("timestamp").lte(end);
            hasFilter = true;
        }

        Aggregation agg;
        if ("count".equals(op)) {
            agg = hasFilter
                ? Aggregation.newAggregation(
                    Aggregation.match(matchCriteria),
                    Aggregation.group().count().as("value"))
                : Aggregation.newAggregation(Aggregation.group().count().as("value"));
        } else {
            var groupOp = Aggregation.group();
            switch (op) {
                case "avg" -> groupOp = groupOp.avg(field).as("value");
                case "max" -> groupOp = groupOp.max(field).as("value");
                case "min" -> groupOp = groupOp.min(field).as("value");
                default -> throw new IllegalArgumentException("Op must be avg, max, min, or count");
            }
            agg = hasFilter
                ? Aggregation.newAggregation(Aggregation.match(matchCriteria), groupOp)
                : Aggregation.newAggregation(groupOp);
        }

        var results = mongoTemplate.aggregate(agg, COLLECTION, Map.class);
        var doc = results.getUniqueMappedResult();
        Object value = doc != null ? doc.get("value") : null;

        return new AggregationResult(op, field, deviceId, start, end, value);
    }

    public record AggregationResult(String op, String field, String deviceId, Instant start, Instant end, Object value) {}
}

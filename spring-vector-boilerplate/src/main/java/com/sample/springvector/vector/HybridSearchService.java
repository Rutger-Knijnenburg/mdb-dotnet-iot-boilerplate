package com.sample.springvector.vector;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Hybrid search: combines semantic (vector) and keyword (full-text) search
 * using Reciprocal Rank Fusion (RRF) for result merging.
 */
@Service
public class HybridSearchService {

    private static final int RRF_K = 60; // rank constant for RRF formula

    private final VectorStore vectorStore;
    private final MongoTemplate mongoTemplate;
    private final String collectionName;

    public HybridSearchService(VectorStore vectorStore, MongoTemplate mongoTemplate,
                               @Value("${vector.store.collection-name:vector_store}") String collectionName) {
        this.vectorStore = vectorStore;
        this.mongoTemplate = mongoTemplate;
        this.collectionName = collectionName;
    }

    /**
     * Performs hybrid search: vector + keyword, fused with RRF.
     *
     * @param query        search query
     * @param topK         max results to return
     * @param vectorWeight weight for vector pipeline (0–1), keyword gets (1 - vectorWeight)
     */
    public List<HybridSearchResult> search(String query, int topK, double vectorWeight) {
        double keywordWeight = 1.0 - vectorWeight;
        int fetchLimit = Math.max(topK * 3, 20); // fetch extra for fusion

        // 1. Vector (semantic) search
        List<Document> vectorResults = vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(fetchLimit)
        );

        // 2. Keyword (full-text) search
        List<VectorStoreDoc> keywordResults = keywordSearch(query, fetchLimit);

        // 3. Reciprocal Rank Fusion
        return reciprocalRankFusion(vectorResults, keywordResults, topK, vectorWeight, keywordWeight);
    }

    private List<VectorStoreDoc> keywordSearch(String query, int limit) {
        try {
            var textQuery = Query.query(TextCriteria.forDefaultLanguage().matching(query)).limit(limit);
            return mongoTemplate.find(textQuery, VectorStoreDoc.class, collectionName);
        } catch (Exception e) {
            // Text index may not exist yet
            return List.of();
        }
    }

    private List<HybridSearchResult> reciprocalRankFusion(
            List<Document> vectorResults,
            List<VectorStoreDoc> keywordResults,
            int topK,
            double vectorWeight,
            double keywordWeight) {

        // Use content as stable key for deduplication across both result sets
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, HybridSearchResult> byKey = new HashMap<>();

        for (int rank = 0; rank < vectorResults.size(); rank++) {
            Document doc = vectorResults.get(rank);
            String key = doc.getContent();
            double rrf = vectorWeight / (RRF_K + rank + 1);
            rrfScores.merge(key, rrf, Double::sum);
            byKey.put(key, new HybridSearchResult(doc.getContent(), doc.getMetadata(), 0)); // score updated below
        }

        for (int rank = 0; rank < keywordResults.size(); rank++) {
            VectorStoreDoc doc = keywordResults.get(rank);
            String key = doc.content();
            double rrf = keywordWeight / (RRF_K + rank + 1);
            rrfScores.merge(key, rrf, Double::sum);
            if (!byKey.containsKey(key)) {
                byKey.put(key, new HybridSearchResult(doc.content(),
                    doc.metadata() != null ? doc.metadata() : Map.of(), 0));
            }
        }

        // Update all results with final RRF scores
        byKey.replaceAll((k, v) -> new HybridSearchResult(v.content(), v.metadata(), rrfScores.getOrDefault(k, 0.0)));

        return rrfScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(topK)
            .map(e -> byKey.get(e.getKey()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    record VectorStoreDoc(String id, String content, Map<String, Object> metadata) {}
    record HybridSearchResult(String content, Map<String, Object> metadata, double score) {}
}

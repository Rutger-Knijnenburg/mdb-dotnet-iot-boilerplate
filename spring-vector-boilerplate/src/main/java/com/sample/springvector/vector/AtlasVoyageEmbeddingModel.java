package com.sample.springvector.vector;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Embedding model that uses MongoDB Atlas Embedding API (Voyage AI models).
 * API docs: https://www.mongodb.com/docs/api/doc/atlas-embedding-and-reranking-api/
 */
@Component
public class AtlasVoyageEmbeddingModel implements EmbeddingModel {

    private static final String EMBEDDINGS_ENDPOINT = "/embeddings";

    private final RestClient restClient;
    private final String model;

    public AtlasVoyageEmbeddingModel(
            @Value("${vector.atlas.api-key:}") String apiKey,
            @Value("${vector.atlas.base-url:https://ai.mongodb.com/v1}") String baseUrl,
            @Value("${vector.store.embedding-model:voyage-4-large}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        var input = request.getInstructions().stream()
            .map(Object::toString)
            .toList();

        var response = restClient.post()
            .uri(EMBEDDINGS_ENDPOINT)
            .body(Map.of(
                "input", input,
                "model", model
            ))
            .retrieve()
            .body(AtlasEmbeddingResponse.class);

        if (response == null || response.data == null) {
            throw new IllegalStateException("Empty response from Atlas Embedding API");
        }

        List<Embedding> embeddings = new ArrayList<>();
        for (int i = 0; i < response.data.size(); i++) {
            var d = response.data.get(i);
            float[] floats = new float[d.embedding.size()];
            for (int j = 0; j < d.embedding.size(); j++) {
                floats[j] = d.embedding.get(j).floatValue();
            }
            embeddings.add(new Embedding(floats, i));
        }

        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getContent());
    }

    @Override
    public int dimensions() {
        return 1024; // voyage-4-large default
    }

    record AtlasEmbeddingResponse(List<EmbeddingData> data) {}
    record EmbeddingData(List<Double> embedding) {}
}

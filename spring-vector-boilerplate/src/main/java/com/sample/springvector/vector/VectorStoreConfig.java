package com.sample.springvector.vector;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.mongodb.atlas.MongoDBAtlasVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(
            MongoTemplate mongoTemplate,
            EmbeddingModel embeddingModel,
            @Value("${vector.store.collection-name:vector_store}") String collectionName,
            @Value("${vector.store.index-name:vector_index}") String indexName,
            @Value("${vector.store.initialize-schema:true}") boolean initializeSchema) {

        return MongoDBAtlasVectorStore.builder(mongoTemplate, embeddingModel)
            .collectionName(collectionName)
            .vectorIndexName(indexName)
            .initializeSchema(initializeSchema)
            .build();
    }
}

package com.sample.springvector.vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

/**
 * Ensures a text index exists on the vector store collection for hybrid search keyword lookup.
 */
@Configuration
public class VectorStoreIndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreIndexInitializer.class);

    @Bean
    public ApplicationRunner initVectorStoreTextIndex(
            MongoTemplate mongoTemplate,
            @Value("${vector.store.collection-name:vector_store}") String collectionName) {
        return args -> {
            try {
                var indexOps = mongoTemplate.indexOps(collectionName);
                var textIndex = new TextIndexDefinition.TextIndexDefinitionBuilder()
                    .onField("content")
                    .build();
                indexOps.ensureIndex(textIndex);
                log.info("Text index on 'content' ensured for hybrid search: {}", collectionName);
            } catch (Exception e) {
                log.warn("Could not create text index for hybrid search (keyword search may be unavailable): {}", e.getMessage());
            }
        };
    }
}

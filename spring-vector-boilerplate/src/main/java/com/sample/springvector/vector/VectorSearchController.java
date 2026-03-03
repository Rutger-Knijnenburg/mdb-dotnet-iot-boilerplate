package com.sample.springvector.vector;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Vector Search use case: Semantic and hybrid search using MongoDB Atlas Vector Search + Voyage AI.
 */
@RestController
@RequestMapping("/api/vector")
public class VectorSearchController {

    private final VectorStore vectorStore;
    private final HybridSearchService hybridSearchService;

    public VectorSearchController(VectorStore vectorStore, HybridSearchService hybridSearchService) {
        this.vectorStore = vectorStore;
        this.hybridSearchService = hybridSearchService;
    }

    @PostMapping("/add")
    public ResponseEntity<String> addDocuments(@RequestBody AddDocumentsRequest request) {
        var docs = request.documents().stream()
            .map(d -> new Document(d.content(), d.metadata() != null ? d.metadata() : Map.of()))
            .toList();
        vectorStore.add(docs);
        return ResponseEntity.ok("Documents added successfully");
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "semantic") String mode,
            @RequestParam(defaultValue = "0.5") double vectorWeight) {
        if ("hybrid".equalsIgnoreCase(mode)) {
            return hybridSearchService.search(query, topK, vectorWeight).stream()
                .map(r -> Map.<String, Object>of(
                    "content", r.content(),
                    "metadata", r.metadata(),
                    "score", r.score()
                ))
                .collect(Collectors.toList());
        }
        var results = vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(topK)
        );
        return results.stream()
            .map(doc -> Map.<String, Object>of(
                "content", doc.getContent(),
                "metadata", doc.getMetadata()
            ))
            .collect(Collectors.toList());
    }
}

record AddDocumentsRequest(List<DocumentInput> documents) {}
record DocumentInput(String content, Map<String, Object> metadata) {}

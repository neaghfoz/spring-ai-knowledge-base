package com.wantwant.sakb.model;

import java.time.Instant;
import java.util.UUID;

public class KnowledgeBase {
    private final String id;
    private String name;
    private String description;
    private final Instant createdAt;

    // Constructor for new (non-persisted) KB
    public KnowledgeBase(String name, String description) {
        this(UUID.randomUUID().toString(), name, description, Instant.now());
    }

    // Full constructor for DB reconstruction
    public KnowledgeBase(String id, String name, String description, Instant createdAt) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
}


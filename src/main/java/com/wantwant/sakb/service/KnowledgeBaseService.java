package com.wantwant.sakb.service;

import com.wantwant.sakb.model.KnowledgeBase;

import java.util.List;
import java.util.Optional;

public interface KnowledgeBaseService {
    KnowledgeBase create(String name, String description);
    Optional<KnowledgeBase> get(String id);
    List<KnowledgeBase> list();
    void delete(String id);
    boolean exists(String id);
}


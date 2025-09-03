package com.wantwant.sakb.service;

import com.wantwant.sakb.model.KnowledgeBase;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("!pg")
@Primary
public class InMemoryKnowledgeBaseService implements KnowledgeBaseService {
    private final Map<String, KnowledgeBase> store = new ConcurrentHashMap<>();

    @Override
    public KnowledgeBase create(String name, String description) {
        KnowledgeBase kb = new KnowledgeBase(name, description);
        store.put(kb.getId(), kb);
        return kb;
    }

    @Override
    public Optional<KnowledgeBase> get(String id) { return Optional.ofNullable(store.get(id)); }

    @Override
    public List<KnowledgeBase> list() { return new ArrayList<>(store.values()); }

    @Override
    public void delete(String id) { store.remove(id); }

    @Override
    public boolean exists(String id) { return store.containsKey(id); }
}

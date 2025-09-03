package com.wantwant.sakb.service;

import com.wantwant.sakb.model.KnowledgeBase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Profile("pg")
public class PostgresKnowledgeBaseService implements KnowledgeBaseService {

    private final JdbcTemplate jdbcTemplate;

    public PostgresKnowledgeBaseService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public KnowledgeBase create(String name, String description) {
        String sql = "INSERT INTO knowledge_bases (id, name, description, created_at) VALUES (gen_random_uuid(), ?, ?, now()) RETURNING id, name, description, created_at";
        return jdbcTemplate.queryForObject(sql, mapper(), name, description);
    }

    @Override
    public Optional<KnowledgeBase> get(String id) {
        String sql = "SELECT id, name, description, created_at FROM knowledge_bases WHERE id = ?::uuid";
        List<KnowledgeBase> list = jdbcTemplate.query(sql, mapper(), id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<KnowledgeBase> list() {
        String sql = "SELECT id, name, description, created_at FROM knowledge_bases ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, mapper());
    }

    @Override
    public void delete(String id) {
        jdbcTemplate.update("DELETE FROM knowledge_bases WHERE id = ?::uuid", id);
    }

    @Override
    public boolean exists(String id) {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM knowledge_bases WHERE id=?::uuid", Integer.class, id);
        return c != null && c > 0;
    }

    private RowMapper<KnowledgeBase> mapper() {
        return (ResultSet rs, int rowNum) -> new KnowledgeBase(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}

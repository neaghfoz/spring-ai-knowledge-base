package com.wantwant.sakb.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pgvector.PGvector;
import com.wantwant.sakb.model.DocumentChunk;
import org.postgresql.util.PGobject;

@Service
@Profile("pg")
public class PostgresVectorStore implements VectorStore {

    private final JdbcTemplate jdbcTemplate;
    private final int dim;
    private final ObjectMapper mapper = new ObjectMapper();

    public PostgresVectorStore(JdbcTemplate jdbcTemplate,
                               @Value("${kb.vector.dim:1536}") int dim) {
        this.jdbcTemplate = jdbcTemplate;
        this.dim = dim;
    }

    @Override
    public void addAll(String kbId, List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) return;
        String sql = "INSERT INTO kb_chunks (id, kb_id, source_name, chunk_index, text, embedding, metadata, created_at) " +
                "VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?::jsonb, now())";
        List<Object[]> batch = new ArrayList<>();
        for (DocumentChunk c : chunks) {
            PGvector vec = new PGvector(padOrTruncate(c.getEmbedding()));
            String metaJson;
            try {
                metaJson = mapper.writeValueAsString(c.getMetadata() == null ? java.util.Map.of() : c.getMetadata());
            } catch (Exception e) {
                metaJson = "{}";
            }
            batch.add(new Object[]{kbId, c.getSourceName(), c.getChunkIndex(), c.getText(), vec, metaJson});
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    @Override
    public List<DocumentChunk> getAll(String kbId) {
        String sql = "SELECT kb_id, source_name, chunk_index, text, embedding, metadata FROM kb_chunks WHERE kb_id=? ORDER BY source_name, chunk_index";
        return jdbcTemplate.query(sql, rowMapper(), kbId);
    }

    @Override
    public void deleteKb(String kbId) {
        jdbcTemplate.update("DELETE FROM kb_chunks WHERE kb_id=?", kbId);
    }

    @Override
    public List<Scored<DocumentChunk>> search(String kbId, float[] queryEmbedding, int topK) {
        PGvector q = new PGvector(padOrTruncate(queryEmbedding));
        String sql = "SELECT kb_id, source_name, chunk_index, text, embedding, metadata, (1 - (embedding <=> ?)) as score " +
                "FROM kb_chunks WHERE kb_id=? ORDER BY embedding <=> ? ASC LIMIT ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            DocumentChunk c = mapChunk(rs);
            double score = rs.getDouble("score");
            return new Scored<>(c, score);
        }, q, kbId, q, topK);
    }

    private RowMapper<DocumentChunk> rowMapper() {
        return (rs, rowNum) -> mapChunk(rs);
    }

    private DocumentChunk mapChunk(ResultSet rs) throws SQLException {
        String kbId = rs.getString("kb_id");
        String source = rs.getString("source_name");
        int idx = rs.getInt("chunk_index");
        String text = rs.getString("text");
        Object embObj = rs.getObject("embedding");
        float[] emb = parseEmbedding(embObj);
        // metadata parsing skipped (stored as jsonb); could parse to Map if needed
        return new DocumentChunk(kbId, source, idx, text, emb, java.util.Map.of());
    }

    private float[] padOrTruncate(float[] v) {
        float[] out = new float[dim];
        if (v == null) return out;
        int n = Math.min(v.length, dim);
        System.arraycopy(v, 0, out, 0, n);
        return out;
    }

    private float[] parseEmbedding(Object obj) throws SQLException {
        if (obj == null) return new float[dim];
        if (obj instanceof PGvector v) {
            return toFloatArray(v.getValue());
        }
        if (obj instanceof PGobject pgo) {
            // Expect type "vector" with textual value like "[0.1,0.2,...]"
            String s = pgo.getValue();
            return toFloatArray(s);
        }
        if (obj instanceof String s) {
            return toFloatArray(s);
        }
        // Fallback: try toString
        return toFloatArray(obj.toString());
    }

    private float[] toFloatArray(PGvector v) {
        if (v == null || v.getValue() == null) return new float[dim];
        return toFloatArray(v.getValue());
    }

    private float[] toFloatArray(String s) {
        if (s == null) return new float[dim];
        s = s.trim();
        if (s.startsWith("[")) s = s.substring(1);
        if (s.endsWith("]")) s = s.substring(0, s.length() - 1);
        if (s.isBlank()) return new float[dim];
        String[] parts = s.split(",");
        float[] f = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            f[i] = Float.parseFloat(parts[i].trim());
        }
        return f;
    }
}

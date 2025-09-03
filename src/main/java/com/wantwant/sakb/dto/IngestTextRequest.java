package com.wantwant.sakb.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class IngestTextRequest {
    @NotBlank
    private String kbId;
    @NotBlank
    private String text;
    private String sourceName;
    private Map<String, Object> metadata;

    public String getKbId() { return kbId; }
    public void setKbId(String kbId) { this.kbId = kbId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}


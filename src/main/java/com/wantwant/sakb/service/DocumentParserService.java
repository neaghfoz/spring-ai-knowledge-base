package com.wantwant.sakb.service;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class DocumentParserService {
    private final Tika tika = new Tika();

    public static class ParsedDocument {
        public final String text;
        public final Map<String, Object> metadata;
        public ParsedDocument(String text, Map<String, Object> metadata) {
            this.text = text;
            this.metadata = metadata;
        }
    }

    public ParsedDocument parse(InputStream is, String filename, String contentType) throws Exception {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        if (filename != null) metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
        if (contentType != null) metadata.set(Metadata.CONTENT_TYPE, contentType);
        parser.parse(is, handler, metadata, new ParseContext());
        String text = handler.toString();
        if (text == null) text = "";
        text = new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8).trim();
        Map<String, Object> meta = new HashMap<>();
        for (String name : metadata.names()) {
            meta.put(name, metadata.get(name));
        }
        if (contentType == null) {
            try { meta.put("detectedContentType", tika.detect(filename)); } catch (Exception ignored) {}
        }
        return new ParsedDocument(text, meta);
    }

    public boolean isTextLike(String contentType) {
        if (contentType == null) return false;
        return contentType.startsWith(MediaType.TEXT_PLAIN_VALUE)
                || contentType.contains("json")
                || contentType.contains("xml");
    }
}

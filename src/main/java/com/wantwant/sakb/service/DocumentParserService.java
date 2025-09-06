package com.wantwant.sakb.service;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

// POI
import org.apache.poi.ss.usermodel.*;

@Service
public class DocumentParserService {
    private final Tika tika = new Tika();

    @Value("${kb.ocr.enabled:false}")
    private boolean ocrEnabled;
    @Value("${kb.ocr.lang:chi_sim+eng}")
    private String ocrLang;

    public static class ParsedDocument {
        public final String text;
        public final Map<String, Object> metadata;
        public ParsedDocument(String text, Map<String, Object> metadata) {
            this.text = text;
            this.metadata = metadata;
        }
    }

    public ParsedDocument parse(InputStream is, String filename, String contentType) throws Exception {
        String lcName = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        String ct = contentType == null ? null : contentType.toLowerCase(Locale.ROOT);
        if (isExcel(lcName, ct)) {
            return parseExcel(is, filename);
        }
        // Fallback to Tika (optionally with OCR)
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        if (filename != null) metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
        if (contentType != null) metadata.set(Metadata.CONTENT_TYPE, contentType);
        ParseContext ctx = new ParseContext();
        maybeEnableOcr(ctx);
        parser.parse(is, handler, metadata, ctx);
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

    private boolean isExcel(String filenameLower, String contentTypeLower) {
        if (filenameLower != null && (filenameLower.endsWith(".xlsx") || filenameLower.endsWith(".xlsm") || filenameLower.endsWith(".xls"))) return true;
        if (contentTypeLower == null) return false;
        return contentTypeLower.contains("spreadsheet") || contentTypeLower.contains("excel");
    }

    private ParsedDocument parseExcel(InputStream is, String filename) throws Exception {
        try (Workbook wb = WorkbookFactory.create(is)) {
            List<String> lines = new ArrayList<>();
            Map<String, Object> meta = new HashMap<>();
            meta.put("parser", "poi");
            meta.put("workbookName", filename);
            List<String> sheets = new ArrayList<>();
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                if (sheet == null) continue;
                String sheetName = sheet.getSheetName();
                sheets.add(sheetName);
                List<String> headers = null;
                int rowCount = 0;
                for (Row row : sheet) {
                    if (row == null) continue;
                    List<String> cells = new ArrayList<>();
                    int lastCell = row.getLastCellNum();
                    if (lastCell < 0) continue;
                    for (int c = 0; c < lastCell; c++) {
                        Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        cells.add(formatCell(cell));
                    }
                    // Determine header on first non-empty row
                    boolean allEmpty = cells.stream().allMatch(v -> v == null || v.isBlank());
                    if (allEmpty) continue;
                    if (headers == null) {
                        headers = normalizeHeaders(cells);
                        continue;
                    }
                    rowCount++;
                    String kv = toKeyValueLine(headers, cells);
                    lines.add("##[TABLE_ROW] sheet=" + sheetName + " row=" + (row.getRowNum()+1) + " | " + kv);
                }
                meta.put("rows:" + sheetName, rowCount);
            }
            meta.put("sheets", sheets);
            String text = String.join("\n", lines);
            return new ParsedDocument(text, meta);
        }
    }

    private List<String> normalizeHeaders(List<String> cells) {
        List<String> out = new ArrayList<>();
        int idx = 0;
        for (String h : cells) {
            String name = (h == null || h.isBlank()) ? ("col_" + idx) : h.trim();
            out.add(name);
            idx++;
        }
        return out;
    }

    private String toKeyValueLine(List<String> headers, List<String> cells) {
        StringBuilder sb = new StringBuilder();
        int n = Math.max(headers.size(), cells.size());
        for (int i = 0; i < n; i++) {
            String k = i < headers.size() ? headers.get(i) : ("col_" + i);
            String v = i < cells.size() ? Optional.ofNullable(cells.get(i)).orElse("") : "";
            if (i > 0) sb.append(" | ");
            sb.append(k).append("=").append(v);
        }
        return sb.toString();
    }

    private String formatCell(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    java.util.Date d = cell.getDateCellValue();
                    yield new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
                } else {
                    double v = cell.getNumericCellValue();
                    // Trim trailing .0 for integers
                    String s = String.valueOf(v);
                    if (s.endsWith(".0")) s = s.substring(0, s.length()-2);
                    yield s;
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                    CellValue cv = evaluator.evaluate(cell);
                    yield cv == null ? "" : cv.formatAsString();
                } catch (Exception ex) {
                    yield cell.getCellFormula();
                }
            }
            default -> "";
        };
    }

    private void maybeEnableOcr(ParseContext ctx) {
        if (!ocrEnabled) return;
        try {
            Class<?> cfgClz = Class.forName("org.apache.tika.parser.ocr.TesseractOCRConfig");
            Object cfg = cfgClz.getDeclaredConstructor().newInstance();
            try { cfgClz.getMethod("setLanguage", String.class).invoke(cfg, ocrLang); } catch (NoSuchMethodException ignored) {}
            // Reflective call to avoid generics issues: ctx.set(Class, Object)
            java.lang.reflect.Method m = ParseContext.class.getMethod("set", Class.class, Object.class);
            m.invoke(ctx, cfgClz, cfg);
        } catch (ClassNotFoundException e) {
            // OCR module not on classpath; skip silently
        } catch (Throwable t) {
            // Ignore OCR setup failure to keep parsing resilient
        }
    }
}

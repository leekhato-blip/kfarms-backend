package com.kfarms.reports;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class ExportFactory {

    private final Map<String, Exporter> exporters;


    /**
     * Expects beans named/coded with keys like: "csv", "xlsx", "pdf".
     * You can register additional exporters as beans and include the keys you want.
     */
    @Autowired
    public ExportFactory(Map<String, Exporter> exporters) {
        this.exporters = exporters;
    }

    /**
     * Returns an exporter for the requested type (csv | xlsx | pdf).
     * Keys are matched case-insensitively against:
     *  - bean names (preferred), or
     *  - common aliases when bean names differ.
     */
    public Exporter getExporter(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Export type is required (csv, xlsx, pdf).");
        }
        String key = type.trim().toLowerCase(Locale.ROOT);

        // Direct match first (common case: bean named "csvExporter" or "csv")
        // Normalize known patterns to a tiny set of canonical keys
        if (key.endsWith("exporter")) {
            key = key.replaceFirst("exporter", "");
        }

        // Try exact key
        if (exporters.containsKey(key)) {
            return exporters.get(key);
        }

        // Try common bean name variants
        String[] tries = new String[] { key, key + "Exporter", key + "-exporter", key + "_exporter" };
        for (String t : tries) {
            if (exporters.containsKey(t)) return exporters.get(t);
        }

        // Last attempt: match any bean whose name contains the key
        for (Map.Entry<String, Exporter> e : exporters.entrySet()) {
            if (e.getKey().toLowerCase(Locale.ROOT).contains(key)) return e.getValue();
        }

        throw new UnsupportedOperationException("Unsupported export type: " + type +
                ". Supported types: " + exporters.keySet());
    }
    
}

package com.group22.reportnow.export;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group22.reportnow.model.SessionSummary;

public class SessionHistoryLoader {
    public List<SessionSummary> loadHistory(File baseDir, String currentSessionId) {
        List<SessionSummary> history = new ArrayList<>();
        if (!baseDir.exists() || !baseDir.isDirectory()) return history;

        ObjectMapper mapper = new ObjectMapper();
        File[] files = baseDir.listFiles((dir, name) -> name.startsWith("reportnow-") && name.endsWith(".json"));
        if (files == null) return history;

        for (File file : files) {
            try {
                JsonNode root = mapper.readTree(file);
                JsonNode meta = root.get("session_metadata");
                if (meta != null) {
                    SessionSummary summary = mapper.treeToValue(meta, SessionSummary.class);
                    if (summary.getSessionId() != null && !summary.getSessionId().equals(currentSessionId)) {
                        history.add(summary);
                    }
                }
            } catch (Exception e) {
                System.err.println("[ReportNow] Failed to read history file " + file.getName() + ": " + e.getMessage());
            }
        }
        //sort newest first
        history.sort((a, b) -> Long.compare(b.getSessionStartMs(), a.getSessionStartMs()));
        return history;
    }
}
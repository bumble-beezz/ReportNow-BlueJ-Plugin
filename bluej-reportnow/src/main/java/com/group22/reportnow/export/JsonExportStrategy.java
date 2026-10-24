package com.group22.reportnow.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.group22.reportnow.repository.TelemetryRepository;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * serialises the entire session as a JSON document with metadata + events.
 * the polymorphic event hierarchy is handled by Jackson with @JsonSubTypes.
 */
public class JsonExportStrategy implements ExportStrategy {

    private final ObjectMapper mapper;
    private final String studentId; 

    public JsonExportStrategy(String studentId) {
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.studentId = studentId;
    }

    @Override
    public void export(TelemetryRepository repo, File file) throws ExportException {
        try {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("session_metadata", buildMetadata(repo));
            root.put("events",           repo.getEvents());
            mapper.writeValue(file, root);
        } catch (Exception e) {
            throw new ExportException("JSON export failed", e);
        }
    }

    private Map<String, Object> buildMetadata(TelemetryRepository repo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("student_id",              studentId); 
        m.put("session_id",              repo.getSessionId());
        m.put("session_start_ms",        repo.getSessionStartMs());
        m.put("session_duration_ms",     repo.getSessionDurationMs());
        m.put("total_compilations",      repo.getTotalCompilations());
        m.put("successful_compilations", repo.getSuccessfulCompilations());
        m.put("failed_compilations",     repo.getFailedCompilations());
        m.put("total_execution_attempts",repo.getTotalExecutions());
        m.put("successful_executions",   repo.getSuccessfulExecutions());
        m.put("failed_executions",       repo.getFailedExecutions());
        m.put("active_time_ms",          repo.getActiveTimeMs());
        m.put("idle_time_ms",            repo.getIdleTimeMs());
        return m;
    }
}
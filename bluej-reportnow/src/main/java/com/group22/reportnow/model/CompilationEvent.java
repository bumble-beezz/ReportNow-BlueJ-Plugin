package com.group22.reportnow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompilationEvent extends TelemetryEvent {

    @JsonProperty("project_name") private final String projectName;
    @JsonProperty("success")     private final boolean success;
    @JsonProperty("duration_ms") private final long    durationMs;
    @JsonProperty("errors")      private final List<CompilationError> errors;

    public CompilationEvent(long timestampMs, String projectName,
                            boolean success, long durationMs,
                            List<CompilationError> errors) {
        super(timestampMs);
        this.projectName = projectName;
        this.success    = success;
        this.durationMs = durationMs;
        this.errors     = errors == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(errors));
    }

    @Override public String getType() { return "COMPILATION"; }

    public String getProjectName() { return projectName; }
    public boolean isSuccess()           { return success; }
    public long    getDurationMs()       { return durationMs; }
    public List<CompilationError> getErrors() { return errors; }
    public int     getErrorCount()       { return errors.size(); }
}
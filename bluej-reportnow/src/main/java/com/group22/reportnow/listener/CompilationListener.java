package com.group22.reportnow.listener;

import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.CompileListener;
import com.group22.reportnow.factory.TelemetryEventFactory;
import com.group22.reportnow.model.CompilationError;
import com.group22.reportnow.parser.CompilationErrorParser;
import com.group22.reportnow.repository.TelemetryRepository;
import java.util.ArrayList;
import java.util.List;

public class CompilationListener implements CompileListener {

    private final TelemetryRepository repository;
    private final CompilationErrorParser errorParser;
    private final List<CompilationError> pendingErrors = new ArrayList<>();
    private long startTimeMs = 0L;

    public CompilationListener(TelemetryRepository repository) {
        this.repository = repository;
        this.errorParser = new CompilationErrorParser();
    }

    @Override
    public void compileStarted(CompileEvent event) {
        startTimeMs = System.currentTimeMillis();
        pendingErrors.clear();
    }

    @Override
    public void compileError(CompileEvent event) {
        addCompilationMessage(event, "ERROR");
    }

    @Override
    public void compileWarning(CompileEvent event) {
        addCompilationMessage(event, "WARNING");
    }

    @Override
    public void compileSucceeded(CompileEvent event) {
        long duration = System.currentTimeMillis() - startTimeMs;
        repository.addEvent(TelemetryEventFactory.createCompilationEvent(
                repository.getPrimaryProjectName(), true, duration, new ArrayList<>()));
        pendingErrors.clear();
    }

    @Override
    public void compileFailed(CompileEvent event) {
        long duration = System.currentTimeMillis() - startTimeMs;
        repository.addEvent(TelemetryEventFactory.createCompilationEvent(
                repository.getPrimaryProjectName(), false, duration, new ArrayList<>(pendingErrors)));
        pendingErrors.clear();
    }

    private void addCompilationMessage(CompileEvent event, String severity) {
        String rawMsg = event.getErrorMessage();
        List<CompilationError> parsed = errorParser.parse(rawMsg);
        
        if (!parsed.isEmpty()) {
            pendingErrors.addAll(parsed);
        } else {
            pendingErrors.add(TelemetryEventFactory.createCompilationError(
                    event.getFiles().length > 0 ? event.getFiles()[0].getName() : "unknown",
                    event.getErrorLineNumber(),
                    severity,
                    rawMsg));
        }
    }
}
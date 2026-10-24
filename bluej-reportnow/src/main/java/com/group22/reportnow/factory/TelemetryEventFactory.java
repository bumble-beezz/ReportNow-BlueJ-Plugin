package com.group22.reportnow.factory;

import com.group22.reportnow.model.CompilationError;
import com.group22.reportnow.model.CompilationEvent;
import com.group22.reportnow.model.ExecutionEvent;
import java.util.List;

/**
 * one area for creation of telemetry events so that
 * listeners only gather data and never build concrete events directly.
 *
 * tests can mock or inspect the factory instead of every event
 * constructor, and event construction can evolve independently of listeners.
 */
public final class TelemetryEventFactory {

    private TelemetryEventFactory() { /* utility class */ }

    public static CompilationEvent createCompilationEvent(boolean success,
                                                          long durationMs,
                                                          List<CompilationError> errors) {
        return new CompilationEvent(System.currentTimeMillis(), success, durationMs, errors);
    }

    public static ExecutionEvent createExecutionEvent(boolean success,
                                                      long durationMs,
                                                      String runtimeException,
                                                      String consoleOutput) {
        return new ExecutionEvent(System.currentTimeMillis(), success, durationMs, runtimeException, consoleOutput);
    }

    public static CompilationError createCompilationError(String fileName,
                                                          int lineNumber,
                                                          String errorCode,
                                                          String message) {
        return new CompilationError(fileName, lineNumber, errorCode, message);
    }
}
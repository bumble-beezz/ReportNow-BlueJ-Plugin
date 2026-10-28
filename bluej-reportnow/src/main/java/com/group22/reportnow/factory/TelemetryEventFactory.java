package com.group22.reportnow.factory;

import com.group22.reportnow.model.CompilationError;
import com.group22.reportnow.model.CompilationEvent;
import com.group22.reportnow.model.ExecutionEvent;
import com.group22.reportnow.model.ClassEvent;
import com.group22.reportnow.model.ProjectEvent;
import java.util.List;

public final class TelemetryEventFactory {

    private TelemetryEventFactory() { }

    public static CompilationEvent createCompilationEvent(String projectName, boolean success,
                                                          long durationMs, List<CompilationError> errors) {
        return new CompilationEvent(System.currentTimeMillis(), projectName, success, durationMs, errors);
    }

    public static ExecutionEvent createExecutionEvent(String projectName, boolean success,
                                                      long durationMs, String runtimeException,
                                                      String consoleOutput) {
        return new ExecutionEvent(System.currentTimeMillis(), projectName, success, durationMs, runtimeException, consoleOutput);
    }

    public static CompilationError createCompilationError(String fileName, int lineNumber,
                                                          String errorCode, String message) {
        return new CompilationError(fileName, lineNumber, errorCode, message);
    }

    public static ClassEvent createClassEvent(String action, String className, String projectName) {
        return new ClassEvent(System.currentTimeMillis(), action, className, projectName);
    }

    public static ProjectEvent createProjectEvent(String action, String projectName) {
        return new ProjectEvent(System.currentTimeMillis(), action, projectName);
    }
}
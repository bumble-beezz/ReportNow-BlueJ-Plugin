package com.group22.reportnow.repository;

import com.group22.reportnow.model.CompilationEvent;
import com.group22.reportnow.model.ExecutionEvent;
import com.group22.reportnow.model.TelemetryEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * memory storage of telemetry events for the current session.
 *
 * NOT a Singleton. The extension creates exactly one instance
 *  and passes it to each listener.
 */
public class TelemetryRepository {

    private final String sessionId;
    private final long   sessionStartMs;
    private final List<TelemetryEvent> events = new ArrayList<>();
    private long sessionEndMs = 0L;

    public TelemetryRepository() {
        this.sessionId      = UUID.randomUUID().toString();
        this.sessionStartMs = System.currentTimeMillis();
    }

    public synchronized void addEvent(TelemetryEvent event) {
        if (event != null) events.add(event);
    }

    public synchronized List<TelemetryEvent> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public synchronized List<CompilationEvent> getCompilationEvents() {
        return events.stream()
                .filter(CompilationEvent.class::isInstance)
                .map(CompilationEvent.class::cast)
                .collect(Collectors.toList());
    }

    public synchronized List<ExecutionEvent> getExecutionEvents() {
        return events.stream()
                .filter(ExecutionEvent.class::isInstance)
                .map(ExecutionEvent.class::cast)
                .collect(Collectors.toList());
    }

    public String getSessionId()        { return sessionId; }
    public long   getSessionStartMs()   { return sessionStartMs; }
    public synchronized long getSessionEndMs() { return sessionEndMs; }
    public synchronized void endSession()      { this.sessionEndMs = System.currentTimeMillis(); }

    public synchronized long getSessionDurationMs() {
        long end = sessionEndMs > 0 ? sessionEndMs : System.currentTimeMillis();
        return end - sessionStartMs;
    }

    //stats used by JSON/PDF

    public synchronized int getTotalCompilations()      { return getCompilationEvents().size(); }
    public synchronized int getSuccessfulCompilations() {
        return (int) getCompilationEvents().stream().filter(CompilationEvent::isSuccess).count();
    }
    public synchronized int getFailedCompilations() {
        return getTotalCompilations() - getSuccessfulCompilations();
    }

    public synchronized int getTotalExecutions()   { return getExecutionEvents().size(); }
    public synchronized int getSuccessfulExecutions() {
        return (int) getExecutionEvents().stream().filter(ExecutionEvent::isSuccess).count();
    }
    public synchronized int getFailedExecutions() {
        return getTotalExecutions() - getSuccessfulExecutions();
    }

    /**
     * proxy for "active coding time": sum of time spent compiling + running.
     * TODO: replace with real editor-focus telemetry when the
     * EditorListener is created.
     */
    public synchronized long getActiveTimeMs() {
        long total = 0L;
        for (TelemetryEvent e : events) {
            if (e instanceof CompilationEvent ce)      total += ce.getDurationMs();
            else if (e instanceof ExecutionEvent ee)   total += ee.getDurationMs();
        }
        return total;
    }

    /** time in the session not spent compiling or running a program. */
    public synchronized long getIdleTimeMs() {
        return Math.max(0L, getSessionDurationMs() - getActiveTimeMs());
    }
}
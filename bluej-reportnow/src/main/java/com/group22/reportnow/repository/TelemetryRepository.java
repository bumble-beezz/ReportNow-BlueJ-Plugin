package com.group22.reportnow.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.group22.reportnow.listener.EditorInteractionTracker;
import com.group22.reportnow.model.ClassEvent;
import com.group22.reportnow.model.CompilationEvent;
import com.group22.reportnow.model.ExecutionEvent;
import com.group22.reportnow.model.ProjectEvent;
import com.group22.reportnow.model.SessionSummary;
import com.group22.reportnow.model.TelemetryEvent;

public class TelemetryRepository {

    private final String sessionId;
    private final long   sessionStartMs;
    private final List<TelemetryEvent> events = new ArrayList<>();
    private long sessionEndMs = 0L;
    
    private EditorInteractionTracker editorTracker;
    private List<SessionSummary> pastSessions = new ArrayList<>();

    public TelemetryRepository() {
        this.sessionId      = UUID.randomUUID().toString();
        this.sessionStartMs = System.currentTimeMillis();
    }

    public synchronized void setEditorTracker(EditorInteractionTracker tracker) {
        this.editorTracker = tracker;
    }

    public synchronized void setPastSessions(List<SessionSummary> history) {
        this.pastSessions = history;
    }

    public synchronized List<SessionSummary> getPastSessions() {
        return pastSessions;
    }

    public synchronized List<ProjectEvent> getProjectEvents(){
        return events.stream().filter(ProjectEvent.class::isInstance).map(ProjectEvent.class::cast).collect(Collectors.toList());
    }

    public synchronized List<ClassEvent> getClassEvents(){
        return events.stream().filter(ClassEvent.class::isInstance).map(ClassEvent.class::cast).collect(Collectors.toList());
    }

    public synchronized void addEvent(TelemetryEvent event) {
        if (event != null) events.add(event);
    }

    public synchronized List<TelemetryEvent> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public synchronized List<CompilationEvent> getCompilationEvents() {
        return events.stream().filter(CompilationEvent.class::isInstance).map(CompilationEvent.class::cast).collect(Collectors.toList());
    }

    public synchronized List<ExecutionEvent> getExecutionEvents() {
        return events.stream().filter(ExecutionEvent.class::isInstance).map(ExecutionEvent.class::cast).collect(Collectors.toList());
    }

    public String getSessionId()        { return sessionId; }
    public long   getSessionStartMs()   { return sessionStartMs; }
    public synchronized long getSessionEndMs() { return sessionEndMs; }
    public synchronized void endSession()      { this.sessionEndMs = System.currentTimeMillis(); }

    public synchronized long getSessionDurationMs() {
        long end = sessionEndMs > 0 ? sessionEndMs : System.currentTimeMillis();
        return end - sessionStartMs;
    }

    public synchronized int getTotalCompilations()      { return getCompilationEvents().size(); }
    public synchronized int getSuccessfulCompilations() { return (int) getCompilationEvents().stream().filter(CompilationEvent::isSuccess).count(); }
    public synchronized int getFailedCompilations()     { return getTotalCompilations() - getSuccessfulCompilations(); }
    public synchronized int getTotalExecutions()        { return getExecutionEvents().size(); }
    public synchronized int getSuccessfulExecutions()   { return (int) getExecutionEvents().stream().filter(ExecutionEvent::isSuccess).count(); }
    public synchronized int getFailedExecutions()       { return getTotalExecutions() - getSuccessfulExecutions(); }

    //integrates editor tracker for active time
    public synchronized long getActiveTimeMs() {
        if (editorTracker != null && editorTracker.getEditCount() > 1) {
            long active = 0;
            for (long interval : editorTracker.getEditIntervals()) {
                if (interval < 180_000) { //less than 3 minutes is considered active typing
                    active += interval;
                }
            }
            if (active > 0) return active;
        }
        
        //fallback to compile+exec time if no typing is recorded
        long total = 0L;
        for (TelemetryEvent e : events) {
            if (e instanceof CompilationEvent ce)      total += ce.getDurationMs();
            else if (e instanceof ExecutionEvent ee)   total += ee.getDurationMs();
        }
        return total;
    }

    public synchronized long getIdleTimeMs() {
        return Math.max(0L, getSessionDurationMs() - getActiveTimeMs());
    }

    public synchronized String getPrimaryProjectName() {
        String latestProject = "UnknownProject";
        for (TelemetryEvent e : events) {
            if (e instanceof ProjectEvent pe) {
                if ("ProjectOpened".equals(pe.getAction()) || "ProjectCreated".equals(pe.getAction())) {
                    latestProject = pe.getProjectName(); // Keeps updating to the newest one
                }
            }
        }
        return latestProject;
    }
}
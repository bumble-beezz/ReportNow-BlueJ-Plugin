package com.group22.reportnow.listener;

import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.InvocationFinishedListener;
import com.group22.reportnow.factory.TelemetryEventFactory;
import com.group22.reportnow.repository.TelemetryRepository;

public class ExecutionListener implements InvocationFinishedListener {

    private final TelemetryRepository repository;
    private long startTimeMs = System.currentTimeMillis();

    public ExecutionListener(TelemetryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void invocationFinished(InvocationFinishedEvent event) {
        long duration = System.currentTimeMillis() - startTimeMs;

        InvocationFinishedEvent.EventType type = event.getEventType();
        // TODO: Capture console output here in the future if BlueJ exposes a getter
        String consoleOutput = null; 

        switch (type) {
            case NORMAL_EXIT:
                repository.addEvent(TelemetryEventFactory.createExecutionEvent(
                        true, duration, null, consoleOutput));
                break;

            case EXCEPTION_EXIT:
                repository.addEvent(TelemetryEventFactory.createExecutionEvent(
                        false, duration, "Runtime exception", consoleOutput));
                break;

            case TERMINATED_EXIT:
            case UNKNOWN_EXIT:
            default:
                repository.addEvent(TelemetryEventFactory.createExecutionEvent(
                        false, duration, type.name(), consoleOutput));
                break;
        }

        // reset for the next run
        startTimeMs = System.currentTimeMillis();
    }
}
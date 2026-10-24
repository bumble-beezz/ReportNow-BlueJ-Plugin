package com.group22.reportnow.listener;

import bluej.extensions2.event.InvocationFinishedEvent;
import com.group22.reportnow.model.ExecutionEvent;
import com.group22.reportnow.repository.TelemetryRepository;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ExecutionListenerTest {

    private TelemetryRepository repo;
    private ExecutionListener listener;

    @BeforeEach
    void setUp() {
        repo = new TelemetryRepository();
        listener = new ExecutionListener(repo);
    }

    private InvocationFinishedEvent event(InvocationFinishedEvent.EventType type) {
        InvocationFinishedEvent e = mock(InvocationFinishedEvent.class);
        when(e.getEventType()).thenReturn(type);
        return e;
    }

    @Test
    void normalExitIsRecordedAsSuccess() {
        listener.invocationFinished(event(InvocationFinishedEvent.EventType.NORMAL_EXIT));

        List<ExecutionEvent> execs = repo.getExecutionEvents();
        assertEquals(1, execs.size());
        assertTrue(execs.get(0).isSuccess());
    }

    @Test
    void exceptionExitIsRecordedAsFailure() {
        listener.invocationFinished(event(InvocationFinishedEvent.EventType.EXCEPTION_EXIT));

        List<ExecutionEvent> execs = repo.getExecutionEvents();
        assertEquals(1, execs.size());
        assertFalse(execs.get(0).isSuccess());
        assertNotNull(execs.get(0).getRuntimeException());
    }

    @Test
    void terminatedExitIsRecordedAsFailure() {
        listener.invocationFinished(event(InvocationFinishedEvent.EventType.TERMINATED_EXIT));

        List<ExecutionEvent> execs = repo.getExecutionEvents();
        assertEquals(1, execs.size());
        assertFalse(execs.get(0).isSuccess());
    }

    @Test
    void unknownExitIsRecordedAsFailure() {
        listener.invocationFinished(event(InvocationFinishedEvent.EventType.UNKNOWN_EXIT));

        List<ExecutionEvent> execs = repo.getExecutionEvents();
        assertEquals(1, execs.size());
        assertFalse(execs.get(0).isSuccess());
    }

    @Test
    void multipleRunsAreRecorded() {
        listener.invocationFinished(event(InvocationFinishedEvent.EventType.NORMAL_EXIT));
        listener.invocationFinished(event(InvocationFinishedEvent.EventType.EXCEPTION_EXIT));

        assertEquals(2, repo.getTotalExecutions());
        assertEquals(1, repo.getSuccessfulExecutions());
        assertEquals(1, repo.getFailedExecutions());
    }
}
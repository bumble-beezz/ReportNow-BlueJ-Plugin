package com.group22.reportnow.repository;

import com.group22.reportnow.factory.TelemetryEventFactory;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TelemetryRepositoryTest {

    private TelemetryRepository repo;

    @BeforeEach
    void setUp() {
        repo = new TelemetryRepository();
    }

    @Test
    void newSessionHasUuidAndZeroCounts() {
        assertNotNull(repo.getSessionId());
        assertFalse(repo.getSessionId().isBlank());
        assertEquals(0, repo.getTotalCompilations());
        assertEquals(0, repo.getTotalExecutions());
        assertEquals(0, repo.getActiveTimeMs());
    }

    @Test
    void tracksCompilationStatistics() {
        repo.addEvent(TelemetryEventFactory.createCompilationEvent(true, 100, List.of()));
        repo.addEvent(TelemetryEventFactory.createCompilationEvent(false, 200, List.of()));
        repo.addEvent(TelemetryEventFactory.createCompilationEvent(true, 50, List.of()));

        assertEquals(3, repo.getTotalCompilations());
        assertEquals(2, repo.getSuccessfulCompilations());
        assertEquals(1, repo.getFailedCompilations());
    }

    @Test
    void tracksExecutionStatistics() {
        repo.addEvent(TelemetryEventFactory.createExecutionEvent(true, 100, null, null));
        repo.addEvent(TelemetryEventFactory.createExecutionEvent(false, 150, "NullPointerException", null));

        assertEquals(2, repo.getTotalExecutions());
        assertEquals(1, repo.getSuccessfulExecutions());
        assertEquals(1, repo.getFailedExecutions());
    }

    @Test
    void activeTimeSumsCompileAndExecDurations() {
        repo.addEvent(TelemetryEventFactory.createCompilationEvent(true, 100, List.of()));
        repo.addEvent(TelemetryEventFactory.createCompilationEvent(false, 200, List.of()));
        repo.addEvent(TelemetryEventFactory.createExecutionEvent(true, 50, null, null));

        assertEquals(350, repo.getActiveTimeMs());
    }

    @Test
    void idleTimeIsPositiveWhenSessionIsLongerThanActive() throws InterruptedException {
        repo.addEvent(TelemetryEventFactory.createCompilationEvent(true, 5, List.of()));
        Thread.sleep(50);
        repo.endSession();
        assertTrue(repo.getIdleTimeMs() > 0);
    }

    @Test
    void idleTimeIsClampedToZeroWhenActiveExceedsDuration() {
        repo.addEvent(TelemetryEventFactory.createCompilationEvent(true, 10_000, List.of()));
        repo.endSession();
        assertEquals(0, repo.getIdleTimeMs());
    }

    @Test
    void nullEventsAreIgnored() {
        repo.addEvent(null);
        assertEquals(0, repo.getEvents().size());
    }

    @Test
    void getEventsReturnsImmutableCopy() {
        repo.addEvent(TelemetryEventFactory.createExecutionEvent(true, 0, null, null));
        List<?> snapshot = repo.getEvents();
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
    }

    @Test
    void endSessionFreezesDuration() {
        repo.endSession();
        long first = repo.getSessionDurationMs();
        long second = repo.getSessionDurationMs();
        assertEquals(first, second);
    }
}
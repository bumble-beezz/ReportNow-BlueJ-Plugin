package com.group22.reportnow.listener;

import bluej.extensions2.event.CompileEvent;
import com.group22.reportnow.model.CompilationEvent;
import com.group22.reportnow.repository.TelemetryRepository;
import java.io.File;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class CompilationListenerTest {

    private TelemetryRepository repo;
    private CompilationListener listener;
    private File realFile;

    @BeforeEach
    void setUp() {
        repo = new TelemetryRepository();
        listener = new CompilationListener(repo);
        realFile = new File("Foo.java");
    }

    private CompileEvent errorEvent(String message, int lineNumber) {
        CompileEvent e = mock(CompileEvent.class);
        when(e.getFiles()).thenReturn(new File[]{realFile});
        when(e.getErrorMessage()).thenReturn(message);
        when(e.getErrorLineNumber()).thenReturn(lineNumber);
        return e;
    }

    @Test
    void successfulCompilationIsRecorded() {
        listener.compileStarted(mock(CompileEvent.class));
        listener.compileSucceeded(mock(CompileEvent.class));

        List<CompilationEvent> compiles = repo.getCompilationEvents();
        assertEquals(1, compiles.size());
        assertTrue(compiles.get(0).isSuccess());
        assertTrue(compiles.get(0).getErrors().isEmpty());
    }

    @Test
    void failedCompilationWithOneError() {
        listener.compileStarted(mock(CompileEvent.class));
        listener.compileError(errorEvent("cannot find symbol", 15));
        listener.compileFailed(mock(CompileEvent.class));

        List<CompilationEvent> compiles = repo.getCompilationEvents();
        assertEquals(1, compiles.size());
        CompilationEvent ce = compiles.get(0);
        assertFalse(ce.isSuccess());
        assertEquals(1, ce.getErrors().size());
        assertEquals("Foo.java", ce.getErrors().get(0).getFileName());
        assertEquals(15, ce.getErrors().get(0).getLineNumber());
        assertEquals("ERROR", ce.getErrors().get(0).getErrorCode());
        assertEquals("cannot find symbol", ce.getErrors().get(0).getMessage());
    }

    @Test
    void failedCompilationWithMultipleErrors() {
        listener.compileStarted(mock(CompileEvent.class));
        listener.compileError(errorEvent("cannot find symbol", 15));
        listener.compileError(errorEvent("incompatible types", 42));
        listener.compileError(errorEvent("missing return statement", 88));
        listener.compileFailed(mock(CompileEvent.class));

        CompilationEvent ce = repo.getCompilationEvents().get(0);
        assertFalse(ce.isSuccess());
        assertEquals(3, ce.getErrors().size());
    }

    @Test
    void errorsAreClearedBetweenCompilations() {
        listener.compileStarted(mock(CompileEvent.class));
        listener.compileError(errorEvent("boom", 1));
        listener.compileFailed(mock(CompileEvent.class));

        listener.compileStarted(mock(CompileEvent.class));
        listener.compileSucceeded(mock(CompileEvent.class));

        List<CompilationEvent> compiles = repo.getCompilationEvents();
        assertEquals(2, compiles.size());
        assertEquals(1, compiles.get(0).getErrors().size());
        assertEquals(0, compiles.get(1).getErrors().size());
    }

    @Test
    void handlesEmptyFileArrayGracefully() {
        CompileEvent e = mock(CompileEvent.class);
        when(e.getFiles()).thenReturn(new File[0]);
        when(e.getErrorMessage()).thenReturn("generic error");
        when(e.getErrorLineNumber()).thenReturn(0);

        listener.compileStarted(mock(CompileEvent.class));
        listener.compileError(e);
        listener.compileFailed(mock(CompileEvent.class));

        CompilationEvent ce = repo.getCompilationEvents().get(0);
        assertEquals(1, ce.getErrors().size());
        assertEquals("unknown", ce.getErrors().get(0).getFileName());
    }
}
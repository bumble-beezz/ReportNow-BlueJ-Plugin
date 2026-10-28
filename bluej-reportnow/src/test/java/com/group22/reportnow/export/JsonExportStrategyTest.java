package com.group22.reportnow.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group22.reportnow.factory.TelemetryEventFactory;
import com.group22.reportnow.model.CompilationError;
import com.group22.reportnow.model.SessionSummary;
import com.group22.reportnow.repository.TelemetryRepository;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonExportStrategyTest {

    @TempDir
    Path tempDir;

    @Test
    void exportProducesValidJsonWithMetadata() throws Exception {
        TelemetryRepository repo = new TelemetryRepository();
        repo.addEvent(TelemetryEventFactory.createCompilationEvent(true, 100, List.of()));
        repo.addEvent(TelemetryEventFactory.createCompilationEvent(false, 200,
                List.of(new CompilationError("Foo.java", 15, "ERROR", "cannot find symbol"))));
        repo.addEvent(TelemetryEventFactory.createExecutionEvent(true, 50, null, null));
        repo.endSession();

        SessionSummary past = new SessionSummary("TEST_STUDENT", "TestProject", "past-session-id", 1000L, 500L, 1, 1, 0, 1, 300L, 200L);
        repo.setPastSessions(List.of(past));

        File out = tempDir.resolve("session.json").toFile();
        new JsonExportStrategy("TEST_STUDENT").export(repo, out);

        assertTrue(out.exists());
        assertTrue(out.length() > 0);

        JsonNode root = new ObjectMapper().readTree(out);
        JsonNode meta = root.get("session_metadata");
        assertNotNull(meta);
        assertEquals(repo.getSessionId(), meta.get("session_id").asText());
        assertEquals(2, meta.get("total_compilations").asInt());
        assertEquals(1, meta.get("successful_compilations").asInt());
        assertEquals(1, meta.get("failed_compilations").asInt());
        assertEquals(1, meta.get("total_execution_attempts").asInt());
        assertEquals("TEST_STUDENT", meta.get("student_id").asText());

        JsonNode events = root.get("events");
        assertNotNull(events);
        assertEquals(3, events.size());

        JsonNode pastSessions = root.get("past_sessions");
        assertNotNull(pastSessions);
        assertEquals(1, pastSessions.size());
        assertEquals("past-session-id", pastSessions.get(0).get("session_id").asText());
    }

    @Test
    void compilationEventsIncludeErrorsArray() throws Exception {
        TelemetryRepository repo = new TelemetryRepository();
        repo.addEvent(TelemetryEventFactory.createCompilationEvent(false, 200,
                List.of(
                        new CompilationError("A.java", 5, "ERROR", "boom"),
                        new CompilationError("B.java", 10, "WARNING", "unused"))));

        File out = tempDir.resolve("errors.json").toFile();
        new JsonExportStrategy("TEST_STUDENT").export(repo, out);

        JsonNode compile = new ObjectMapper().readTree(out).get("events").get(0);
        assertNotNull(compile.get("event_type"));
        assertFalse(compile.get("success").asBoolean());
        JsonNode errs = compile.get("errors");
        assertEquals(2, errs.size());
        assertEquals("A.java", errs.get(0).get("file_name").asText());
        assertEquals(5, errs.get(0).get("line_number").asInt());
        assertEquals("ERROR", errs.get(0).get("error_code").asText());
    }

    @Test
    void executionEventsIncludeExceptionField() throws Exception {
        TelemetryRepository repo = new TelemetryRepository();
        repo.addEvent(TelemetryEventFactory.createExecutionEvent(false, 150,
                "java.lang.NullPointerException: boom", null));

        File out = tempDir.resolve("exec.json").toFile();
        new JsonExportStrategy("TEST_STUDENT").export(repo, out);

        JsonNode exec = new ObjectMapper().readTree(out).get("events").get(0);
        assertNotNull(exec.get("event_type"));
        assertFalse(exec.get("success").asBoolean());
        assertTrue(exec.get("runtime_exception").asText().contains("NullPointerException"));
    }
}
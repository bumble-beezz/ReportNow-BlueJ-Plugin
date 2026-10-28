package com.group22.reportnow.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group22.reportnow.model.SessionSummary;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SessionHistoryLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsValidPastSessionsAndExcludesCurrentSession() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SessionHistoryLoader loader = new SessionHistoryLoader();

        //create a fake past session JSON file
        Map<String, Object> meta1 = new LinkedHashMap<>();
        meta1.put("student_id", "STUDENT");
        meta1.put("project_name", "TestProject");
        meta1.put("session_id", "past-session-1");
        meta1.put("session_start_ms", 1000L);
        meta1.put("session_duration_ms", 500L);
        meta1.put("total_compilations", 2);
        meta1.put("successful_compilations", 1);
        meta1.put("failed_compilations", 1);
        meta1.put("total_execution_attempts", 1);
        meta1.put("active_time_ms", 300L);
        meta1.put("idle_time_ms", 200L);

        Map<String, Object> root1 = new LinkedHashMap<>();
        root1.put("session_metadata", meta1);
        File file1 = tempDir.resolve("reportnow-111.json").toFile();
        mapper.writeValue(file1, root1);

        //create a fake current session JSON file (should be excluded)
        Map<String, Object> meta2 = new LinkedHashMap<>(meta1);
        meta2.put("session_id", "current-session");
        
        Map<String, Object> root2 = new LinkedHashMap<>();
        root2.put("session_metadata", meta2);
        File file2 = tempDir.resolve("reportnow-222.json").toFile();
        mapper.writeValue(file2, root2);

        //load and verify
        List<SessionSummary> history = loader.loadHistory(tempDir.toFile(), "current-session");

        assertEquals(1, history.size());
        assertEquals("past-session-1", history.get(0).getSessionId());
    }

    @Test
    void handlesInvalidJsonGracefully() throws Exception {
        SessionHistoryLoader loader = new SessionHistoryLoader();
        
        //create a corrupt file
        File corruptFile = tempDir.resolve("reportnow-corrupt.json").toFile();
        Files.writeString(corruptFile.toPath(), "{ invalid json: ");

        List<SessionSummary> history = loader.loadHistory(tempDir.toFile(), "current-session");

        //should not throw an exception, just return empty list
        assertTrue(history.isEmpty());
    }

    @Test
    void handlesMissingDirectoryGracefully() {
        SessionHistoryLoader loader = new SessionHistoryLoader();
        File nonExistentDir = new File(tempDir.toFile(), "does-not-exist");

        List<SessionSummary> history = loader.loadHistory(nonExistentDir, "current-session");

        assertTrue(history.isEmpty());
    }
}
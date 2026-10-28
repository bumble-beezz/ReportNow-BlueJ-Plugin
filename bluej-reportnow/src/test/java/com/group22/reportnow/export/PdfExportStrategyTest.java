package com.group22.reportnow.export;

import com.group22.reportnow.factory.TelemetryEventFactory;
import com.group22.reportnow.repository.TelemetryRepository;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfExportStrategyTest {

    @TempDir
    Path tempDir;

    @Test
    void exportProducesNonEmptyPdfFile() throws Exception {
        TelemetryRepository repo = new TelemetryRepository();
        repo.addEvent(TelemetryEventFactory.createCompilationEvent(true, 100, List.of()));
        repo.addEvent(TelemetryEventFactory.createExecutionEvent(true, 50, null, null));
        repo.endSession();

        File out = tempDir.resolve("report.pdf").toFile();
        new PdfExportStrategy("TEST_STUDENT", "TestProject").export(repo, out);

        assertTrue(out.exists());
        assertTrue(out.length() > 0);
    }

    @Test
    void pdfStartsWithMagicBytes() throws Exception {
        TelemetryRepository repo = new TelemetryRepository();
        repo.endSession();

        File out = tempDir.resolve("empty.pdf").toFile();
        new PdfExportStrategy("TEST_STUDENT", "TestProject").export(repo, out);

        byte[] bytes = Files.readAllBytes(out.toPath());
        assertTrue(bytes.length >= 4);
        assertEquals('%', bytes[0]);
        assertEquals('P', bytes[1]);
        assertEquals('D', bytes[2]);
        assertEquals('F', bytes[3]);
    }

    @Test
    void exportHandlesEmptyRepository() {
        TelemetryRepository repo = new TelemetryRepository();
        repo.endSession();
        File out = tempDir.resolve("empty-session.pdf").toFile();
        assertDoesNotThrow(() -> new PdfExportStrategy("TEST_STUDENT", "TestProject").export(repo, out));
        assertTrue(out.length() > 0);
    }
}
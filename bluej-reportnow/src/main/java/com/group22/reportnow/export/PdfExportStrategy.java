package com.group22.reportnow.export;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Instant;
import java.util.List;

import com.group22.reportnow.model.ClassEvent;
import com.group22.reportnow.model.CompilationError;
import com.group22.reportnow.model.CompilationEvent;
import com.group22.reportnow.model.ExecutionEvent;
import com.group22.reportnow.model.ProjectEvent;
import com.group22.reportnow.model.SessionSummary;
import com.group22.reportnow.model.TelemetryEvent;
import com.group22.reportnow.repository.TelemetryRepository;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

public class PdfExportStrategy implements ExportStrategy {

    private final String studentId;

    public PdfExportStrategy(String studentId) {
        this.studentId = studentId;
    }

    @Override
    public void export(TelemetryRepository repo, File file) throws ExportException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter writer = PdfWriter.getInstance(doc, fos);
            doc.open();

            addTitle(doc, repo);
            addSummary(doc, repo);

            PdfPTable chartTable = new PdfPTable(2);
            chartTable.setWidthPercentage(100);
            chartTable.setSpacingBefore(10f);
            chartTable.setSpacingAfter(10f);

            chartTable.addCell(new PdfPCell(renderBarChart(writer, repo), false));
            chartTable.addCell(new PdfPCell(renderPieChart(writer, repo), false));

            doc.add(chartTable);
            addCompilationTable(doc, repo);

            doc.newPage();
            addActivityTimeline(doc, repo);
            addExecutionTable(doc, repo);
            
            //add past Sessions to Page 3+
            addHistoryTable(doc, repo);

            doc.close();
        } catch (Exception e) {
            throw new ExportException("PDF export failed", e);
        }
    }

    private void addTitle(Document doc, TelemetryRepository repo) throws DocumentException {
        Paragraph title = new Paragraph("ReportNow — Session Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(60, 90, 150)));
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        Paragraph sub = new Paragraph(
                "Student ID: " + studentId + "   |   Session: " + repo.getSessionId() + "   |   Started: " + Instant.ofEpochMilli(repo.getSessionStartMs()),
                FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY));
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(15f);
        doc.add(sub);
    }

    private void addSummary(Document doc, TelemetryRepository repo) throws DocumentException {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        addSummaryRow(t, "Session duration", String.format("%.1f s", repo.getSessionDurationMs() / 1000.0), "Active time", String.format("%.1f s", repo.getActiveTimeMs() / 1000.0));
        addSummaryRow(t, "Idle time", String.format("%.1f s", repo.getIdleTimeMs() / 1000.0), "Total compilations", String.valueOf(repo.getTotalCompilations()));
        addSummaryRow(t, "Successful compiles", String.valueOf(repo.getSuccessfulCompilations()), "Failed compiles", String.valueOf(repo.getFailedCompilations()));
        addSummaryRow(t, "Total executions", String.valueOf(repo.getTotalExecutions()), "", "");
        doc.add(t);
    }

    private Image renderBarChart(PdfWriter writer, TelemetryRepository repo) throws Exception {
        int ok = repo.getSuccessfulCompilations();
        int fail = repo.getFailedCompilations();
        int total = Math.max(1, ok + fail);

        final int W = 220, H = 130;
        PdfTemplate template = writer.getDirectContent().createTemplate(W, H);
        Graphics2D g2 = template.createGraphics(W, H);

        int barW = 40, baseY = 30, maxH = 70;
        int okH = (int) Math.round(maxH * ((double) ok / total));
        int failH = (int) Math.round(maxH * ((double) fail / total));

        g2.setColor(new Color(46, 160, 67)); g2.fillRect(40, baseY, barW, okH);
        g2.setColor(new Color(200, 60, 60)); g2.fillRect(140, baseY, barW, failH);

        g2.setColor(Color.BLACK);
        g2.drawString("" + ok, 40 + barW / 2 - 6, baseY + okH + 12);
        g2.drawString("" + fail, 140 + barW / 2 - 6, baseY + failH + 12);
        g2.drawString("Successful", 30, H - 5);
        g2.drawString("Failed", 140, H - 5);
        g2.drawString("Compilation Results", 50, 15);
        g2.dispose();
        return Image.getInstance(template);
    }

    private Image renderPieChart(PdfWriter writer, TelemetryRepository repo) throws Exception {
        long active = repo.getActiveTimeMs();
        long idle = repo.getIdleTimeMs();
        long total = Math.max(1L, active + idle);

        final int W = 220, H = 130;
        PdfTemplate template = writer.getDirectContent().createTemplate(W, H);
        Graphics2D g2 = template.createGraphics(W, H);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        int diameter = 70, x = 10, y = 30;
        int activeAngle = (int) Math.round(360.0 * ((double) active / total));

        g2.setColor(new Color(66, 133, 244)); g2.fillArc(x, y, diameter, diameter, 90, -activeAngle);
        g2.setColor(new Color(200, 200, 200)); g2.fillArc(x, y, diameter, diameter, 90 - activeAngle, -(360 - activeAngle));

        g2.setColor(Color.BLACK); g2.setStroke(new BasicStroke(1f)); g2.drawOval(x, y, diameter, diameter);
        g2.setColor(new Color(66, 133, 244)); g2.fillRect(100, 70, 10, 10);
        g2.setColor(Color.BLACK); g2.drawString(String.format("Active: %.1f s", active / 1000.0), 115, 80);

        g2.setColor(new Color(200, 200, 200)); g2.fillRect(100, 50, 10, 10);
        g2.setColor(Color.BLACK); g2.drawString(String.format("Idle:   %.1f s", idle / 1000.0), 115, 60);
        g2.dispose();
        return Image.getInstance(template);
    }

    private void addCompilationTable(Document doc, TelemetryRepository repo) throws DocumentException {
        doc.add(new Paragraph("Compilation Summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(60, 90, 150))));
        doc.add(Chunk.NEWLINE);

        PdfPTable t = new PdfPTable(new float[]{3f, 2f, 2f, 2f, 5f});
        t.setWidthPercentage(100);
        header(t, "Timestamp", "Result", "Duration", "# Errors", "First error");

        boolean alternate = false;
        for (CompilationEvent e : repo.getCompilationEvents()) {
            Color bg = alternate ? new Color(245, 245, 250) : Color.WHITE;
            addCell(t, Instant.ofEpochMilli(e.getTimestampMs()).toString(), bg);
            addCell(t, e.isSuccess() ? "SUCCESS" : "FAILED", bg);
            addCell(t, e.getDurationMs() + " ms", bg);
            addCell(t, String.valueOf(e.getErrorCount()), bg);
            String detail = e.getErrors().isEmpty() ? "-" : e.getErrors().get(0).getFileName() + ":" + e.getErrors().get(0).getLineNumber() + " — " + e.getErrors().get(0).getMessage();
            addCell(t, detail, bg);
            alternate = !alternate;
        }
        doc.add(t);
    }

    private void addActivityTimeline(Document doc, TelemetryRepository repo) throws DocumentException {
        doc.add(new Paragraph("Activity Timeline", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(60, 90, 150))));
        doc.add(Chunk.NEWLINE);

        PdfPTable t = new PdfPTable(new float[]{3f, 2f, 6f});
        t.setWidthPercentage(100);
        header(t, "Time", "Type", "Detail");

        boolean alternate = false;
        for (TelemetryEvent e : repo.getEvents()) {
            //filter out ProjectModified spam to make the timeline useful
            if (e instanceof ProjectEvent pe && pe.getAction().equals("ProjectModified")) {
                continue;
            }

            Color bg = alternate ? new Color(245, 245, 250) : Color.WHITE;
            addCell(t, Instant.ofEpochMilli(e.getTimestampMs()).toString(), bg);
            addCell(t, getFriendlyType(e), bg);
            addCell(t, describe(e), bg);
            alternate = !alternate;
        }
        doc.add(t);
        doc.add(Chunk.NEWLINE);
    }

    private void addExecutionTable(Document doc, TelemetryRepository repo) throws DocumentException {
        doc.add(new Paragraph("Execution Summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(60, 90, 150))));
        doc.add(Chunk.NEWLINE);

        PdfPTable t = new PdfPTable(new float[]{3f, 2f, 2f, 5f});
        t.setWidthPercentage(100);
        header(t, "Timestamp", "Result", "Duration", "Exception / Output");

        boolean alternate = false;
        for (ExecutionEvent e : repo.getExecutionEvents()) {
            Color bg = alternate ? new Color(245, 245, 250) : Color.WHITE;
            addCell(t, Instant.ofEpochMilli(e.getTimestampMs()).toString(), bg);
            addCell(t, e.isSuccess() ? "OK" : "FAILED", bg);
            addCell(t, e.getDurationMs() + " ms", bg);
            String detail = "-";
            if (e.getRuntimeException() != null) detail = e.getRuntimeException();
            else if (e.getConsoleOutput() != null) detail = "Output: " + e.getConsoleOutput();
            addCell(t, detail, bg);
            alternate = !alternate;
        }
        doc.add(t);
    }

    private void addHistoryTable(Document doc, TelemetryRepository repo) throws DocumentException {
        List<SessionSummary> history = repo.getPastSessions();
        if (history.isEmpty()) return;

        doc.newPage();
        doc.add(new Paragraph("Past Sessions", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(60, 90, 150))));
        doc.add(Chunk.NEWLINE);

        PdfPTable t = new PdfPTable(new float[]{2f, 3f, 2f, 2f, 2f, 2f, 2f});
        t.setWidthPercentage(100);
        header(t, "Project", "Date/Time", "Duration", "Compiles", "Success", "Executions", "Active Time");

        boolean alternate = false;
        for (SessionSummary s : history) {
            Color bg = alternate ? new Color(245, 245, 250) : Color.WHITE;
            addCell(t, s.getProjectName(), bg); 
            addCell(t, Instant.ofEpochMilli(s.getSessionStartMs()).toString(), bg);
            addCell(t, String.format("%.1f s", s.getSessionDurationMs() / 1000.0), bg);
            addCell(t, String.valueOf(s.getTotalCompilations()), bg);
            addCell(t, String.valueOf(s.getSuccessfulCompilations()), bg);
            addCell(t, String.valueOf(s.getTotalExecutionAttempts()), bg);
            addCell(t, String.format("%.1f s", s.getActiveTimeMs() / 1000.0), bg);
            alternate = !alternate;
        }
        doc.add(t);
    }

    private String getFriendlyType(TelemetryEvent e) {
        if (e instanceof CompilationEvent) return "Compile";
        if (e instanceof ExecutionEvent) return "Run";
        if (e instanceof ProjectEvent) return "Project";
        if (e instanceof ClassEvent) return "Class";
        return e.getType();
    }

    private String describe(TelemetryEvent e) {
        if (e instanceof CompilationEvent ce) {
            if (ce.isSuccess()) return "Success (" + ce.getDurationMs() + " ms)";
            CompilationError first = ce.getErrors().isEmpty() ? null : ce.getErrors().get(0);
            return "Failed" + (first == null ? "" : " — " + first.getFileName() + ":" + first.getLineNumber());
        }
        if (e instanceof ExecutionEvent ee) {
            return ee.isSuccess() ? "Finished (" + ee.getDurationMs() + " ms)" : "Failed — " + ee.getRuntimeException();
        }
        if (e instanceof ProjectEvent pe) {
            return pe.getAction().replace("Project", "") + " — " + pe.getProjectName();
        }
        if (e instanceof ClassEvent ce) {
            String cleanAction = ce.getAction().replace("Class", "").replace("Project", ""); 
            return cleanAction + ": " + ce.getClassName();
        }
        return e.getType();
    }

    private void addSummaryRow(PdfPTable t, String k1, String v1, String k2, String v2) {
        t.addCell(createSummaryCell(k1, true)); t.addCell(createSummaryCell(v1, false));
        t.addCell(createSummaryCell(k2, true)); t.addCell(createSummaryCell(v2, false));
    }

    private PdfPCell createSummaryCell(String text, boolean isBold) {
        PdfPCell cell = new PdfPCell(new Phrase(text, isBold ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY) : FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cell.setBorder(Rectangle.NO_BORDER); cell.setPaddingBottom(6f); return cell;
    }

    private void header(PdfPTable t, String... cols) {
        for (String c : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(c, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
            cell.setBackgroundColor(new Color(60, 90, 150)); cell.setPadding(6f); t.addCell(cell);
        }
    }

    private void addCell(PdfPTable t, String s, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(s == null ? "" : s, FontFactory.getFont(FontFactory.HELVETICA, 8)));
        cell.setBackgroundColor(bg); cell.setPadding(5f); t.addCell(cell);
    }
}
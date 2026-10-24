package com.group22.reportnow.export;

import com.group22.reportnow.model.CompilationError;
import com.group22.reportnow.model.CompilationEvent;
import com.group22.reportnow.model.ExecutionEvent;
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
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Instant;

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

            //charts are side-by-side using a layout table to save vertical space
            PdfPTable chartTable = new PdfPTable(2);
            chartTable.setWidthPercentage(100);
            chartTable.setSpacingBefore(10f);
            chartTable.setSpacingAfter(10f);

            Image barChart = renderBarChart(writer, repo);
            Image pieChart = renderPieChart(writer, repo);

            PdfPCell cell1 = new PdfPCell(barChart, false);
            cell1.setBorder(Rectangle.NO_BORDER);
            cell1.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell cell2 = new PdfPCell(pieChart, false);
            cell2.setBorder(Rectangle.NO_BORDER);
            cell2.setHorizontalAlignment(Element.ALIGN_CENTER);

            chartTable.addCell(cell1);
            chartTable.addCell(cell2);

            doc.add(chartTable);
            addCompilationTable(doc, repo);

            //force exactly to page 2
            doc.newPage();
            
            addActivityTimeline(doc, repo);
            addExecutionTable(doc, repo);

            doc.close();
        } catch (Exception e) {
            throw new ExportException("PDF export failed", e);
        }
    }

    //sections

    private void addTitle(Document doc, TelemetryRepository repo) throws DocumentException {
        Paragraph title = new Paragraph("ReportNow — Session Report",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(60, 90, 150)));
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        Paragraph sub = new Paragraph(
                "Student ID: " + studentId + "   |   " +
                "Session: " + repo.getSessionId() + "   |   " +
                "Started: " + Instant.ofEpochMilli(repo.getSessionStartMs()),
                FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY));
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(15f);
        doc.add(sub);
    }

    private void addSummary(Document doc, TelemetryRepository repo) throws DocumentException {
        PdfPTable t = new PdfPTable(4); // 4 columns to save space (2 label-value pairs per row)
        t.setWidthPercentage(100);

        addSummaryRow(t, "Session duration", String.format("%.1f s", repo.getSessionDurationMs() / 1000.0), "Active time", String.format("%.1f s", repo.getActiveTimeMs() / 1000.0));
        addSummaryRow(t, "Idle time", String.format("%.1f s", repo.getIdleTimeMs() / 1000.0), "Total compilations", String.valueOf(repo.getTotalCompilations()));
        addSummaryRow(t, "Successful compiles", String.valueOf(repo.getSuccessfulCompilations()), "Failed compiles", String.valueOf(repo.getFailedCompilations()));
        addSummaryRow(t, "Total executions", String.valueOf(repo.getTotalExecutions()), "", "");

        doc.add(t);
    }

    //charts

    private Image renderBarChart(PdfWriter writer, TelemetryRepository repo) throws Exception {
        int ok = repo.getSuccessfulCompilations();
        int fail = repo.getFailedCompilations();
        int total = Math.max(1, ok + fail);

        final int W = 220, H = 130;
        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate template = cb.createTemplate(W, H);
        Graphics2D g2 = template.createGraphics(W, H);

        int barW = 40, baseY = 30, maxH = 70;
        int okH = (int) Math.round(maxH * ((double) ok / total));
        int failH = (int) Math.round(maxH * ((double) fail / total));

        g2.setColor(new Color(46, 160, 67));
        g2.fillRect(40, baseY, barW, okH);
        g2.setColor(new Color(200, 60, 60));
        g2.fillRect(140, baseY, barW, failH);

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
        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate template = cb.createTemplate(W, H);
        Graphics2D g2 = template.createGraphics(W, H);
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        int diameter = 70, x = 10, y = 30;
        int activeAngle = (int) Math.round(360.0 * ((double) active / total));

        g2.setColor(new Color(66, 133, 244));
        g2.fillArc(x, y, diameter, diameter, 90, -activeAngle);
        g2.setColor(new Color(200, 200, 200));
        g2.fillArc(x, y, diameter, diameter, 90 - activeAngle, -(360 - activeAngle));

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(x, y, diameter, diameter);

        g2.setColor(new Color(66, 133, 244));
        g2.fillRect(100, 70, 10, 10);
        g2.setColor(Color.BLACK);
        g2.drawString(String.format("Active: %.1f s", active / 1000.0), 115, 80);

        g2.setColor(new Color(200, 200, 200));
        g2.fillRect(100, 50, 10, 10);
        g2.setColor(Color.BLACK);
        g2.drawString(String.format("Idle:   %.1f s", idle / 1000.0), 115, 60);

        g2.dispose();
        return Image.getInstance(template);
    }

    // tables

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
            String detail = e.getErrors().isEmpty() ? "-" : 
                    e.getErrors().get(0).getFileName() + ":" + e.getErrors().get(0).getLineNumber() + " — " + e.getErrors().get(0).getMessage();
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
            Color bg = alternate ? new Color(245, 245, 250) : Color.WHITE;
            addCell(t, Instant.ofEpochMilli(e.getTimestampMs()).toString(), bg);
            addCell(t, e.getType(), bg);
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

    // helpers

    private String describe(TelemetryEvent e) {
        if (e instanceof CompilationEvent ce) {
            if (ce.isSuccess()) return "Compile OK (" + ce.getDurationMs() + " ms)";
            CompilationError first = ce.getErrors().isEmpty() ? null : ce.getErrors().get(0);
            return "Compile FAILED" + (first == null ? "" : " — " + first.getFileName() + ":" + first.getLineNumber());
        }
        if (e instanceof ExecutionEvent ee) {
            return ee.isSuccess() ? "Run finished (" + ee.getDurationMs() + " ms)" : "Run FAILED — " + ee.getRuntimeException();
        }
        return e.getType();
    }

    private void addSummaryRow(PdfPTable t, String k1, String v1, String k2, String v2) {
        t.addCell(createSummaryCell(k1, true));
        t.addCell(createSummaryCell(v1, false));
        t.addCell(createSummaryCell(k2, true));
        t.addCell(createSummaryCell(v2, false));
    }

    private PdfPCell createSummaryCell(String text, boolean isBold) {
        PdfPCell cell = new PdfPCell(new Phrase(text, 
                isBold ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.DARK_GRAY) 
                       : FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(6f);
        return cell;
    }

    private void header(PdfPTable t, String... cols) {
        for (String c : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(c, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
            cell.setBackgroundColor(new Color(60, 90, 150));
            cell.setPadding(6f);
            t.addCell(cell);
        }
    }

    private void addCell(PdfPTable t, String s, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(s == null ? "" : s, FontFactory.getFont(FontFactory.HELVETICA, 8)));
        cell.setBackgroundColor(bg);
        cell.setPadding(5f);
        t.addCell(cell);
    }
}
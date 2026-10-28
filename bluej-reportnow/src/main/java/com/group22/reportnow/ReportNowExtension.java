package com.group22.reportnow;

import bluej.extensions2.BlueJ;
import bluej.extensions2.BPackage;
import bluej.extensions2.Extension;
import bluej.extensions2.MenuGenerator;
import com.group22.reportnow.config.StudentIdManager;
import com.group22.reportnow.export.ExportContext;
import com.group22.reportnow.export.ExportException;
import com.group22.reportnow.export.JsonExportStrategy;
import com.group22.reportnow.export.PdfExportStrategy;
import com.group22.reportnow.listener.CompilationListener;
import com.group22.reportnow.listener.ExecutionListener;
import com.group22.reportnow.repository.TelemetryRepository;
import java.io.File;
import javafx.scene.control.MenuItem;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import com.group22.reportnow.listener.EditorInteractionTracker;
import com.group22.reportnow.listener.EditorInteractionManager;
import com.group22.reportnow.listener.EditorPackageListener;

public class ReportNowExtension extends Extension {

    private BlueJ blueJ;
    private TelemetryRepository repository;
    private CompilationListener compilationListener;
    private ExecutionListener executionListener;
    private String studentId;

    private EditorInteractionTracker editorInteractionTracker;
    private EditorInteractionManager editorInteractionManager;
    private EditorPackageListener editorPackageListener;
    
    @Override public String getName()      { return "ReportNow Telemetry"; }
    @Override public String getVersion()   { return "1.0.0"; }
    @Override public boolean isCompatible() { return true; }

    @Override
    public String getDescription() {
        return "Collects coding-session telemetry and exports JSON + PDF reports.";
    }

    @Override
    public void startup(BlueJ blueJ) {
        this.blueJ = blueJ;
        this.repository = new TelemetryRepository();
        this.studentId = new StudentIdManager().getStudentId();

        this.compilationListener = new CompilationListener(repository);
        this.executionListener   = new ExecutionListener(repository);
        this.editorInteractionTracker = new EditorInteractionTracker();
        this.editorInteractionManager =
                new EditorInteractionManager(blueJ, editorInteractionTracker);
        this.editorPackageListener =
                new EditorPackageListener(editorInteractionManager);


        blueJ.addCompileListener(compilationListener);
        blueJ.addInvocationFinishedListener(executionListener);
        blueJ.addPackageListener(editorPackageListener);
        editorInteractionManager.scanOpenEditors();

        blueJ.setMenuGenerator(new MenuGenerator() {
            @Override
            public MenuItem getToolsMenuItem(BPackage bp) {
                MenuItem item = new MenuItem("Export ReportNow Session...");
                item.setOnAction(e -> manualExport());
                return item;
            }
        });

        System.out.println("[ReportNow] session " + repository.getSessionId() + " started for student " + studentId);
    }

    @Override
    public void terminate() {
        if (blueJ != null) {
            blueJ.removeCompileListener(compilationListener);
            blueJ.removeInvocationFinishedListener(executionListener);
            blueJ.removePackageListener(editorPackageListener);
            editorInteractionManager.detachAllListeners();
        }
        repository.endSession();

        File defaultDir = new File(System.getProperty("user.home"), "reportnow");
        exportToDirectory(defaultDir, false);

        System.out.println("[ReportNow] session ended");
    }

    private void manualExport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Folder to Save ReportNow Files");

        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File targetDir = chooser.getSelectedFile();
            exportToDirectory(targetDir, true);
        }
    }

    private void exportToDirectory(File baseDir, boolean showPopup) {
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            System.err.println("[ReportNow] could not create output directory: " + baseDir.getAbsolutePath());
            if (showPopup) {
                JOptionPane.showMessageDialog(null, "Could not create output directory.", "Error", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }

        String stamp = String.valueOf(System.currentTimeMillis());
        File json = new File(baseDir, "reportnow-" + stamp + ".json");
        File pdf  = new File(baseDir, "reportnow-" + stamp + ".pdf");

        try {
            ExportContext ctx = new ExportContext();
            ctx.setStrategy(new JsonExportStrategy(studentId));
            ctx.execute(repository, json);

            ctx.setStrategy(new PdfExportStrategy(studentId));
            ctx.execute(repository, pdf);

            System.out.println("[ReportNow] wrote: " + json + " and " + pdf);

            if (showPopup) {
                JOptionPane.showMessageDialog(null,
                    "Export successful!\n\nJSON:\n" + json.getAbsolutePath() + "\n\nPDF:\n" + pdf.getAbsolutePath(),
                    "ReportNow Export",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (ExportException e) {
            System.err.println("[ReportNow] export failed: " + e.getMessage());
            e.printStackTrace();
            if (showPopup) {
                JOptionPane.showMessageDialog(null, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void showSummary() {
        String msg = String.format(
                "Session: %s%nDuration: %.1f s%nCompilations: %d (%d ok / %d failed)%nExecutions: %d (%d ok / %d failed)",
                repository.getSessionId(),
                repository.getSessionDurationMs() / 1000.0,
                repository.getTotalCompilations(),
                repository.getSuccessfulCompilations(),
                repository.getFailedCompilations(),
                repository.getTotalExecutions(),
                repository.getSuccessfulExecutions(),
                repository.getFailedExecutions());

        JOptionPane.showMessageDialog(
                null, msg,
                "ReportNow — Session Summary",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
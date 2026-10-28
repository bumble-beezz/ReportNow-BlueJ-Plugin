package com.group22.reportnow;

import bluej.extensions2.BlueJ;
import bluej.extensions2.BPackage;
import bluej.extensions2.Extension;
import bluej.extensions2.MenuGenerator;
import com.group22.reportnow.config.StudentIdManager;
import com.group22.reportnow.export.*;
import com.group22.reportnow.listener.*;
import com.group22.reportnow.repository.TelemetryRepository;
import java.io.File;
import javafx.scene.control.MenuItem;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class ReportNowExtension extends Extension {

    private BlueJ blueJ;
    private TelemetryRepository repository;
    private CompilationListener compilationListener;
    private ExecutionListener executionListener;
    private ProjectLifecycleListener projectListener;
    private ClassLifecycleListener classListener;
    private String studentId;
    private boolean isShuttingDown = false; // Flag to prevent double export

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
        this.editorInteractionManager = new EditorInteractionManager(blueJ, editorInteractionTracker);
        this.editorPackageListener = new EditorPackageListener(editorInteractionManager);
        
        this.repository.setEditorTracker(editorInteractionTracker);

        this.classListener = new ClassLifecycleListener(repository);
        
        //pass the callback to trigger an export when a project closes
        this.projectListener = new ProjectLifecycleListener(blueJ, repository, classListener, projectName -> {
            if (!isShuttingDown) {
                File defaultDir = new File(System.getProperty("user.home"), "reportnow");
                exportToDirectory(defaultDir, projectName, false);
            }
        });

        blueJ.addCompileListener(compilationListener);
        blueJ.addInvocationFinishedListener(executionListener);
        blueJ.addPackageListener(projectListener);
        blueJ.addClassListener(classListener);

        blueJ.addPackageListener(editorPackageListener);
        editorInteractionManager.scanOpenEditors();

        projectListener.startTimer();
        classListener.startTimer();

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
        isShuttingDown = true; // BlueJ is closing, prevent packageClosing from exporting

        if (projectListener != null) projectListener.stopTimer();
        if (classListener != null) classListener.stopTimer();

        if (blueJ != null) {
            blueJ.removeCompileListener(compilationListener);
            blueJ.removeInvocationFinishedListener(executionListener);
            blueJ.removePackageListener(editorPackageListener);
            editorInteractionManager.detachAllListeners();
            blueJ.removePackageListener(projectListener);
            blueJ.removeClassListener(classListener);
        }
        repository.endSession();

        File defaultDir = new File(System.getProperty("user.home"), "reportnow");
        // Fallback export on full BlueJ shutdown
        exportToDirectory(defaultDir, repository.getPrimaryProjectName(), false);

        System.out.println("[ReportNow] session ended");
    }

    private void manualExport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Folder to Save ReportNow Files");

        if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File targetDir = chooser.getSelectedFile();
            exportToDirectory(targetDir, repository.getPrimaryProjectName(), true);
        }
    }

    private void exportToDirectory(File baseDir, String projectName, boolean showPopup) {
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            System.err.println("[ReportNow] could not create output directory: " + baseDir.getAbsolutePath());
            if (showPopup) {
                JOptionPane.showMessageDialog(null, "Could not create output directory.", "Error", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
        
        SessionHistoryLoader loader = new SessionHistoryLoader();
        repository.setPastSessions(loader.loadHistory(baseDir, repository.getSessionId()));

        File projectDir = new File(baseDir, projectName);
        if (!projectDir.exists() && !projectDir.mkdirs()) {
             System.err.println("[ReportNow] could not create project directory.");
             return;
        }

        String stamp = String.valueOf(System.currentTimeMillis());
        File json = new File(projectDir, "reportnow-" + stamp + ".json");
        File pdf  = new File(projectDir, "reportnow-" + stamp + ".pdf");

        try {
            ExportContext ctx = new ExportContext();
            ctx.setStrategy(new JsonExportStrategy(studentId));
            ctx.execute(repository, json);

            //pass projectName to PDF for heading logic
            ctx.setStrategy(new PdfExportStrategy(studentId, projectName)); 
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
}
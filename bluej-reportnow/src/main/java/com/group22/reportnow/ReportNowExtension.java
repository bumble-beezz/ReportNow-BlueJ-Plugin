package com.group22.reportnow;

import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;
import com.group22.reportnow.config.StudentIdManager;
import com.group22.reportnow.export.ExportContext;
import com.group22.reportnow.export.ExportException;
import com.group22.reportnow.export.JsonExportStrategy;
import com.group22.reportnow.export.PdfExportStrategy;
import com.group22.reportnow.listener.CompilationListener;
import com.group22.reportnow.listener.ExecutionListener;
import com.group22.reportnow.repository.TelemetryRepository;
import java.io.File;

/**
 * behaviour:
 *  - startup():    creates the session repository and registers both listeners
 *  - (session):    both listeners accumulate CompilationEvent / ExecutionEvent
 *  - terminate():  unregisters listeners, ends the session, auto-exports JSON + PDF
 *
 */
public class ReportNowExtension extends Extension {

    private BlueJ blueJ;
    private TelemetryRepository  repository;
    private CompilationListener  compilationListener;
    private ExecutionListener    executionListener;

    //required abstract methods

    @Override public String  getName()      { return "ReportNow Telemetry"; }
    @Override public String  getVersion()   { return "1.0.0"; }
    @Override public boolean isCompatible() { return true; }

    //optional overrides 

    @Override
    public String getDescription() {
        return "Collects coding-session telemetry and exports JSON + PDF reports.";
    }

    //lifecycle 

    private String studentId; //to hold the ID

    @Override
    public void startup(BlueJ blueJ) {
        this.blueJ = blueJ;
        this.repository = new TelemetryRepository();
        
        //initialize student ID
        this.studentId = new StudentIdManager().getStudentId();

        this.compilationListener = new CompilationListener(repository);
        this.executionListener   = new ExecutionListener(repository);

        blueJ.addCompileListener(compilationListener);
        blueJ.addInvocationFinishedListener(executionListener);

        System.out.println("[ReportNow] session " + repository.getSessionId() + " started for student " + studentId);
    }

    @Override
    public void terminate() {
        if (blueJ != null) {
            blueJ.removeCompileListener(compilationListener);
            blueJ.removeInvocationFinishedListener(executionListener);
        }
        repository.endSession();
        exportSession();
        System.out.println("[ReportNow] session ended");
    }

    // public API (available for future menu wiring) 

    public void exportSession() {
        File baseDir = new File(System.getProperty("user.home"), "reportnow");
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            System.err.println("[ReportNow] could not create output directory");
            return;
        }

        String stamp = String.valueOf(System.currentTimeMillis());
        File json = new File(baseDir, "reportnow-" + stamp + ".json");
        File pdf  = new File(baseDir, "reportnow-" + stamp + ".pdf");

        try {
            ExportContext ctx = new ExportContext();

            ctx.setStrategy(new JsonExportStrategy(studentId)); //pass the ID here
            ctx.execute(repository, json);

            ctx.setStrategy(new PdfExportStrategy(studentId)); 
            ctx.execute(repository, pdf);

            System.out.println("[ReportNow] wrote: " + json + " and " + pdf);
        } catch (ExportException e) {
            System.err.println("[ReportNow] export failed: " + e.getMessage());
            e.printStackTrace();
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

        javax.swing.JOptionPane.showMessageDialog(
                null, msg,
                "ReportNow — Session Summary",
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    
}
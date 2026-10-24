package com.group22.reportnow.export;

import com.group22.reportnow.repository.TelemetryRepository;
import java.io.File;

/**
 * context that gives export to the currently configured strategy.
 * the context knows nothing about JSON, PDF, or any other format.
 */
public class ExportContext {

    private ExportStrategy strategy;

    public void setStrategy(ExportStrategy strategy) {
        this.strategy = strategy;
    }

    public void execute(TelemetryRepository repo, File file) throws ExportException {
        if (strategy == null) throw new IllegalStateException("No ExportStrategy set");
        strategy.export(repo, file);
    }
}
package com.group22.reportnow.export;

import com.group22.reportnow.repository.TelemetryRepository;
import java.io.File;

/**
 * strategy pattern: interchangeable export algorithms.
 * adding CSV, XML, or an HTML report later requires only a new implementation
 * of this interface. callers stay unchanged (open/closed, LSP, ISP, DIP).
 */
public interface ExportStrategy {
    void export(TelemetryRepository repository, File outputFile) throws ExportException;
}
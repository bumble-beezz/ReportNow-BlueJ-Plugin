package com.group22.reportnow.parser;

import com.group22.reportnow.factory.TelemetryEventFactory;
import com.group22.reportnow.model.CompilationError;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * recognised line format:
 *     <file>:<line>: <severity>: <message>
 */
public class CompilationErrorParser {

    private static final Pattern LINE = Pattern.compile(
            "^(.+?):(\\d+):\\s*(error|warning|note):\\s*(.+)$",
            Pattern.CASE_INSENSITIVE);

    public List<CompilationError> parse(String rawMessage) {
        List<CompilationError> result = new ArrayList<>();
        if (rawMessage == null || rawMessage.isEmpty()) return result;

        for (String line : rawMessage.split("\\R")) {
            Matcher m = LINE.matcher(line.trim());
            if (!m.matches()) continue;

            result.add(TelemetryEventFactory.createCompilationError(
                    m.group(1).trim(),
                    Integer.parseInt(m.group(2)),
                    m.group(3).toUpperCase(),
                    m.group(4).trim()));
        }
        return result;
    }
}
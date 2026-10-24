package com.group22.reportnow.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExecutionEvent extends TelemetryEvent {

    @JsonProperty("success")            private final boolean success;
    @JsonProperty("duration_ms")        private final long    durationMs;
    @JsonProperty("runtime_exception")  private final String  runtimeException;
    @JsonProperty("console_output")     private final String  consoleOutput;

    public ExecutionEvent(long timestampMs, boolean success, long durationMs, 
                          String runtimeException, String consoleOutput) {
        super(timestampMs);
        this.success          = success;
        this.durationMs       = durationMs;
        this.runtimeException = runtimeException;
        this.consoleOutput    = consoleOutput;
    }

    @Override public String getType() { return "EXECUTION"; }

    public boolean isSuccess()            { return success; }
    public long    getDurationMs()        { return durationMs; }
    public String  getRuntimeException()  { return runtimeException; }
    public String  getConsoleOutput()     { return consoleOutput; }
}
package com.group22.reportnow.export;

public class ExportException extends Exception {
    public ExportException(String msg)              { super(msg); }
    public ExportException(String msg, Throwable t) { super(msg, t); }
}
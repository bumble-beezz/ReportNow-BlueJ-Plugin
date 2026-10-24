package com.group22.reportnow.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * base class for every event ReportNow records.
 *
 * Serialisation contract: every event writes an `event_type` field (the
 * value returned by {@link #getType()}) plus `timestamp_ms` / `timestamp_iso`.
 *
 * NOTE: i deliberately did NOT use @JsonTypeInfo/@JsonSubTypes here because
 * events are serialised inside Map<String,Object>, where Jackson dispatches
 * on the runtime class and would skip the polymorphic type-info mechanism.
 * the discriminator is instead exposed as a plain property on getType().
 *
 * adding a new event type is a one-step change: create a new subclass of
 * TelemetryEvent whose getType() returns a unique string.
 */
public abstract class TelemetryEvent {

    @JsonProperty("timestamp_ms")
    private final long timestampMs;

    @JsonProperty("timestamp_iso")
    private final String timestampIso;

    protected TelemetryEvent(long timestampMs) {
        this.timestampMs  = timestampMs;
        this.timestampIso = Instant.ofEpochMilli(timestampMs).toString();
    }

    public long   getTimestampMs()  { return timestampMs; }
    public String getTimestampIso() { return timestampIso; }

    /** Discriminator field — always written to JSON as `event_type`. */
    @JsonProperty("event_type")
    public abstract String getType();
}
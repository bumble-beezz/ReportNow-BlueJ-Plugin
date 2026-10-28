
package com.group22.reportnow.listener;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class EditorInteractionTracker {

    private static final long IDLE_THRESHOLD_MILLIS = 180_000;

    private final List<Instant> editTimestamps = new ArrayList<>();
    private int editCount = 0;

    public void recordEdit() {
        recordEditAt(Instant.now());
    }

    public void recordEditAt(Instant timestamp) {
        editTimestamps.add(timestamp);
        editCount++;
    }

    public int getEditCount() {
        return editCount;
    }

    public List<Instant> getEditTimestamps() {
        return new ArrayList<>(editTimestamps);
    }

    public List<Long> getEditIntervals() {
        List<Long> intervals = new ArrayList<>();

        for (int i = 1; i < editTimestamps.size(); i++) {
            Instant previous = editTimestamps.get(i - 1);
            Instant current = editTimestamps.get(i);

            long milliseconds =
                    Duration.between(previous, current).toMillis();

            intervals.add(milliseconds);
        }

        return intervals;
    }

    public long getTotalIdleMillis() {
        long totalIdleMillis = 0;

        for (int i = 1; i < editTimestamps.size(); i++) {
            Instant previous = editTimestamps.get(i - 1);
            Instant current = editTimestamps.get(i);

            long gapMillis =
                    Duration.between(previous, current).toMillis();

            if (gapMillis > IDLE_THRESHOLD_MILLIS) {
                totalIdleMillis += gapMillis - IDLE_THRESHOLD_MILLIS;
            }
        }

        return totalIdleMillis;
    }
}


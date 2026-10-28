
package com.group22.reportnow.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EditorInteractionTrackerTest {

    private EditorInteractionTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new EditorInteractionTracker();
    }

    @Test
    void noEditsShouldReturnZeroCountAndIdleTime() {
        assertEquals(0, tracker.getEditCount());
        assertEquals(0L, tracker.getTotalIdleMillis());
        assertEquals(List.of(), tracker.getEditIntervals());
    }

    @Test
    void shouldCountRecordedEdits() {
        tracker.recordEditAt(Instant.parse("2026-01-01T00:00:00Z"));
        tracker.recordEditAt(Instant.parse("2026-01-01T00:00:01Z"));

        assertEquals(2, tracker.getEditCount());
    }

    @Test
    void shouldCalculateEditIntervalsInMilliseconds() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        tracker.recordEditAt(start);
        tracker.recordEditAt(start.plusMillis(1000));
        tracker.recordEditAt(start.plusMillis(3500));

        assertEquals(List.of(1000L, 2500L), tracker.getEditIntervals());
    }

    @Test
    void shouldCountIdleTimeBeyondThreeMinuteThreshold() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        tracker.recordEditAt(start);
        tracker.recordEditAt(start.plusMillis(240_000));

        // Four-minute gap minus the three-minute threshold = one minute idle.
        assertEquals(60_000L, tracker.getTotalIdleMillis());
    }

    @Test
    void shouldNotCountGapOfExactlyThreeMinutesAsIdle() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");

        tracker.recordEditAt(start);
        tracker.recordEditAt(start.plusMillis(180_000));

        assertEquals(0L, tracker.getTotalIdleMillis());
    }

    @Test
    void oneEditShouldHaveNoIntervalsOrIdleTime() {
        tracker.recordEditAt(Instant.parse("2026-01-01T00:00:00Z"));

        assertEquals(1, tracker.getEditCount());
        assertEquals(List.of(), tracker.getEditIntervals());
        assertEquals(0L, tracker.getTotalIdleMillis());
    }
}

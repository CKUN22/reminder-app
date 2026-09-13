package com.ckun.reminder;

import java.time.Instant;
import java.util.List;

/** JVM regression tests; no Android runtime or third-party dependencies required. */
public final class ReminderRulesTest {
    private static int assertions;
    private static void equal(Object expected, Object actual) {
        assertions++;
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
    public static void main(String[] args) {
        long now = Instant.parse("2026-09-13T10:00:00Z").toEpochMilli();
        Task t = new Task(); t.title = "读书"; t.start = now + 2 * 86400000L; t.earlyTen = true; t.earlyDay = true;
        equal(List.of(0, 1, 2), ReminderRules.pending(t, now));
        t.start = now + 600000L;
        equal(List.of(0), ReminderRules.pending(t, now)); // Equality is already expired.
        t.start = now + 600001L;
        equal(List.of(0, 1), ReminderRules.pending(t, now));
        t.start = now; equal(List.of(), ReminderRules.pending(t, now));
        t.start = now - 1; equal(List.of(), ReminderRules.pending(t, now));
        t.start = now + 2 * 86400000L; t.done = true;
        equal(List.of(), ReminderRules.pending(t, now));
        t.done = false; t.earlyTen = false; t.earlyDay = false;
        equal(List.of(0), ReminderRules.pending(t, now));
        t.start = Instant.parse("2026-09-13T23:45:00Z").toEpochMilli(); t.duration = 30;
        equal(Instant.parse("2026-09-14T00:15:00Z").toEpochMilli(), ReminderRules.end(t));
        t.duration = 0; equal(t.start, ReminderRules.end(t));
        t.title = "  "; equal("请填写事项名称", ReminderRules.validate(t, now, false));
        t.title = "事项"; t.start = now; equal("请选择未来的开始时间", ReminderRules.validate(t, now, false));
        equal(true, ReminderRules.validate(t, now, true) == null);
        t.duration = -1; equal(true, ReminderRules.validate(t, now, true) != null);
        t.duration = Integer.MAX_VALUE; equal(true, ReminderRules.validate(t, now, true) != null);
        t.duration = 525600; equal(true, ReminderRules.validate(t, now, true) == null);
        // Moving an event recomputes every reminder relative to the new start.
        t.duration = 30; t.earlyDay = true; t.start = now + 86400001L;
        equal(List.of(0, 2), ReminderRules.pending(t, now));
        t.start -= 2; equal(List.of(0), ReminderRules.pending(t, now));
        System.out.println("PASS: " + assertions + " reminder rule assertions");
    }
}

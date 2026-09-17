package com.ckun.reminder;

import java.util.*;

public final class AgendaRulesTest {
    public static void main(String[] args) {
        Calendar day = Calendar.getInstance(); day.set(2026, Calendar.SEPTEMBER, 16, 12, 0, 0);
        Task lateOpen = task(day, 14, false), earlyDone = task(day, 8, true), earlyOpen = task(day, 9, false), lateDone = task(day, 16, true);
        Calendar tomorrow = (Calendar) day.clone(); tomorrow.add(Calendar.DATE, 1);
        Task otherDay = task(tomorrow, 7, false);

        List<Task> result = AgendaRules.forDay(List.of(lateDone, otherDay, lateOpen, earlyDone, earlyOpen), day.getTimeInMillis());
        check(result.equals(List.of(earlyOpen, lateOpen, earlyDone, lateDone)), "open tasks precede completed tasks and each group is chronological");
        check(!result.contains(otherDay), "only the selected day is included");
        earlyOpen.priority = 3; lateOpen.priority = 0; earlyDone.priority = 2;
        check(AgendaRules.highestPriorityForDay(List.of(earlyOpen, lateOpen, earlyDone, otherDay), day.getTimeInMillis()) == 0, "calendar uses only the highest priority on a day");
        check(AgendaRules.highestPriorityForDay(List.of(otherDay), day.getTimeInMillis()) == -1, "calendar has no marker without tasks");
    }

    private static Task task(Calendar day, int hour, boolean done) {
        Calendar time = (Calendar) day.clone(); time.set(Calendar.HOUR_OF_DAY, hour);
        Task task = new Task(); task.start = time.getTimeInMillis(); task.done = done; return task;
    }

    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}

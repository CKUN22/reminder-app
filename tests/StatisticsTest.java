package com.ckun.reminder;

import java.util.*;

public final class StatisticsTest {
    private static int assertions;
    private static void check(boolean value, String message) { assertions++; if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        Calendar monday = Calendar.getInstance(); monday.clear(); monday.set(2026, Calendar.SEPTEMBER, 14, 0, 0, 0);
        Calendar sunday = Calendar.getInstance(); sunday.clear(); sunday.set(2026, Calendar.SEPTEMBER, 20, 23, 59, 0);
        Calendar nextMonday = Calendar.getInstance(); nextMonday.clear(); nextMonday.set(2026, Calendar.SEPTEMBER, 21, 0, 0, 0);
        check(Statistics.weekStart(sunday.getTimeInMillis()) == monday.getTimeInMillis(), "week starts on Monday");
        Task first = new Task(); first.done = true; first.completedAt = monday.getTimeInMillis();
        Task second = new Task(); second.done = true; second.completedAt = sunday.getTimeInMillis();
        Task next = new Task(); next.done = true; next.completedAt = nextMonday.getTimeInMillis();
        Task open = new Task(); open.done = false; open.completedAt = monday.getTimeInMillis();
        check(Statistics.completedInWeek(List.of(first, second, next, open), monday.getTimeInMillis()) == 2, "counts only completed tasks inside week");
        check(Statistics.completedInWeek(List.of(next), nextMonday.getTimeInMillis()) == 1, "next week is counted separately");
        Calendar old = Calendar.getInstance(); old.clear(); old.set(2026, Calendar.JULY, 6);
        Task peakA = new Task(); peakA.done = true; peakA.completedAt = old.getTimeInMillis();
        Task peakB = new Task(); peakB.done = true; peakB.completedAt = old.getTimeInMillis() + 86400000L;
        Calendar older = Calendar.getInstance(); older.clear(); older.set(2026, Calendar.JUNE, 1);
        Task other = new Task(); other.done = true; other.completedAt = older.getTimeInMillis();
        List<Statistics.WeekCount> peaks = Statistics.topHistoricalWeeks(List.of(first, peakA, peakB, other), monday.getTimeInMillis(), 2);
        check(peaks.size() == 2, "returns two historical weeks");
        check(peaks.get(0).count == 2 && peaks.get(0).start == Statistics.weekStart(old.getTimeInMillis()), "highest historical week comes first");
        check(peaks.stream().noneMatch(week -> week.start == monday.getTimeInMillis()), "recent weeks are excluded from historical peaks");
        System.out.println("PASS: " + assertions + " statistics assertions");
    }
}

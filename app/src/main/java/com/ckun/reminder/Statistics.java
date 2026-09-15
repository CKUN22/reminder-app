package com.ckun.reminder;

import java.util.*;

final class Statistics {
    static final class WeekCount {
        final long start; final int count;
        WeekCount(long start, int count) { this.start = start; this.count = count; }
    }
    static long weekStart(long time) {
        Calendar date = Calendar.getInstance(); date.setTimeInMillis(time);
        int offset = (date.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        date.add(Calendar.DAY_OF_MONTH, -offset);
        date.set(Calendar.HOUR_OF_DAY, 0); date.set(Calendar.MINUTE, 0); date.set(Calendar.SECOND, 0); date.set(Calendar.MILLISECOND, 0);
        return date.getTimeInMillis();
    }
    static int completedInWeek(List<Task> tasks, long start) {
        Calendar end = Calendar.getInstance(); end.setTimeInMillis(start); end.add(Calendar.DAY_OF_MONTH, 7);
        int count = 0;
        for (Task task : tasks) if (task.done && task.completedAt >= start && task.completedAt < end.getTimeInMillis()) count++;
        return count;
    }
    static List<WeekCount> topHistoricalWeeks(List<Task> tasks, long recentCutoff, int limit) {
        Map<Long, Integer> totals = new HashMap<>();
        for (Task task : tasks) if (task.done && task.completedAt > 0) {
            long start = weekStart(task.completedAt);
            if (start < recentCutoff) totals.put(start, totals.getOrDefault(start, 0) + 1);
        }
        List<WeekCount> weeks = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : totals.entrySet()) weeks.add(new WeekCount(entry.getKey(), entry.getValue()));
        weeks.sort((a, b) -> a.count != b.count ? Integer.compare(b.count, a.count) : Long.compare(b.start, a.start));
        return weeks.size() <= limit ? weeks : new ArrayList<>(weeks.subList(0, limit));
    }
}

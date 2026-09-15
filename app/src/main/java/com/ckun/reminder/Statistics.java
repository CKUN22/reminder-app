package com.ckun.reminder;

import java.util.*;

final class Statistics {
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
}

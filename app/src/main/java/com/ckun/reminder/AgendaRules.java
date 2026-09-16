package com.ckun.reminder;

import java.util.*;

final class AgendaRules {
    private AgendaRules() {}

    static List<Task> forDay(List<Task> tasks, long day) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) if (sameDay(task.start, day)) result.add(task);
        result.sort(Comparator.comparing((Task task) -> task.done).thenComparingLong(task -> task.start));
        return result;
    }

    private static boolean sameDay(long first, long second) {
        Calendar a = Calendar.getInstance(), b = Calendar.getInstance();
        a.setTimeInMillis(first); b.setTimeInMillis(second);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
}

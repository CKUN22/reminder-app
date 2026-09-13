package com.ckun.reminder;

import java.util.ArrayList;
import java.util.List;

public final class ReminderRules {
    public static final long[] OFFSETS = {0, 10 * 60_000L, 24 * 60 * 60_000L};
    private ReminderRules() {}
    public static List<Integer> pending(Task task, long now) {
        List<Integer> result = new ArrayList<>();
        if (task.done) return result;
        for (int i = 0; i < OFFSETS.length; i++) {
            if ((i == 0 || (i == 1 && task.earlyTen) || (i == 2 && task.earlyDay))
                    && task.start - OFFSETS[i] > now) result.add(i);
        }
        return result;
    }
    public static long end(Task task) { return Math.addExact(task.start, task.duration * 60_000L); }
    public static String validate(Task task, long now, boolean timeUnchanged) {
        if (task.title.trim().isEmpty()) return "请填写事项名称";
        if (!timeUnchanged && task.start <= now) return "请选择未来的开始时间";
        if (task.duration < 0 || task.duration > 525600) return "持续时间请输入 1–525600 分钟，或留空";
        return null;
    }
}

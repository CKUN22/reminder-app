package com.ckun.reminder;

import java.util.ArrayList;
import java.util.List;

public final class ReminderRules {
    private ReminderRules() {}
    public static String reminderLabel(int minutes) {
        if (minutes == 0) return "日程发生时";
        if (minutes % 1440 == 0) return (minutes / 1440) + " 天前";
        if (minutes % 60 == 0) return (minutes / 60) + " 小时前";
        return minutes + " 分钟前";
    }
    public static List<Integer> pendingMinutes(Task task, long now) {
        List<Integer> result = new ArrayList<>();
        if (!task.done) for (int minutes : task.reminders()) if (task.start - minutes * 60_000L > now) result.add(minutes);
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

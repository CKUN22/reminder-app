package com.ckun.reminder;

import android.content.Context;
import java.util.*;

final class GoalGenerator {
    static int ensureThroughDeadline(Context context, TaskStore tasks, ReminderScheduler scheduler, long now) {
        long firstDay = GoalRules.dayStart(now); int created = 0;
        try (GoalStore goals = new GoalStore(context)) {
            for (Goal goal : goals.all()) {
                for (long day = firstDay; GoalRules.eligible(goal, day); day = GoalRules.nextDay(day)) {
                    if (tasks.hasGeneratedTask(goal.id, day)) continue;
                    Task task = new Task(); task.title = goal.dailyTitle; task.note = "来自目标：" + goal.title; task.start = GoalRules.taskTime(day, goal.hour, goal.minute);
                    task.sourceGoalId = goal.id; task.generatedDay = day; task.reminderMinutes = new TreeSet<>(List.of(0)); tasks.save(task); scheduler.schedule(task); created++;
                }
            }
        }
        return created;
    }
}

package com.ckun.reminder;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import static org.junit.Assert.*;
import java.util.*;

public class GoalStoreTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    @Before public void setUp() { context.deleteDatabase("goals.db"); context.deleteDatabase("tasks.db"); }
    @After public void tearDown() { context.deleteDatabase("goals.db"); context.deleteDatabase("tasks.db"); }
    @Test public void goalPersistsAndGeneratesOnlyOneTaskPerDay() {
        long today = GoalRules.dayStart(System.currentTimeMillis()); Goal goal = new Goal(); goal.title = "通过六级"; goal.dailyTitle = "背30个单词"; goal.deadline = today + 30L * 86400000; goal.hour = 21; goal.minute = 30;
        try (GoalStore goals = new GoalStore(context)) { goals.save(goal); assertEquals("通过六级", goals.get(goal.id).title); }
        try (TaskStore tasks = new TaskStore(context)) {
            ReminderScheduler scheduler = new ReminderScheduler(context);
            assertEquals(1, GoalGenerator.ensureToday(context, tasks, scheduler, System.currentTimeMillis()));
            assertEquals(0, GoalGenerator.ensureToday(context, tasks, scheduler, System.currentTimeMillis()));
            Task task = tasks.all().get(0); assertEquals(goal.id, task.sourceGoalId); assertEquals(today, task.generatedDay); assertEquals("背30个单词", task.title); assertEquals(new TreeSet<>(List.of(0)), task.reminders());
            scheduler.cancel(task);
        }
    }
}

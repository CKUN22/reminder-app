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
    @Test public void goalPersistsAndGeneratesEveryDayThroughDeadlineOnlyOnce() {
        long today = GoalRules.dayStart(System.currentTimeMillis()); Goal goal = new Goal(); goal.title = "通过六级"; goal.dailyTitle = "背30个单词"; goal.deadline = GoalRules.nextDay(GoalRules.nextDay(today)); goal.hour = 21; goal.minute = 30;
        try (GoalStore goals = new GoalStore(context)) { goals.save(goal); Goal saved=goals.get(goal.id);assertEquals("通过六级", saved.title);assertNotNull(UUID.fromString(saved.syncId));assertTrue(saved.createdAt>0); }
        try (TaskStore tasks = new TaskStore(context)) {
            ReminderScheduler scheduler = new ReminderScheduler(context);
            assertEquals(3, GoalGenerator.ensureThroughDeadline(context, tasks, scheduler, System.currentTimeMillis()));
            assertEquals(0, GoalGenerator.ensureThroughDeadline(context, tasks, scheduler, System.currentTimeMillis()));
            List<Task> generated = tasks.all(); assertEquals(3, generated.size());
            Set<Long> days = new HashSet<>();
            for (Task task : generated) {
                assertEquals(goal.id, task.sourceGoalId); assertEquals("背30个单词", task.title); assertEquals(new TreeSet<>(List.of(0)), task.reminders());
                days.add(task.generatedDay); scheduler.cancel(task);
            }
            assertEquals(new HashSet<>(List.of(today, GoalRules.nextDay(today), goal.deadline)), days);
        }
    }
    @Test public void deleteKeepsGoalTombstoneForSync(){Goal goal=new Goal();goal.title="临时目标";try(GoalStore goals=new GoalStore(context)){goals.save(goal);goals.delete(goal.id);assertNull(goals.get(goal.id));assertTrue(goals.all().isEmpty());try(android.database.Cursor row=goals.getReadableDatabase().query("goals",new String[]{"deleted_at"},"id=?",new String[]{String.valueOf(goal.id)},null,null,null)){assertTrue(row.moveToFirst());assertTrue(row.getLong(0)>0);}}}
}

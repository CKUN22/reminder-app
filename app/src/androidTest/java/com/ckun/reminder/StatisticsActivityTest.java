package com.ckun.reminder;

import android.app.*;
import android.content.*;
import android.view.View;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

public class StatisticsActivityTest {
    @Test public void homeOpensWeeklyStatisticsWithCurrentCount() {
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext(); context.deleteDatabase("tasks.db");
        Task task = new Task(); task.title = "本周完成"; task.start = System.currentTimeMillis(); task.done = true; task.completedAt = System.currentTimeMillis();
        try (TaskStore store = new TaskStore(context)) { store.save(task); }
        Activity main = instrumentation.startActivitySync(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(StatisticsActivity.class.getName(), null, false);
        try {
            instrumentation.runOnMainSync(() -> main.getWindow().getDecorView().findViewWithTag("statistics").performClick());
            Activity stats = instrumentation.waitForMonitorWithTimeout(monitor, 5000); assertNotNull(stats);
            instrumentation.runOnMainSync(() -> {
                assertEquals("1", ((TextView) stats.getWindow().getDecorView().findViewWithTag("current-week-count")).getText().toString());
                View chart = stats.getWindow().getDecorView().findViewWithTag("weekly-bar-chart"); assertNotNull(chart);
                assertTrue(chart.getContentDescription().toString().startsWith("近10周完成统计"));
                assertNotNull(stats.getWindow().getDecorView().findViewWithTag("statistics-scroll"));
            });
            instrumentation.runOnMainSync(stats::finish);
        } finally { instrumentation.removeMonitor(monitor); instrumentation.runOnMainSync(main::finish); context.deleteDatabase("tasks.db"); }
    }
}

package com.ckun.reminder;

import android.app.*;
import android.content.*;
import android.view.View;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

public class GoalsActivityTest {
    @Test public void homeOpensGoalCreatorWithDailyControls() {
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        Activity main = instrumentation.startActivitySync(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(GoalsActivity.class.getName(), null, false);
        try {
            instrumentation.runOnMainSync(() -> main.getWindow().getDecorView().findViewWithTag("goals").performClick());
            Activity goals = instrumentation.waitForMonitorWithTimeout(monitor, 5000); assertNotNull(goals);
            instrumentation.runOnMainSync(() -> goals.getWindow().getDecorView().findViewWithTag("create-goal").performClick());
            instrumentation.runOnMainSync(() -> {
                assertNotNull(goals.getWindow().getDecorView().findViewWithTag("goal-deadline"));
                assertNotNull(goals.getWindow().getDecorView().findViewWithTag("goal-time"));
                View automatic = goals.getWindow().getDecorView().findViewWithTag("goal-auto-add"); assertNotNull(automatic);
                assertNotNull(goals.getWindow().getDecorView().findViewWithTag("save-goal"));
            });
            instrumentation.runOnMainSync(goals::finish);
        } finally { instrumentation.removeMonitor(monitor); instrumentation.runOnMainSync(main::finish); }
    }
}

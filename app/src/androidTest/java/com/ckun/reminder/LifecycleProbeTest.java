package com.ckun.reminder;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Assume;
import org.junit.Test;
import static org.junit.Assert.*;

/** Run in two phases around process termination/reboot; see scripts/device-test.ps1. */
public class LifecycleProbeTest {
    @Test public void persistedReminder() {
        String phase = InstrumentationRegistry.getArguments().getString("lifecyclePhase");
        Assume.assumeTrue("Requires external lifecycle test script", phase != null);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (TaskStore store = new TaskStore(context)) {
            if ("seed".equals(phase)) {
                ReminderScheduler scheduler = new ReminderScheduler(context);
                for (Task t : store.all()) { scheduler.cancel(t); store.delete(t.id); }
                Task t = new Task(); t.title = "重启恢复验证"; t.start = System.currentTimeMillis() + 3600000;
                t.earlyTen = true; t.duration = 30; store.save(t); scheduler.schedule(t);
            } else {
                assertEquals(1, store.all().size()); Task t = store.all().get(0);
                assertEquals("重启恢复验证", t.title); assertFalse(t.done); assertTrue(t.earlyTen);
                assertEquals(30, t.duration); assertTrue(t.start > System.currentTimeMillis());
            }
        }
    }
}

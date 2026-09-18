package com.ckun.reminder;

import android.app.*;
import android.content.*;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Bitmap;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import static org.junit.Assert.*;
import java.io.File;
import java.io.FileOutputStream;

public class AppearanceTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = instrumentation.getTargetContext();
    @Before public void reset() { context.getSharedPreferences("appearance", 0).edit().clear().commit(); }
    @After public void cleanup() { reset(); }
    private Activity launch(Class<?> type) {
        return instrumentation.startActivitySync(new Intent(context, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    private View tagged(Activity activity, String tag) { return activity.getWindow().getDecorView().findViewWithTag(tag); }
    private View root(Activity activity) { return ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0); }
    private void assertHomeHeaderFits(Activity activity) {
        instrumentation.runOnMainSync(() -> {
            TextView date = (TextView) tagged(activity, "home-date");
            View settings = tagged(activity, "settings");
            assertTrue("Date must remain fully readable next to navigation", date.getWidth() >= date.getPaint().measureText(date.getText().toString()));
            assertTrue("Settings must fit inside the header", settings.getRight() <= ((View) settings.getParent()).getWidth());
        });
    }
    private void awaitHomeAppearance(Activity activity, boolean glass) {
        long deadline = android.os.SystemClock.uptimeMillis() + 5000;
        boolean[] matches = {false};
        do {
            instrumentation.runOnMainSync(() -> matches[0] = (root(activity).getBackground() instanceof Appearance.FrostedBackground) == glass);
            if (matches[0]) return;
            android.os.SystemClock.sleep(50);
        } while (android.os.SystemClock.uptimeMillis() < deadline);
        fail("Home did not apply the selected appearance after returning from settings");
    }
    private void screenshot(String name) throws Exception {
        instrumentation.waitForIdleSync();
        android.os.SystemClock.sleep(300);
        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot(); assertNotNull(bitmap);
        try (FileOutputStream out = new FileOutputStream(new File(context.getExternalFilesDir(null), name))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
        } finally { bitmap.recycle(); }
    }
    @Test public void settingsSwitchPersistsAndUpdatesHomeInBothDirections() throws Exception {
        assertFalse(Appearance.glass(context));
        Activity main = launch(MainActivity.class);
        Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(SettingsActivity.class.getName(), null, false);
        Activity settings = null;
        try {
            instrumentation.runOnMainSync(() -> tagged(main, "settings").performClick());
            settings = instrumentation.waitForMonitorWithTimeout(monitor, 5000); assertNotNull(settings);
            final Activity first = settings;
            instrumentation.runOnMainSync(() -> {
                assertTrue(((RadioButton) tagged(first, "appearance-simple")).isChecked());
                tagged(first, "appearance-glass").performClick();
                assertTrue(Appearance.glass(context));
                assertTrue(root(first).getBackground() instanceof Appearance.FrostedBackground);
            });
            screenshot("appearance-settings-glass.png");
            instrumentation.runOnMainSync(() -> tagged(first, "settings-back").performClick());
            awaitHomeAppearance(main, true);
            instrumentation.runOnMainSync(() -> assertTrue(root(main).getBackground() instanceof Appearance.FrostedBackground));
            screenshot("appearance-home-glass.png");
            assertHomeHeaderFits(main);
            settings = launch(SettingsActivity.class);
            final Activity reopened = settings;
            instrumentation.runOnMainSync(() -> {
                assertTrue(((RadioButton) tagged(reopened, "appearance-glass")).isChecked());
                tagged(reopened, "appearance-simple").performClick();
                assertFalse(Appearance.glass(context));
                assertTrue(root(reopened).getBackground() instanceof ColorDrawable);
                reopened.finish();
            });
            awaitHomeAppearance(main, false);
            instrumentation.runOnMainSync(() -> assertTrue(root(main).getBackground() instanceof ColorDrawable));
            screenshot("appearance-home-simple.png");
            assertHomeHeaderFits(main);
        } finally {
            if (settings != null) { Activity last = settings; instrumentation.runOnMainSync(last::finish); }
            instrumentation.removeMonitor(monitor); instrumentation.runOnMainSync(main::finish);
        }
    }
    @Test public void glassPropagatesToAllScreensAndTaskEditor() {
        Appearance.setGlass(context, true);
        for (Class<?> type : new Class<?>[]{MainActivity.class, GoalsActivity.class, TimetableActivity.class, StatisticsActivity.class, ReminderSettingsActivity.class}) {
            Activity activity = launch(type);
            try {
                instrumentation.runOnMainSync(() -> {
                    assertTrue(type.getSimpleName(), root(activity).getBackground() instanceof Appearance.FrostedBackground);
                    if (activity instanceof MainActivity) {
                        tagged(activity, "add-task").performClick();
                        assertTrue(root(activity).getBackground() instanceof Appearance.FrostedBackground);
                    }
                });
            } finally { instrumentation.runOnMainSync(activity::finish); }
        }
    }
}

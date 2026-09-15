package com.ckun.reminder;

import android.app.*;
import android.content.*;
import android.os.*;
import android.graphics.Bitmap;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.io.*;

public class ReminderSettingsTest {
    @Test public void separateReminderKindsUseSeparateNotificationTags() {
        assertNotEquals(ReminderScheduler.notificationTag(42, 0), ReminderScheduler.notificationTag(42, 5));
        assertEquals("task-42-0", ReminderScheduler.notificationTag(42, 0));
        assertEquals("task-42-5", ReminderScheduler.notificationTag(42, 5));
    }
    @Test public void presetsCustomAndReopenPersist() throws Exception {
        Instrumentation runner = InstrumentationRegistry.getInstrumentation(); Context context = runner.getTargetContext();
        Task task = new Task(); task.title = "提醒配置测试"; task.start = System.currentTimeMillis() + 10 * 86400000L;
        try (TaskStore store = new TaskStore(context)) { store.save(task); }
        Activity parent = runner.startActivitySync(new Intent(context, MainActivity.class).putExtra("id", task.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        Activity settings = null;
        try {
            Instrumentation.ActivityMonitor monitor = runner.addMonitor(ReminderSettingsActivity.class.getName(), null, false);
            runner.runOnMainSync(() -> parent.getWindow().getDecorView().findViewWithTag("reminder-settings").performClick());
            settings = runner.waitForMonitorWithTimeout(monitor, 5000); assertNotNull(settings); runner.removeMonitor(monitor);
            Activity child = settings;
            runner.runOnMainSync(() -> {
                ((CheckBox) child.getWindow().getDecorView().findViewWithTag("reminder-0")).setChecked(false);
                ((CheckBox) child.getWindow().getDecorView().findViewWithTag("reminder-15")).setChecked(true);
                ((CheckBox) child.getWindow().getDecorView().findViewWithTag("reminder-30")).setChecked(true);
                child.getWindow().getDecorView().findViewWithTag("custom-reminder").performClick();
            }); runner.waitForIdleSync();
            SystemClock.sleep(700);
            AccessibilityNodeInfo root = runner.getUiAutomation().getRootInActiveWindow();
            AccessibilityNodeInfo input = findEditable(root); assertNotNull(input);
            Bundle text = new Bundle(); text.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "75");
            assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, text));
            List<AccessibilityNodeInfo> add = root.findAccessibilityNodeInfosByText("添加");
            assertTrue(add.stream().anyMatch(node -> "添加".contentEquals(node.getText() == null ? "" : node.getText()) && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)));
            runner.waitForIdleSync(); SystemClock.sleep(500);
            runner.runOnMainSync(() -> assertTrue(((CheckBox) child.getWindow().getDecorView().findViewWithTag("reminder-75")).isChecked()));
            Bitmap bitmap = runner.getUiAutomation().takeScreenshot();
            try (FileOutputStream out = new FileOutputStream(new File(context.getExternalFilesDir(null), "reminder-settings.png"))) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); } bitmap.recycle();
            runner.runOnMainSync(() -> child.getWindow().getDecorView().findViewWithTag("reminders-back").performClick()); runner.waitForIdleSync();
            runner.runOnMainSync(() -> clickSave(parent.getWindow().getDecorView())); runner.waitForIdleSync();
            try (TaskStore store = new TaskStore(context)) { assertEquals(new TreeSet<>(List.of(15, 30, 75)), store.get(task.id).reminders()); }
        } finally {
            Activity child = settings; runner.runOnMainSync(() -> { if (child != null) child.finish(); parent.finish(); });
            try (TaskStore store = new TaskStore(context)) { Task fresh = store.get(task.id); if (fresh != null) { new ReminderScheduler(context).cancel(fresh); store.delete(task.id); } }
        }
    }
    private boolean clickSave(View view) {
        if (view instanceof Button && ((Button) view).getText().toString().equals("保存事项")) { view.performClick(); return true; }
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) if (clickSave(((ViewGroup) view).getChildAt(i))) return true;
        return false;
    }
    private AccessibilityNodeInfo findEditable(AccessibilityNodeInfo node) {
        if (node == null) return null; if (node.isEditable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) { AccessibilityNodeInfo found = findEditable(node.getChild(i)); if (found != null) return found; }
        return null;
    }
}

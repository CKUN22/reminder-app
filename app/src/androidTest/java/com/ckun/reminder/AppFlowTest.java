package com.ckun.reminder;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

public class AppFlowTest {
    @Test public void backReturnsDirectlyWithoutSavingDraft() {
        Activity activity = instrumentation.startActivitySync(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            int before; try (TaskStore store = new TaskStore(context)) { before = store.all().size(); }
            click(activity, "＋  新建待办");
            instrumentation.runOnMainSync(() -> {
                for (View v : all(activity.getWindow().getDecorView())) if (v instanceof EditText) { ((EditText) v).setText("未保存草稿"); break; }
            });
            click(activity, "‹  返回列表");
            click(activity, "＋  新建待办");
            instrumentation.runOnMainSync(activity::onBackPressed);
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(() -> assertTrue(all(activity.getWindow().getDecorView()).stream().anyMatch(v -> v instanceof Button && ((Button) v).getText().toString().equals("＋  新建待办"))));
            try (TaskStore store = new TaskStore(context)) { assertEquals(before, store.all().size()); }
        } finally { instrumentation.runOnMainSync(activity::finish); }
    }
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = instrumentation.getTargetContext();
    private List<View> all(View view) {
        List<View> result = new ArrayList<>(); result.add(view);
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) result.addAll(all(((ViewGroup) view).getChildAt(i)));
        return result;
    }
    private void click(Activity activity, String label) {
        instrumentation.runOnMainSync(() -> {
            for (View v : all(activity.getWindow().getDecorView())) {
                if (v instanceof Button && ((Button) v).getText().toString().equals(label)) { v.performClick(); return; }
            }
            throw new AssertionError("Button missing: " + label);
        }); instrumentation.waitForIdleSync();
    }
    private void screenshot(String name) throws Exception {
        SystemClock.sleep(2500);
        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot(); assertNotNull(bitmap);
        try (FileOutputStream out = new FileOutputStream(new File(context.getExternalFilesDir(null), name))) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); }
        bitmap.recycle();
    }
    @Test public void createEditCompleteThroughInterface() throws Exception {
        context.deleteDatabase("tasks.db");
        Activity activity = instrumentation.startActivitySync(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.waitForIdleSync(); screenshot("empty.png");
            click(activity, "＋  新建待办");
            instrumentation.runOnMainSync(() -> {
                List<EditText> inputs = new ArrayList<>(); List<CheckBox> options = new ArrayList<>();
                for (View v : all(activity.getWindow().getDecorView())) { if (v instanceof EditText) inputs.add((EditText) v); if (v instanceof CheckBox) options.add((CheckBox) v); }
                assertEquals(3, inputs.size()); inputs.get(0).setText("读半小时书"); inputs.get(1).setText("读完第二章，记下一个有趣的想法。"); inputs.get(2).setText("30");
                assertEquals(0, options.size());
                activity.getWindow().getDecorView().clearFocus();
            });
            Instrumentation.ActivityMonitor monitor = instrumentation.addMonitor(ReminderSettingsActivity.class.getName(), null, false);
            instrumentation.runOnMainSync(() -> activity.getWindow().getDecorView().findViewWithTag("reminder-settings").performClick());
            Activity settings = instrumentation.waitForMonitorWithTimeout(monitor, 5000); assertNotNull(settings); instrumentation.removeMonitor(monitor);
            instrumentation.runOnMainSync(() -> {
                CheckBox ten = settings.getWindow().getDecorView().findViewWithTag("reminder-10"); ten.setChecked(true);
                assertFalse(settings.getWindow().getDecorView().findViewWithTag("reminder-1440").isEnabled());
                settings.getWindow().getDecorView().findViewWithTag("reminders-back").performClick();
            }); instrumentation.waitForIdleSync();
            screenshot("editor.png"); click(activity, "保存事项");
            try (TaskStore store = new TaskStore(context)) {
                assertEquals(1, store.all().size()); Task t = store.all().get(0);
                assertEquals("读半小时书", t.title); assertEquals(30, t.duration); assertTrue(t.reminders().contains(10)); assertFalse(t.reminders().contains(1440));
            }
            screenshot("list.png");
            instrumentation.runOnMainSync(() -> {
                for (View v : all(activity.getWindow().getDecorView())) {
                    if (v instanceof TextView && ((TextView) v).getText().toString().equals("读半小时书")) { ((View) v.getParent()).performClick(); return; }
                }
                throw new AssertionError("Saved task card missing");
            });
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(() -> {
                List<EditText> inputs = new ArrayList<>();
                for (View v : all(activity.getWindow().getDecorView())) if (v instanceof EditText) inputs.add((EditText) v);
                inputs.get(1).setText("更新后的备注"); inputs.get(2).setText("60");
            });
            click(activity, "保存事项");
            try (TaskStore store = new TaskStore(context)) { assertEquals(60, store.all().get(0).duration); assertEquals("更新后的备注", store.all().get(0).note); }
            click(activity, "✓  标记完成");
            try (TaskStore store = new TaskStore(context)) { assertTrue(store.all().get(0).done); }
            click(activity, "已完成"); screenshot("completed.png");
            instrumentation.runOnMainSync(() -> {
                for (View v : all(activity.getWindow().getDecorView())) if (v instanceof TextView && ((TextView) v).getText().toString().equals("读半小时书")) { ((View) v.getParent()).performClick(); return; }
            }); instrumentation.waitForIdleSync(); click(activity, "改为未完成");
            try (TaskStore store = new TaskStore(context)) { assertFalse(store.all().get(0).done); assertTrue(store.all().get(0).reminders().contains(10)); }
        } finally { instrumentation.runOnMainSync(activity::finish); }
    }
    @Test public void systemAlarmDeliversAndCompletionCancelsNotification() throws Exception {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        ReminderScheduler scheduler = new ReminderScheduler(context);
        assertTrue("Grant notification permission before device tests", scheduler.notificationsAllowed());
        assertTrue("Grant exact alarm access before device tests", scheduler.exactAllowed());
        Task t = new Task(); t.title = "系统提醒测试"; t.start = System.currentTimeMillis() + 2000;
        try (TaskStore store = new TaskStore(context)) {
            store.save(t); scheduler.schedule(t);
            long deadline = SystemClock.elapsedRealtime() + 12000;
            boolean delivered = false;
            while (SystemClock.elapsedRealtime() < deadline) {
                for (android.service.notification.StatusBarNotification n : manager.getActiveNotifications()) {
                    if (("task-" + t.id).equals(n.getTag())) delivered = true;
                }
                if (delivered) break; SystemClock.sleep(100);
            }
            assertTrue("System AlarmManager should deliver the reminder", delivered);
            new ReminderReceiver().onReceive(context, new Intent("COMPLETE").putExtra("id", t.id).putExtra("revision", t.revision));
            assertTrue(store.get(t.id).done);
            SystemClock.sleep(200);
            for (android.service.notification.StatusBarNotification n : manager.getActiveNotifications()) assertNotEquals("task-" + t.id, n.getTag());
        } finally { scheduler.cancel(t); try (TaskStore store = new TaskStore(context)) { store.delete(t.id); } }
    }
    @Test public void savingOpenEditorDoesNotUndoNotificationCompletion() {
        Task t = new Task(); t.title = "编辑中的事项"; t.start = System.currentTimeMillis() + 3600000;
        try (TaskStore store = new TaskStore(context)) { store.save(t); }
        Activity activity = instrumentation.startActivitySync(new Intent(context, MainActivity.class).putExtra("id", t.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.waitForIdleSync();
            new ReminderReceiver().onReceive(context, new Intent("COMPLETE").putExtra("id", t.id).putExtra("revision", t.revision));
            click(activity, "保存事项");
            try (TaskStore store = new TaskStore(context)) { assertTrue(store.get(t.id).done); }
        } finally {
            instrumentation.runOnMainSync(activity::finish);
            try (TaskStore store = new TaskStore(context)) { store.delete(t.id); }
        }
    }
}

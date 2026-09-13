package com.ckun.reminder;

import android.app.*;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.*;
import android.view.View;
import android.widget.NumberPicker;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import static org.junit.Assert.*;
import java.io.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicLong;

public class StartTimeDialogTest {
    private final Instrumentation runner = InstrumentationRegistry.getInstrumentation();
    private Activity activity;
    private StartTimeDialog dialog;
    private AtomicLong result;
    private long initial;
    @Before public void open() {
        initial = LocalDate.of(2026, 9, 13).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        result = new AtomicLong(initial);
        activity = runner.startActivitySync(new Intent(runner.getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        runner.runOnMainSync(() -> { dialog = new StartTimeDialog(activity, initial, null, result::set); dialog.show(); });
        runner.waitForIdleSync();
    }
    @After public void close() { runner.runOnMainSync(() -> { dialog.dismiss(); activity.finish(); }); }
    private View find(String tag) { return dialog.getWindow().getDecorView().findViewWithTag(tag); }
    private void click(String tag) {
        runner.runOnMainSync(() -> { View v = find(tag); assertNotNull("Missing " + tag, v); v.performClick(); }); runner.waitForIdleSync();
    }
    private void set(String tag, int value) { runner.runOnMainSync(() -> ((NumberPicker) find(tag)).setValue(value)); }
    private void screenshot(String name) throws Exception {
        SystemClock.sleep(800); Bitmap bitmap = runner.getUiAutomation().takeScreenshot();
        try (FileOutputStream out = new FileOutputStream(new File(runner.getTargetContext().getExternalFilesDir(null), name))) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); }
        bitmap.recycle();
    }
    @Test public void calendarAndTimeSwitchRetainsSelection() throws Exception {
        screenshot("date-picker.png"); click("day-2026-09-20"); click("time-tab");
        set("hour-wheel", 23); set("minute-wheel", 59); click("date-tab"); click("time-tab");
        runner.runOnMainSync(() -> { assertEquals(23, ((NumberPicker) find("hour-wheel")).getValue()); assertEquals(59, ((NumberPicker) find("minute-wheel")).getValue()); });
        screenshot("time-picker.png"); click("picker-confirm");
        ZonedDateTime selected = Instant.ofEpochMilli(result.get()).atZone(ZoneId.systemDefault());
        assertEquals(LocalDate.of(2026, 9, 20), selected.toLocalDate()); assertEquals(23, selected.getHour()); assertEquals(59, selected.getMinute());
        assertFalse(dialog.isShowing());
    }
    @Test public void cancelDiscardsBothDateAndTime() {
        click("next-month"); click("day-2026-10-05"); click("time-tab"); set("hour-wheel", 0); set("minute-wheel", 1); click("picker-cancel");
        assertEquals(initial, result.get()); assertFalse(dialog.isShowing());
    }
    @Test public void leapDayYearNavigationAndRestoredDraft() {
        click("month-select"); set("year-wheel", 2028); set("month-wheel", 2); click("picker-confirm");
        assertEquals(initial, result.get()); assertTrue(dialog.isShowing()); click("day-2028-02-29"); click("time-tab");
        set("hour-wheel", 0); set("minute-wheel", 0);
        runner.runOnMainSync(() -> { Bundle state = dialog.selectionState(); dialog.dismiss(); dialog = new StartTimeDialog(activity, initial, state, result::set); dialog.show(); });
        click("date-tab"); runner.runOnMainSync(() -> assertTrue(find("day-2028-02-29").isSelected()));
        click("picker-confirm"); assertEquals(LocalDate.of(2028, 2, 29).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), result.get());
    }
    @Test public void monthArrowsCrossYearWithoutChangingSelection() {
        click("month-select"); set("year-wheel", 2026); set("month-wheel", 12); click("picker-confirm"); click("next-month");
        runner.runOnMainSync(() -> assertNotNull(find("day-2027-01-15")));
        click("previous-month"); runner.runOnMainSync(() -> assertNotNull(find("day-2026-12-15")));
        click("picker-cancel"); assertEquals(initial, result.get());
    }
    @Test public void timeWheelScrollChangesMinuteWithoutCommitting() {
        click("time-tab");
        runner.runOnMainSync(() -> assertTrue(find("minute-wheel").getAccessibilityNodeProvider().performAction(View.NO_ID, android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, null)));
        SystemClock.sleep(600); click("date-tab"); click("time-tab");
        runner.runOnMainSync(() -> assertEquals(1, ((NumberPicker) find("minute-wheel")).getValue()));
        assertEquals(initial, result.get()); click("picker-confirm"); assertEquals(initial + 60000, result.get());
    }
}

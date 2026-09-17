package com.ckun.reminder;

import android.app.*;
import android.content.*;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class AgendaCalendarTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = instrumentation.getTargetContext();
    @Test public void homeUsesCompactCreamLayoutWithoutRedundantCopy() {
        Task task = new Task(); task.title = "紧凑卡片"; task.start = System.currentTimeMillis() + 60000;
        try (TaskStore store = new TaskStore(context)) { store.save(task); }
        Activity activity = instrumentation.startActivitySync(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.runOnMainSync(() -> {
                View root = activity.getWindow().getDecorView();
                TextView homeDate = root.findViewWithTag("home-date"); assertNotNull(homeDate); assertTrue(homeDate.getText().toString().matches("\\d{1,2}月\\d{1,2}日")); assertEquals(1, homeDate.getMaxLines());
                assertEquals(dp(30), root.findViewWithTag("goals").getLayoutParams().height); assertEquals(dp(30), root.findViewWithTag("timetable").getLayoutParams().height); assertEquals(dp(30), root.findViewWithTag("statistics").getLayoutParams().height);
                assertNotNull(root.findViewWithTag("task-card-" + task.id));
                assertNull(findText(root, "轻待办"));
                assertNull(findText(root, "仅保存在此设备 · 无需联网"));
                View handle = root.findViewWithTag("agenda-handle");
                assertEquals(dp(40), handle.getLayoutParams().height);
                View complete = root.findViewWithTag("complete-" + task.id);
                assertEquals(dp(38), complete.getLayoutParams().width);
                assertNull(findText(root, Task.PRIORITY_LABELS[task.priorityIndex()]));
            });
        } finally {
            instrumentation.runOnMainSync(activity::finish);
            try (TaskStore store = new TaskStore(context)) { store.delete(task.id); }
        }
    }
    @Test public void monthHeadingIsAnAccessiblePickerAction() {
        Activity activity = instrumentation.startActivitySync(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.runOnMainSync(() -> {
                View heading = activity.getWindow().getDecorView().findViewWithTag("calendar-month");
                assertEquals("选择年份和月份", heading.getContentDescription());
                assertTrue(heading.performClick());
            });
        } finally { instrumentation.runOnMainSync(activity::finish); }
    }
    @Test public void selectedDayShowsOpenThenCompletedAndHandleRespondsToSwipe() {
        Task today = new Task(), tomorrow = new Task();
        today.title = "今天的事项"; today.start = System.currentTimeMillis(); today.priority = 0;
        Calendar next = Calendar.getInstance(); next.add(Calendar.DATE, 1);
        tomorrow.title = "明天的事项"; tomorrow.start = next.getTimeInMillis(); tomorrow.done = true;
        try (TaskStore store = new TaskStore(context)) { store.save(today); store.save(tomorrow); }
        Activity activity = instrumentation.startActivitySync(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.runOnMainSync(() -> {
                View root = activity.getWindow().getDecorView();
                assertNotNull(root.findViewWithTag("task-time-" + today.id));
                assertNull(root.findViewWithTag("task-time-" + tomorrow.id));
                Button complete = root.findViewWithTag("complete-" + today.id); assertEquals("✓", complete.getText().toString());
                assertTrue(complete.getWidth() > 0);
                android.graphics.Rect visible = new android.graphics.Rect();
                assertTrue("Agenda must be visible below the month", complete.getGlobalVisibleRect(visible));
                assertEquals(complete.getWidth(), complete.getHeight());
                View calendar = root.findViewWithTag("agenda-calendar"); assertEquals(1f, calendar.getAlpha(), 0f);
                root.findViewWithTag("agenda-handle").performClick();
                assertSame("mode toggle must not rebuild and flash the calendar", calendar, root.findViewWithTag("agenda-calendar"));
                activity.getWindow().getDecorView().findViewWithTag("complete-" + today.id).performClick();
                assertNotNull(activity.getWindow().getDecorView().findViewWithTag("task-time-" + today.id));
                assertEquals(.48f, ((View) activity.getWindow().getDecorView().findViewWithTag("task-card-" + today.id).getParent()).getAlpha(), 0f);
                assertNull(findText(activity.getWindow().getDecorView(), "未完成"));
                assertNull(findText(activity.getWindow().getDecorView(), "已完成"));
                String tag = "day-" + new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(tomorrow.start));
                activity.getWindow().getDecorView().findViewWithTag(tag).performClick();
                assertNotNull(activity.getWindow().getDecorView().findViewWithTag("task-time-" + tomorrow.id));
                assertNull(activity.getWindow().getDecorView().findViewWithTag("task-time-" + today.id));
            });
        } finally {
            instrumentation.runOnMainSync(activity::finish);
            try (TaskStore store = new TaskStore(context)) { store.delete(today.id); store.delete(tomorrow.id); }
        }
    }
    @Test public void swipingAnywhereOnCalendarCollapsesAndExpandsIt() {
        Activity activity = instrumentation.startActivitySync(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.runOnMainSync(() -> {
                View calendar = activity.getWindow().getDecorView().findViewWithTag("agenda-calendar");
                View viewport = activity.getWindow().getDecorView().findViewWithTag("calendar-viewport"); int expanded = viewport.getLayoutParams().height;
                long now = SystemClock.uptimeMillis();
                calendar.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 30, 160, 0));
                calendar.dispatchTouchEvent(MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_MOVE, 30, 90, 0));
                assertTrue("calendar height follows the finger", viewport.getLayoutParams().height < expanded);
                calendar.dispatchTouchEvent(MotionEvent.obtain(now, now + 80, MotionEvent.ACTION_UP, 30, 40, 0));
            });
            SystemClock.sleep(300); instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(() -> {
                View viewport = activity.getWindow().getDecorView().findViewWithTag("calendar-viewport");
                View grid = activity.getWindow().getDecorView().findViewWithTag("calendar-grid"); assertTrue(viewport.getLayoutParams().height < grid.getLayoutParams().height);
                View calendar = activity.getWindow().getDecorView().findViewWithTag("agenda-calendar");
                long now = SystemClock.uptimeMillis(); int collapsed = viewport.getLayoutParams().height;
                calendar.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 30, 40, 0));
                calendar.dispatchTouchEvent(MotionEvent.obtain(now, now + 40, MotionEvent.ACTION_MOVE, 30, 100, 0));
                assertTrue("calendar expands continuously while dragging", viewport.getLayoutParams().height > collapsed);
                calendar.dispatchTouchEvent(MotionEvent.obtain(now, now + 80, MotionEvent.ACTION_UP, 30, 160, 0));
            });
            SystemClock.sleep(300); instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(() -> {
                View viewport = activity.getWindow().getDecorView().findViewWithTag("calendar-viewport"); View grid = activity.getWindow().getDecorView().findViewWithTag("calendar-grid");
                assertEquals(grid.getLayoutParams().height, viewport.getLayoutParams().height);
            });
        } finally { instrumentation.runOnMainSync(activity::finish); }
    }
    @Test public void leapDayAndYearNavigationKeepValidDates() {
        instrumentation.runOnMainSync(() -> {
            Calendar date = Calendar.getInstance(); date.clear(); date.set(2028, Calendar.FEBRUARY, 29);
            long[] selected = {0}; boolean[] week = {false};
            AgendaCalendar calendar = new AgendaCalendar(context, date.getTimeInMillis(), false, new ArrayList<>(), (d, w) -> { selected[0] = d; week[0] = w; }, w -> week[0] = w);
            assertNotNull(calendar.findViewWithTag("day-2028-02-29"));
            clickText(calendar, "›"); date.setTimeInMillis(selected[0]); assertEquals(Calendar.MARCH, date.get(Calendar.MONTH)); assertEquals(29, date.get(Calendar.DATE));
            date.set(2028, Calendar.DECEMBER, 31);
            calendar = new AgendaCalendar(context, date.getTimeInMillis(), false, new ArrayList<>(), (d, w) -> selected[0] = d, w -> week[0] = w);
            clickText(calendar, "›"); date.setTimeInMillis(selected[0]); assertEquals(2029, date.get(Calendar.YEAR)); assertEquals(Calendar.JANUARY, date.get(Calendar.MONTH));
            date.clear(); date.set(2028, Calendar.JUNE, 15); selected[0] = 0;
            calendar = new AgendaCalendar(context, date.getTimeInMillis(), false, new ArrayList<>(), (d, w) -> selected[0] = d, w -> {});
            long now = SystemClock.uptimeMillis(); calendar.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 200, 100, 0)); calendar.dispatchTouchEvent(MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, 80, 100, 0));
            date.setTimeInMillis(selected[0]); assertEquals(Calendar.JULY, date.get(Calendar.MONTH));
        });
    }
    @Test public void eachDayShowsOnlyOneHighestPriorityDot() {
        instrumentation.runOnMainSync(() -> {
            Calendar day=Calendar.getInstance();day.clear();day.set(2028,Calendar.JUNE,15,9,0);Task normal=new Task();normal.start=day.getTimeInMillis();normal.priority=3;Task urgent=new Task();urgent.start=day.getTimeInMillis();urgent.priority=0;
            AgendaCalendar calendar=new AgendaCalendar(context,day.getTimeInMillis(),false,List.of(normal,urgent),(d,w)->{},w->{});View dot=calendar.findViewWithTag("day-dot-2028-06-15");assertNotNull(dot);assertEquals(1,((ViewGroup)dot.getParent()).getChildCount());
        });
    }
    private boolean clickText(View view, String value) {
        if (view instanceof Button && value.contentEquals(((Button) view).getText())) { view.performClick(); return true; }
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) if (clickText(((ViewGroup) view).getChildAt(i), value)) return true;
        return false;
    }
    private TextView findText(View view, String value) {
        if (view instanceof TextView && value.contentEquals(((TextView) view).getText())) return (TextView) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            TextView found = findText(((ViewGroup) view).getChildAt(i), value); if (found != null) return found;
        }
        return null;
    }
    private int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
}

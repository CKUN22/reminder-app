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
    @Test public void selectedDayFiltersBothTabsAndHandleRespondsToSwipe() {
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
                View handle = root.findViewWithTag("agenda-handle"); long now = SystemClock.uptimeMillis();
                MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 20, 100, 0);
                MotionEvent up = MotionEvent.obtain(now, now + 100, MotionEvent.ACTION_UP, 20, 0, 0);
                handle.dispatchTouchEvent(down); handle.dispatchTouchEvent(up); down.recycle(); up.recycle();
                LinearLayout grid = activity.getWindow().getDecorView().findViewWithTag("calendar-grid"); assertEquals(1, grid.getChildCount());
                activity.getWindow().getDecorView().findViewWithTag("agenda-handle").performClick();
                grid = activity.getWindow().getDecorView().findViewWithTag("calendar-grid"); assertTrue(grid.getChildCount() >= 4);
                activity.getWindow().getDecorView().findViewWithTag("complete-" + today.id).performClick();
                assertNull(activity.getWindow().getDecorView().findViewWithTag("task-time-" + today.id));
                clickText(activity.getWindow().getDecorView(), "已完成");
                assertNotNull(activity.getWindow().getDecorView().findViewWithTag("task-time-" + today.id));
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
    @Test public void leapDayAndYearNavigationKeepValidDates() {
        instrumentation.runOnMainSync(() -> {
            Calendar date = Calendar.getInstance(); date.clear(); date.set(2028, Calendar.FEBRUARY, 29);
            long[] selected = {0}; boolean[] week = {false};
            AgendaCalendar calendar = new AgendaCalendar(context, date.getTimeInMillis(), false, false, new ArrayList<>(), (d, w) -> { selected[0] = d; week[0] = w; });
            assertNotNull(calendar.findViewWithTag("day-2028-02-29"));
            clickText(calendar, "›"); date.setTimeInMillis(selected[0]); assertEquals(Calendar.MARCH, date.get(Calendar.MONTH)); assertEquals(29, date.get(Calendar.DATE));
            date.set(2028, Calendar.DECEMBER, 31);
            calendar = new AgendaCalendar(context, date.getTimeInMillis(), false, false, new ArrayList<>(), (d, w) -> selected[0] = d);
            clickText(calendar, "›"); date.setTimeInMillis(selected[0]); assertEquals(2029, date.get(Calendar.YEAR)); assertEquals(Calendar.JANUARY, date.get(Calendar.MONTH));
        });
    }
    private boolean clickText(View view, String value) {
        if (view instanceof Button && value.contentEquals(((Button) view).getText())) { view.performClick(); return true; }
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) if (clickText(((ViewGroup) view).getChildAt(i), value)) return true;
        return false;
    }
}

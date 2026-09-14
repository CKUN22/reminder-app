package com.ckun.reminder;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Month/week selector with a draggable, accessible agenda handle. */
@android.annotation.SuppressLint("ViewConstructor") // Built with task data, never inflated from XML.
final class AgendaCalendar extends LinearLayout {
    interface Selection { void select(long day, boolean week); }
    private static final int GREEN = 0xff52694D, INK = 0xff27332B, MUTED = 0xff758073;
    private final long selected;
    private final boolean week;
    private final Selection selection;

    @android.annotation.SuppressLint("ClickableViewAccessibility") // Taps return false to Button's native performClick; swipes have an equivalent click action.
    AgendaCalendar(Context context, long selected, boolean week, boolean completed, List<Task> tasks, Selection selection) {
        super(context); this.selected = selected; this.week = week; this.selection = selection;
        setOrientation(VERTICAL);
        LinearLayout header = new LinearLayout(context); header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(action("‹", "上一" + (week ? "周" : "月"), () -> move(-1)), new LayoutParams(dp(48), dp(48)));
        TextView month = label(new SimpleDateFormat("yyyy年M月", Locale.CHINA).format(new Date(selected)), 21, INK);
        month.setTypeface(null, Typeface.BOLD); month.setTag("calendar-month"); header.addView(month, new LayoutParams(0, dp(48), 1));
        header.addView(action("›", "下一" + (week ? "周" : "月"), () -> move(1)), new LayoutParams(dp(48), dp(48)));
        header.addView(action("今天", "回到今天", () -> selection.select(System.currentTimeMillis(), week)), new LayoutParams(dp(54), dp(48)));
        addView(header);
        LinearLayout weekdays = new LinearLayout(context);
        for (String day : new String[]{"一", "二", "三", "四", "五", "六", "日"}) weekdays.addView(label(day, 12, MUTED), new LayoutParams(0, dp(28), 1));
        addView(weekdays);
        Calendar cursor = Calendar.getInstance(); cursor.setTimeInMillis(selected);
        int monthIndex = cursor.get(Calendar.MONTH);
        if (!week) cursor.set(Calendar.DAY_OF_MONTH, 1);
        int offset = (cursor.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        int rows = week ? 1 : (offset + cursor.getActualMaximum(Calendar.DAY_OF_MONTH) + 6) / 7;
        cursor.add(Calendar.DAY_OF_MONTH, -offset);
        LinearLayout grid = new LinearLayout(context); grid.setOrientation(VERTICAL); grid.setTag("calendar-grid");
        // Compact cells in landscape leave room for the agenda and its handle.
        int height = getResources().getConfiguration().screenHeightDp < 600 ? 32 : 48;
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(context);
            for (int c = 0; c < 7; c++) {
                final long day = cursor.getTimeInMillis(); boolean active = sameDay(day, selected);
                LinearLayout cell = new LinearLayout(context); cell.setOrientation(VERTICAL); cell.setGravity(Gravity.CENTER);
                cell.setTag("day-" + new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(day)));
                if (active) cell.setBackground(shape(GREEN, 18));
                cell.addView(label(String.valueOf(cursor.get(Calendar.DAY_OF_MONTH)), 17, active ? Color.WHITE : cursor.get(Calendar.MONTH) == monthIndex ? INK : MUTED));
                LinearLayout dots = new LinearLayout(context); dots.setGravity(Gravity.CENTER);
                for (int priority = 0; priority < 4; priority++) {
                    boolean exists = false;
                    for (Task task : tasks) if (task.done == completed && task.priorityIndex() == priority && sameDay(task.start, day)) { exists = true; break; }
                    if (exists) {
                        View dot = new View(context); dot.setBackground(shape(active ? Color.WHITE : Task.PRIORITY_COLORS[priority], 3));
                        LayoutParams params = new LayoutParams(dp(5), dp(5)); params.setMargins(dp(1), 0, dp(1), 0); dots.addView(dot, params);
                    }
                }
                cell.addView(dots, new LayoutParams(-1, dp(8)));
                cell.setContentDescription(new SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(new Date(day)) + (active ? "，已选择" : ""));
                cell.setFocusable(true); cell.setOnClickListener(v -> selection.select(day, week));
                row.addView(cell, new LayoutParams(0, dp(height), 1)); cursor.add(Calendar.DAY_OF_MONTH, 1);
            }
            grid.addView(row);
        }
        addView(grid);
        Button handle = action(week ? "⌄  下拉展开月历" : "⌃  上拉查看日程", week ? "展开月历" : "收起月历", () -> selection.select(selected, !week));
        handle.setTag("agenda-handle");
        final float[] down = new float[1];
        handle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) { down[0] = event.getRawY(); v.getParent().requestDisallowInterceptTouchEvent(true); }
            if (event.getActionMasked() == MotionEvent.ACTION_UP && Math.abs(event.getRawY() - down[0]) > dp(20)) {
                selection.select(selected, event.getRawY() < down[0]); return true;
            }
            return false;
        });
        addView(handle, new LayoutParams(-1, dp(48)));
    }
    static boolean sameDay(long a, long b) {
        Calendar first = Calendar.getInstance(), second = Calendar.getInstance(); first.setTimeInMillis(a); second.setTimeInMillis(b);
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }
    private void move(int direction) {
        Calendar date = Calendar.getInstance(); date.setTimeInMillis(selected);
        date.add(week ? Calendar.WEEK_OF_YEAR : Calendar.MONTH, direction); selection.select(date.getTimeInMillis(), week);
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private TextView label(String value, int size, int color) { TextView t = new TextView(getContext()); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER); return t; }
    private Button action(String value, String description, Runnable run) {
        Button b = new Button(getContext()); b.setText(value); b.setTextSize(13); b.setTextColor(GREEN); b.setAllCaps(false);
        b.setMinWidth(0); b.setMinHeight(0); b.setPadding(0, 0, 0, 0); b.setBackground(shape(0xffE8EDDF, 16));
        b.setContentDescription(description); b.setOnClickListener(v -> run.run()); return b;
    }
}

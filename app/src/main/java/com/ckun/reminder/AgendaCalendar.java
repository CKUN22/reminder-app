package com.ckun.reminder;

import android.animation.ValueAnimator;
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
    interface ModeChange { void change(boolean week); }
    private static final int GREEN = 0xff7C9270, INK = 0xff4A3E35, MUTED = 0xff8E7D70, CREAM = 0xffF5EBDD;
    private final long selected;
    private boolean week;
    private final Selection selection;
    private final ModeChange modeChange;
    private float gestureDownX, gestureDownY;
    private boolean trackingGesture;
    private float dragStartProgress, expansionProgress;
    private final FrameLayout gridViewport;
    private final LinearLayout grid;
    private final int cellHeight, rowCount, selectedRow;
    private final Button handle;

    @android.annotation.SuppressLint("ClickableViewAccessibility") // Taps return false to Button's native performClick; swipes have an equivalent click action.
    AgendaCalendar(Context context, long selected, boolean week, List<Task> tasks, Selection selection, ModeChange modeChange) {
        super(context); this.selected = selected; this.week = week; this.selection = selection; this.modeChange = modeChange;
        setOrientation(VERTICAL); setTag("agenda-calendar");
        LinearLayout header = new LinearLayout(context); header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(action("‹", "上一" + (week ? "周" : "月"), () -> move(-1)), new LayoutParams(dp(40), dp(40)));
        TextView month = label(new SimpleDateFormat("yyyy年M月", Locale.CHINA).format(new Date(selected)), 18, INK);
        month.setTypeface(Typeface.create("serif", Typeface.BOLD)); month.setTag("calendar-month");
        month.setContentDescription("选择年份和月份"); month.setFocusable(true); month.setOnClickListener(v -> chooseMonth());
        header.addView(month, new LayoutParams(0, dp(40), 1));
        header.addView(action("›", "下一" + (week ? "周" : "月"), () -> move(1)), new LayoutParams(dp(40), dp(40)));
        header.addView(action("今天", "回到今天", () -> selection.select(System.currentTimeMillis(), week)), new LayoutParams(dp(50), dp(40)));
        addView(header);
        LinearLayout weekdays = new LinearLayout(context);
        for (String day : new String[]{"一", "二", "三", "四", "五", "六", "日"}) weekdays.addView(label(day, 11, MUTED), new LayoutParams(0, dp(22), 1));
        addView(weekdays);
        Calendar cursor = Calendar.getInstance(); cursor.setTimeInMillis(selected);
        int monthIndex = cursor.get(Calendar.MONTH), selectedDay = cursor.get(Calendar.DAY_OF_MONTH);
        cursor.set(Calendar.DAY_OF_MONTH, 1);
        int offset = (cursor.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        rowCount = (offset + cursor.getActualMaximum(Calendar.DAY_OF_MONTH) + 6) / 7;
        selectedRow = (offset + selectedDay - 1) / 7;
        cursor.add(Calendar.DAY_OF_MONTH, -offset);
        grid = new LinearLayout(context); grid.setOrientation(VERTICAL); grid.setTag("calendar-grid");
        // Compact cells in landscape leave room for the agenda and its handle.
        cellHeight = dp(getResources().getConfiguration().screenHeightDp < 600 ? 30 : 40);
        for (int r = 0; r < rowCount; r++) {
            LinearLayout row = new LinearLayout(context);
            for (int c = 0; c < 7; c++) {
                final long day = cursor.getTimeInMillis(); boolean active = sameDay(day, selected);
                LinearLayout cell = new LinearLayout(context); cell.setOrientation(VERTICAL); cell.setGravity(Gravity.CENTER);
                cell.setTag("day-" + new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(day)));
                if (active) cell.setBackground(shape(GREEN, 20));
                cell.addView(label(String.valueOf(cursor.get(Calendar.DAY_OF_MONTH)), 15, active ? Color.WHITE : cursor.get(Calendar.MONTH) == monthIndex ? INK : MUTED));
                LinearLayout dots = new LinearLayout(context); dots.setGravity(Gravity.CENTER);
                int priority = AgendaRules.highestPriorityForDay(tasks, day);
                if (priority >= 0) { View dot = new View(context); dot.setTag("day-dot-" + new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(day))); dot.setBackground(shape(active ? Color.WHITE : Task.PRIORITY_COLORS[priority], 3)); dots.addView(dot, new LayoutParams(dp(5), dp(5))); }
                cell.addView(dots, new LayoutParams(-1, dp(8)));
                cell.setContentDescription(new SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(new Date(day)) + (active ? "，已选择" : ""));
                cell.setFocusable(true); cell.setOnClickListener(v -> selection.select(day, week));
                row.addView(cell, new LayoutParams(0, cellHeight, 1)); cursor.add(Calendar.DAY_OF_MONTH, 1);
            }
            grid.addView(row);
        }
        gridViewport = new FrameLayout(context); gridViewport.setTag("calendar-viewport"); gridViewport.setClipChildren(true); gridViewport.addView(grid, new FrameLayout.LayoutParams(-1, rowCount * cellHeight));
        addView(gridViewport); expansionProgress = week ? 0f : 1f; applyExpansion(expansionProgress);
        handle = action(week ? "⌄  下拉展开月历" : "⌃  上拉查看日程", week ? "展开月历" : "收起月历", () -> animateTo(week ? 1f : 0f));
        handle.setTag("agenda-handle");
        addView(handle, new LayoutParams(-1, dp(40)));
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            gestureDownX = event.getRawX(); gestureDownY = event.getRawY(); dragStartProgress = expansionProgress; trackingGesture = true;
            getParent().requestDisallowInterceptTouchEvent(true);
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && trackingGesture) {
            if (Math.abs(event.getRawY() - gestureDownY) > Math.abs(event.getRawX() - gestureDownX)) updateDrag(event.getRawY());
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP && trackingGesture) {
            float horizontal = event.getRawX() - gestureDownX, distance = event.getRawY() - gestureDownY; trackingGesture = false;
            if (Math.abs(horizontal) > dp(40) && Math.abs(horizontal) > Math.abs(distance)) { applyExpansion(dragStartProgress); move(horizontal < 0 ? 1 : -1); return true; }
            updateDrag(event.getRawY());
            if (Math.abs(distance) > dp(8)) { settleToNearest(); return true; }
        } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) trackingGesture = false;
        return super.dispatchTouchEvent(event);
    }
    private void updateDrag(float rawY) {
        float range = Math.max(dp(80), (rowCount - 1) * cellHeight);
        applyExpansion(Math.max(0f, Math.min(1f, dragStartProgress + (rawY - gestureDownY) / range)));
    }
    private void applyExpansion(float progress) {
        progress = Math.max(0f, Math.min(1f, progress)); expansionProgress = progress;
        ViewGroup.LayoutParams params = gridViewport.getLayoutParams();
        if (params == null) params = new LayoutParams(-1, cellHeight);
        params.height = Math.round(cellHeight + (rowCount - 1) * cellHeight * progress); gridViewport.setLayoutParams(params);
        grid.setTranslationY(-selectedRow * cellHeight * (1f - progress)); grid.setAlpha(.72f + .28f * progress);
    }
    private void settleToNearest() {
        animateTo(expansionProgress >= .5f ? 1f : 0f);
    }
    private void animateTo(float target) {
        boolean targetWeek = target == 0f;
        ValueAnimator animator = ValueAnimator.ofFloat(expansionProgress, target); animator.setDuration(220); animator.setInterpolator(new android.view.animation.OvershootInterpolator(.65f));
        animator.addUpdateListener(value -> applyExpansion((float) value.getAnimatedValue()));
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                applyExpansion(target);
                if (targetWeek != week) {
                    week = targetWeek; modeChange.change(week); updateHandle(); performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                }
            }
        }); animator.start();
    }
    private void updateHandle() {
        handle.setText(week ? "⌄  下拉展开月历" : "⌃  上拉查看日程");
        handle.setContentDescription(week ? "展开月历" : "收起月历");
    }
    static boolean sameDay(long a, long b) {
        Calendar first = Calendar.getInstance(), second = Calendar.getInstance(); first.setTimeInMillis(a); second.setTimeInMillis(b);
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }
    private void move(int direction) {
        Calendar date = Calendar.getInstance(); date.setTimeInMillis(selected);
        date.add(week ? Calendar.WEEK_OF_YEAR : Calendar.MONTH, direction); selection.select(date.getTimeInMillis(), week);
    }
    private void chooseMonth() {
        Calendar current = Calendar.getInstance(); current.setTimeInMillis(selected);
        LinearLayout pickers = new LinearLayout(getContext()); pickers.setPadding(dp(20), dp(8), dp(20), 0);
        NumberPicker year = new NumberPicker(getContext()); year.setTag("year-picker"); year.setMinValue(1970); year.setMaxValue(2100); year.setValue(current.get(Calendar.YEAR));
        NumberPicker month = new NumberPicker(getContext()); month.setTag("month-picker"); month.setMinValue(1); month.setMaxValue(12); month.setValue(current.get(Calendar.MONTH) + 1);
        pickers.addView(year, new LayoutParams(0, -2, 1)); pickers.addView(month, new LayoutParams(0, -2, 1));
        new android.app.AlertDialog.Builder(getContext()).setTitle("选择年份和月份").setView(pickers)
                .setNegativeButton("取消", null).setPositiveButton("确定", (dialog, which) -> {
                    Calendar chosen = Calendar.getInstance(); chosen.setTimeInMillis(selected);
                    int day = chosen.get(Calendar.DAY_OF_MONTH); chosen.set(Calendar.DAY_OF_MONTH, 1);
                    chosen.set(Calendar.YEAR, year.getValue()); chosen.set(Calendar.MONTH, month.getValue() - 1);
                    chosen.set(Calendar.DAY_OF_MONTH, Math.min(day, chosen.getActualMaximum(Calendar.DAY_OF_MONTH)));
                    selection.select(chosen.getTimeInMillis(), week);
                }).show();
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private TextView label(String value, int size, int color) { TextView t = new TextView(getContext()); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setTypeface(Typeface.create("serif", Typeface.NORMAL)); t.setGravity(Gravity.CENTER); return t; }
    private Button action(String value, String description, Runnable run) {
        Button b = new Button(getContext()); b.setText(value); b.setTextSize(13); b.setTextColor(GREEN); b.setAllCaps(false);
        b.setTypeface(Typeface.create("serif", Typeface.NORMAL)); b.setMinWidth(0); b.setMinHeight(0); b.setPadding(0, 0, 0, 0); b.setBackground(shape(CREAM, 20));
        b.setContentDescription(description); b.setOnClickListener(v -> run.run()); return b;
    }
}

package com.ckun.reminder;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class StatisticsActivity extends Activity {
    private static final int BG = 0xffFFF8ED, INK = 0xff4A3E35, MUTED = 0xff8E7D70, GREEN = 0xff7C9270, CARD = 0xffFFFCF7;
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setTypeface(Typeface.create("serif", Typeface.NORMAL)); view.setPadding(0, dp(4), 0, dp(4)); return view;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setBackgroundColor(BG); scroll.setTag("statistics-scroll");
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(22), dp(6), dp(22), dp(32)); scroll.addView(page); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop() + dp(12), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        Button back = new Button(this); back.setTag("statistics-back"); back.setText("‹  返回"); back.setAllCaps(false); back.setTextColor(GREEN); back.setTextSize(14); back.setTypeface(Typeface.create("serif", Typeface.NORMAL)); back.setBackground(shape(0xffF5EBDD, 20)); back.setOnClickListener(v -> finish());
        page.addView(back, new LinearLayout.LayoutParams(-2, dp(42)));
        TextView title = text("每周小成就", 28, INK); title.setTypeface(Typeface.create("serif", Typeface.BOLD)); title.setPadding(0, dp(20), 0, dp(2)); page.addView(title);
        page.addView(text("慢慢完成，也是在认真生活。", 14, MUTED));
        List<Task> tasks; try (TaskStore store = new TaskStore(this)) { tasks = store.all(); }
        long thisWeek = Statistics.weekStart(System.currentTimeMillis()); int current = Statistics.completedInWeek(tasks, thisWeek);
        LinearLayout hero = new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL); hero.setGravity(Gravity.CENTER); hero.setPadding(dp(20), dp(20), dp(20), dp(20)); hero.setBackground(shape(CARD, 28));
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, -2); heroParams.topMargin = dp(18); page.addView(hero, heroParams);
        hero.addView(center("本周已完成", 14, MUTED)); TextView number = center(String.valueOf(current), 44, GREEN); number.setTag("current-week-count"); number.setTypeface(Typeface.create("serif", Typeface.BOLD)); hero.addView(number); hero.addView(center("件小事", 14, MUTED));
        TextView history = text("近 10 周", 17, INK); history.setTypeface(Typeface.create("serif", Typeface.BOLD)); history.setPadding(0, dp(22), 0, dp(8)); page.addView(history);
        int[] counts = new int[10]; long[] starts = new long[10];
        for (int i = 0; i < 10; i++) {
            Calendar start = Calendar.getInstance(); start.setTimeInMillis(thisWeek); start.add(Calendar.DAY_OF_MONTH, -7 * (9 - i));
            starts[i] = start.getTimeInMillis(); counts[i] = Statistics.completedInWeek(tasks, starts[i]);
        }
        WeeklyBarChart chart = new WeeklyBarChart(this, counts, starts); chart.setTag("weekly-bar-chart"); chart.setBackground(shape(CARD, 24));
        page.addView(chart, new LinearLayout.LayoutParams(-1, dp(270)));
    }
    private TextView center(String value, int size, int color) { TextView view = text(value, size, color); view.setGravity(Gravity.CENTER); return view; }
}

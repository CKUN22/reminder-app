package com.ckun.reminder;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
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
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(22), dp(18), dp(22), dp(32)); scroll.addView(page); setContentView(scroll);
        Button back = new Button(this); back.setTag("statistics-back"); back.setText("‹  返回"); back.setAllCaps(false); back.setTextColor(GREEN); back.setTextSize(14); back.setTypeface(Typeface.create("serif", Typeface.NORMAL)); back.setBackground(shape(0xffF5EBDD, 20)); back.setOnClickListener(v -> finish());
        page.addView(back, new LinearLayout.LayoutParams(-2, dp(42)));
        TextView title = text("每周小成就", 28, INK); title.setTypeface(Typeface.create("serif", Typeface.BOLD)); title.setPadding(0, dp(20), 0, dp(2)); page.addView(title);
        page.addView(text("慢慢完成，也是在认真生活。", 14, MUTED));
        List<Task> tasks; try (TaskStore store = new TaskStore(this)) { tasks = store.all(); }
        long thisWeek = Statistics.weekStart(System.currentTimeMillis()); int current = Statistics.completedInWeek(tasks, thisWeek);
        LinearLayout hero = new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL); hero.setGravity(Gravity.CENTER); hero.setPadding(dp(20), dp(20), dp(20), dp(20)); hero.setBackground(shape(CARD, 28));
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, -2); heroParams.topMargin = dp(18); page.addView(hero, heroParams);
        hero.addView(center("本周已完成", 14, MUTED)); TextView number = center(String.valueOf(current), 44, GREEN); number.setTag("current-week-count"); number.setTypeface(Typeface.create("serif", Typeface.BOLD)); hero.addView(number); hero.addView(center("件小事", 14, MUTED));
        TextView history = text("近 8 周", 17, INK); history.setTypeface(Typeface.create("serif", Typeface.BOLD)); history.setPadding(0, dp(22), 0, dp(8)); page.addView(history);
        SimpleDateFormat date = new SimpleDateFormat("M月d日", Locale.CHINA); int max = 1; int[] counts = new int[8];
        for (int i = 0; i < 8; i++) { Calendar start = Calendar.getInstance(); start.setTimeInMillis(thisWeek); start.add(Calendar.DAY_OF_MONTH, -7 * i); counts[i] = Statistics.completedInWeek(tasks, start.getTimeInMillis()); max = Math.max(max, counts[i]); }
        for (int i = 0; i < 8; i++) {
            Calendar start = Calendar.getInstance(); start.setTimeInMillis(thisWeek); start.add(Calendar.DAY_OF_MONTH, -7 * i);
            LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(14), dp(9), dp(14), dp(9)); row.setBackground(shape(CARD, 20));
            row.addView(text(i == 0 ? "本周" : date.format(start.getTime()) + " 起", 13, MUTED), new LinearLayout.LayoutParams(dp(86), -2));
            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); bar.setMax(max); bar.setProgress(counts[i]); bar.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN)); row.addView(bar, new LinearLayout.LayoutParams(0, dp(8), 1));
            TextView count = text(counts[i] + " 件", 14, INK); count.setGravity(Gravity.RIGHT); row.addView(count, new LinearLayout.LayoutParams(dp(52), -2));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.bottomMargin = dp(8); page.addView(row, params);
        }
    }
    private TextView center(String value, int size, int color) { TextView view = text(value, size, color); view.setGravity(Gravity.CENTER); return view; }
}

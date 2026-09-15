package com.ckun.reminder;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.*;

final class WeeklyBarChart extends View {
    private static final int GREEN = 0xff7C9270, ORANGE = 0xffD98A4E, PALE = 0xffE9DFD1, INK = 0xff4A3E35, MUTED = 0xff8E7D70;
    private final int[] counts;
    private final long[] starts;
    private final boolean[] highlights;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SimpleDateFormat date = new SimpleDateFormat("M/d", Locale.CHINA);
    WeeklyBarChart(Context context, int[] counts, long[] starts, boolean[] highlights) {
        super(context); this.counts = counts.clone(); this.starts = starts.clone(); this.highlights = highlights.clone();
        StringBuilder description = new StringBuilder("10周完成统计，最近8周和历史高峰2周：");
        for (int i = 0; i < counts.length; i++) description.append(label(i)).append('，').append(counts[i]).append("件；");
        setContentDescription(description.toString()); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float labelX = dp(14), barLeft = dp(76), right = getWidth() - dp(38), top = dp(18), bottom = getHeight() - dp(18);
        int max = 1; for (int count : counts) max = Math.max(max, count);
        float slot = (bottom - top) / counts.length; float barHeight = Math.min(dp(20), slot * .55f);
        paint.setTypeface(Typeface.create("serif", Typeface.NORMAL)); paint.setTextAlign(Paint.Align.LEFT);
        for (int i = 0; i < counts.length; i++) {
            float center = top + slot * (i + .5f); float length = counts[i] == 0 ? dp(7) : (right - barLeft) * counts[i] / max;
            paint.setTextSize(dp(10)); paint.setColor(highlights[i] ? ORANGE : MUTED); canvas.drawText(label(i), labelX, center + dp(4), paint);
            paint.setColor(counts[i] == 0 ? PALE : highlights[i] ? ORANGE : GREEN);
            float barRight = barLeft + length, radius = barHeight / 2;
            canvas.drawRoundRect(barLeft, center - barHeight / 2, barRight, center + barHeight / 2, radius, radius, paint);
            canvas.drawRect(barLeft, center - barHeight / 2, Math.min(barRight, barLeft + radius), center + barHeight / 2, paint);
            paint.setTextSize(dp(10)); paint.setColor(INK); canvas.drawText(counts[i] + "件", barRight + dp(6), center + dp(4), paint);
        }
    }
    private String label(int index) {
        if (starts[index] == 0) return "暂无历史";
        if (index == 0 && !highlights[index]) return "本周";
        return date.format(new Date(starts[index]));
    }
}

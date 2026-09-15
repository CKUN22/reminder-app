package com.ckun.reminder;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.*;

final class WeeklyBarChart extends View {
    private static final int GREEN = 0xff7C9270, PALE = 0xffE9DFD1, INK = 0xff4A3E35, MUTED = 0xff8E7D70;
    private final int[] counts;
    private final long[] starts;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SimpleDateFormat date = new SimpleDateFormat("M/d", Locale.CHINA);
    WeeklyBarChart(Context context, int[] counts, long[] starts) {
        super(context); this.counts = counts.clone(); this.starts = starts.clone();
        StringBuilder description = new StringBuilder("近10周完成统计：");
        for (int i = 0; i < counts.length; i++) description.append(date.format(new Date(starts[i]))).append('，').append(counts[i]).append("件；");
        setContentDescription(description.toString()); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = dp(14), right = getWidth() - dp(14), top = dp(28), bottom = getHeight() - dp(42);
        int max = 1; for (int count : counts) max = Math.max(max, count);
        float slot = (right - left) / counts.length; float barWidth = Math.min(dp(20), slot * .56f);
        paint.setTypeface(Typeface.create("serif", Typeface.NORMAL)); paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < counts.length; i++) {
            float center = left + slot * (i + .5f); float height = counts[i] == 0 ? dp(5) : (bottom - top) * counts[i] / max;
            paint.setColor(counts[i] == 0 ? PALE : GREEN);
            canvas.drawRoundRect(center - barWidth / 2, bottom - height, center + barWidth / 2, bottom, dp(9), dp(9), paint);
            paint.setTextSize(dp(11)); paint.setColor(INK); canvas.drawText(String.valueOf(counts[i]), center, bottom - height - dp(7), paint);
            paint.setTextSize(dp(9)); paint.setColor(MUTED); canvas.drawText(i == counts.length - 1 ? "本周" : date.format(new Date(starts[i])), center, bottom + dp(20), paint);
        }
    }
}

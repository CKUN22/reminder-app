package com.ckun.reminder;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class ReminderSettingsActivity extends Activity {
    private final TreeSet<Integer> selected = new TreeSet<>(), options = new TreeSet<>();
    private long start;
    private LinearLayout page;
    private TextView count;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); start = getIntent().getLongExtra("start", 0);
        int[] values = state == null ? getIntent().getIntArrayExtra("reminders") : state.getIntArray("reminders");
        if (values != null) for (int n : values) selected.add(n);
        for (int n : new int[]{0, 5, 10, 15, 30, 60, 120, 1440, 2880, 10080}) options.add(n);
        if (state != null) for (int n : state.getIntArray("options")) options.add(n);
        options.addAll(selected); render();
    }
    private int[] values(TreeSet<Integer> set) { return set.stream().mapToInt(Integer::intValue).toArray(); }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putIntArray("reminders", values(selected)); out.putIntArray("options", values(options)); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private GradientDrawable background(int color) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(18)); return d; }
    private TextView text(String value, int size) { TextView t = new TextView(this); t.setText(value); t.setTextColor(0xff202020); t.setTextSize(size); t.setGravity(Gravity.CENTER_VERTICAL); return t; }
    private void render() {
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(0xffF0F1F5);
        page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(18), dp(12), dp(18), dp(24)); scroll.addView(page); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> { v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()); return insets; });
        TextView back = text("‹    提醒", 24); back.setTypeface(null, Typeface.BOLD); back.setTag("reminders-back"); back.setContentDescription("返回事项，保留提醒选择"); back.setOnClickListener(v -> finishSelection()); page.addView(back, new LinearLayout.LayoutParams(-1, dp(64)));
        count = text("", 15); count.setGravity(Gravity.CENTER); count.setTextColor(0xff808080); updateCount(); page.addView(count, new LinearLayout.LayoutParams(-1, dp(50)));
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setBackground(background(Color.WHITE)); card.setPadding(dp(18), 0, dp(18), 0); page.addView(card);
        for (int offset : options) {
            LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
            boolean expired = start - offset * 60_000L <= System.currentTimeMillis();
            TextView label = text(ReminderRules.reminderLabel(offset) + (expired ? " · 时间已过" : ""), 17);
            if (expired) label.setTextColor(0xff999999);
            row.addView(label, new LinearLayout.LayoutParams(0, dp(56), 1));
            CheckBox box = new CheckBox(this); box.setTag("reminder-" + offset); box.setContentDescription(ReminderRules.reminderLabel(offset));
            box.setButtonTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}}, new int[]{0xff0068FF, 0xffBDBDBD})); box.setChecked(selected.contains(offset)); box.setEnabled(!expired || box.isChecked());
            box.setOnCheckedChangeListener((v, checked) -> { if (checked) selected.add(offset); else selected.remove(offset); if (expired && !checked) box.setEnabled(false); updateCount(); });
            row.addView(box, new LinearLayout.LayoutParams(dp(48), dp(56))); row.setOnClickListener(v -> { if (box.isEnabled()) box.toggle(); }); card.addView(row);
            if (offset != options.last()) { View line = new View(this); line.setBackgroundColor(0xffEEEEEE); card.addView(line, new LinearLayout.LayoutParams(-1, dp(1))); }
        }
        TextView custom = text("添加自定义  ›", 18); custom.setTag("custom-reminder"); custom.setPadding(dp(18), 0, dp(18), 0); custom.setBackground(background(Color.WHITE)); custom.setOnClickListener(v -> custom());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(60)); params.topMargin = dp(18); page.addView(custom, params);
        TextView hint = text("返回后保留选择，保存事项后生效。\n可不选提醒；已过期的提醒不会补发。", 13); hint.setTextColor(0xff808080); hint.setPadding(0, dp(16), 0, 0); page.addView(hint);
    }
    private void updateCount() { count.setText(getString(R.string.reminder_count, selected.size())); }
    private void custom() {
        EditText input = new EditText(this); input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); input.setHint("提前多少分钟（1–525600）"); input.setTag("custom-minutes");
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("自定义提前提醒").setView(input).setNegativeButton("取消", null).setPositiveButton("添加", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            try {
                int minutes = Integer.parseInt(input.getText().toString());
                if (minutes < 1 || minutes > 525600) { input.setError("请输入 1–525600 分钟"); return; }
                if (start - minutes * 60_000L <= System.currentTimeMillis()) { input.setError("这个提醒时间已经过去"); return; }
                options.add(minutes); selected.add(minutes); dialog.dismiss(); render();
            } catch (NumberFormatException e) { input.setError("请输入有效的分钟数"); }
        })); dialog.show();
    }
    private void finishSelection() { setResult(RESULT_OK, new Intent().putExtra("reminders", values(selected))); finish(); }
    @Override public void onBackPressed() { finishSelection(); }
}

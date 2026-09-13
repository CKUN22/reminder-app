package com.ckun.reminder;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.LongConsumer;

/** A single transactional date/time selector: only Confirm changes the editor. */
public final class StartTimeDialog extends Dialog {
    private static final int BLUE = 0xff0068FF, INK = 0xff202020, GRAY = 0xff999999;
    private LocalDate date;
    private YearMonth month;
    private int hour, minute;
    private String mode = "date";
    private final LongConsumer onConfirm;
    private LinearLayout root, body;
    private TextView dateChip, timeChip;
    private NumberPicker hours, minutes, years, months;

    public StartTimeDialog(Context context, long initial, Bundle state, LongConsumer onConfirm) {
        super(context, R.style.TimeSelectorTheme);
        this.onConfirm = onConfirm;
        ZonedDateTime value = Instant.ofEpochMilli(initial).atZone(ZoneId.systemDefault());
        date = value.toLocalDate(); hour = value.getHour(); minute = value.getMinute(); month = YearMonth.from(date);
        if (state != null) {
            date = LocalDate.parse(state.getString("date")); month = YearMonth.parse(state.getString("month"));
            hour = state.getInt("hour"); minute = state.getInt("minute"); mode = state.getString("mode", "date");
        }
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); requestWindowFeature(Window.FEATURE_NO_TITLE);
        root = column(); root.setPadding(dp(18), dp(22), dp(18), dp(14)); root.setBackground(shape(Color.WHITE, 28));
        TextView title = label("选择开始时间", 22, INK); title.setTypeface(null, Typeface.BOLD); root.addView(title, size(-1, 50));
        LinearLayout selection = new LinearLayout(getContext()); selection.setGravity(Gravity.CENTER_VERTICAL);
        TextView start = label("开始", 18, INK); start.setGravity(Gravity.CENTER_VERTICAL); selection.addView(start, new LinearLayout.LayoutParams(0, dp(64), 0.8f));
        dateChip = control("", "date-tab", () -> switchMode("date"));
        selection.addView(dateChip, new LinearLayout.LayoutParams(0, dp(42), 2.4f));
        timeChip = control("", "time-tab", () -> switchMode("time"));
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(0, dp(42), 1.1f); timeParams.leftMargin = dp(8); selection.addView(timeChip, timeParams);
        root.addView(selection); divider(root);
        body = column(); root.addView(body);
        LinearLayout actions = new LinearLayout(getContext()); actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.addView(control("取消", "picker-cancel", this::dismiss), new LinearLayout.LayoutParams(0, dp(56), 1));
        View split = new View(getContext()); split.setBackgroundColor(0xffDDDDDD); actions.addView(split, size(1, 24));
        actions.addView(control("确定", "picker-confirm", () -> {
            captureWheels();
            if (mode.equals("month")) { mode = "date"; render(); return; }
            long result = date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            onConfirm.accept(result); dismiss();
        }), new LinearLayout.LayoutParams(0, dp(56), 1));
        root.addView(actions);
        ScrollView scroll = new ScrollView(getContext()); scroll.setFillViewport(false); scroll.setBackground(shape(Color.WHITE, 28)); scroll.setClipToOutline(true); scroll.addView(root);
        setContentView(scroll); setCanceledOnTouchOutside(true); render();
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams p = window.getAttributes(); p.dimAmount = 0.25f; p.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL; p.y = dp(16); window.setAttributes(p);
        }
    }
    @Override public void show() { super.show(); resize(); }
    private void resize() {
        if (getWindow() == null) return;
        int width = Math.min(getContext().getResources().getDisplayMetrics().widthPixels - dp(28), dp(480));
        int maxHeight = getContext().getResources().getDisplayMetrics().heightPixels - dp(80);
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        getWindow().setLayout(width, Math.min(root.getMeasuredHeight(), maxHeight));
    }
    public Bundle selectionState() {
        captureWheels(); Bundle state = new Bundle(); state.putString("date", date.toString()); state.putString("month", month.toString());
        state.putInt("hour", hour); state.putInt("minute", minute); state.putString("mode", mode); return state;
    }
    private void captureWheels() {
        if (mode.equals("time") && hours != null) { hour = hours.getValue(); minute = minutes.getValue(); }
        if (mode.equals("month") && years != null) month = YearMonth.of(years.getValue(), months.getValue());
    }
    private void switchMode(String next) { captureWheels(); mode = next; render(); }
    private void render() {
        body.removeAllViews(); hours = null; minutes = null; years = null; months = null;
        dateChip.setText(date.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)));
        timeChip.setText(String.format(Locale.CHINA, "%02d:%02d", hour, minute));
        boolean timeMode = mode.equals("time");
        dateChip.setBackground(shape(timeMode ? 0xffF1F1F1 : BLUE, 24)); dateChip.setTextColor(timeMode ? INK : Color.WHITE);
        timeChip.setBackground(shape(timeMode ? BLUE : 0xffF1F1F1, 24)); timeChip.setTextColor(timeMode ? Color.WHITE : INK);
        dateChip.setSelected(!timeMode); timeChip.setSelected(timeMode);
        if (timeMode) renderTime(); else if (mode.equals("month")) renderMonth(); else renderCalendar();
        if (isShowing()) resize();
    }
    private void renderCalendar() {
        LinearLayout nav = new LinearLayout(getContext()); nav.setGravity(Gravity.CENTER_VERTICAL);
        TextView previous = control("‹", "previous-month", () -> { month = month.minusMonths(1); render(); });
        previous.setTextSize(30); previous.setTextColor(INK); previous.setContentDescription("上个月"); nav.addView(previous, size(44, 56));
        TextView monthTitle = control(month.getYear() + "年" + month.getMonthValue() + "月 ▾", "month-select", () -> switchMode("month"));
        monthTitle.setTextColor(INK); monthTitle.setTypeface(null, Typeface.BOLD); nav.addView(monthTitle, new LinearLayout.LayoutParams(0, dp(56), 1));
        TextView next = control("›", "next-month", () -> { month = month.plusMonths(1); render(); });
        next.setTextSize(30); next.setTextColor(INK); next.setContentDescription("下个月"); nav.addView(next, size(44, 56)); body.addView(nav);
        LinearLayout week = new LinearLayout(getContext());
        for (String name : new String[]{"日", "一", "二", "三", "四", "五", "六"}) week.addView(label(name, 15, GRAY), new LinearLayout.LayoutParams(0, dp(34), 1));
        body.addView(week);
        LocalDate first = month.atDay(1); int offset = first.getDayOfWeek().getValue() % 7;
        LocalDate from = first.minusDays(offset); int rows = (offset + month.lengthOfMonth() + 6) / 7;
        for (int row = 0; row < rows; row++) {
            LinearLayout line = new LinearLayout(getContext());
            for (int col = 0; col < 7; col++) {
                LocalDate day = from.plusDays(row * 7L + col);
                FrameLayout slot = new FrameLayout(getContext());
                TextView cell = control(String.valueOf(day.getDayOfMonth()), "day-" + day, () -> { date = day; month = YearMonth.from(day); render(); });
                cell.setTextSize(18); cell.setContentDescription(day.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)));
                boolean selected = day.equals(date); cell.setSelected(selected);
                cell.setTextColor(selected ? Color.WHITE : YearMonth.from(day).equals(month) ? INK : 0xffBBBBBB);
                if (selected) cell.setBackground(shape(BLUE, 24));
                slot.addView(cell, new FrameLayout.LayoutParams(dp(38), dp(38), Gravity.CENTER));
                line.addView(slot, new LinearLayout.LayoutParams(0, dp(46), 1));
            }
            body.addView(line);
        }
        body.setPadding(0, dp(8), 0, dp(16));
    }
    private NumberPicker wheel(int min, int max, int value, String tag) {
        NumberPicker picker = new NumberPicker(getContext()); picker.setMinValue(min); picker.setMaxValue(max); picker.setValue(value);
        picker.setTag(tag); picker.setContentDescription(tag.equals("hour-wheel") ? "小时" : tag.equals("minute-wheel") ? "分钟" : tag.equals("year-wheel") ? "年份" : "月份");
        picker.setWrapSelectorWheel(true); picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        if (max <= 59) picker.setFormatter(n -> String.format(Locale.CHINA, "%02d", n));
        if (android.os.Build.VERSION.SDK_INT >= 29) { picker.setTextColor(INK); picker.setTextSize(dp(25)); picker.setSelectionDividerHeight(dp(1)); }
        return picker;
    }
    private LinearLayout wheelRow() {
        LinearLayout row = new LinearLayout(getContext()); row.setGravity(Gravity.CENTER); body.setPadding(0, dp(24), 0, dp(24)); body.addView(row, size(-1, 210)); return row;
    }
    private void renderTime() {
        LinearLayout row = wheelRow(); hours = wheel(0, 23, hour, "hour-wheel"); minutes = wheel(0, 59, minute, "minute-wheel");
        row.addView(hours, size(100, 210)); row.addView(label("", 20, INK), size(20, 210)); row.addView(minutes, size(100, 210));
        NumberPicker.OnValueChangeListener changed = (picker, oldValue, newValue) -> {
            hour = hours.getValue(); minute = minutes.getValue(); timeChip.setText(String.format(Locale.CHINA, "%02d:%02d", hour, minute));
        };
        hours.setOnValueChangedListener(changed); minutes.setOnValueChangedListener(changed);
    }
    private void renderMonth() {
        LinearLayout row = wheelRow(); int year = month.getYear();
        years = wheel(Math.min(1900, year), Math.max(2199, year), year, "year-wheel"); months = wheel(1, 12, month.getMonthValue(), "month-wheel");
        row.addView(years, size(130, 210)); row.addView(label("年", 16, GRAY), size(30, 210)); row.addView(months, size(90, 210)); row.addView(label("月", 16, GRAY), size(30, 210));
    }
    private int dp(int n) { return Math.round(n * getContext().getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout c = new LinearLayout(getContext()); c.setOrientation(LinearLayout.VERTICAL); return c; }
    private LinearLayout.LayoutParams size(int width, int height) { return new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height)); }
    private GradientDrawable shape(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private TextView label(String text, int sp, int color) {
        TextView v = new TextView(getContext()); v.setText(text); v.setTextSize(sp); v.setTextColor(color); v.setGravity(Gravity.CENTER); return v;
    }
    private TextView control(String text, String tag, Runnable action) {
        TextView v = label(text, 17, BLUE); v.setTag(tag); v.setFocusable(true); v.setOnClickListener(view -> action.run()); return v;
    }
    private void divider(LinearLayout parent) { View line = new View(getContext()); line.setBackgroundColor(0xffEEEEEE); parent.addView(line, size(-1, 1)); }
}

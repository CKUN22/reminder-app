package com.ckun.reminder;

import android.app.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class GoalsActivity extends Activity {
    private static final int BG = 0xffFFF8ED, INK = 0xff4A3E35, MUTED = 0xff8E7D70, GREEN = 0xff7C9270, CREAM = 0xffF5EBDD, CARD = 0xffFFFCF7;
    private LinearLayout page; private Goal draft; private EditText title, daily; private Button deadline, time; private CheckBox auto;
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color, int radius) { GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable; }
    private TextView text(String value, int size, int color) { TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); view.setTypeface(Typeface.create("serif", Typeface.NORMAL)); view.setPadding(0, dp(4), 0, dp(4)); return view; }
    private Button button(String value, boolean primary, View.OnClickListener listener) { Button button = new Button(this); button.setText(value); button.setAllCaps(false); button.setTextSize(14); button.setTypeface(Typeface.create("serif", Typeface.NORMAL)); button.setTextColor(primary ? Color.WHITE : GREEN); button.setBackground(shape(primary ? GREEN : CREAM, 22)); button.setOnClickListener(listener); return button; }
    @Override public void onCreate(Bundle state) { super.onCreate(state); showList(); }
    private void shell() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setBackgroundColor(BG);
        page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(22), dp(6), dp(22), dp(36)); scroll.addView(page); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> { view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop() + dp(12), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()); return insets; });
    }
    private void showList() {
        draft = null; shell(); page.addView(button("‹  返回", false, v -> finish()), new LinearLayout.LayoutParams(-2, dp(42)));
        TextView heading = text("长远目标", 28, INK); heading.setTypeface(Typeface.create("serif", Typeface.BOLD)); heading.setPadding(0, dp(18), 0, 0); page.addView(heading);
        page.addView(text("把远方拆成今天能完成的一小步。", 14, MUTED));
        Button create = button("＋  创建目标", true, v -> showEditor(newGoal())); create.setTag("create-goal"); LinearLayout.LayoutParams createParams = new LinearLayout.LayoutParams(-1, dp(50)); createParams.topMargin = dp(18); page.addView(create, createParams);
        List<Goal> goals; try (GoalStore store = new GoalStore(this)) { goals = store.all(); }
        if (goals.isEmpty()) { TextView empty = text("还没有目标。比如：12 月通过六级。", 15, MUTED); empty.setGravity(Gravity.CENTER); empty.setPadding(0, dp(42), 0, dp(42)); page.addView(empty); }
        for (Goal goal : goals) {
            LinearLayout card = new LinearLayout(this); card.setTag("goal-" + goal.id); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(16), dp(13), dp(16), dp(13)); card.setBackground(shape(CARD, 24));
            TextView name = text(goal.title, 18, INK); name.setTypeface(Typeface.create("serif", Typeface.BOLD)); card.addView(name);
            card.addView(text("截止 " + formatDate(goal.deadline), 13, MUTED));
            card.addView(text(goal.autoAdd ? String.format(Locale.CHINA, "每天 %02d:%02d · %s", goal.hour, goal.minute, goal.dailyTitle) : "每日事项已关闭", 14, goal.autoAdd ? GREEN : MUTED));
            card.setOnClickListener(v -> { try (GoalStore store = new GoalStore(this)) { showEditor(store.get(goal.id)); } });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(10); page.addView(card, params);
        }
    }
    private Goal newGoal() { Goal goal = new Goal(); Calendar date = Calendar.getInstance(); date.add(Calendar.MONTH, 3); goal.deadline = GoalRules.dayStart(date.getTimeInMillis()); return goal; }
    private void showEditor(Goal goal) {
        if (goal == null) { showList(); return; } draft = goal; shell(); page.addView(button("‹  返回目标", false, v -> showList()), new LinearLayout.LayoutParams(-2, dp(42)));
        TextView heading = text(goal.id == 0 ? "创建一个目标" : "编辑目标", 27, INK); heading.setTypeface(Typeface.create("serif", Typeface.BOLD)); heading.setPadding(0, dp(18), 0, dp(8)); page.addView(heading);
        page.addView(text("目标名称", 14, GREEN)); title = input("例如：12 月通过六级", goal.title);
        page.addView(text("每日小目标", 14, GREEN)); daily = input("例如：背 30 个单词", goal.dailyTitle);
        page.addView(text("截止日期", 14, GREEN)); deadline = button(formatDate(goal.deadline) + "  ›", false, v -> pickDeadline()); deadline.setTag("goal-deadline"); page.addView(deadline, new LinearLayout.LayoutParams(-1, dp(50)));
        page.addView(text("每日提醒时间", 14, GREEN)); time = button(formatTime(goal) + "  ›", false, v -> pickTime()); time.setTag("goal-time"); page.addView(time, new LinearLayout.LayoutParams(-1, dp(50)));
        auto = new CheckBox(this); auto.setTag("goal-auto-add"); auto.setText("每天自动加入主页并在设定时间提醒"); auto.setTextColor(INK); auto.setTextSize(14); auto.setTypeface(Typeface.create("serif", Typeface.NORMAL)); auto.setChecked(goal.autoAdd); auto.setPadding(0, dp(12), 0, dp(12)); page.addView(auto);
        Button save = button("保存目标", true, v -> save()); save.setTag("save-goal"); page.addView(save, new LinearLayout.LayoutParams(-1, dp(52)));
        if (goal.id != 0) { Button delete = button("删除目标", false, v -> confirmDelete()); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(50)); params.topMargin = dp(10); page.addView(delete, params); }
    }
    private EditText input(String hint, String value) { EditText input = new EditText(this); input.setHint(hint); input.setText(value); input.setSingleLine(true); input.setTextColor(INK); input.setTextSize(16); input.setTypeface(Typeface.create("serif", Typeface.NORMAL)); input.setPadding(dp(14), dp(10), dp(14), dp(10)); input.setBackground(shape(CARD, 18)); LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.bottomMargin = dp(12); page.addView(input, params); return input; }
    private void pickDeadline() {
        Calendar date = Calendar.getInstance(); date.setTimeInMillis(draft.deadline);
        new DatePickerDialog(this, (view, year, month, day) -> { date.set(year, month, day, 0, 0, 0); date.set(Calendar.MILLISECOND, 0); draft.deadline = date.getTimeInMillis(); deadline.setText(formatDate(draft.deadline) + "  ›"); }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void pickTime() { new TimePickerDialog(this, (view, hour, minute) -> { draft.hour = hour; draft.minute = minute; time.setText(formatTime(draft) + "  ›"); }, draft.hour, draft.minute, true).show(); }
    private void save() {
        draft.title = title.getText().toString().trim(); draft.dailyTitle = daily.getText().toString().trim(); draft.autoAdd = auto.isChecked();
        if (draft.title.isEmpty()) { title.setError("请填写目标名称"); return; }
        if (draft.dailyTitle.isEmpty()) { daily.setError("请填写每日小目标"); return; }
        if (draft.deadline < GoalRules.dayStart(System.currentTimeMillis())) { Toast.makeText(this, "截止日期不能早于今天", Toast.LENGTH_LONG).show(); return; }
        try (GoalStore store = new GoalStore(this)) { store.save(draft); }
        try (TaskStore tasks = new TaskStore(this)) { GoalGenerator.ensureThroughDeadline(this, tasks, new ReminderScheduler(this), System.currentTimeMillis()); }
        Toast.makeText(this, draft.autoAdd ? "目标已保存，每日小目标已准备好" : "目标已保存", Toast.LENGTH_SHORT).show(); showList();
    }
    private void confirmDelete() { new AlertDialog.Builder(this).setTitle("删除这个目标？").setMessage("之后不会再生成每日事项，已经生成的待办会保留。").setNegativeButton("保留", null).setPositiveButton("删除", (dialog, which) -> { try (GoalStore store = new GoalStore(this)) { store.delete(draft.id); } showList(); }).show(); }
    private String formatDate(long time) { return new SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(new Date(time)); }
    private String formatTime(Goal goal) { return String.format(Locale.CHINA, "%02d:%02d", goal.hour, goal.minute); }
    @Override public void onBackPressed() { if (draft != null) showList(); else super.onBackPressed(); }
}

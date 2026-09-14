package com.ckun.reminder;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int BG = 0xffF7F8F2, INK = 0xff27332B, MUTED = 0xff758073, GREEN = 0xff52694D;
    private LinearLayout page;
    private ScrollView scroll;
    private FrameLayout screen;
    private TextView dateLabel;
    private List<Task> displayedTasks = new ArrayList<>();
    private final Map<Long, TextView> taskTimeLabels = new HashMap<>();
    private boolean shownNotifications, shownExact;
    private TaskStore store;
    private ReminderScheduler scheduler;
    private boolean completedTab, editing, exactBefore;
    private Task draft;
    private long originalStart;
    private EditText titleInput, noteInput, durationInput;
    private Button reminderButton;
    private Button timeButton;
    private TextView endLabel;
    private StartTimeDialog startTimeDialog;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!editing) refreshList();
            handler.postDelayed(this, 30_000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new TaskStore(this); scheduler = new ReminderScheduler(this); exactBefore = scheduler.exactAllowed();
        scheduler.restore();
        if (state != null) {
            completedTab = state.getBoolean("tab");
            if (state.getBoolean("editing")) {
                Task t = new Task(); t.id = state.getLong("id"); t.start = state.getLong("start");
                t.title = state.getString("title", ""); t.note = state.getString("note", "");
                t.earlyTen = state.getBoolean("ten"); t.earlyDay = state.getBoolean("day");
                t.done = state.getBoolean("done"); t.revision = state.getLong("revision");
                if (state.containsKey("reminders")) { t.reminderMinutes = new TreeSet<>(); for (int n : state.getIntArray("reminders")) t.reminderMinutes.add(n); }
                showEditor(t); originalStart = state.getLong("original");
                durationInput.setText(state.getString("duration", ""));
                if (state.containsKey("timePicker")) openTimePicker(state.getBundle("timePicker"));
                return;
            }
        }
        openIntent(getIntent());
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); openIntent(intent); }
    private void openIntent(Intent intent) {
        Task t = store.get(intent.getLongExtra("id", -1));
        if (t != null) showEditor(t); else showList();
    }
    @Override protected void onResume() {
        super.onResume();
        if (scheduler.exactAllowed() != exactBefore) { scheduler.restore(); exactBefore = scheduler.exactAllowed(); }
        if (!editing) refreshList();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, 30_000);
    }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(tick); }
    @Override protected void onDestroy() { if (startTimeDialog != null) startTimeDialog.dismiss(); store.close(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putBoolean("tab", completedTab); out.putBoolean("editing", editing);
        if (startTimeDialog != null && startTimeDialog.isShowing()) out.putBundle("timePicker", startTimeDialog.selectionState());
        if (editing) {
            out.putLong("id", draft.id); out.putLong("start", draft.start); out.putLong("original", originalStart);
            out.putLong("revision", draft.revision); out.putBoolean("done", draft.done);
            out.putString("title", titleInput.getText().toString()); out.putString("note", noteInput.getText().toString());
            out.putString("duration", durationInput.getText().toString()); out.putIntArray("reminders", draft.reminders().stream().mapToInt(Integer::intValue).toArray());
        }
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private void shell() {
        scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(24), dp(20), dp(24), dp(32));
        screen = new FrameLayout(this); screen.setBackgroundColor(BG);
        scroll.addView(page); screen.addView(scroll, new FrameLayout.LayoutParams(-1, -1)); setContentView(screen);
        screen.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
    }
    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setPadding(0, dp(5), 0, dp(5)); return t;
    }
    private void heading(String title, String sub) {
        TextView h = text(title, 30, INK); h.setTypeface(null, Typeface.BOLD); page.addView(h); page.addView(text(sub, 14, MUTED)); gap(18);
    }
    private void gap(int n) { View v = new View(this); page.addView(v, new LinearLayout.LayoutParams(1, dp(n))); }
    private Button button(String name, boolean primary, Runnable action) {
        Button b = new Button(this); b.setText(name); b.setTextSize(15); b.setAllCaps(false);
        b.setTextColor(primary ? Color.WHITE : GREEN); b.setBackground(shape(primary ? GREEN : 0xffE8EDDF, 16));
        b.setMinHeight(dp(52)); b.setPadding(dp(14), dp(10), dp(14), dp(10));
        b.setOnClickListener(v -> action.run()); return b;
    }
    private String format(long time) { return new SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(new Date(time)); }
    private void showList() {
        editing = false; shell();
        taskTimeLabels.clear();
        page.setPadding(dp(24), dp(20), dp(24), dp(104));
        page.addView(text("轻待办  /  OFFLINE", 12, GREEN));
        TextView heading = text("留点时间，做好小事", 30, INK); heading.setTypeface(null, Typeface.BOLD); page.addView(heading);
        dateLabel = text(today(), 14, MUTED); page.addView(dateLabel); gap(18);
        LinearLayout tabs = new LinearLayout(this);
        Button pending = button("未完成", !completedTab, () -> { completedTab = false; showList(); });
        Button done = button("已完成", completedTab, () -> { completedTab = true; showList(); });
        tabs.addView(pending, new LinearLayout.LayoutParams(0, dp(52), 1));
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(0, dp(52), 1); tabParams.leftMargin = dp(10); tabs.addView(done, tabParams); page.addView(tabs); gap(16);
        shownNotifications = scheduler.notificationsAllowed(); shownExact = scheduler.exactAllowed();
        if (!shownNotifications || !shownExact) {
            String warning = !scheduler.notificationsAllowed() ? "通知未开启 · 点此设置提醒权限" : "精确提醒未开启 · 提醒可能延迟";
            Button banner = button(warning, false, this::permissions); banner.setTextSize(13); page.addView(banner); gap(16);
        }
        List<Task> tasks = store.all(); int count = 0;
        displayedTasks = tasks;
        for (Task t : tasks) if (t.done == completedTab) count++;
        page.addView(text(completedTab ? "已完成 · " + count : "接下来的安排 · " + count, 13, MUTED)); gap(8);
        if (count == 0) {
            LinearLayout empty = new LinearLayout(this); empty.setOrientation(LinearLayout.VERTICAL); empty.setPadding(dp(24), dp(40), dp(24), dp(40)); empty.setBackground(shape(Color.WHITE, 24));
            TextView mark = text("✓", 44, GREEN); mark.setGravity(Gravity.CENTER); empty.addView(mark);
            TextView label = text(completedTab ? "每一件完成，都值得记录" : "给下一件小事留个位置", 18, INK); label.setGravity(Gravity.CENTER); empty.addView(label);
            TextView help = text(completedTab ? "完成的事项会出现在这里" : "添加开始时间，让提醒替你记住", 13, MUTED); help.setGravity(Gravity.CENTER); empty.addView(help); page.addView(empty);
        }
        for (Task t : tasks) {
            if (t.done != completedTab) continue;
            LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(18), dp(14), dp(18), dp(14)); card.setBackground(shape(Color.WHITE, 20));
            TextView time = text("", 13, GREEN); time.setTag("task-time-" + t.id); taskTimeLabels.put(t.id, time); updateTaskTime(t, time); card.addView(time);
            TextView title = text(t.title, 20, INK); title.setTypeface(null, Typeface.BOLD); card.addView(title);
            if (t.duration > 0) card.addView(text("预计 " + t.duration + " 分钟 · 至 " + format(ReminderRules.end(t)), 13, MUTED));
            if (!t.note.isEmpty()) { TextView note = text(t.note, 14, MUTED); note.setMaxLines(2); note.setEllipsize(TextUtils.TruncateAt.END); card.addView(note); }
            card.setOnClickListener(v -> showEditor(store.get(t.id)));
            if (!t.done) { Button finish = button("✓  标记完成", false, () -> complete(t)); card.addView(finish); }
            page.addView(card); gap(12);
        }
        ImageButton add = new ImageButton(this); add.setTag("add-task"); add.setContentDescription("新建待办");
        add.setImageResource(R.drawable.ic_add); add.setPadding(dp(18), dp(18), dp(18), dp(18));
        add.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33FFFFFF), shape(GREEN, 32), null));
        add.setElevation(dp(6));
        add.setOnClickListener(v -> {
            Task t = new Task(); t.start = ((System.currentTimeMillis() / 60_000) + 30) * 60_000; showEditor(t);
        });
        FrameLayout.LayoutParams floating = new FrameLayout.LayoutParams(dp(60), dp(60), Gravity.RIGHT | Gravity.BOTTOM);
        floating.rightMargin = dp(24); floating.bottomMargin = dp(24); screen.addView(add, floating);
        gap(12); TextView local = text("仅保存在此设备 · 无需联网", 12, MUTED); local.setGravity(Gravity.CENTER); page.addView(local);
    }
    private String today() { return new SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(new Date()); }
    private void updateTaskTime(Task task, TextView label) {
        boolean overdue = !task.done && task.start <= System.currentTimeMillis();
        String value = (overdue ? "已过开始时间 · " : "") + format(task.start);
        if (!value.contentEquals(label.getText())) { label.setText(value); label.setTextColor(overdue ? 0xffA26343 : GREEN); }
    }
    private void refreshList() {
        List<Task> current = store.all();
        boolean changed = current.size() != displayedTasks.size() || shownNotifications != scheduler.notificationsAllowed() || shownExact != scheduler.exactAllowed();
        if (!changed) for (int i = 0; i < current.size(); i++) {
            if (current.get(i).id != displayedTasks.get(i).id || current.get(i).revision != displayedTasks.get(i).revision) { changed = true; break; }
        }
        if (changed) {
            int position = scroll.getScrollY(); showList(); ScrollView target = scroll; target.post(() -> target.scrollTo(0, position)); return;
        }
        String date = today(); if (!date.contentEquals(dateLabel.getText())) dateLabel.setText(date);
        for (Task task : displayedTasks) { TextView label = taskTimeLabels.get(task.id); if (label != null) updateTaskTime(task, label); }
    }
    private EditText input(String hint, String value, int type) {
        EditText e = new EditText(this); e.setTextColor(INK); e.setTextSize(16); e.setHint(hint); e.setInputType(type);
        e.setText(value); e.setPadding(dp(14), dp(12), dp(14), dp(12)); e.setBackground(shape(Color.WHITE, 12));
        page.addView(e, new LinearLayout.LayoutParams(-1, -2)); gap(12); return e;
    }
    private void label(String value) { page.addView(text(value, 14, GREEN)); }
    private void showEditor(Task t) {
        if (t == null) { showList(); return; }
        editing = true; draft = t; originalStart = t.start; shell();
        page.addView(button("‹  返回列表", false, this::leaveEditor)); gap(16);
        heading(t.id == 0 ? "安排一件小事" : t.done ? "已完成的事项" : "编辑待办", "让时间有安排，让心里少一件事。");
        label("事项名称 *"); titleInput = input("例如：读半小时书", t.title, 1); titleInput.setSingleLine(true);
        label("备注 · 选填"); noteInput = input("补充一点细节", t.note, android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        noteInput.setMinLines(2); noteInput.setMaxLines(5);
        label("预计开始时间"); timeButton = button("", false, this::pickTime); page.addView(timeButton); gap(8);
        label("提醒"); reminderButton = button("", false, () -> startActivityForResult(new Intent(this, ReminderSettingsActivity.class).putExtra("start", draft.start).putExtra("reminders", draft.reminders().stream().mapToInt(Integer::intValue).toArray()), 20));
        reminderButton.setTag("reminder-settings"); page.addView(reminderButton); gap(14);
        label("预计持续时间 · 选填");
        durationInput = input("自定义分钟数", t.duration == 0 ? "" : String.valueOf(t.duration), android.text.InputType.TYPE_CLASS_NUMBER);
        LinearLayout presets = new LinearLayout(this);
        for (int minutes : new int[]{15, 30, 60}) {
            Button b = button(minutes + " 分钟", false, () -> durationInput.setText(String.valueOf(minutes)));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(50), 1); p.setMargins(dp(3), 0, dp(3), 0); presets.addView(b, p);
        }
        page.addView(presets); endLabel = text("", 13, MUTED); page.addView(endLabel);
        durationInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {} public void onTextChanged(CharSequence s, int start, int before, int count) { updateEnd(); } public void afterTextChanged(Editable e) {}
        });
        updateTime(); gap(20);
        page.addView(button("保存事项", true, this::save));
        if (t.id != 0 && !t.done) { gap(10); page.addView(button("✓  标记完成", false, () -> complete(t))); }
        if (t.id != 0 && t.done) { gap(10); page.addView(button("改为未完成", false, () -> reopen(t))); }
        if (t.id != 0) { gap(10); page.addView(button("删除事项", false, () -> new AlertDialog.Builder(this).setTitle("删除这条事项？").setMessage("事项和剩余提醒会一并移除，无法撤销。").setNegativeButton("保留", null).setPositiveButton("删除", (d, w) -> {
            store.delete(t.id); scheduler.cancel(t); showList();
        }).show())); }
    }
    private void updateTime() {
        timeButton.setText(format(draft.start));
        reminderButton.setText(draft.reminders().isEmpty() ? "不提醒  ›" : "已设置 " + draft.reminders().size() + " 个提醒  ›"); updateEnd();
    }
    private void updateEnd() {
        if (endLabel == null || durationInput == null) return;
        try {
            int minutes = Integer.parseInt(durationInput.getText().toString());
            endLabel.setText(minutes > 0 && minutes <= 525600 ? "预计结束：" + format(draft.start + minutes * 60_000L) + "\n仅用于安排，不会自动完成或响铃" : "请输入 1–525600 分钟");
        } catch (NumberFormatException e) { endLabel.setText("选填，仅用于安排和展示"); }
    }
    private void pickTime() {
        openTimePicker(null);
    }
    private void openTimePicker(Bundle state) {
        startTimeDialog = new StartTimeDialog(this, draft.start, state, selected -> { draft.start = selected; updateTime(); });
        startTimeDialog.show();
    }
    private void save() {
        if (draft.id != 0) {
            Task current = store.get(draft.id);
            if (current == null || current.revision != draft.revision) {
                Toast.makeText(this, "事项状态已变化，请重新打开后编辑", Toast.LENGTH_LONG).show(); showList(); return;
            }
        }
        draft.title = titleInput.getText().toString().trim(); draft.note = noteInput.getText().toString().trim();
        String duration = durationInput.getText().toString().trim();
        try { draft.duration = duration.isEmpty() ? 0 : Integer.parseInt(duration); }
        catch (NumberFormatException e) { durationInput.setError("请输入有效分钟数"); return; }
        if (!duration.isEmpty() && draft.duration == 0) { durationInput.setError("持续时间应大于 0"); return; }
        String error = ReminderRules.validate(draft, System.currentTimeMillis(), draft.id != 0 && draft.start == originalStart);
        if (error != null) { Toast.makeText(this, error, Toast.LENGTH_LONG).show(); return; }
        updateTime();
        if (draft.id != 0) scheduler.cancel(store.get(draft.id));
        store.save(draft); scheduler.schedule(draft); boolean needsPermission = !draft.done && !draft.reminders().isEmpty() && (!scheduler.notificationsAllowed() || !scheduler.exactAllowed());
        completedTab = draft.done; showList();
        Toast.makeText(this, "事项已保存", Toast.LENGTH_SHORT).show();
        if (needsPermission) permissions();
    }
    private void complete(Task task) {
        Task fresh = store.get(task.id); if (fresh == null) { showList(); return; }
        fresh.done = true; store.save(fresh); scheduler.cancel(fresh); showList();
        Toast.makeText(this, "又完成了一件小事", Toast.LENGTH_SHORT).show();
    }
    private void reopen(Task task) {
        Task fresh = store.get(task.id); if (fresh == null) { showList(); return; }
        fresh.done = false; store.save(fresh); scheduler.schedule(fresh); completedTab = false; showEditor(fresh);
        Toast.makeText(this, "已改为未完成，过期提醒不会补发", Toast.LENGTH_SHORT).show();
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 20 && resultCode == RESULT_OK && data != null) {
            draft.reminderMinutes = new TreeSet<>(); for (int n : data.getIntArrayExtra("reminders")) draft.reminderMinutes.add(n); updateTime();
        }
    }
    private void permissions() {
        new AlertDialog.Builder(this).setTitle("让提醒准时到达")
                .setMessage("请开启通知和“闹钟和提醒”权限。未开启通知将无法显示提醒；未开启精确提醒可能延迟。\n\n所有事项只保存在本机。")
                .setNegativeButton("稍后", null).setPositiveButton("去开启", (d, w) -> {
                    if (!scheduler.notificationsAllowed()) {
                        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED && !getPreferences(0).getBoolean("askedNotification", false)) {
                            getPreferences(0).edit().putBoolean("askedNotification", true).apply();
                            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
                        } else launchSettings(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID, ReminderScheduler.CHANNEL));
                    } else if (!scheduler.exactAllowed()) launchSettings(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
                }).show();
    }
    private void launchSettings(Intent intent) {
        try { startActivity(intent); } catch (ActivityNotFoundException e) { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))); }
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!editing) showList();
        if (requestCode == 10 && scheduler.notificationsAllowed() && !scheduler.exactAllowed()) permissions();
    }
    private void leaveEditor() {
        showList();
    }
    @Override public void onBackPressed() { if (editing) leaveEditor(); else super.onBackPressed(); }
}

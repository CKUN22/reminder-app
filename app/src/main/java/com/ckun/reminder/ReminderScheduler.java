package com.ckun.reminder;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;

public final class ReminderScheduler {
    public static final String CHANNEL = "task_reminders";
    private final Context context;
    private final AlarmManager alarms;
    public ReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        alarms = context.getSystemService(AlarmManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL, "待办提醒", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("事项开始及提前提醒");
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    public boolean exactAllowed() { return Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms(); }
    public boolean notificationsAllowed() {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        return manager.areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL).getImportance() != NotificationManager.IMPORTANCE_NONE;
    }
    private PendingIntent alarm(Task t, int kind) {
        Intent intent = new Intent(context, ReminderReceiver.class).setAction("REMIND")
                .setData(Uri.parse("qingdan://alarm/" + t.id + "/" + kind))
                .putExtra("id", t.id).putExtra("kind", kind).putExtra("revision", t.revision);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    public void cancel(Task t) {
        cancelAlarms(t);
        context.getSystemService(NotificationManager.class).cancel("task-" + t.id, 0);
    }
    private void cancelAlarms(Task t) {
        for (int kind = 0; kind < 3; kind++) alarms.cancel(alarm(t, kind));
    }
    public void schedule(Task t) {
        cancel(t);
        scheduleFuture(t);
    }
    private void scheduleFuture(Task t) {
        for (int kind : ReminderRules.pending(t, System.currentTimeMillis())) {
            long at = t.start - ReminderRules.OFFSETS[kind];
            PendingIntent pending = alarm(t, kind);
            try {
                if (exactAllowed()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
                else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
            } catch (SecurityException e) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
            }
        }
    }
    public void restore() {
        try (TaskStore store = new TaskStore(context)) {
            for (Task t : store.all()) { cancelAlarms(t); scheduleFuture(t); }
        }
    }
}

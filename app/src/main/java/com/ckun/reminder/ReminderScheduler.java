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
    private PendingIntent show(Task t, int kind) {
        Intent intent = new Intent(context, MainActivity.class)
                .setData(Uri.parse("qingdan://task/" + t.id + "/" + kind)).putExtra("id", t.id);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private PendingIntent alarm(Task t, int kind) {
        Intent intent = new Intent(context, ReminderReceiver.class).setAction("REMIND")
                .setData(Uri.parse("qingdan://reminder/" + t.id + "/" + kind))
                .putExtra("id", t.id).putExtra("minutes", kind).putExtra("revision", t.revision);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    public void cancel(Task t) {
        cancelAlarms(t);
        context.getSystemService(NotificationManager.class).cancel("task-" + t.id, 0);
        for (int minutes : t.reminders())
            context.getSystemService(NotificationManager.class).cancel(notificationTag(t.id, minutes), 0);
    }
    private void cancelAlarms(Task t) {
        for (int minutes : t.reminders()) alarms.cancel(alarm(t, minutes));
        for (int kind = 0; kind < 3; kind++) {
            Intent old = new Intent(context, ReminderReceiver.class).setAction("REMIND").setData(Uri.parse("qingdan://alarm/" + t.id + "/" + kind));
            PendingIntent pending = PendingIntent.getBroadcast(context, 0, old, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pending != null) { alarms.cancel(pending); pending.cancel(); }
        }
    }
    public void schedule(Task t) {
        cancel(t);
        scheduleFuture(t);
    }
    private void scheduleFuture(Task t) {
        for (int kind : ReminderRules.pendingMinutes(t, System.currentTimeMillis())) {
            long at = t.start - kind * 60_000L;
            PendingIntent pending = alarm(t, kind);
            try {
                if (exactAllowed()) alarms.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show(t, kind)), pending);
                else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
            } catch (SecurityException e) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
            }
        }
    }
    public static String notificationTag(long taskId, int minutes) { return "task-" + taskId + "-" + minutes; }
    public void restore() {
        try (TaskStore store = new TaskStore(context)) {
            for (Task t : store.all()) { cancelAlarms(t); scheduleFuture(t); }
        }
    }
}

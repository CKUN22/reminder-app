package com.ckun.reminder;

import android.app.*;
import android.content.*;
import android.net.Uri;
import java.text.SimpleDateFormat;
import java.util.*;

public final class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        try (TaskStore store = new TaskStore(context)) {
            Task t = store.get(intent.getLongExtra("id", -1));
            if (t == null || t.done || t.revision != intent.getLongExtra("revision", -1)) return;
            ReminderScheduler scheduler = new ReminderScheduler(context);
            if ("COMPLETE".equals(intent.getAction())) {
                t.done = true; store.save(t); scheduler.cancel(t); return;
            }
            int kind = intent.getIntExtra("minutes", -1);
            if (!t.reminders().contains(kind)) return;
            // A queued broadcast from a replaced alarm must not fire before the new time.
            if (System.currentTimeMillis() < t.start - kind * 60_000L) return;
            if (!scheduler.notificationsAllowed()) return;
            Intent open = new Intent(context, MainActivity.class).setData(Uri.parse("qingdan://task/" + t.id)).putExtra("id", t.id);
            PendingIntent content = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Intent complete = new Intent(context, ReminderReceiver.class).setAction("COMPLETE")
                    .setData(Uri.parse("qingdan://complete/" + t.id)).putExtra("id", t.id).putExtra("revision", t.revision);
            PendingIntent action = PendingIntent.getBroadcast(context, 0, complete, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            String prefix = kind == 0 ? "现在开始" : ReminderRules.reminderLabel(kind) + "提醒";
            String time = new SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(new Date(t.start));
            Notification notification = new Notification.Builder(context, ReminderScheduler.CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification).setContentTitle(t.title)
                    .setContentText(prefix + " · " + time).setStyle(new Notification.BigTextStyle().bigText(prefix + " · " + time + (t.note.isEmpty() ? "" : "\n" + t.note)))
                    .setContentIntent(content).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER)
                    .addAction(new Notification.Action.Builder(null, "完成", action).build()).build();
            try { context.getSystemService(NotificationManager.class).notify("task-" + t.id, 0, notification); }
            catch (SecurityException ignored) { /* Permission can be revoked between check and delivery. */ }
        }
    }
}

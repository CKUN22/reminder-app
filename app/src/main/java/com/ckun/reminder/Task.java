package com.ckun.reminder;

public class Task {
    public long id, start, revision;
    public String title = "", note = "";
    public int duration;
    public boolean earlyTen, earlyDay, done;
    public java.util.TreeSet<Integer> reminderMinutes;
    public java.util.TreeSet<Integer> reminders() {
        if (reminderMinutes != null) return new java.util.TreeSet<>(reminderMinutes);
        java.util.TreeSet<Integer> values = new java.util.TreeSet<>(); values.add(0);
        if (earlyTen) values.add(10); if (earlyDay) values.add(1440); return values;
    }
}

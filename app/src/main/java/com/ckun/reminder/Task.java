package com.ckun.reminder;

public class Task {
    public long id, start, revision;
    public String title = "", note = "";
    public int duration;
    public int priority = 3;
    public static final String[] PRIORITY_LABELS = {"重要且紧急", "不重要但紧急", "重要不紧急", "不重要不紧急"};
    public static final int[] PRIORITY_COLORS = {0xffB84C48, 0xffA66B20, 0xff416EAD, 0xff52694D};
    public int priorityIndex() { return priority >= 0 && priority < 4 ? priority : 3; }
    public boolean earlyTen, earlyDay, done;
    public java.util.TreeSet<Integer> reminderMinutes;
    public java.util.TreeSet<Integer> reminders() {
        if (reminderMinutes != null) return new java.util.TreeSet<>(reminderMinutes);
        java.util.TreeSet<Integer> values = new java.util.TreeSet<>(); values.add(0);
        if (earlyTen) values.add(10); if (earlyDay) values.add(1440); return values;
    }
}

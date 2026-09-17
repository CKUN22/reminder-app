package com.ckun.reminder;

import java.util.Calendar;

final class GoalRules {
    static long dayStart(long time) {
        Calendar day = Calendar.getInstance(); day.setTimeInMillis(time); day.set(Calendar.HOUR_OF_DAY, 0); day.set(Calendar.MINUTE, 0); day.set(Calendar.SECOND, 0); day.set(Calendar.MILLISECOND, 0); return day.getTimeInMillis();
    }
    static long taskTime(long dayStart, int hour, int minute) {
        Calendar time = Calendar.getInstance(); time.setTimeInMillis(dayStart); time.set(Calendar.HOUR_OF_DAY, hour); time.set(Calendar.MINUTE, minute); return time.getTimeInMillis();
    }
    static long nextDay(long dayStart) {
        Calendar day = Calendar.getInstance(); day.setTimeInMillis(dayStart); day.add(Calendar.DAY_OF_MONTH, 1); return dayStart(day.getTimeInMillis());
    }
    static boolean eligible(Goal goal, long dayStart) { return goal.autoAdd && dayStart <= dayStart(goal.deadline); }
}

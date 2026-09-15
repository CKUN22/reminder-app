package com.ckun.reminder;

import java.util.*;

public final class GoalRulesTest {
    private static int assertions;
    private static void check(boolean value, String message) { assertions++; if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        Calendar day = Calendar.getInstance(); day.clear(); day.set(2026, Calendar.SEPTEMBER, 15, 14, 20);
        long start = GoalRules.dayStart(day.getTimeInMillis()); Calendar result = Calendar.getInstance(); result.setTimeInMillis(start);
        check(result.get(Calendar.HOUR_OF_DAY) == 0 && result.get(Calendar.MINUTE) == 0, "normalizes day start");
        result.setTimeInMillis(GoalRules.taskTime(start, 21, 30)); check(result.get(Calendar.HOUR_OF_DAY) == 21 && result.get(Calendar.MINUTE) == 30, "builds daily reminder time");
        Goal goal = new Goal(); goal.autoAdd = true; goal.deadline = start;
        check(GoalRules.eligible(goal, start), "deadline day is included");
        check(!GoalRules.eligible(goal, start + 86400000L), "does not generate after deadline");
        goal.autoAdd = false; check(!GoalRules.eligible(goal, start), "respects disabled auto add");
        System.out.println("PASS: " + assertions + " goal assertions");
    }
}

package com.ckun.reminder;

import java.time.*;

public final class CourseRules {
    public static final LocalDate TERM_START = LocalDate.of(2026, 9, 7);
    public static final int TERM_WEEKS = 16;
    public static final LocalTime[] PERIOD_STARTS = {
            LocalTime.of(8, 15), LocalTime.of(9, 0), LocalTime.of(10, 5), LocalTime.of(10, 50),
            LocalTime.of(13, 0), LocalTime.of(13, 45), LocalTime.of(14, 50), LocalTime.of(15, 35),
            LocalTime.of(16, 20), LocalTime.of(18, 0), LocalTime.of(18, 45), LocalTime.of(19, 50),
            LocalTime.of(20, 35)
    };
    private CourseRules() {}
    public static String locationLabel(Course course) { return course.location.isEmpty() ? "地点待定" : course.location; }
    public static int weekOf(LocalDate date) { return (int) Math.floorDiv(java.time.temporal.ChronoUnit.DAYS.between(TERM_START, date), 7) + 1; }
    public static LocalDate weekMonday(int week) { return TERM_START.plusWeeks(week - 1L); }
    public static LocalDateTime occurrence(Course course, int week) {
        return LocalDateTime.of(weekMonday(week).plusDays(course.weekday - 1L), PERIOD_STARTS[course.startPeriod - 1]);
    }
    public static LocalDateTime end(Course course, int week) {
        return LocalDateTime.of(weekMonday(week).plusDays(course.weekday - 1L), PERIOD_STARTS[course.endPeriod - 1].plusMinutes(45));
    }
    public static LocalDateTime nextOccurrence(Course course, LocalDateTime now) {
        for (int week = course.startWeek; week <= course.endWeek; week++) {
            LocalDateTime candidate = occurrence(course, week);
            if (candidate.isAfter(now)) return candidate;
        }
        return null;
    }
    public static LocalDateTime defaultHomeworkReminder(LocalDateTime nextClass, LocalDateTime now) {
        LocalDate saturday = nextClass.toLocalDate().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));
        LocalDateTime preferred = saturday.atTime(12, 0);
        if (preferred.isAfter(now) && preferred.isBefore(nextClass)) return preferred;
        LocalDateTime fallback = nextClass.toLocalDate().minusDays(1).atTime(12, 0);
        return fallback.isAfter(now) ? fallback : null;
    }
    public static String validate(Course course) {
        if (course.name.trim().isEmpty()) return "请填写课程名称";
        if (course.weekday < 1 || course.weekday > 7) return "请选择正确的星期";
        if (course.startPeriod < 1 || course.endPeriod > 13 || course.startPeriod > course.endPeriod) return "请选择正确的课程节数";
        if (course.startWeek < 1 || course.endWeek > TERM_WEEKS || course.startWeek > course.endWeek) return "请选择正确的周数范围";
        return null;
    }
}

package com.ckun.reminder;

import java.time.*;

public final class CourseRulesTest {
    private static int assertions;
    private static void equal(Object expected,Object actual){assertions++;if(!java.util.Objects.equals(expected,actual))throw new AssertionError("Expected "+expected+", got "+actual);}
    public static void main(String[] args){
        Course location=new Course();equal("@地点待定",CourseRules.locationLabel(location));location.location="松2105";equal("@松2105",CourseRules.locationLabel(location));
        equal(1,CourseRules.weekOf(LocalDate.of(2026,9,7)));
        equal(0,CourseRules.weekOf(LocalDate.of(2026,9,6)));
        equal(16,CourseRules.weekOf(LocalDate.of(2026,12,21)));
        equal(LocalDate.of(2026,9,14),CourseRules.weekMonday(2));
        Course c=new Course();c.name="高等数学";c.weekday=1;c.startPeriod=5;c.endPeriod=6;c.startWeek=2;c.endWeek=7;
        equal(LocalDateTime.of(2026,9,14,13,0),CourseRules.occurrence(c,2));
        equal(LocalDateTime.of(2026,9,14,14,30),CourseRules.end(c,2));
        equal(LocalDateTime.of(2026,9,21,13,0),CourseRules.nextOccurrence(c,LocalDateTime.of(2026,9,14,13,1)));
        equal(null,CourseRules.nextOccurrence(c,LocalDateTime.of(2026,10,19,13,0)));
        equal(LocalDateTime.of(2026,9,19,12,0),CourseRules.defaultHomeworkReminder(LocalDateTime.of(2026,9,21,13,0),LocalDateTime.of(2026,9,14,14,0)));
        equal(LocalDateTime.of(2026,9,17,12,0),CourseRules.defaultHomeworkReminder(LocalDateTime.of(2026,9,18,13,0),LocalDateTime.of(2026,9,16,14,0)));
        equal(null,CourseRules.defaultHomeworkReminder(LocalDateTime.of(2026,9,17,8,15),LocalDateTime.of(2026,9,16,14,0)));
        equal(null,CourseRules.validate(c));c.startWeek=8;c.endWeek=7;equal("请选择正确的周数范围",CourseRules.validate(c));
        System.out.println("PASS: "+assertions+" course rule assertions");
    }
}

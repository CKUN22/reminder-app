namespace LightTodo.Windows;
public static class Rules {
    public static string? Validate(TodoItem item, DateTime now, bool existingStart = false) {
        if (string.IsNullOrWhiteSpace(item.Title)) return "请填写事项名称";
        if (!existingStart && item.Start <= now) return "请选择未来的开始时间";
        if (item.DurationMinutes < 0 || item.DurationMinutes > 525600) return "持续时间应在 1–525600 分钟之间";
        return null;
    }
    public static IReadOnlyList<int> PendingReminders(TodoItem item, DateTime now) => item.Done ? [] : item.ReminderMinutes.Where(m => item.Start.AddMinutes(-m) > now).Order().ToList();
    public static DateTime End(TodoItem item) => item.Start.AddMinutes(item.DurationMinutes);
    public static int WeekOf(DateTime date) => (int)((date.Date - new DateTime(2026, 9, 7)).TotalDays / 7) + 1;
    public static readonly TimeSpan[] PeriodStarts = [new(8,15,0),new(9,0,0),new(10,5,0),new(10,50,0),new(13,0,0),new(13,45,0),new(14,50,0),new(15,35,0),new(16,20,0),new(18,0,0),new(18,45,0),new(19,50,0),new(20,35,0)];
    public static DateTime CourseOccurrence(CourseItem c, int week) => new DateTime(2026,9,7).AddDays((week-1)*7 + ((int)c.Weekday + 6)%7).Add(PeriodStarts[c.StartPeriod-1]);
}

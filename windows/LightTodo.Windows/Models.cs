namespace LightTodo.Windows;

public enum Priority { ImportantUrgent, Urgent, Important, Normal }
public sealed class TodoItem {
    public long Id { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
    public string Title { get; set; } = "";
    public string Note { get; set; } = "";
    public DateTime Start { get; set; } = DateTime.Now.AddMinutes(30);
    public int DurationMinutes { get; set; }
    public Priority Priority { get; set; } = Priority.Normal;
    public bool Done { get; set; }
    public DateTime? CompletedAt { get; set; }
    public SortedSet<int> ReminderMinutes { get; set; } = [0];
    public long? GoalId { get; set; }
    public long? CourseId { get; set; }
}
public sealed class GoalItem {
    public long Id { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
    public string Title { get; set; } = "";
    public string DailyTitle { get; set; } = "";
    public DateTime Deadline { get; set; } = DateTime.Today.AddDays(7);
    public TimeSpan DailyTime { get; set; } = new(21, 30, 0);
    public bool AutoAdd { get; set; } = true;
}
public sealed class CourseItem {
    public long Id { get; set; } = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
    public string Name { get; set; } = "";
    public string Location { get; set; } = "";
    public string Teacher { get; set; } = "";
    public DayOfWeek Weekday { get; set; } = DayOfWeek.Monday;
    public int StartPeriod { get; set; } = 1;
    public int EndPeriod { get; set; } = 2;
    public int StartWeek { get; set; } = 1;
    public int EndWeek { get; set; } = 16;
    public int ColorIndex { get; set; }
}
public sealed class AppData {
    public List<TodoItem> Tasks { get; set; } = [];
    public List<GoalItem> Goals { get; set; } = [];
    public List<CourseItem> Courses { get; set; } = [];
}

using LightTodo.Windows;
int assertions=0;
void Equal<T>(T expected,T actual,string name){assertions++;if(!EqualityComparer<T>.Default.Equals(expected,actual))throw new Exception($"{name}: expected {expected}, got {actual}");}
var now=new DateTime(2026,9,13,10,0,0);
var task=new TodoItem{Title="读书",Start=now.AddDays(2),ReminderMinutes=[0,10,1440]};
Equal("0,10,1440",string.Join(',',Rules.PendingReminders(task,now)),"future reminders");
task.Start=now.AddMinutes(10);Equal("0",string.Join(',',Rules.PendingReminders(task,now)),"expired equality");
task.Start=now.AddMinutes(10).AddMilliseconds(1);Equal("0,10",string.Join(',',Rules.PendingReminders(task,now)),"future boundary");
task.Done=true;Equal(0,Rules.PendingReminders(task,now).Count,"completed reminders");
task.Done=false;task.Start=new DateTime(2026,9,13,23,45,0);task.DurationMinutes=30;Equal(new DateTime(2026,9,14,0,15,0),Rules.End(task),"cross-day end");
task.Title=" ";Equal("请填写事项名称",Rules.Validate(task,now),"required title");
task.Title="事项";task.DurationMinutes=525601;Equal("持续时间应在 1–525600 分钟之间",Rules.Validate(task,now,true),"duration maximum");
Equal(1,Rules.WeekOf(new DateTime(2026,9,7)),"semester first week");
Equal(16,Rules.WeekOf(new DateTime(2026,12,21)),"semester last week");
var course=new CourseItem{Weekday=DayOfWeek.Monday,StartPeriod=5};Equal(new DateTime(2026,9,14,13,0,0),Rules.CourseOccurrence(course,2),"course occurrence");
var temp=Path.Combine(Path.GetTempPath(),$"lighttodo-test-{Guid.NewGuid():N}.json");
try{var store=new LocalStore(temp);store.Data.Tasks.Add(new(){Title="持久化",Start=now});store.Save();var restored=new LocalStore(temp);Equal("持久化",restored.Data.Tasks.Single().Title,"local persistence");}finally{if(File.Exists(temp))File.Delete(temp);}
var envTemp=Path.Combine(Path.GetTempPath(),$"lighttodo-env-test-{Guid.NewGuid():N}.json");
Environment.SetEnvironmentVariable("LIGHTTODO_DATA_PATH",envTemp);
try{var store=new LocalStore();store.Data.Tasks.Add(new(){Title="环境路径",Start=now});store.Save();Equal(true,File.Exists(envTemp),"environment data path");}finally{Environment.SetEnvironmentVariable("LIGHTTODO_DATA_PATH",null);if(File.Exists(envTemp))File.Delete(envTemp);}
var legacyTemp=Path.Combine(Path.GetTempPath(),$"lighttodo-legacy-{Guid.NewGuid():N}.json");
try{
    File.WriteAllText(legacyTemp,"{\"Tasks\":[{\"Id\":1,\"Title\":\"旧事项\",\"Start\":\"2026-09-13T10:00:00\"}],\"Goals\":[],\"Courses\":[]}");
    var migrated=new LocalStore(legacyTemp);var old=migrated.Data.Tasks.Single();
    Equal(true,Guid.TryParse(old.SyncId,out _),"legacy sync id");Equal(true,old.CreatedAtUtc!=default,"legacy created timestamp");Equal(0L,old.SyncVersion,"legacy sync version");
    var persisted=new LocalStore(legacyTemp).Data.Tasks.Single();Equal(old.SyncId,persisted.SyncId,"legacy metadata persisted");
}finally{if(File.Exists(legacyTemp))File.Delete(legacyTemp);}
Equal<string?>(null,AccountRules.ValidateEmail("student@example.com"),"valid account email");
Equal("请输入有效的邮箱地址",AccountRules.ValidateEmail("bad-email"),"invalid account email");
Equal<string?>(null,AccountRules.ValidatePassword("abc12345"),"valid account password");
var accountTemp=Path.Combine(Path.GetTempPath(),$"lighttodo-account-{Guid.NewGuid():N}.dat");
try{var session=new WindowsAccountSession(accountTemp);session.Save(new(true,"ok","student@example.com","access-secret","refresh-secret"));var bytes=File.ReadAllBytes(accountTemp);Equal(false,System.Text.Encoding.UTF8.GetString(bytes).Contains("access-secret"),"account token encrypted");var restored=new WindowsAccountSession(accountTemp);Equal(true,restored.IsSignedIn,"account session restored");Equal("student@example.com",restored.Email,"account email restored");restored.Clear();Equal(false,File.Exists(accountTemp),"account session cleared");}finally{if(File.Exists(accountTemp))File.Delete(accountTemp);}
Console.WriteLine($"PASS: {assertions} Windows assertions");

using System.Windows;
using System.Windows.Threading;
namespace LightTodo.Windows;
public sealed class ReminderService : IDisposable {
    readonly LocalStore store; readonly DispatcherTimer timer = new() { Interval = TimeSpan.FromSeconds(20) }; readonly HashSet<string> fired = [];
    public ReminderService(LocalStore store) { this.store=store; timer.Tick += Tick; timer.Start(); }
    void Tick(object? s, EventArgs e) { var now=DateTime.Now; foreach(var t in store.Data.Tasks.Where(x=>!x.Done)) foreach(var m in t.ReminderMinutes) { var when=t.Start.AddMinutes(-m); var key=$"{t.Id}:{m}"; if(when<=now && when>now.AddSeconds(-30) && fired.Add(key)) MessageBox.Show($"{t.Title}\n\n{t.Start:M月d日 HH:mm}", "轻待办提醒", MessageBoxButton.OK, MessageBoxImage.Information); } }
    public void Dispose() => timer.Stop();
}

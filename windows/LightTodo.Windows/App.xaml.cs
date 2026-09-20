using System.Windows;
namespace LightTodo.Windows;
public partial class App : Application {
    protected override void OnStartup(StartupEventArgs e) {
        base.OnStartup(e);
        MainWindow = new MainWindow();
        MainWindow.Show();
    }
}

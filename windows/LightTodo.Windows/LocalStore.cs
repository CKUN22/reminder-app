using System.Text.Json;
using System.IO;
namespace LightTodo.Windows;
public sealed class LocalStore {
    readonly string path;
    readonly JsonSerializerOptions options = new() { WriteIndented = true };
    public AppData Data { get; private set; }
    public LocalStore(string? path = null) {
        this.path = path ?? Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "LightTodo", "data.json");
        Data = Load();
        if (Data.Courses.Count == 0) { SeedCourses(); Save(); }
    }
    AppData Load() { try { return File.Exists(path) ? JsonSerializer.Deserialize<AppData>(File.ReadAllText(path), options) ?? new() : new(); } catch { return new(); } }
    public void Save() { Directory.CreateDirectory(Path.GetDirectoryName(path)!); var temp = path + ".tmp"; File.WriteAllText(temp, JsonSerializer.Serialize(Data, options)); File.Move(temp, path, true); }
    void SeedCourses() {
        string[] names = ["高等数学", "大学英语", "程序设计", "思想道德与法治", "大学体育", "计算机导论"];
        string[] rooms = ["松2105", "博雅楼B203", "信科楼401", "博雅楼A108", "东操场", "信科楼305"];
        for (int i=0;i<names.Length;i++) Data.Courses.Add(new() { Id=100+i, Name=names[i], Location=rooms[i], Weekday=(DayOfWeek)(i%5+1), StartPeriod=1+(i*2)%10, EndPeriod=2+(i*2)%10, ColorIndex=i });
    }
}

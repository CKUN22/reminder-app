using System.Text.Json;
using System.IO;
namespace LightTodo.Windows;
public sealed class LocalStore {
    readonly string path;
    readonly JsonSerializerOptions options = new() { WriteIndented = true };
    public AppData Data { get; private set; }
    public LocalStore(string? path = null) {
        this.path = path
            ?? Environment.GetEnvironmentVariable("LIGHTTODO_DATA_PATH")
            ?? Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "LightTodo", "data.json");
        Data = Load();
        var migrated = NormalizeSyncMetadata();
        if (Data.Courses.Count == 0) { SeedCourses(); migrated = true; }
        if (migrated) Save(false);
    }
    AppData Load() { try { return File.Exists(path) ? JsonSerializer.Deserialize<AppData>(File.ReadAllText(path), options) ?? new() : new(); } catch { return new(); } }
    public void Save(bool markDirty = true) { if(markDirty){var now=DateTime.UtcNow;foreach(var entity in Data.Tasks.Cast<SyncEntity>().Concat(Data.Goals).Concat(Data.Courses).Where(x=>x.DeletedAtUtc==null)){entity.UpdatedAtUtc=now;entity.SyncDirty=true;}}Directory.CreateDirectory(Path.GetDirectoryName(path)!); var temp = path + ".tmp"; File.WriteAllText(temp, JsonSerializer.Serialize(Data, options)); File.Move(temp, path, true); }
    bool NormalizeSyncMetadata() {
        var changed = false;
        foreach (var entity in Data.Tasks.Cast<SyncEntity>().Concat(Data.Goals).Concat(Data.Courses)) {
            if (!Guid.TryParse(entity.SyncId, out _)) { entity.SyncId = Guid.NewGuid().ToString("D"); changed = true; }
            if (entity.CreatedAtUtc == default) { entity.CreatedAtUtc = DateTime.UtcNow; changed = true; }
            if (entity.UpdatedAtUtc == default) { entity.UpdatedAtUtc = entity.CreatedAtUtc; changed = true; }
        }
        return changed;
    }
    void SeedCourses() {
        string[] names = ["高等数学", "大学英语", "程序设计", "思想道德与法治", "大学体育", "计算机导论"];
        string[] rooms = ["松2105", "博雅楼B203", "信科楼401", "博雅楼A108", "东操场", "信科楼305"];
        for (int i=0;i<names.Length;i++) Data.Courses.Add(new() { Id=100+i, Name=names[i], Location=rooms[i], Weekday=(DayOfWeek)(i%5+1), StartPeriod=1+(i*2)%10, EndPeriod=2+(i*2)%10, ColorIndex=i });
    }
}

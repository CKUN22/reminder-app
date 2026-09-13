package com.ckun.reminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public final class TaskStore extends SQLiteOpenHelper {
    public TaskStore(Context context) { super(context, "tasks.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, note TEXT NOT NULL, start INTEGER NOT NULL, duration INTEGER NOT NULL, ten INTEGER NOT NULL, day INTEGER NOT NULL, done INTEGER NOT NULL, revision INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
    public List<Task> all() {
        List<Task> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("tasks", null, null, null, null, null, "start ASC, id ASC")) {
            while (c.moveToNext()) result.add(read(c));
        }
        return result;
    }
    public Task get(long id) {
        try (Cursor c = getReadableDatabase().query("tasks", null, "id=?", new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? read(c) : null;
        }
    }
    public void save(Task t) {
        t.revision++;
        ContentValues v = new ContentValues();
        v.put("title", t.title); v.put("note", t.note); v.put("start", t.start); v.put("duration", t.duration);
        v.put("ten", t.earlyTen ? 1 : 0); v.put("day", t.earlyDay ? 1 : 0); v.put("done", t.done ? 1 : 0); v.put("revision", t.revision);
        if (t.id == 0) t.id = getWritableDatabase().insertOrThrow("tasks", null, v);
        else getWritableDatabase().update("tasks", v, "id=?", new String[]{String.valueOf(t.id)});
    }
    public void delete(long id) { getWritableDatabase().delete("tasks", "id=?", new String[]{String.valueOf(id)}); }
    private Task read(Cursor c) {
        Task t = new Task();
        t.id = c.getLong(0); t.title = c.getString(1); t.note = c.getString(2); t.start = c.getLong(3);
        t.duration = c.getInt(4); t.earlyTen = c.getInt(5) != 0; t.earlyDay = c.getInt(6) != 0;
        t.done = c.getInt(7) != 0; t.revision = c.getLong(8); return t;
    }
}

package com.ckun.reminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public final class TaskStore extends SQLiteOpenHelper {
    public TaskStore(Context context) { super(context, "tasks.db", null, 6); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, note TEXT NOT NULL, start INTEGER NOT NULL, duration INTEGER NOT NULL, ten INTEGER NOT NULL, day INTEGER NOT NULL, done INTEGER NOT NULL, revision INTEGER NOT NULL)");
        db.execSQL("ALTER TABLE tasks ADD COLUMN reminder_offsets TEXT");
        db.execSQL("ALTER TABLE tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 3");
        db.execSQL("ALTER TABLE tasks ADD COLUMN completed_at INTEGER NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE tasks ADD COLUMN source_goal_id INTEGER NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE tasks ADD COLUMN generated_day INTEGER NOT NULL DEFAULT 0");
        db.execSQL("ALTER TABLE tasks ADD COLUMN source_course_id INTEGER NOT NULL DEFAULT 0");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE tasks ADD COLUMN reminder_offsets TEXT");
        if (oldVersion < 3) db.execSQL("ALTER TABLE tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 3");
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN completed_at INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE tasks SET completed_at=start WHERE done=1");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN source_goal_id INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE tasks ADD COLUMN generated_day INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 6) db.execSQL("ALTER TABLE tasks ADD COLUMN source_course_id INTEGER NOT NULL DEFAULT 0");
    }
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
        v.put("priority", t.priorityIndex());
        v.put("title", t.title); v.put("note", t.note); v.put("start", t.start); v.put("duration", t.duration);
        v.put("ten", t.earlyTen ? 1 : 0); v.put("day", t.earlyDay ? 1 : 0); v.put("done", t.done ? 1 : 0); v.put("revision", t.revision);
        v.put("reminder_offsets", android.text.TextUtils.join(",", t.reminders()));
        v.put("completed_at", t.completedAt);
        v.put("source_goal_id", t.sourceGoalId); v.put("source_course_id", t.sourceCourseId); v.put("generated_day", t.generatedDay);
        if (t.id == 0) t.id = getWritableDatabase().insertOrThrow("tasks", null, v);
        else getWritableDatabase().update("tasks", v, "id=?", new String[]{String.valueOf(t.id)});
    }
    public void delete(long id) { getWritableDatabase().delete("tasks", "id=?", new String[]{String.valueOf(id)}); }
    public boolean hasGeneratedTask(long goalId, long day) {
        try (Cursor c = getReadableDatabase().query("tasks", new String[]{"id"}, "source_goal_id=? AND generated_day=?", new String[]{String.valueOf(goalId), String.valueOf(day)}, null, null, null)) { return c.moveToFirst(); }
    }
    public List<Task> unfinishedForCourse(long courseId) {
        List<Task> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("tasks", null, "source_course_id=? AND done=0", new String[]{String.valueOf(courseId)}, null, null, "start ASC, id ASC")) {
            while (c.moveToNext()) result.add(read(c));
        }
        return result;
    }
    private Task read(Cursor c) {
        Task t = new Task();
        t.id = c.getLong(0); t.title = c.getString(1); t.note = c.getString(2); t.start = c.getLong(3);
        t.duration = c.getInt(4); t.earlyTen = c.getInt(5) != 0; t.earlyDay = c.getInt(6) != 0;
        t.done = c.getInt(7) != 0; t.revision = c.getLong(8);
        t.priority = c.getInt(c.getColumnIndexOrThrow("priority"));
        t.completedAt = c.getLong(c.getColumnIndexOrThrow("completed_at"));
        t.sourceGoalId = c.getLong(c.getColumnIndexOrThrow("source_goal_id")); t.generatedDay = c.getLong(c.getColumnIndexOrThrow("generated_day"));
        t.sourceCourseId = c.getLong(c.getColumnIndexOrThrow("source_course_id"));
        String offsets = c.getString(c.getColumnIndexOrThrow("reminder_offsets"));
        if (offsets != null) {
            t.reminderMinutes = new java.util.TreeSet<>();
            if (!offsets.isEmpty()) for (String value : offsets.split(",")) t.reminderMinutes.add(Integer.parseInt(value));
        }
        return t;
    }
}

package com.ckun.reminder;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

public final class CourseStore extends SQLiteOpenHelper {
    public CourseStore(Context context) { super(context, "courses.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) { db.execSQL("CREATE TABLE courses (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, weekday INTEGER NOT NULL, start_period INTEGER NOT NULL, end_period INTEGER NOT NULL, start_week INTEGER NOT NULL, end_week INTEGER NOT NULL, location TEXT NOT NULL, teacher TEXT NOT NULL, note TEXT NOT NULL, color_index INTEGER NOT NULL)"); }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
    public List<Course> all() { List<Course> result = new ArrayList<>(); try (Cursor c = getReadableDatabase().query("courses", null, null, null, null, null, "weekday,start_period,id")) { while (c.moveToNext()) result.add(read(c)); } return result; }
    public Course get(long id) { try (Cursor c = getReadableDatabase().query("courses", null, "id=?", new String[]{String.valueOf(id)}, null, null, null)) { return c.moveToFirst() ? read(c) : null; } }
    public void save(Course c) { ContentValues v = new ContentValues(); v.put("name", c.name); v.put("weekday", c.weekday); v.put("start_period", c.startPeriod); v.put("end_period", c.endPeriod); v.put("start_week", c.startWeek); v.put("end_week", c.endWeek); v.put("location", c.location); v.put("teacher", c.teacher); v.put("note", c.note); v.put("color_index", c.colorIndex); if (c.id == 0) c.id = getWritableDatabase().insertOrThrow("courses", null, v); else getWritableDatabase().update("courses", v, "id=?", new String[]{String.valueOf(c.id)}); }
    public void delete(long id) { getWritableDatabase().delete("courses", "id=?", new String[]{String.valueOf(id)}); }
    private Course read(Cursor c) { Course v = new Course(); v.id=c.getLong(0); v.name=c.getString(1); v.weekday=c.getInt(2); v.startPeriod=c.getInt(3); v.endPeriod=c.getInt(4); v.startWeek=c.getInt(5); v.endWeek=c.getInt(6); v.location=c.getString(7); v.teacher=c.getString(8); v.note=c.getString(9); v.colorIndex=c.getInt(10); return v; }
}

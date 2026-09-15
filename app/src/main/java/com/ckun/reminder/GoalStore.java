package com.ckun.reminder;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

public final class GoalStore extends SQLiteOpenHelper {
    GoalStore(Context context) { super(context, "goals.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) { db.execSQL("CREATE TABLE goals (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, deadline INTEGER NOT NULL, daily_title TEXT NOT NULL, hour INTEGER NOT NULL, minute INTEGER NOT NULL, auto_add INTEGER NOT NULL)"); }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
    List<Goal> all() {
        List<Goal> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("goals", null, null, null, null, null, "deadline ASC, id ASC")) { while (cursor.moveToNext()) result.add(read(cursor)); }
        return result;
    }
    Goal get(long id) {
        try (Cursor cursor = getReadableDatabase().query("goals", null, "id=?", new String[]{String.valueOf(id)}, null, null, null)) { return cursor.moveToFirst() ? read(cursor) : null; }
    }
    void save(Goal goal) {
        ContentValues values = new ContentValues(); values.put("title", goal.title); values.put("deadline", goal.deadline); values.put("daily_title", goal.dailyTitle);
        values.put("hour", goal.hour); values.put("minute", goal.minute); values.put("auto_add", goal.autoAdd ? 1 : 0);
        if (goal.id == 0) goal.id = getWritableDatabase().insertOrThrow("goals", null, values); else getWritableDatabase().update("goals", values, "id=?", new String[]{String.valueOf(goal.id)});
    }
    void delete(long id) { getWritableDatabase().delete("goals", "id=?", new String[]{String.valueOf(id)}); }
    private Goal read(Cursor cursor) {
        Goal goal = new Goal(); goal.id = cursor.getLong(cursor.getColumnIndexOrThrow("id")); goal.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        goal.deadline = cursor.getLong(cursor.getColumnIndexOrThrow("deadline")); goal.dailyTitle = cursor.getString(cursor.getColumnIndexOrThrow("daily_title"));
        goal.hour = cursor.getInt(cursor.getColumnIndexOrThrow("hour")); goal.minute = cursor.getInt(cursor.getColumnIndexOrThrow("minute")); goal.autoAdd = cursor.getInt(cursor.getColumnIndexOrThrow("auto_add")) != 0; return goal;
    }
}

package com.ckun.reminder;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

public final class GoalStore extends SQLiteOpenHelper {
    GoalStore(Context context) { super(context, "goals.db", null, 3); }
    @Override public void onCreate(SQLiteDatabase db) { db.execSQL("CREATE TABLE goals (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, deadline INTEGER NOT NULL, daily_title TEXT NOT NULL, hour INTEGER NOT NULL, minute INTEGER NOT NULL, auto_add INTEGER NOT NULL, sync_id TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER NOT NULL DEFAULT 0, sync_version INTEGER NOT NULL DEFAULT 0, sync_dirty INTEGER NOT NULL DEFAULT 1)"); }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { if (oldVersion < 2) addSyncColumns(db); if(oldVersion<3)db.execSQL("ALTER TABLE goals ADD COLUMN sync_dirty INTEGER NOT NULL DEFAULT 1"); }
    List<Goal> all() {
        List<Goal> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query("goals", null, "deleted_at=0", null, null, null, "deadline ASC, id ASC")) { while (cursor.moveToNext()) result.add(read(cursor)); }
        return result;
    }
    Goal get(long id) {
        try (Cursor cursor = getReadableDatabase().query("goals", null, "id=? AND deleted_at=0", new String[]{String.valueOf(id)}, null, null, null)) { return cursor.moveToFirst() ? read(cursor) : null; }
    }
    void save(Goal goal) {
        long now=System.currentTimeMillis(); if(goal.syncId==null||goal.syncId.isEmpty())goal.syncId=UUID.randomUUID().toString(); if(goal.createdAt==0)goal.createdAt=now; goal.updatedAt=now; goal.deletedAt=0;
        ContentValues values = new ContentValues(); values.put("title", goal.title); values.put("deadline", goal.deadline); values.put("daily_title", goal.dailyTitle);
        values.put("hour", goal.hour); values.put("minute", goal.minute); values.put("auto_add", goal.autoAdd ? 1 : 0);
        putSync(values,goal);
        values.put("sync_dirty",1);
        if (goal.id == 0) goal.id = getWritableDatabase().insertOrThrow("goals", null, values); else getWritableDatabase().update("goals", values, "id=?", new String[]{String.valueOf(goal.id)});
    }
    void delete(long id) { ContentValues v=new ContentValues();long now=System.currentTimeMillis();v.put("deleted_at",now);v.put("updated_at",now);v.put("sync_dirty",1);getWritableDatabase().update("goals",v,"id=?",new String[]{String.valueOf(id)}); }
    private Goal read(Cursor cursor) {
        Goal goal = new Goal(); goal.id = cursor.getLong(cursor.getColumnIndexOrThrow("id")); goal.title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
        goal.deadline = cursor.getLong(cursor.getColumnIndexOrThrow("deadline")); goal.dailyTitle = cursor.getString(cursor.getColumnIndexOrThrow("daily_title"));
        goal.hour = cursor.getInt(cursor.getColumnIndexOrThrow("hour")); goal.minute = cursor.getInt(cursor.getColumnIndexOrThrow("minute")); goal.autoAdd = cursor.getInt(cursor.getColumnIndexOrThrow("auto_add")) != 0;
        goal.syncId=cursor.getString(cursor.getColumnIndexOrThrow("sync_id"));goal.createdAt=cursor.getLong(cursor.getColumnIndexOrThrow("created_at"));goal.updatedAt=cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"));goal.deletedAt=cursor.getLong(cursor.getColumnIndexOrThrow("deleted_at"));goal.syncVersion=cursor.getLong(cursor.getColumnIndexOrThrow("sync_version"));return goal;
    }
    private static void addSyncColumns(SQLiteDatabase db){db.execSQL("ALTER TABLE goals ADD COLUMN sync_id TEXT");db.execSQL("ALTER TABLE goals ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0");db.execSQL("ALTER TABLE goals ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0");db.execSQL("ALTER TABLE goals ADD COLUMN deleted_at INTEGER NOT NULL DEFAULT 0");db.execSQL("ALTER TABLE goals ADD COLUMN sync_version INTEGER NOT NULL DEFAULT 0");long now=System.currentTimeMillis();try(Cursor c=db.query("goals",new String[]{"id"},null,null,null,null,null)){while(c.moveToNext()){ContentValues v=new ContentValues();v.put("sync_id",UUID.randomUUID().toString());v.put("created_at",now);v.put("updated_at",now);db.update("goals",v,"id=?",new String[]{String.valueOf(c.getLong(0))});}}}
    private static void putSync(ContentValues v,Goal g){v.put("sync_id",g.syncId);v.put("created_at",g.createdAt);v.put("updated_at",g.updatedAt);v.put("deleted_at",g.deletedAt);v.put("sync_version",g.syncVersion);}
}

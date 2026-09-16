package com.ckun.reminder;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

public final class CourseStore extends SQLiteOpenHelper {
    public CourseStore(Context context) { super(context, "courses.db", null, 2); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE courses (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, weekday INTEGER NOT NULL, start_period INTEGER NOT NULL, end_period INTEGER NOT NULL, start_week INTEGER NOT NULL, end_week INTEGER NOT NULL, location TEXT NOT NULL, teacher TEXT NOT NULL, note TEXT NOT NULL, color_index INTEGER NOT NULL)");
        seedInitialTimetable(db);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { if (oldVersion < 2) seedInitialTimetable(db); }
    public List<Course> all() { List<Course> result = new ArrayList<>(); try (Cursor c = getReadableDatabase().query("courses", null, null, null, null, null, "weekday,start_period,id")) { while (c.moveToNext()) result.add(read(c)); } return result; }
    public Course get(long id) { try (Cursor c = getReadableDatabase().query("courses", null, "id=?", new String[]{String.valueOf(id)}, null, null, null)) { return c.moveToFirst() ? read(c) : null; } }
    public void save(Course c) { ContentValues v = new ContentValues(); v.put("name", c.name); v.put("weekday", c.weekday); v.put("start_period", c.startPeriod); v.put("end_period", c.endPeriod); v.put("start_week", c.startWeek); v.put("end_week", c.endWeek); v.put("location", c.location); v.put("teacher", c.teacher); v.put("note", c.note); v.put("color_index", c.colorIndex); if (c.id == 0) c.id = getWritableDatabase().insertOrThrow("courses", null, v); else getWritableDatabase().update("courses", v, "id=?", new String[]{String.valueOf(c.id)}); }
    public void delete(long id) { getWritableDatabase().delete("courses", "id=?", new String[]{String.valueOf(id)}); }
    private static void seedInitialTimetable(SQLiteDatabase db) {
        Object[][] rows = {
                {"工程训练",1,1,4,1,16,"工训中心329",5},
                {"流体力学",1,5,6,1,14,"松2105",3},
                {"工程数学",1,7,9,1,16,"松2225",2},
                {"工程热力学",1,10,13,1,14,"松2105",4},
                {"流体力学",2,3,4,1,14,"松1217",3},
                {"大学物理",3,3,4,1,16,"松1334",2},
                {"大物实验",3,5,6,2,16,"",1},
                {"形势政策",3,7,8,2,7,"1137",5},
                {"软式曲棍球",4,3,4,1,16,"",0},
                {"热工基础实验",4,5,8,10,13,"",1},
                {"计算机组装",4,7,8,1,16,"1号学院楼142",4},
                {"信息检索",4,10,11,1,8,"图文8号机房",5},
                {"西方文化史",4,12,13,1,16,"松2250",3},
                {"服务器硬件拆解",5,3,4,1,8,"一号学院楼142",3},
                {"大学物理",5,5,6,1,8,"松1334",2}
        };
        for (Object[] row : rows) {
            db.execSQL("INSERT INTO courses (name,weekday,start_period,end_period,start_week,end_week,location,teacher,note,color_index) " +
                            "SELECT ?,?,?,?,?,?,?,'','',? WHERE NOT EXISTS (SELECT 1 FROM courses WHERE name=? AND weekday=? AND start_period=? AND end_period=? AND start_week=? AND end_week=?)",
                    new Object[]{row[0],row[1],row[2],row[3],row[4],row[5],row[6],row[7],row[0],row[1],row[2],row[3],row[4],row[5]});
        }
    }
    private Course read(Cursor c) { Course v = new Course(); v.id=c.getLong(0); v.name=c.getString(1); v.weekday=c.getInt(2); v.startPeriod=c.getInt(3); v.endPeriod=c.getInt(4); v.startWeek=c.getInt(5); v.endWeek=c.getInt(6); v.location=c.getString(7); v.teacher=c.getString(8); v.note=c.getString(9); v.colorIndex=c.getInt(10); return v; }
}

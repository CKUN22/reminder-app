package com.ckun.reminder;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import android.content.Intent;

public class TaskStoreTest {
    private Context getContext() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    @Before public void setUp() { getContext().deleteDatabase("tasks.db"); }
    @After public void tearDown() { getContext().deleteDatabase("tasks.db"); }
    @Test public void testPersistenceUpdateCompleteAndDelete() {
        long id;
        try (TaskStore store = new TaskStore(getContext())) {
            Task t = new Task(); t.title = "读书"; t.note = "第二章"; t.start = System.currentTimeMillis() + 3600000;
            t.duration = 30; t.earlyTen = true; store.save(t); id = t.id;
        }
        try (TaskStore store = new TaskStore(getContext())) {
            Task t = store.get(id); assertEquals("读书", t.title); assertEquals("第二章", t.note);
            assertEquals(30, t.duration); assertTrue(t.earlyTen); assertFalse(t.done);
            long revision = t.revision; t.title = "散步"; t.start += 86400000; store.save(t);
            assertEquals(revision + 1, store.get(id).revision); assertEquals("散步", store.get(id).title);
            t.done = true; store.save(t); assertTrue(store.get(id).done);
            store.delete(id); assertNull(store.get(id)); assertEquals(0, store.all().size());
        }
    }
    @Test public void testStaleNotificationCannotCompleteEditedTask() {
        try (TaskStore store = new TaskStore(getContext())) {
            Task t = new Task(); t.title = "旧事项"; t.start = System.currentTimeMillis() + 3600000; store.save(t);
            long old = t.revision; t.title = "新事项"; store.save(t);
            ReminderReceiver receiver = new ReminderReceiver();
            receiver.onReceive(getContext(), new Intent("COMPLETE").putExtra("id", t.id).putExtra("revision", old));
            assertFalse(store.get(t.id).done);
            receiver.onReceive(getContext(), new Intent("COMPLETE").putExtra("id", t.id).putExtra("revision", t.revision));
            assertTrue(store.get(t.id).done);
        }
    }
    @Test public void migrationPreservesLegacyRemindersAndCustomSelections() {
        try (android.database.sqlite.SQLiteDatabase db = getContext().openOrCreateDatabase("tasks.db", 0, null)) {
            db.execSQL("CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, note TEXT NOT NULL, start INTEGER NOT NULL, duration INTEGER NOT NULL, ten INTEGER NOT NULL, day INTEGER NOT NULL, done INTEGER NOT NULL, revision INTEGER NOT NULL)");
            db.execSQL("INSERT INTO tasks VALUES (1, '旧事项', '', 2000000000000, 30, 1, 1, 0, 3)"); db.setVersion(1);
        }
        try (TaskStore store = new TaskStore(getContext())) {
            Task task = store.get(1); assertEquals(new java.util.TreeSet<>(java.util.List.of(0, 10, 1440)), task.reminders());
            task.reminderMinutes = new java.util.TreeSet<>(java.util.List.of(15, 75, 2880)); store.save(task);
        }
        try (TaskStore store = new TaskStore(getContext())) {
            Task task = store.get(1); assertEquals(new java.util.TreeSet<>(java.util.List.of(15, 75, 2880)), task.reminders());
            task.reminderMinutes.clear(); store.save(task); assertTrue(store.get(1).reminders().isEmpty());
        }
    }
}

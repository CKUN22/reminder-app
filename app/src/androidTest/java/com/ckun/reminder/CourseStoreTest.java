package com.ckun.reminder;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import static org.junit.Assert.*;

public final class CourseStoreTest {
    private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    @Before public void before(){context().deleteDatabase("courses.db");}
    @After public void after(){context().deleteDatabase("courses.db");}
    @Test public void persistsUpdatesAndDeletesCourse(){long id;try(CourseStore store=new CourseStore(context())){Course c=new Course();c.name="高等数学";c.weekday=1;c.startPeriod=5;c.endPeriod=6;c.startWeek=2;c.endWeek=7;c.location="教学楼101";c.teacher="王老师";c.note="带教材";c.colorIndex=3;store.save(c);id=c.id;}try(CourseStore store=new CourseStore(context())){Course c=store.get(id);assertEquals("高等数学",c.name);assertEquals(5,c.startPeriod);assertEquals(7,c.endWeek);assertEquals("教学楼101",c.location);assertEquals(3,c.colorIndex);c.name="线性代数";store.save(c);assertEquals("线性代数",store.get(id).name);store.delete(id);assertNull(store.get(id));}}
    @Test public void seedsProvidedTimetableExactlyOnce(){try(CourseStore store=new CourseStore(context())){assertEquals(15,store.all().size());Course first=store.all().get(0);assertEquals("工程训练",first.name);assertEquals(1,first.startPeriod);assertEquals(4,first.endPeriod);assertEquals("工训中心329",first.location);assertEquals(16,first.endWeek);}try(CourseStore store=new CourseStore(context())){assertEquals(15,store.all().size());}}
}

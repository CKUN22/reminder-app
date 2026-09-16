package com.ckun.reminder;

import android.app.*;
import android.content.*;
import android.view.View;
import android.widget.HorizontalScrollView;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

public final class TimetableActivityTest {
    @Test public void showsInteractiveWeeklyGridWithoutHorizontalScrolling(){android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();Context context=instrumentation.getTargetContext();Activity activity=instrumentation.startActivitySync(new Intent(context,TimetableActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));try{instrumentation.runOnMainSync(()->{View grid=activity.getWindow().getDecorView().findViewWithTag("timetable-grid");assertNotNull(grid);assertEquals("每周课程表网格",grid.getContentDescription());assertFalse(grid.getParent() instanceof HorizontalScrollView);});}finally{instrumentation.runOnMainSync(activity::finish);}}
}

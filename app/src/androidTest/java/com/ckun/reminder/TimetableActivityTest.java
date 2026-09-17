package com.ckun.reminder;

import android.app.*;
import android.content.*;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

public final class TimetableActivityTest {
    @Test public void showsCombinedHomeworkModeAndInteractiveGrid(){android.app.Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();Context context=instrumentation.getTargetContext();Activity activity=instrumentation.startActivitySync(new Intent(context,TimetableActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));try{instrumentation.runOnMainSync(()->{View root=activity.getWindow().getDecorView();View grid=root.findViewWithTag("timetable-grid");assertNotNull(grid);assertEquals("每周课程表网格",grid.getContentDescription());assertFalse(grid.getParent() instanceof HorizontalScrollView);Button combined=root.findViewWithTag("timetable-mode-0");assertEquals("查看&作业",combined.getText().toString());assertNull(findButton(root,"作业"));String before=((TextView)root.findViewWithTag("timetable-week")).getText().toString();long now=android.os.SystemClock.uptimeMillis();grid.dispatchTouchEvent(android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_DOWN,200,200,0));grid.dispatchTouchEvent(android.view.MotionEvent.obtain(now,now+50,android.view.MotionEvent.ACTION_UP,80,200,0));String after=((TextView)activity.getWindow().getDecorView().findViewWithTag("timetable-week")).getText().toString();assertNotEquals(before,after);});}finally{instrumentation.runOnMainSync(activity::finish);}}
    private Button findButton(View view,String value){if(view instanceof Button&&value.contentEquals(((Button)view).getText()))return (Button)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){Button found=findButton(((ViewGroup)view).getChildAt(i),value);if(found!=null)return found;}return null;}
}

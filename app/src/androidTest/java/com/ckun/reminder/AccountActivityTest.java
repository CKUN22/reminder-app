package com.ckun.reminder;

import android.app.*;
import android.content.*;
import android.view.View;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import static org.junit.Assert.*;

public final class AccountActivityTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private final Context context=instrumentation.getTargetContext();
    static final class FakeAuth implements AuthService {
        public void signIn(String email,String password,Callback callback){callback.complete(AuthResult.session(email,"access-token","refresh-token"));}
        public void signUp(String email,String password,Callback callback){callback.complete(AuthResult.verification(email));}
        public void resetPassword(String email,Callback callback){callback.complete(AuthResult.ok("密码重置邮件已发送"));}
    }
    @Before public void before(){context.getSharedPreferences("account_session",0).edit().clear().commit();AccountActivity.serviceFactory=FakeAuth::new;}
    @After public void after(){context.getSharedPreferences("account_session",0).edit().clear().commit();AccountActivity.serviceFactory=SupabaseAuthService::new;}
    private Activity launch(){return instrumentation.startActivitySync(new Intent(context,AccountActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    private View tagged(Activity activity,String tag){return activity.getWindow().getDecorView().findViewWithTag(tag);}
    @Test public void validatesThenSignsInStoresEncryptedSessionAndLogsOut(){Activity activity=launch();try{
        instrumentation.runOnMainSync(()->tagged(activity,"account-login").performClick());TextView error=(TextView)tagged(activity,"account-status");assertEquals("请输入有效的邮箱地址",error.getText().toString());assertEquals(0xffB3261E,error.getCurrentTextColor());
        instrumentation.runOnMainSync(()->{View email=tagged(activity,"account-email-input"),password=tagged(activity,"account-password-input"),status=tagged(activity,"account-status"),login=tagged(activity,"account-login"),signup=tagged(activity,"account-signup"),reset=tagged(activity,"account-reset");assertTrue(email.getBottom()<=password.getTop());assertTrue(password.getBottom()<=status.getTop());assertTrue(status.getBottom()<=login.getTop());assertTrue(login.getBottom()<=signup.getTop());assertTrue(signup.getBottom()<=reset.getTop());});
        instrumentation.runOnMainSync(()->{((EditText)tagged(activity,"account-email-input")).setText("student@example.com");((EditText)tagged(activity,"account-password-input")).setText("abc12345");tagged(activity,"account-login").performClick();});instrumentation.waitForIdleSync();
        assertTrue(AccountSession.open(context).isSignedIn());assertEquals("student@example.com",AccountSession.open(context).email());assertNotNull(tagged(activity,"account-logout"));
        String stored=context.getSharedPreferences("account_session",0).getString("encrypted_session","");assertFalse(stored.contains("access-token"));assertFalse(stored.contains("student@example.com"));
        instrumentation.runOnMainSync(()->tagged(activity,"account-logout").performClick());assertFalse(AccountSession.open(context).isSignedIn());assertNotNull(tagged(activity,"account-login"));
    }finally{instrumentation.runOnMainSync(activity::finish);}}
    @Test public void signupAndResetShowSafeStatusMessages(){Activity activity=launch();try{instrumentation.runOnMainSync(()->{((EditText)tagged(activity,"account-email-input")).setText("student@example.com");((EditText)tagged(activity,"account-password-input")).setText("abc12345");tagged(activity,"account-signup").performClick();});instrumentation.waitForIdleSync();assertTrue(((TextView)tagged(activity,"account-status")).getText().toString().contains("验证邮件"));instrumentation.runOnMainSync(()->tagged(activity,"account-reset").performClick());instrumentation.waitForIdleSync();assertTrue(((TextView)tagged(activity,"account-status")).getText().toString().contains("重置邮件"));}finally{instrumentation.runOnMainSync(activity::finish);}}
}

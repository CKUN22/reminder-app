package com.ckun.reminder;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;

public final class AccountActivity extends Activity {
    interface ServiceFactory { AuthService create(); }
    static ServiceFactory serviceFactory=SupabaseAuthService::new;
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override public void onCreate(Bundle state){super.onCreate(state);render();}
    private void render(){
        AccountSession session=AccountSession.open(this);ScrollView scroll=new ScrollView(this);scroll.setTag("account-page");scroll.setBackground(Appearance.background(this,0xffFFF8ED));LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(24),dp(20),dp(24),dp(36));scroll.addView(page);setContentView(scroll);
        Button back=button("‹  返回","account-back");back.setOnClickListener(v->finish());page.addView(back,new LinearLayout.LayoutParams(dp(96),dp(48)));TextView title=text("账户与同步",30);title.setTypeface(null,Typeface.BOLD);page.addView(title);
        if(session.isSignedIn()){page.addView(text("已登录",16));TextView email=text(session.email(),20);email.setTag("account-email");page.addView(email);page.addView(text("账户已连接。云端数据同步将在下一阶段启用。",14));Button logout=button("退出登录","account-logout");logout.setOnClickListener(v->{session.clear();render();});page.addView(logout,new LinearLayout.LayoutParams(-1,dp(56)));return;}
        page.addView(text("使用邮箱和密码登录。登录前，本地数据不会上传。",14));EditText email=input("邮箱",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,"account-email-input");EditText password=input("密码（至少 8 位，包含字母和数字）",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD,"account-password-input");page.addView(email);page.addView(password);
        TextView status=text(BuildConfig.SUPABASE_URL.isEmpty()?"账户服务尚未配置，可先完成界面和本地测试。":"",14);status.setTag("account-status");page.addView(status);ProgressBar progress=new ProgressBar(this);progress.setTag("account-progress");progress.setVisibility(View.GONE);page.addView(progress);
        Button login=button("登录","account-login"),signup=button("注册","account-signup"),reset=button("忘记密码","account-reset");page.addView(login,new LinearLayout.LayoutParams(-1,dp(56)));page.addView(signup,new LinearLayout.LayoutParams(-1,dp(56)));page.addView(reset,new LinearLayout.LayoutParams(-1,dp(52)));
        AuthService service=serviceFactory.create();login.setOnClickListener(v->submit(false,email,password,status,progress,login,signup,reset,service));signup.setOnClickListener(v->submit(true,email,password,status,progress,login,signup,reset,service));reset.setOnClickListener(v->{String error=AuthRules.validateEmail(email.getText().toString());if(error!=null){status.setText(error);return;}busy(true,progress,login,signup,reset);service.resetPassword(email.getText().toString().trim(),r->runOnUiThread(()->finishRequest(r,status,progress,login,signup,reset)));});
    }
    private void submit(boolean signup,EditText email,EditText password,TextView status,ProgressBar progress,Button login,Button register,Button reset,AuthService service){String mail=email.getText().toString().trim(),error=AuthRules.validateEmail(mail);if(error==null)error=AuthRules.validatePassword(password.getText().toString());if(error!=null){status.setText(error);return;}busy(true,progress,login,register,reset);AuthService.Callback done=r->runOnUiThread(()->{if(r.success&&!r.accessToken.isEmpty()){AccountSession.open(this).save(r);render();}else finishRequest(r,status,progress,login,register,reset);});if(signup)service.signUp(mail,password.getText().toString(),done);else service.signIn(mail,password.getText().toString(),done);}
    private void finishRequest(AuthResult result,TextView status,ProgressBar progress,Button...buttons){busy(false,progress,buttons);status.setText(result.message);}
    private void busy(boolean value,ProgressBar progress,Button...buttons){progress.setVisibility(value?View.VISIBLE:View.GONE);for(Button button:buttons)button.setEnabled(!value);}
    private EditText input(String hint,int type,String tag){EditText input=new EditText(this);input.setHint(hint);input.setInputType(type);input.setTag(tag);input.setSingleLine(true);input.setPadding(dp(16),dp(12),dp(16),dp(12));return input;}
    private Button button(String value,String tag){Button button=new Button(this);button.setText(value);button.setTag(tag);button.setTextColor(0xff4A3E35);button.setBackground(Appearance.shape(this,0xffFFFCF7,22));return button;}
    private TextView text(String value,int size){TextView text=new TextView(this);text.setText(value);text.setTextSize(size);text.setTextColor(0xff4A3E35);text.setPadding(0,dp(12),0,dp(12));return text;}
}

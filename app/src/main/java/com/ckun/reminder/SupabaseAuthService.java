package com.ckun.reminder;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public final class SupabaseAuthService implements AuthService {
    private final String baseUrl, key;
    public SupabaseAuthService(){baseUrl=BuildConfig.SUPABASE_URL.trim();key=BuildConfig.SUPABASE_ANON_KEY.trim();}
    public void signIn(String email,String password,Callback callback){request("/auth/v1/token?grant_type=password",body(email,password),email,false,callback);}
    public void signUp(String email,String password,Callback callback){request("/auth/v1/signup",body(email,password),email,true,callback);}
    public void resetPassword(String email,Callback callback){request("/auth/v1/recover",body(email,null),email,false,r->callback.complete(r.success?AuthResult.ok("密码重置邮件已发送"):r));}
    private static JSONObject body(String email,String password){JSONObject value=new JSONObject();try{value.put("email",email);if(password!=null)value.put("password",password);}catch(org.json.JSONException impossible){throw new IllegalStateException(impossible);}return value;}
    private void request(String path,JSONObject body,String email,boolean signup,Callback callback){
        if(baseUrl.isEmpty()||key.isEmpty()){callback.complete(AuthResult.error("尚未配置云端账户服务"));return;}
        Executors.newSingleThreadExecutor().execute(()->{AuthResult result;HttpURLConnection connection=null;try{
            connection=(HttpURLConnection)new URL(baseUrl.replaceAll("/+$","")+path).openConnection();connection.setRequestMethod("POST");connection.setConnectTimeout(12000);connection.setReadTimeout(12000);connection.setDoOutput(true);
            connection.setRequestProperty("apikey",key);connection.setRequestProperty("Authorization","Bearer "+key);connection.setRequestProperty("Content-Type","application/json");
            try(OutputStream out=connection.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
            int code=connection.getResponseCode();String text=read(code>=200&&code<300?connection.getInputStream():connection.getErrorStream());JSONObject json=text.isEmpty()?new JSONObject():new JSONObject(text);
            if(code>=200&&code<300){String access=json.optString("access_token");String refresh=json.optString("refresh_token");result=!access.isEmpty()?AuthResult.session(email,access,refresh):(signup?AuthResult.verification(email):AuthResult.ok("请求已完成"));}
            else result=AuthResult.error(friendly(json.optString("msg",json.optString("error_description",json.optString("message","请求失败")))));
        }catch(Exception e){result=AuthResult.error("无法连接账户服务，请检查网络后重试");}finally{if(connection!=null)connection.disconnect();}callback.complete(result);});
    }
    private static String read(InputStream input)throws IOException{if(input==null)return "";try(BufferedReader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))){StringBuilder out=new StringBuilder();String line;while((line=reader.readLine())!=null)out.append(line);return out.toString();}}
    private static String friendly(String value){String lower=value.toLowerCase();if(lower.contains("invalid login"))return "邮箱或密码不正确";if(lower.contains("already registered"))return "该邮箱已经注册";if(lower.contains("rate limit"))return "请求过于频繁，请稍后再试";return value.isEmpty()?"账户请求失败":value;}
}

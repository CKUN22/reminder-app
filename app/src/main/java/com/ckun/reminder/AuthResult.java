package com.ckun.reminder;

public final class AuthResult {
    public final boolean success, needsEmailVerification;
    public final String message, email, accessToken, refreshToken;
    private AuthResult(boolean success, boolean verify, String message, String email, String access, String refresh) {
        this.success=success; this.needsEmailVerification=verify; this.message=message; this.email=email; this.accessToken=access; this.refreshToken=refresh;
    }
    public static AuthResult session(String email,String access,String refresh){return new AuthResult(true,false,"登录成功",email,access,refresh);}
    public static AuthResult verification(String email){return new AuthResult(true,true,"验证邮件已发送，请查收后登录",email,"","");}
    public static AuthResult ok(String message){return new AuthResult(true,false,message,"","","");}
    public static AuthResult error(String message){return new AuthResult(false,false,message,"","","");}
}

package com.ckun.reminder;

public interface AuthService {
    interface Callback { void complete(AuthResult result); }
    void signIn(String email,String password,Callback callback);
    void signUp(String email,String password,Callback callback);
    void resetPassword(String email,Callback callback);
}

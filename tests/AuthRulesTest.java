package com.ckun.reminder;

public final class AuthRulesTest {
    static int assertions;
    static void ok(boolean value,String name){assertions++;if(!value)throw new AssertionError(name);}
    public static void main(String[] args){
        ok(AuthRules.validateEmail("student@example.com")==null,"valid email");
        ok(AuthRules.validateEmail("not-an-email")!=null,"invalid email");
        ok(AuthRules.validatePassword("abc12345")==null,"valid password");
        ok(AuthRules.validatePassword("short1")!=null,"short password");
        ok(AuthRules.validatePassword("onlyletters")!=null,"password requires number");
        ok(AuthRules.validatePassword("12345678")!=null,"password requires letter");
        System.out.println("PASS: "+assertions+" auth rule assertions");
    }
}

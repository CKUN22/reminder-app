package com.ckun.reminder;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class AccountSession {
    private static final String ALIAS="lighttodo-account-session-v1", PREFS="account_session", VALUE="encrypted_session";
    private final Context context;
    private String email="",accessToken="",refreshToken="";
    private AccountSession(Context context){this.context=context.getApplicationContext();load();}
    public static AccountSession open(Context context){return new AccountSession(context);}
    public boolean isSignedIn(){return !accessToken.isEmpty();}
    public String email(){return email;}
    public String accessToken(){return accessToken;}
    public String refreshToken(){return refreshToken;}
    public void save(AuthResult result){if(!result.success||result.accessToken.isEmpty())throw new IllegalArgumentException("A signed-in result is required");email=result.email;accessToken=result.accessToken;refreshToken=result.refreshToken;persist();}
    public void clear(){email="";accessToken="";refreshToken="";context.getSharedPreferences(PREFS,0).edit().remove(VALUE).apply();}
    private void persist(){try{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());byte[] encrypted=cipher.doFinal((email+"\n"+accessToken+"\n"+refreshToken).getBytes(StandardCharsets.UTF_8));String value=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+"."+Base64.encodeToString(encrypted,Base64.NO_WRAP);context.getSharedPreferences(PREFS,0).edit().putString(VALUE,value).apply();}catch(Exception e){throw new IllegalStateException("Unable to protect account session",e);}}
    private void load(){String value=context.getSharedPreferences(PREFS,0).getString(VALUE,"");if(value.isEmpty())return;try{String[] pieces=value.split("\\.",2);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(pieces[0],Base64.NO_WRAP)));String[] fields=new String(cipher.doFinal(Base64.decode(pieces[1],Base64.NO_WRAP)),StandardCharsets.UTF_8).split("\n",-1);if(fields.length==3){email=fields[0];accessToken=fields[1];refreshToken=fields[2];}else clear();}catch(Exception e){clear();}}
    private SecretKey key()throws Exception{KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);if(store.containsAlias(ALIAS))return (SecretKey)store.getKey(ALIAS,null);KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return generator.generateKey();}
}

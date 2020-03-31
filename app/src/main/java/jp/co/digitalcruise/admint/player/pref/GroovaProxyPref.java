package jp.co.digitalcruise.admint.player.pref;

import android.content.Context;
import android.preference.PreferenceManager;

import jp.co.digitalcruise.admint.player.AdmintApplication;
import jp.co.digitalcruise.admint.player.R;

public class GroovaProxyPref {

    private static final String PREF_NAME = "groova_proxy_pref";

    private static final String KEY_GROOVA_ENABLE = "groova_proxy_enable";
    private static final String KEY_GROOVA_HOST = "groova_proxy_host";
    private static final String KEY_GROOVA_PORT = "groova_proxy_port";
    private static final String KEY_GROOVA_USER = "groova_proxy_user";
    private static final String KEY_GROOVA_PASS = "groova_proxy_pass";

    public static void setGroovaProxyEnable(boolean val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_GROOVA_ENABLE, val).commit();
    }

    public static boolean getGroovaProxyEnable(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean(KEY_GROOVA_ENABLE, false);
    }

    public static void setGroovaProxyHost(String val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString(KEY_GROOVA_HOST, val).commit();
    }

    public static String getGroovaProxyHost(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_GROOVA_HOST, "");
    }

    public static void setGroovaProxyPort(String val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString(KEY_GROOVA_PORT, val).commit();
    }

    public static String getGroovaProxyPort(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_GROOVA_PORT, "8080");
    }

    public static void setGroovaProxyUser(String val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString(KEY_GROOVA_USER, val).commit();
    }

    public static String getGroovaProxyUser(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_GROOVA_USER, "");
    }

    public static void setGroovaProxyPassword(String val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString(KEY_GROOVA_PASS, val).commit();
    }

    public static String getGroovaProxyPassword(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_GROOVA_PASS, "");
    }

    public static void setCheckGroovaProxy(boolean val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(R.string.setting_key_check_toto_proxy), val).apply();
    }
    public static boolean getCheckGroovaProxy(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_check_toto_proxy), false);
    }
}

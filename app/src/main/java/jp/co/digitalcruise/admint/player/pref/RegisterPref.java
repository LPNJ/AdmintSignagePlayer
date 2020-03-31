package jp.co.digitalcruise.admint.player.pref;

import android.content.Context;

import jp.co.digitalcruise.admint.player.AdmintApplication;

public class RegisterPref {

    private static final String PREF_NAME = "register";

    private static final String SEND_FROM_REGISTER = "send_from_register";

    private static final String WIFI_DISCONNECTED_TIME = "wifi_disconnected_time";

    public static void setOpenProxySettingFromRegister(boolean val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putBoolean(SEND_FROM_REGISTER, val).apply();
    }

    public static boolean getOpenProxySettingFromRegister(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean(SEND_FROM_REGISTER, false);
    }

    public static void setDetectDisconnectedWifi(long val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putLong(WIFI_DISCONNECTED_TIME, val).apply();
    }

    public static Long getDetectDisconnectedWifi(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getLong(WIFI_DISCONNECTED_TIME, 0);
    }

}

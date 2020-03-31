package jp.co.digitalcruise.admint.player.pref;

import android.app.Activity;
import android.content.Context;

import jp.co.digitalcruise.admint.player.AdmintApplication;

public class NetWorkTestPref {
    private static final String PREF_NAME = "network_test";
    /**
     * このクラス内のプリファレンスのKey達
     */
    private static final String LATEST_CHECK_TIME = "latest_check_time";


    //setter
    public static void setLatestCheckTime(String val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putString(LATEST_CHECK_TIME, val).apply();
    }


    //getter
    public static String getLatestCheckTime(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getString(LATEST_CHECK_TIME, "");
    }

    public static void clearLatestCheckTime(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().clear().apply();
    }

}

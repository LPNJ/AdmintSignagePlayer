package jp.co.digitalcruise.admint.player.pref;

import android.app.Activity;
import android.content.Context;

import jp.co.digitalcruise.admint.player.AdmintApplication;

public class UpdaterPref {
    // プリファレンス（ファイル）名
    private static final String PREF_NAME = "updater";

    // 再起動アラーム時刻
    private static final String KEY_REBOOT_ALARM_TIME = "reboot_alarm_time";


    // 再起動アラーム時刻
    public static long getRebootAlarmTime() {
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getLong(KEY_REBOOT_ALARM_TIME, 0);
    }

    public static void setRebootAlarmTime(long val) {
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putLong(KEY_REBOOT_ALARM_TIME, val).apply();
    }

    public static void clearRebootAlarmTime(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().clear().apply();
    }
}


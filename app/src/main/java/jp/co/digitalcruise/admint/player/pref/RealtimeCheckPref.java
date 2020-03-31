package jp.co.digitalcruise.admint.player.pref;

import android.app.Activity;
import android.content.Context;

import jp.co.digitalcruise.admint.player.AdmintApplication;

public class RealtimeCheckPref {
    // プリファレンス（ファイル）名
    private static final String PREF_NAME = "realtime_check";

    private static final String KEY_RTC_FILE_MODIFIED = "rtc_file_modified";

    // リアルタイムチェックファイル更新日時
    public static long getRtcFileModified(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getLong(KEY_RTC_FILE_MODIFIED,0);
    }

    public static void setRtcFileModified(long val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putLong(KEY_RTC_FILE_MODIFIED, val).apply();
    }

    public static void clearRtcFileModified(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().clear().apply();
    }

}

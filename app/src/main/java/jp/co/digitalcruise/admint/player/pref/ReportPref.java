package jp.co.digitalcruise.admint.player.pref;

import android.app.Activity;
import android.content.Context;

import jp.co.digitalcruise.admint.player.AdmintApplication;

public class ReportPref {

    // プリファレンス（ファイル）名
    private static final String PREF_NAME = "report";

    private static final String KEY_TOUCH_LOG_LATEST_UPLOAD_TIME = "touch_log_latest_upload_time";
    private static final String KEY_PLAY_LOG_LATEST_UPLOAD_TIME = "play_log_latest_upload_time";

    // タッチログファイルアップロード成功日時
    public static long getTouchLogLatestUploadTime(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getLong(KEY_TOUCH_LOG_LATEST_UPLOAD_TIME,0);
    }

    public static void setTouchLogLatestUploadTime(long val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putLong(KEY_TOUCH_LOG_LATEST_UPLOAD_TIME, val).apply();
    }

    // 再生ログファイルアップロード成功日時
    public static long getPlayLogLatestUploadTime(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getLong(KEY_PLAY_LOG_LATEST_UPLOAD_TIME,0);
    }

    public static void setPlayLogLatestUploadTime(long val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putLong(KEY_PLAY_LOG_LATEST_UPLOAD_TIME, val).apply();
    }

    public static void clearLatestUploadTime(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().clear().apply();
    }

}

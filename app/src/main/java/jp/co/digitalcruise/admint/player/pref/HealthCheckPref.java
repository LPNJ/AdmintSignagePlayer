package jp.co.digitalcruise.admint.player.pref;

import android.app.Activity;
import android.content.Context;

import jp.co.digitalcruise.admint.player.AdmintApplication;

public class HealthCheckPref {

    // プリファレンス（ファイル）名
    private static final String PREF_NAME = "health_check";

    // 前回ヘルスチェック成功日時
    private static final String KEY_LATEST_HEALTH_CHECK_TIME = "latest_health_check_time";
    // 前回先行取得成功日時
    private static final String KEY_LATEST_AHEAD_LOAD_TIME = "latest_ahead_load_time";
    // 次回ヘルスチェック実行日時
    private static final String KEY_NEXT_HEALTH_CHECK_TIME = "next_health_check_time";
    // 次回先行取得実行日時
    private static final String KEY_NEXT_AHEAD_LOAD_TIME = "next_ahead_load_time";
    // ヘルスチェック連続失敗回数
    private static final String KEY_HEALTH_CHECK_FAILED_COUNT = "health_check_failed_count";
    // 先行取得連続失敗回数
    private static final String KEY_AHEAD_LOAD_FAILED_COUNT = "ahead_load_failed_count";
    // 先行取得時刻
    private static final String KEY_AHEAD_LOAD_TIME = "ahead_load_time";
    // 先行取得日数
    private static final String KEY_AHEAD_LOAD_DATE = "ahead_load_date";
    // ヘルスチェックインターバル(sec)
    private static final String KEY_HEALTH_CHECK_INTERVAL = "health_check_interval";
    // リアルタイムチェックインターバル(sec)
    private static final String KEY_REAL_TIME_CHECK_INTERVAL = "real_time_check_interval";
    // 再生ログアップロード（再生ログ取得）フラグ
    private static final String KEY_UPLOAD_PLAY_COUNT = "upload_play_count";
    // 再起動曜日
    private static final String KEY_REBOOT_WEEK_DAY = "reboot_week_day";
    // 再起動時刻
    private static final String KEY_REBOOT_WEEK_TIME = "reboot_week_time";
    // 直前のヘルスチェック成否
    private static final String KEY_IS_PREVIOUS_HEALTH_CHECK_SUCCESS = "is_previous_health_check_success";

    // 前回ヘルスチェック成功日時
    public static long getLatestHealthCheckTime(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getLong(KEY_LATEST_HEALTH_CHECK_TIME,0);
    }

    public static void setLatestHealthCheckTime(long val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putLong(KEY_LATEST_HEALTH_CHECK_TIME,val).apply();
    }

    // 前回先行取得成功日時
    public static long getLatestAheadLoadTime(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getLong(KEY_LATEST_AHEAD_LOAD_TIME,0);
    }

    public static void setLatestAheadLoadTime(long val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putLong(KEY_LATEST_AHEAD_LOAD_TIME,val).apply();
    }

    // 次回ヘルスチェック実行日時
    public static long getNextHealthCheckTime(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getLong(KEY_NEXT_HEALTH_CHECK_TIME,0);
    }

    public static void setNextHealthCheckTime(long val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putLong(KEY_NEXT_HEALTH_CHECK_TIME,val).apply();
    }

    // 次回先行取得実行日時
    public static long getNextAheadLoadTime(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getLong(KEY_NEXT_AHEAD_LOAD_TIME,0);
    }

    public static void setNextAheadLoadTime(long val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putLong(KEY_NEXT_AHEAD_LOAD_TIME,val).apply();
    }

    // ヘルスチェック連続失敗回数
    public static int getHealthCheckFailedCount(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getInt(KEY_HEALTH_CHECK_FAILED_COUNT,0);
    }

    public static void setHealthCheckFailedCount(int val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putInt(KEY_HEALTH_CHECK_FAILED_COUNT,val).apply();
    }

    // 先行取得連続失敗回数
    public static int getAheadLoadFailedCount(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getInt(KEY_AHEAD_LOAD_FAILED_COUNT,0);
    }

    public static void setAheadLoadFailedCount(int val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putInt(KEY_AHEAD_LOAD_FAILED_COUNT,val).apply();
    }

    // 先行取得時刻
    public static int getAheadLoadTime(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getInt(KEY_AHEAD_LOAD_TIME,0);
    }

    public static void setAheadLoadTime(int val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putInt(KEY_AHEAD_LOAD_TIME,val).apply();
    }

    // 先行取得日数
    public static int getAheadLoadDate(){
        final int DEFAULT_AHEAD_LOAD_DATE = 1;
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getInt(KEY_AHEAD_LOAD_DATE, DEFAULT_AHEAD_LOAD_DATE);
    }

    public static void setAheadLoadDate(int val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putInt(KEY_AHEAD_LOAD_DATE,val).apply();
    }

    // ヘルスチェックインターバル(sec)
    public static int getHealthCheckInterval(){
        final int DEFAULT_HEALTH_CHECK_INTERVAL = 1200;
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getInt(KEY_HEALTH_CHECK_INTERVAL, DEFAULT_HEALTH_CHECK_INTERVAL);
    }

    public static void setHealthCheckInterval(int val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putInt(KEY_HEALTH_CHECK_INTERVAL,val).apply();
    }

    // リアルタイムチェックインターバル(sec)
    public static int getRealTimeCheckInterval(){
        final int DEFAULT_HEALTH_CHECK_INTERVAL = 60;
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getInt(KEY_REAL_TIME_CHECK_INTERVAL, DEFAULT_HEALTH_CHECK_INTERVAL);
    }

    public static void setRealTimeCheckInterval(int val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putInt(KEY_REAL_TIME_CHECK_INTERVAL,val).apply();
    }

    // 再生ログアップロード（再生ログ取得）フラグ
    public static int getUploadPlayCount(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getInt(KEY_UPLOAD_PLAY_COUNT, 0);
    }

    public static void setUploadPlayCount(int val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putInt(KEY_UPLOAD_PLAY_COUNT,val).apply();
    }

    // 再起動曜日
    public static int getRebootWeekDay(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getInt(KEY_REBOOT_WEEK_DAY, 0);
    }

    public static void setRebootWeekDay(int val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putInt(KEY_REBOOT_WEEK_DAY,val).apply();
    }

    // 再起動時刻
    public static int getRebootWeekTime(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getInt(KEY_REBOOT_WEEK_TIME, 0);
    }

    public static void setRebootWeekTime(int val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putInt(KEY_REBOOT_WEEK_TIME,val).apply();
    }

    // 直近のヘルスチェック成否
    public static boolean getIsPreviousHealthCheckSuccess(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getBoolean(KEY_IS_PREVIOUS_HEALTH_CHECK_SUCCESS, false);
    }

    // 直近のヘルスチェック成否
    public static void setIsPreviousHealthCheckSuccess(boolean val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putBoolean(KEY_IS_PREVIOUS_HEALTH_CHECK_SUCCESS,val).apply();
    }

    public static void clearHealthCheckPrefs(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().clear().apply();
    }
}

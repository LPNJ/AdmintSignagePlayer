package jp.co.digitalcruise.admint.player.pref;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

import jp.co.digitalcruise.admint.player.AdmintApplication;

public class ServerUrlPref {

    // プリファレンス（ファイル）名
    private static final String PREF_NAME = "server_url";

    // 配信サーバ
    private static final String KEY_DELIVERY_SERVER_URL = "delivery_server_url";
    // ファイルアップロードサーバ
    private static final String KEY_UPLOAD_SERVER_URL = "upload_server_url";
    // リアルタイムチェックサーバ
    private static final String KEY_RTC_SERVER_URL = "rtc_server_url";
    // デイリーコンテンツサーバ
    private static final String KEY_EXTERNAL_SERVER_URL = "external_server_url";
    // CDNサーバ
    private static final String KEY_CDN_SERVER_URL = "cdn_server_url";

    private static final String KEY_RSC_SERVER_URL = "rsc_server_url";

    // 配信サーバ
    public static @NonNull String getDeliveryServerUrl(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getString(KEY_DELIVERY_SERVER_URL,"");
    }

    public static void setDeliveryServerUrl(String val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putString(KEY_DELIVERY_SERVER_URL,val).apply();
    }

    // ファイルアップロードサーバ
    public static @NonNull String getUploadServerUrl(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getString(KEY_UPLOAD_SERVER_URL,"");
    }

    public static void setUploadServerUrl(String val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putString(KEY_UPLOAD_SERVER_URL,val).apply();
    }

    // リアルタイムチェックサーバ
    public static @NonNull String getRtcServerUrl(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getString(KEY_RTC_SERVER_URL,"");
    }

    public static void setRtcServerUrl(String val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putString(KEY_RTC_SERVER_URL,val).apply();
    }

    // デイリーコンテンツサーバ
    public static @NonNull String getExternalServerUrl(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getString(KEY_EXTERNAL_SERVER_URL,"");
    }

    public static void setExternalServerUrl(String val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putString(KEY_EXTERNAL_SERVER_URL,val).apply();
    }

    // CDNサーバ
    public static @NonNull String getCdnServerUrl(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getString(KEY_CDN_SERVER_URL,"");
    }

    public static void setCdnServerUrl(String val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putString(KEY_CDN_SERVER_URL,val).apply();
    }

    //レスキューサーバー
    public static @NonNull String getRscServerUrl(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getString(KEY_RSC_SERVER_URL,"");
    }

    public static void setRscServerUrl(String val ){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putString(KEY_RSC_SERVER_URL, val).apply();
    }

    public static void clearServerUrlPrefs(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().clear().apply();
    }
}

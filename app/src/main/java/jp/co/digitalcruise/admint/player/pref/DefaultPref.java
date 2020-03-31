package jp.co.digitalcruise.admint.player.pref;


import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import jp.co.digitalcruise.admint.player.AdmintApplication;
import jp.co.digitalcruise.admint.player.R;

public class DefaultPref {

    private static final String DEFAULT_PROXY_PORT = "0";

    //System(設定)
    public static Boolean getUseExtraStorage(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        boolean default_value = context.getResources().getBoolean(R.bool.default_use_extra_storage);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_use_extra_storage), default_value);
    }

    public static @NonNull String getSdcardDrive(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.setting_key_sdcard_drive), "");
    }
    public static void setSdcardDrive(String val){
        if(val != null && val.length() > 0) {
            Context context = AdmintApplication.getInstance().getApplicationContext();
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(context.getString(R.string.setting_key_sdcard_drive), val).apply();
        }
    }

    public static Boolean getWiFiReset(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        boolean default_value = context.getResources().getBoolean(R.bool.default_wifi_reset);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_wifi_reset), default_value);
    }

    public static void setWiFiReset(boolean val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(R.string.setting_key_wifi_reset), val).apply();
    }

    public static Boolean getResetNetwork(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        boolean default_value = context.getResources().getBoolean(R.bool.default_reset_network);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_reset_network), default_value);
    }

//    public static Boolean getEthernetReset(){
//        Context context = AdmintApplication.getInstance().getApplicationContext();
//        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_ethnet_reset), false);
//    }
//
//    public static Boolean get3gReset(){
//        Context context = AdmintApplication.getInstance().getApplicationContext();
//        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_3g_reset), true);
//    }


    public static Boolean getCheckAnrReboot(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        boolean default_value = context.getResources().getBoolean(R.bool.default_check_anr_reboot);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_check_anr_reboot), default_value);
    }

    //Proxy(プロキシ)
    public static Boolean getProxyEnable(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        boolean default_value = context.getResources().getBoolean(R.bool.default_proxy_enable);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_proxy_enable),default_value);
    }

    public static void setProxyEnable(boolean val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(R.string.setting_key_proxy_enable), val).apply();
    }

    public static @NonNull String getProxyHost(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.setting_key_proxy_host),"");
    }

    public static String getProxyPort(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.setting_key_proxy_port), DEFAULT_PROXY_PORT);
    }

    public static @NonNull String getProxyUser(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.setting_key_proxy_user), "");
    }

    public static @NonNull String getProxyPassword(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.setting_key_proxy_password), "");
    }

    public static Boolean getProxyWebcontent(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        boolean default_value = context.getResources().getBoolean(R.bool.default_proxy_webcontent);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_proxy_webcontent), default_value);
    }

    //LOG(ログ)
    public static Boolean getNoticeLog(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        boolean default_value = context.getResources().getBoolean(R.bool.default_notice_log);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_notice_log), default_value);
    }


    //LOG(ログ)
    public static Boolean getFastVideoChange(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        boolean default_value = context.getResources().getBoolean(R.bool.default_fast_video_change);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_fast_video_change), default_value);
    }


    //Develop(開発用)
    public static @NonNull String getManagerUrl(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
//        return "https://request.admintstg.jp/";
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.setting_key_manage_server_url), "https://request.admintstg.jp/");
    }

    public static void setManagerUrl(String val){
        if(val != null && val.length() > 0) {
            Context context = AdmintApplication.getInstance().getApplicationContext();
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(context.getString(R.string.setting_key_manage_server_url), val).apply();
        }
    }

    public static @NonNull String getSiteId(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.setting_key_site_id), "devt21");
    }

    public static void setSiteId(String val) {
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(context.getString(R.string.setting_key_site_id), val).apply();
    }

    public static @NonNull String getAkey(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.setting_key_akey), "SH21TEST032");
    }

    public static void setAkey(String val) {
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(context.getString(R.string.setting_key_akey), val).apply();
    }


    public static @NonNull String getUserStorage(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.setting_key_user_storage), "");
    }

    public static void setUserStorage(String val) {
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(context.getString(R.string.setting_key_user_storage), val).apply();
    }

//    public static String tabletModel(){
//        Context context = AdmintApplication.getInstance().getApplicationContext();
//        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.setting_key_tablet_model), "");
//    }

    //ネットサービス
    public static void setNetworkService(boolean val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(R.string.setting_key_net_service), val).apply();
    }

    public static Boolean getNetworkService(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        boolean default_value = context.getResources().getBoolean(R.bool.default_net_service);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_net_service),default_value);
    }


    public static @NonNull String getTerminalId(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.setting_key_terminal_id),"");
    }

    public static void setTerminalId(String val) {
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(context.getString(R.string.setting_key_terminal_id), val).apply();
    }


    public static Boolean getBootStart(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        boolean default_value = context.getResources().getBoolean(R.bool.default_boot_start);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_boot_start), default_value);
    }

    public static void setBootStart(Boolean val) {
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(R.string.setting_key_boot_start), val).apply();
    }

    public static Boolean getDebugMode(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        boolean default_value = context.getResources().getBoolean(R.bool.default_debug_mode);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_debug_mode), default_value);
    }

    public static @NonNull String getOldDeliveryServerUrl(){
        String PREF_OLD_DELIVERY_SERVER_URL_KEY = "delivery_server_url";
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_OLD_DELIVERY_SERVER_URL_KEY,"");
    }

    public static Boolean getLimitlessContentMode(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        boolean default_value = context.getResources().getBoolean(R.bool.default_limitless_content_mode);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_limitless_content_mode), default_value);
    }

    public static void setLimitlessContentMode(Boolean val) {
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(R.string.setting_key_limitless_content_mode), val).apply();
    }


    //キッティングフラグ
    public static void setRegisteredTerminal(Boolean val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(R.string.setting_key_registered_terminal), val).apply();
    }

    public static Boolean getRegisteredTerminal(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_registered_terminal), false);
    }

    //キッティング実行日
    public static void setRegisteredDay(String str){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(context.getString(R.string.setting_key_registered_activation_day), str).apply();
    }

    public static String getRegisteredDay(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.setting_key_registered_activation_day), null);
    }

    //端末管理番号
    public static void setStbId(String str){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(context.getString(R.string.setting_key_registered_stb_id), str).apply();
    }

    public static String getStbId(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.setting_key_registered_stb_id), null);
    }

    //初回起動時Dialog出現フラグ
    public static void setNotShowDialog(boolean val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(R.string.setting_key_registered_show_dialog), val).apply();
    }

    public static boolean getNotShowDialog(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return  PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_registered_show_dialog), false);
    }

    //機番(S/N)
    public static void setExtraId(String extraId){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(context.getString(R.string.setting_key_registered_extra_id), extraId).apply();
    }

    public static String getExtraId(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getString(R.string.setting_key_registered_extra_id), "");
    }

    public static void setStandAloneMode(boolean val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(R.string.setting_key_stand_alone_mode), val).apply();
    }

    public static boolean getStandAloneMode(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        boolean default_value = context.getResources().getBoolean(R.bool.default_stand_alone_mode);
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_stand_alone_mode), default_value);
    }

    public static void setHaveSetBeforeStandAloneMode(boolean val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(context.getString(R.string.setting_key_have_set_stand_alone), val).apply();
    }
    public static boolean getHaveSetBeforeStandAloneMode(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.setting_key_have_set_stand_alone), false);
    }

    //初期化
    public static void clearDefaultPrefs(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply();
    }
}

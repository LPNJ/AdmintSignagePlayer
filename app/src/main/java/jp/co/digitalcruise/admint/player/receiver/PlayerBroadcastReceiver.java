package jp.co.digitalcruise.admint.player.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;

import jp.co.digitalcruise.admint.player.AdmintApplication;
import jp.co.digitalcruise.admint.player.PlayerActivity;
import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.define.DeviceDef;
import jp.co.digitalcruise.admint.player.component.define.RegisterIntentDef;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.log.NetLog;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.pref.GroovaProxyPref;
import jp.co.digitalcruise.admint.player.pref.HealthCheckPref;
import jp.co.digitalcruise.admint.player.pref.ServerUrlPref;
import jp.co.digitalcruise.admint.player.service.RecoverService;
import jp.co.digitalcruise.admint.player.service.StandAloneContent;
import jp.co.digitalcruise.admint.player.service.network.NetLogService;

import static android.content.Context.WIFI_SERVICE;

public class PlayerBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if(context == null || intent == null || intent.getAction() == null){
            return;
        }

        try {
            String action = intent.getAction();
            String DEVICE_BOOT_START = "jp.co.digitalcruise.admint.player.PlayerBroadcastReceiver.BOOT_START";
            if (Intent.ACTION_BOOT_COMPLETED.equals(action) || action.equals(DEVICE_BOOT_START)) {
                if(!DeviceDef.isGroova()){
                    if(DefaultPref.getBootStart()) {
                        AdmintApplication.MEMORY_BOOT_TIME = System.currentTimeMillis();
                        Logging.info(context.getString(R.string.log_info_boot_completed));
                        // プレイヤー起動
                        Intent activity_intent = new Intent(context, PlayerActivity.class);
                        activity_intent.putExtra(PlayerActivity.INTENT_EXTRA_ON_CREATE_TYPE, PlayerActivity.INTENT_EXTRA_ON_CREATE_TYPE_BOOT_COMPLETE);
                        activity_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(activity_intent);
                    }
                }
            } else if (Intent.ACTION_TIMEZONE_CHANGED.equals(action) || Intent.ACTION_TIME_CHANGED.equals(action)) {
                Logging.info(context.getString(R.string.log_info_time_changed_or_timezone_changed));
//                // アラーム更新（起動時の処理）
//                intentHealthCheckPlayerLaunch(context);
//                // アラーム更新（起動時の処理）
//                intentRecoverPlayerLaunch(context);

                // 起動時処理（アラーム設定）
                if(AdmintApplication.getInstance().getViewerStatus() == AdmintApplication.VIEWER_STATUS_FOREGROUND){
                    AdmintApplication.launchApplication();
                }
            } else if (RegisterIntentDef.ACTION_REGIST.equals(action)) {
                actionRegist(context, intent);
            } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
                actionMyPackageReplaced(context);
            } else if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)){
                if(Build.MODEL.equals(DeviceDef.SHUTTLE)){
                    long check_boot_time = AdmintApplication.MEMORY_BOOT_TIME - System.currentTimeMillis();
                    if(check_boot_time > 300000){
                        resetWifi(intent);
                    }
                }
            } else if(action.equals(Intent.ACTION_MEDIA_MOUNTED)){
                if(DefaultPref.getStandAloneMode() && DefaultPref.getRegisteredTerminal()){
                    Intent standalonecontent = new Intent(context, StandAloneContent.class);
                    standalonecontent.setAction(StandAloneContent.ACTION_CP_CONTENT_DIR);
                    context.startService(standalonecontent);
                }
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionMyPackageReplaced(Context context){

        try {

            // バージョンアップ時
            // パッケージバージョン取得
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            String version = packageInfo.versionName;
            int code = packageInfo.versionCode;

            // ロギング
            String log_suffix = " (version = " + version + ", code = " + code + ")";

            // ネットロギング
            String msg = context.getString(R.string.net_logging_notice_my_pacage_replaced) + log_suffix;
            netLogVersionUp(context, msg);

            Logging.info(context.getString(R.string.log_info_package_changed) + log_suffix);

            if (!DeviceDef.isGroova()) {
                SharedPreferences def_prefs = PreferenceManager.getDefaultSharedPreferences(context);
                boolean boot_start = def_prefs.getBoolean(context.getString(R.string.setting_key_boot_start), true);
                // 端末起動時にプレイヤーを起動する設定の時は、プレイヤー再起動
                // stickはこの値がfalseなので、更新でプレイヤーの起動は行わない（ホームアプリがやってくれる）
                if (boot_start) {
                    // プレイヤーを起動する（これがないとすぐに起動しない）
                    Intent activity_intent = new Intent(context, PlayerActivity.class);
                    activity_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    activity_intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(activity_intent);
                }
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionRegist(Context context, Intent intent){
        try{

            if(intent.hasExtra(RegisterIntentDef.EXTRA_TERMINAL_ID)){
                String terminal_id = intent.getStringExtra(RegisterIntentDef.EXTRA_TERMINAL_ID);
                DefaultPref.setTerminalId(terminal_id);
            }

            if(intent.hasExtra(RegisterIntentDef.EXTRA_MANAGE_SERVER_URL)){
                String manage_server_url = intent.getStringExtra(RegisterIntentDef.EXTRA_MANAGE_SERVER_URL);
                DefaultPref.setManagerUrl(manage_server_url);
            }

            if(intent.hasExtra(RegisterIntentDef.EXTRA_SITE_ID)){
                String site_id = intent.getStringExtra(RegisterIntentDef.EXTRA_SITE_ID);
                DefaultPref.setSiteId(site_id);
            }

            if(intent.hasExtra(RegisterIntentDef.EXTRA_AKEY)){
                String akey = intent.getStringExtra(RegisterIntentDef.EXTRA_AKEY);
                DefaultPref.setAkey(akey);
            }

            if(intent.hasExtra(RegisterIntentDef.EXTRA_SDCARD_DRIVE)){
                String sdcard_drive = intent.getStringExtra(RegisterIntentDef.EXTRA_SDCARD_DRIVE);
                DefaultPref.setSdcardDrive(sdcard_drive);
            }else{
                DefaultPref.setSdcardDrive("");
            }

//            if(intent.hasExtra(RegisterIntentDef.EXTRA_DELIVERY_SERVER_URL)){
//                String delivery_server_url = intent.getStringExtra(RegisterIntentDef.EXTRA_DELIVERY_SERVER_URL);
//                ServerUrlPref.setDeliveryServerUrl(delivery_server_url);
//            }

            if(intent.hasExtra(RegisterIntentDef.EXTRA_USER_STORAGE)){
                String user_storage = intent.getStringExtra(RegisterIntentDef.EXTRA_USER_STORAGE);
                DefaultPref.setUserStorage(user_storage);
            }else{
                DefaultPref.setUserStorage("");
            }

            if(intent.hasExtra(RegisterIntentDef.EXTRA_WIFI_REBOOT)){
                boolean wifi_reboot = intent.getBooleanExtra(RegisterIntentDef.EXTRA_WIFI_REBOOT, false);
                DefaultPref.setWiFiReset(wifi_reboot);
            }

//            if(intent.hasExtra(RegisterIntentDef.EXTRA_TABLET_MODEL)){
//                String tablet_model = intent.getStringExtra(RegisterIntentDef.EXTRA_TABLET_MODEL);
//                editor.putString(context.getString(R.string.setting_key_tablet_model), tablet_model);
//            }else{
//                editor.remove(context.getString(R.string.setting_key_tablet_model));
//            }

            if(!DeviceDef.isGroova()) {
                if(intent.hasExtra(RegisterIntentDef.EXTRA_BOOT_START)){
                    Boolean boot_start = intent.getBooleanExtra(RegisterIntentDef.EXTRA_BOOT_START, true);
                    DefaultPref.setBootStart(boot_start);
                }
            }

//            if(intent.hasExtra(RegisterIntentDef.EXTRA_PROXY_ENABLE)){
//                Boolean proxy_enable = intent.getBooleanExtra(RegisterIntentDef.EXTRA_PROXY_ENABLE, false);
//                editor.putBoolean(context.getString(R.string.setting_key_proxy_enable), proxy_enable);
//            }

            // 先行取得時刻を0:00～8:00の間でランダム設定
            HealthCheckPref.setAheadLoadTime((int)Math.floor(Math.random() * (60 * 60 * 8)));

            // ネットロギング
            NetLog.notice(context.getString(R.string.net_logging_notice_complated_regist));

        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void netLogVersionUp(Context context, String msg){
        if(DeviceDef.isGroova() && !GroovaProxyPref.getCheckGroovaProxy()){
            //3.2へアップデート直後はプリファレンスに値が無く、エラーが出てしまうので数秒後に実行させる
            Intent toto_intent = new Intent(context, NetLogService.class);
            toto_intent.setAction(NetLogService.ACTION_LOG_NOTICE);
            toto_intent.putExtra(NetLogService.INTENT_LOG_MESSAGE, msg);
            AlarmManager alm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, toto_intent, PendingIntent.FLAG_ONE_SHOT);
            assert alm != null;
            alm.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + 5000,  AlarmManager.INTERVAL_DAY, pendingIntent);
            return;
        }
        // preference移行前で値が取得できないため、旧値のURLで通知
        if(ServerUrlPref.getDeliveryServerUrl().length() == 0){
            Intent intent = new Intent(context, NetLogService.class);
            intent.setAction(NetLogService.ACTION_LOG_OLD_DELIVERY_SERVER);
            intent.putExtra(NetLogService.INTENT_LOG_MESSAGE, msg);
            context.startService(intent);
        }else{
            NetLog.notice(msg);
        }
    }

    private void resetWifi(Intent intent){
        Context appContext = AdmintApplication.getInstance().getApplicationContext();
        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        WifiManager manager = (WifiManager) appContext.getSystemService(WIFI_SERVICE);
        if (manager != null && manager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            if(info.getState() == NetworkInfo.State.DISCONNECTED){
                Intent recoverIntent = new Intent(appContext, RecoverService.class);
                recoverIntent.setAction(RecoverService.ACTION_WIFI_DISCONNECT_DETECT);
                AdmintApplication.getInstance().getApplicationContext().startService(recoverIntent);
            }
        }
    }
}

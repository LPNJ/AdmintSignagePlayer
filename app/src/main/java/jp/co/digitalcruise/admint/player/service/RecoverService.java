package jp.co.digitalcruise.admint.player.service;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Method;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.define.DeviceDef;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.log.NetLog;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.pref.RecoverPref;
import jp.co.digitalcruise.admint.player.pref.RegisterPref;

import static jp.co.digitalcruise.admint.player.BuildConfig.APPLICATION_ID;

public class RecoverService extends AbstractService {
    private static final String ACTION_PREFIX = APPLICATION_ID + ".RecoverService.";

    // ヘルスチェック連続失敗
    public static final String ACTION_HEALTH_CHECK_FAILED = ACTION_PREFIX + "HEALTH_CHECK_FAILED";
    // ANR検知
    public static final String ACTION_CHECK_ANR = ACTION_PREFIX + "CHECK_ANR";
    // ANR検知アラーム設定
    public static final String ACTION_SET_CHECK_ANR_ALARM = ACTION_PREFIX + "SET_CHECK_ANR_ALARM";
    // Viewer起動チェック
    public static final String ACTION_CHECK_VIEWER = ACTION_PREFIX + "CHECK_VIEWER";
    // Player起動時処理
    public static final String ACTION_PLAYER_LAUNCH = ACTION_PREFIX + "PLAYER_LAUNCH";
    //WifiDisconnect検知
    public static final String ACTION_WIFI_DISCONNECT_DETECT = ACTION_PREFIX + "WIFI_DISCONNECT";

    public RecoverService() {
        super(RecoverService.class.getName());
    }

    public RecoverService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if(intent == null || intent.getAction() == null){
            return;
        }

        try{
            final String action = intent.getAction();

            Logging.notice(action);

            if(ACTION_HEALTH_CHECK_FAILED.equals(action)){
                actionHealthCheckFailed();
            }else if(ACTION_CHECK_ANR.equals(action)){
                actionCheckAnr();
            }else if(ACTION_SET_CHECK_ANR_ALARM.equals(action)){
                actionSetCheckAnrAlarm();
            }else if(ACTION_CHECK_VIEWER.equals(action)){
                actionCheckViewer();
            }else if(ACTION_PLAYER_LAUNCH.equals(action)){
                actionPlayerLaunch();
            }else if(ACTION_WIFI_DISCONNECT_DETECT.equals(action)){
                actionResetWifiConnect();
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionSetCheckAnrAlarm(){
        final long CHECK_ANR_INTERVAL_TIME = 300000L;
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if(am != null){
            Intent makeIntent = new Intent(this, RecoverService.class);
            makeIntent.setAction(ACTION_CHECK_ANR);
            PendingIntent pintent = PendingIntent.getService(this, 0, makeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            if(DefaultPref.getCheckAnrReboot()) {
                File traces_file = getAnrTracesFile();
                if(traces_file.isFile()) {
                    RecoverPref.setAnrFileModified(traces_file.lastModified());
                }
//                am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + CHECK_ANR_INTERVAL_TIME, CHECK_ANR_INTERVAL_TIME, pintent);
                am.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + CHECK_ANR_INTERVAL_TIME, CHECK_ANR_INTERVAL_TIME, pintent);
            }else{
                am.cancel(pintent);
            }
        }else{
            throw new RuntimeException("(AlarmManager)getSystemService return null");
        }
    }

    private void setCheckViewerAlarm(){
        final long CHECK_VIEWER_INTERVAL_TIME = 600000L;

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if(am != null){
            Intent makeIntent = new Intent(this, RecoverService.class);
            makeIntent.setAction(ACTION_CHECK_VIEWER);
            PendingIntent pintent = PendingIntent.getService(this, 0, makeIntent, PendingIntent.FLAG_CANCEL_CURRENT);
//            am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + CHECK_VIEWER_INTERVAL_TIME, CHECK_VIEWER_INTERVAL_TIME, pintent);
            am.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + CHECK_VIEWER_INTERVAL_TIME, CHECK_VIEWER_INTERVAL_TIME, pintent);
        }else{
            throw new RuntimeException("(AlarmManager)getSystemService return null");
        }
    }

    private void actionPlayerLaunch(){
        try {
            // ANR検知
            actionSetCheckAnrAlarm();

            // Viewer監視
            setCheckViewerAlarm();
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionCheckViewer(){
        try {
            if (!isViewerForegraund()) {
                Logging.info(getString(R.string.log_info_detect_no_launch_viewer));
                intentPlaylistLaunchPlayer();
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void intentPlaylistLaunchPlayer(){
        Intent intent = new Intent(this, PlaylistService.class);
        intent.setAction(PlaylistService.ACTION_PLAYER_LAUNCH);
        startService(intent);
    }

    private void resetEthernetService() {
//        final int ETHERNET_STATE_ENABLED = 2;
        try {
            @SuppressLint("WrongConstant") Object eth = getSystemService("ethernet");
            if(eth == null){
                throw new RuntimeException("getSystemService(ethernet) return null");
            }

            Method method = eth.getClass().getMethod("setEthEnabled", boolean.class);
            if(method == null){
                throw new RuntimeException("EthernetManager.getClass.getMethod(setEthEnabled) return null");
            }

            method.invoke(eth, false);
            method.invoke(eth, true);

            Logging.info(getString(R.string.log_info_reset_ethernet));
            NetLog.notice(getString(R.string.net_logging_notice_reset_ethernet));
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void resetWifiService() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if(wm == null){
                throw new RuntimeException("Context.getSystemService(WIFI_SERVICE) return null");
            }

            wm.setWifiEnabled(false);
            wm.setWifiEnabled(true);

            Logging.info(getString(R.string.log_info_reset_wifi));
            NetLog.notice(getString(R.string.net_logging_notice_reset_wifi));
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private boolean checkSknetMonopoleResetMobile(){
        boolean ret = false;

        boolean use_othre_network = false;
        boolean exist_sim_module = false;

        // 現在有効なネットワークが存在しない（WiFiでもなくEthでもなくついでにWiMaxでもない）
        try{
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if(cm != null){
                NetworkInfo net_info = cm.getActiveNetworkInfo();
                if(net_info != null){
                    switch (net_info.getType()){
                        case ConnectivityManager.TYPE_WIFI:
                        case ConnectivityManager.TYPE_WIMAX:
                        case ConnectivityManager.TYPE_ETHERNET:
                            use_othre_network = true;
                            break;
                        default:
                            use_othre_network = false;
                            break;
                    }
                }
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }

        try{
            TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
            if(tm != null){
                @SuppressLint({"MissingPermission", "HardwareIds"}) long imei = Long.parseLong(tm.getDeviceId());
                if(imei > 0){
                    exist_sim_module = true;
                }
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }

        // 現在有効なネットワークがWifi等ではなく、SIMモジュールが存在し、Sknet-Monopoleシリーズであるならリセット
        if(!use_othre_network && exist_sim_module){
            ret = true;
        }
        return ret;
    }


    private void rset3gModule(){
        final String SKNET_MOBILE_SETTING_DIR_PATH = "/sys/class/sw_3g_module/modem/";
        final String SKNET_MOBILE_SETTING_FILE_NAME = "modem_power";

        File setting_dir = new File(SKNET_MOBILE_SETTING_DIR_PATH);
        // 3Gセッティングディレクトリが存在しない
        if(!setting_dir.isDirectory()){
            return;
        }

        try{
            FileWriter fw = new FileWriter(new File(SKNET_MOBILE_SETTING_DIR_PATH + SKNET_MOBILE_SETTING_FILE_NAME));
            fw.write('0');
            fw.close();
        }catch(Exception e){
            Logging.stackTrace(e);
            return;
        }

        try{
            FileWriter fw = new FileWriter(new File(SKNET_MOBILE_SETTING_DIR_PATH + SKNET_MOBILE_SETTING_FILE_NAME));
            fw.write('1');
            fw.close();
        }catch(Exception e){
            Logging.stackTrace(e);
            return;
        }

        Logging.info(getString(R.string.log_info_reset_3g));
        NetLog.notice(getString(R.string.net_logging_notice_reset_3g));
    }

    private void actionResetWifiConnect(){
        Long latest_time = RegisterPref.getDetectDisconnectedWifi();
        Long now_time = System.currentTimeMillis();
        if((now_time - latest_time) >= 600000){
            RegisterPref.setDetectDisconnectedWifi(now_time);
            Logging.info(getString(R.string.log_info_detect_disconnect_wifi));
            resetWifiService();
        }
    }

    private void actionHealthCheckFailed() {
        try {
            // GroovaBoxの時
            if (Build.MODEL.equals(DeviceDef.GROOVA_BOX)) {
                // ネットワーク再起動チェックがON
                if (DefaultPref.getResetNetwork()) {
                    // WiFiとEthを自動判別して再起動
                    resetNetworkServiceGroovaBox();
                }
            } else {
                // WiFi再起動チェックがON
                if (DefaultPref.getWiFiReset()) {
                    resetWifiService();
                }
            }

            // SK端末の時は3Gも再起動対象
            if (Build.MODEL.equals(DeviceDef.SK_BOX) || Build.MODEL.equals(DeviceDef.SK_POP)) {
                // 現在のネットワーク接続状況およびSimの存在チェック
                if (checkSknetMonopoleResetMobile()) {
                    rset3gModule();
                }
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void resetNetworkServiceGroovaBox(){
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm == null) {
                throw new RuntimeException("Context.getSystemService(WIFI_SERVICE) return null");
            }

            // wifi設定がON(排他的な設定なのでこの場合はEth設定がOFF)
            if (wm.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                // WiFiサービス再起動
                resetWifiService();
            } else {
                // Ethサービス再起動
                resetEthernetService();
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionCheckAnr(){
        if(DefaultPref.getCheckAnrReboot()){

            File traces_file = getAnrTracesFile();

            // アラーム設定から後traces.txtが更新されている
            if(detectAnr(traces_file)){

                // 基準とするModifiedを更新
                RecoverPref.setAnrFileModified(traces_file.lastModified());

                // パッケージ名がtraces.txtに含まれている
                if(existPlayerInTraces(traces_file)){
                    Logging.error(getString(R.string.log_error_detect_anr));
                    // 端末再起動
                    intentUpdaterReboot();
                }
            }
        }
    }

    private void intentUpdaterReboot(){
        Intent intent = new Intent(this, UpdaterService.class);
        intent.setAction(UpdaterService.ACTION_REBOOT);
        startService(intent);
    }

    private boolean detectAnr(@NonNull File traces_file){
        // traces.txtが存在する
        if(traces_file.isFile()) {
            // アプリ起動時に取得したmodifiedの値と比較し更新されている
            if (traces_file.lastModified() > RecoverPref.getAnrFileModified()) {
                return true;
            }
        }
        return false;
    }

    private boolean existPlayerInTraces(@NonNull File traces_file){
        // traces.txtが存在する
        if(traces_file.isFile()) {
            try (BufferedReader in = new BufferedReader(new FileReader(traces_file))) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.contains(APPLICATION_ID)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                Logging.stackTrace(e);
            }
        }
        return false;
    }
 }

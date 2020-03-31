package jp.co.digitalcruise.admint.player.service.network;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

import jp.co.digitalcruise.admint.player.AdmintApplication;
import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.CompatibleSdk;
import jp.co.digitalcruise.admint.player.component.date.DateUtil;
import jp.co.digitalcruise.admint.player.component.file.AdmintPath;
import jp.co.digitalcruise.admint.player.component.file.FileUtil;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.log.NetLog;
import jp.co.digitalcruise.admint.player.component.parsexml.object.ScheduleHeaderObject;
import jp.co.digitalcruise.admint.player.component.parsexml.object.ScheduleObject;
import jp.co.digitalcruise.admint.player.component.parsexml.parser.ScheduleHeaderParser;
import jp.co.digitalcruise.admint.player.component.parsexml.parser.ScheduleParser;
import jp.co.digitalcruise.admint.player.component.rescue.GetRescueDbData;
import jp.co.digitalcruise.admint.player.db.ErrorDbHelper;
import jp.co.digitalcruise.admint.player.db.ScheduleDbHelper;
import jp.co.digitalcruise.admint.player.db.ScheduleDbHelper.TABLE_SCHEDULE_INFO;
import jp.co.digitalcruise.admint.player.db.ViewlistDbHelper;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.pref.HealthCheckPref;
import jp.co.digitalcruise.admint.player.pref.ServerUrlPref;
import jp.co.digitalcruise.admint.player.service.PlaylistService;
import jp.co.digitalcruise.admint.player.service.RecoverService;
import jp.co.digitalcruise.admint.player.service.UpdaterService;

import static jp.co.digitalcruise.admint.player.BuildConfig.APPLICATION_ID;

public class HealthCheckService extends AbstractNetworkService {
    private static final String ACTION_PREFIX = APPLICATION_ID + ".HealthCheckService.";

    // ヘルスチェック
    public static final String ACTION_HEALTH_CHECK = ACTION_PREFIX + "HEALTH_CHECK";
    //リアルタイムチェック
    public static final String ACTION_RTC_HEALTH_CHECK = ACTION_PREFIX + "RTC_HEALTH_CHECK";
    // 先行取得
    public static final String ACTION_AHEAD_LOAD = ACTION_PREFIX + "AHEAD_LOAD";
    // 端末起動時
    public static final String ACTION_PLAYER_LAUNCH = ACTION_PREFIX + "PLAYER_LAUNCH";
    // 拡張ストレージからの読み込み
    public static final String ACTION_LOAD_EXTERNAL_STORAGE = ACTION_PREFIX + "LOAD_EXTERNAL_STORAGE";
    //スタンドアロン
    public static final String ACTION_LOAD_STAND_ALONE_SCHDULE = ACTION_PREFIX + "LOAD_STAND_ALONE_SCHDULE ";
    // スケジュール初期化
    public static final String ACTION_CLEAR_SCHEDULE = ACTION_PREFIX + "CLEAR_SCHEDULE";

    public static final String API_POPKEEPER_SCHEDULE = API_POPKEEPER  + "schedule" + PARAM_KEY;

    public static final int SCHED_TYPE_HEALTH_CHECK = 0;
    public static final int SCHED_TYPE_AHEAD_LOAD = 1;
    public static final int SCHED_TYPE_SD_CARD = 2;
    public static final int SCHED_TYPE_DEFAULT = 3;


    private static final int SCHEDULE_API_TYPE_HEALTH_CHECK = 0;
    private static final int SCHEDULE_API_TYPE_AHEAD_LOAD = 1;
    private static final int SCHEDULE_API_TYPE_HEALTH_CHECK_RTC = 2;

    public static final int CONTENT_TYPE_MOVIE = 0;
    public static final int CONTENT_TYPE_TOUCH = 2;
    public static final int CONTENT_TYPE_PICTURE = 3;
    public static final int CONTENT_TYPE_WEBVIEW = 7;
    public static final int CONTENT_TYPE_ORIGINAL = 8;
    public static final int CONTENT_TYPE_EXTERNAL_MOVIE = 50;
    public static final int CONTENT_TYPE_EXTERNAL_PICTURE = 53;
    public static final int CONTENT_TYPE_THEATA = 63;


    public HealthCheckService() {
        super(HealthCheckService.class.getName());
    }

    public HealthCheckService(String name) {
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

            if(ACTION_HEALTH_CHECK.equals(action)) {
                actionHealthCheck(false);
            }else if(ACTION_RTC_HEALTH_CHECK.equals(action)){
                actionHealthCheck(true);
            }else if(ACTION_AHEAD_LOAD.equals(action)){
                actionAheadLoad();
            }else if(ACTION_LOAD_EXTERNAL_STORAGE.equals(action)){
                actionLoadExternalStorage();
            }else if(ACTION_PLAYER_LAUNCH.equals(action)){
                actionPlayerLaunch();
            }else if(ACTION_CLEAR_SCHEDULE.equals(action)){
                actionClearSchedule();
            } else if(ACTION_LOAD_STAND_ALONE_SCHDULE.equals(action)){
                actionLoadStandAloneSchedule();
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void clearErrorDb() {
        ErrorDbHelper dbh = null;

        try {
            dbh = new ErrorDbHelper(this);
            SQLiteDatabase wdb = dbh.getWriterDb();
            wdb.delete(ErrorDbHelper.TABLE_BLACK_LIST.getName(), null, null);
            wdb.delete(ErrorDbHelper.TABLE_RE_DOWNLOAD.getName(), null, null);
            wdb.delete(ErrorDbHelper.TABLE_VIEW_ERROR.getName(), null, null);
        }finally {
            if(dbh != null){
                dbh.close();
            }
        }
    }

    private void actionClearSchedule(){
        try {
            // エラーDBクリア
            clearErrorDb();
            // スケジュールDBクリア
            clearScheduleDb();

            Logging.info(getString(R.string.log_info_all_clear_schedule));
            sendToast(getString(R.string.toast_msg_clear_schedule_success));
        }catch(Exception e){
            Logging.stackTrace(e);
            sendToast(getString(R.string.toast_msg_clear_schedule_error));
        }
    }

    private void clearScheduleDb(){
        ScheduleDbHelper dbh = null;

        try{
            dbh = new ScheduleDbHelper(this);

            SQLiteDatabase wdb = dbh.getWriterDb();
            wdb.delete(ScheduleDbHelper.TABLE_SCHEDULE_WEBVIEW.getName(),null,null);
            wdb.delete(ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.getName(),null,null);
            wdb.delete(ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName(),null,null);
            wdb.delete(ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.getName(),null,null);
            wdb.delete(ScheduleDbHelper.TABLE_SCHEDULE_INFO.getName(),null,null);
            wdb.delete(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.getName(), null, null);
            wdb.delete(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP_INFO.getName(), null, null);
            wdb.delete(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP.getName(), null, null);

        }finally {
            if(dbh != null){
                dbh.close();
            }
        }
    }

    private void actionPlayerLaunch(){
// 別の起動時処理で行っている
//        // 再起動アラーム
//        intentUpdaterSetRebootAlarm();

        if(DefaultPref.getNetworkService()) {
            // 先行取得アラーム
            long trigger_at = calculateNextAheadLoad(HealthCheckPref.getAheadLoadTime());
            setAlarmAheadLoad(trigger_at);

            // リアルタイムチェックアラーム
            intentRealtimeCheckSetRealtimeCheckAlarm();

            // ヘルスチェック実行
            if (!actionHealthCheck(false)) {
                // アプリ起動直後はヘルスチェック失敗してもプレイリスト更新
                intentPlaylistPostHealthCheck();
            }
        }

        // PlaylistServiceにアプリ起動処理通知
        intentPlaylistPlayerLaunch();
    }

    private void intentRealtimeCheckSetRealtimeCheckAlarm(){
        Intent intent = new Intent(this, RealtimeCheckService.class);
        intent.setAction(RealtimeCheckService.ACTION_SET_REALTIME_CHECK_ALARM);
        startService(intent);
    }


    private void intentPlaylistPlayerLaunch(){
        Intent intent = new Intent(this, PlaylistService.class);
        intent.setAction(PlaylistService.ACTION_PLAYER_LAUNCH);
        startService(intent);
    }

    private void intentCManagerDownloadContent(int sched_type){
        Intent intent = new Intent(this, ContentManagerService.class);
        intent.setAction(ContentManagerService.ACTION_DOWNLOAD_CONTENTS);
        intent.putExtra(ContentManagerService.INTENT_EXTRA_SCHEDULE_TYPE, sched_type);
        startService(intent);
    }

    private void successSchedule(String result_xml, int schead_type){
        try {

            // スケジュールXMLをtemp/debug以下に出力
            if(DefaultPref.getDebugMode()){
                debugWriteScheduleXml(result_xml, schead_type);
            }

            // 失敗回数リセット
            HealthCheckPref.setHealthCheckFailedCount(0);

            // schedule header xmlパース
            ScheduleHeaderParser sh_parser = new ScheduleHeaderParser();
            ScheduleHeaderObject sched_header = sh_parser.parseHeaderXml(result_xml);

            // health check prefの値更新
            updateHealthCheckPref(sched_header);

            // schedule本文xmlパース
            parseAndStoreScheduleXml(result_xml, schead_type);

            // playlistに通知
            intentPlaylistPostHealthCheck();

            // contentsダウンロード通知
            intentCManagerDownloadContent(schead_type);

            // 再生回数レポート
            intentReportUploadPlayLog();

            // タッチコンテンツレポート
            intentReportUploadTouchLog();

            // 端末ログアップロード
            if(sched_header.upload_terminal_log != null && sched_header.upload_terminal_log > 0){
                intentReportUploadTerminalLog();
            }

            //次回ヘルスチェック時に再起動の指示が来ていたら再起動
            if(sched_header.reboot_immediately == 1){
                intentUpdaterReboot();
            }

        }catch (Exception e){
           Logging.stackTrace(e);
        }
    }

    private long calculateNextAheadLoad(int ahead_load_time){
        // 次回日次処理の時間を生成
        Calendar cal = Calendar.getInstance();

        // secとmillsecを0初期化
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.SECOND, 0);

        int cur_hm = ((cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE)) * 60;

        if(cur_hm >= ahead_load_time){
            cal.add(Calendar.DATE, 1);
        }

        // 「現在時間」から加算（減算）して値を算出
        cal.add(Calendar.SECOND, (ahead_load_time - cur_hm));

        return cal.getTimeInMillis();
    }

    private void setAlarmAheadLoad(long trigger_at_mills){
        // 先行取得アラーム設定
        Intent makeIntent = new Intent(this, HealthCheckService.class);
        makeIntent.setAction(ACTION_AHEAD_LOAD);
        PendingIntent pintent = PendingIntent.getService(this, 0, makeIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        CompatibleSdk.setAlarmEx(this,trigger_at_mills, pintent);

        // 次回先行日時設定
        HealthCheckPref.setNextAheadLoadTime(trigger_at_mills);
    }

    private void setAlarmHealthCheck(long interval){
        // ヘルスチェックアラーム設定
        Intent makeIntent = new Intent(this, HealthCheckService.class);
        makeIntent.setAction(ACTION_HEALTH_CHECK);
        PendingIntent pintent = PendingIntent.getService(this, 0, makeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        long trigger_at_mills = System.currentTimeMillis() + interval;
        CompatibleSdk.setAlarmEx(this,trigger_at_mills, pintent);

        // 次回ヘルスチェック日時設定
        HealthCheckPref.setNextHealthCheckTime(trigger_at_mills);
    }

    private void intentUpdaterSetRebootAlarm(){
        Intent intent = new Intent(this, UpdaterService.class);
        intent.setAction(UpdaterService.ACTION_SET_REBOOT_ALARM);
        startService(intent);
    }


    private void intentRecoverHealthCheckFailed(){
        Intent intent = new Intent(this, RecoverService.class);
        intent.setAction(RecoverService.ACTION_HEALTH_CHECK_FAILED);
        startService(intent);
    }

    private void intentPlaylistPostHealthCheck(){
        Intent intent = new Intent(this, PlaylistService.class);
        intent.setAction(PlaylistService.ACTION_POST_HEALTH_CHECK);
        startService(intent);
    }

//    private void intentCManagerDownloadContent(){
//        Intent intent = new Intent(this, ContentManagerService.class);
//        intent.setAction(ContentManagerService.ACTION_DOWNLOAD_CONTENTS);
//        startService(intent);
//    }

    private void intentCManagerLoadExternalStorage(){
        Intent intent = new Intent(this, ContentManagerService.class);
        intent.setAction(ContentManagerService.ACTION_LOAD_EXTERNAL_STORAGE);
        startService(intent);
    }

    private void intentCManagerLoadStandAloneContent(){
        Intent intent = new Intent(this, ContentManagerService.class);
        intent.setAction(ContentManagerService.ACTION_LOAD_STAND_ALONE_SCHEDULE);
        startService(intent);
    }

    private void intentReportUploadPlayLog(){
        Intent intent = new Intent(this, ReportService.class);
        intent.setAction(ReportService.ACTION_UPLOAD_PLAY_LOG);
        startService(intent);
    }

    private void intentReportUploadTouchLog(){
        Intent intent = new Intent(this, ReportService.class);
        intent.setAction(ReportService.ACTION_UPLOAD_TOUCH_LOG);
        startService(intent);
    }

    private void intentReportUploadTerminalLog(){
        Intent intent = new Intent(this, ReportService.class);
        intent.setAction(ReportService.ACTION_UPLOAD_TERMINAL_LOG);
        startService(intent);
    }

    private void intentUpdaterAdustTime(){
        Intent intent = new Intent(this, UpdaterService.class);
        intent.setAction(UpdaterService.ACTION_ADJUST_TIME);
        startService(intent);
    }

    private void checkIncorrectTimestamp() {
        // 時刻がおかしい（端末初期時間）
        if(!DateUtil.isValidCurrentTime()){
            // UpdaterServiceに時刻合わせをインテント
            intentUpdaterAdustTime();
        }
    }

    private void actionLoadExternalStorage(){
        final String FILE_SCHEDULE = "schedule.xml";
        try{

            File sd_dir = AdmintPath.getSdDir();
            if(sd_dir == null){
                throw new RuntimeException("AdmintPath.getSdDir() return null");
            }

            File sched_xml = new File(sd_dir.getAbsolutePath() + File.separator + AdmintPath.DIR_ADMINT_PLAYER + File.separator + FILE_SCHEDULE);

            // schedule.xmlファイル読み込み
            String xmldoc = FileUtil.loadFile(sched_xml.getAbsolutePath());

            // データベース更新
            parseAndStoreScheduleXml(xmldoc,SCHED_TYPE_SD_CARD);

            // ファイルの展開をリクエスト
            intentCManagerLoadExternalStorage();

        }catch(Exception e){
            // ロギング
            Logging.stackTrace(e);
            sendToast(getString(R.string.toast_msg_sdcard_error));
        }
    }

    private void actionLoadStandAloneSchedule(){
        final String FILE_SCHEDULE = "schedule.xml";
        try{

            File sd_dir = AdmintPath.getAplicationDir();

            File sched_xml = new File(sd_dir.getAbsolutePath() + File.separator +  File.separator + FILE_SCHEDULE);

            // schedule.xmlファイル読み込み
            String xmldoc = FileUtil.loadFile(sched_xml.getAbsolutePath());

            // データベース更新
            parseAndStoreScheduleXml(xmldoc, SCHED_TYPE_SD_CARD);

            // ファイルの展開をリクエスト
            intentCManagerLoadStandAloneContent();

        }catch(Exception e){
            // ロギング
            Logging.stackTrace(e);
            sendToast(getString(R.string.toast_msg_sdcard_error));
        }
    }

    private boolean actionHealthCheck(boolean is_rtc_check){

        // ネットワークサービスがOFFの時
        if(!DefaultPref.getNetworkService()){
            return false;
        }

        int health_check_type;

        if(is_rtc_check){
            health_check_type = SCHEDULE_API_TYPE_HEALTH_CHECK_RTC;
        } else {
            health_check_type = SCHEDULE_API_TYPE_HEALTH_CHECK;
        }

        boolean ret = false;
        final int HEALTH_CHECK_MAX_FAILED_COUNT = 5;
        final long FAILED_HEALTH_CHECK_INTERVAL = 600000; // 10min(millsec)

        String result_xml = null;
        try {
            Logging.info(getString(R.string.log_info_start_health_check));
            // ヘルスチェック実行
//            result_xml = requestSchedule(SCHED_TYPE_HEALTH_CHECK);
            result_xml = requestSchedule(health_check_type);

            if (result_xml == null) {
                HealthCheckPref.setIsPreviousHealthCheckSuccess(false);
                Logging.info(getString(R.string.log_error_failed_health_check));
                // 過去の日付（端末初期時間）の時は時刻合わせ
                checkIncorrectTimestamp();

                int failed_count = HealthCheckPref.getHealthCheckFailedCount() + 1;
                if (failed_count >= HEALTH_CHECK_MAX_FAILED_COUNT) {
                    // 連続エラーの時リカバリ依頼通知
                    intentRecoverHealthCheckFailed();

                    // 連続失敗回数リセット
                    HealthCheckPref.setHealthCheckFailedCount(0);
                } else {
                    // 連続失敗回数カウントアップ
                    HealthCheckPref.setHealthCheckFailedCount(failed_count);
                }

            } else {
                // 連続失敗回数リセット
                HealthCheckPref.setHealthCheckFailedCount(0);

                HealthCheckPref.setLatestHealthCheckTime(System.currentTimeMillis());
                HealthCheckPref.setIsPreviousHealthCheckSuccess(true);
                Logging.info(getString(R.string.log_info_success_health_check));
                // ヘルスチェック成功処理
                successSchedule(result_xml, SCHED_TYPE_HEALTH_CHECK);

                ret = true;
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }finally {
            // 次回ヘルスチェック設定
            if(result_xml != null){
                setAlarmHealthCheck(HealthCheckPref.getHealthCheckInterval() * 1000);
            }else{
                setAlarmHealthCheck(FAILED_HEALTH_CHECK_INTERVAL);
            }
        }

        return ret;
    }

    private void actionAheadLoad(){
        // ネットワークサービスがOFFの時
        if(!DefaultPref.getNetworkService()){
            return;
        }

        final int AHEAD_LOAD_MAX_FAILED_COUNT = 5;
        final long FAILED_AHEAD_LOAD_INTERVAL = 600000; // 10min(millsec)

        String result_xml;
        boolean is_retry = false;
        try {

            Logging.info(getString(R.string.log_info_start_ahead_load));

            // ヘルスチェック実行
            result_xml = requestSchedule(SCHEDULE_API_TYPE_AHEAD_LOAD);

            if (result_xml == null) {

                int failed_count = HealthCheckPref.getAheadLoadFailedCount() + 1;
                if (failed_count >= AHEAD_LOAD_MAX_FAILED_COUNT) {
                    // 連続失敗回数リセット
                    HealthCheckPref.setAheadLoadFailedCount(0);
                } else {
                    // 連続失敗回数カウントアップ
                    HealthCheckPref.setAheadLoadFailedCount(failed_count);
                    is_retry = true;
                }

            } else {
                Logging.info(getString(R.string.log_info_success_ahead_load));
                String version = AdmintApplication.getInstance().getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
//                NetLog.notice("AdmintPlayer version = " + version + ", " + getLogStorageStatusStr());
                NetLog.notice("SignagePlayer version = " + version + ", " + getLogStorageStatusStr());

                // 連続失敗回数リセット
                HealthCheckPref.setAheadLoadFailedCount(0);

                HealthCheckPref.setLatestAheadLoadTime(System.currentTimeMillis());
                // 先行取得成功処理
                successSchedule(result_xml, SCHED_TYPE_AHEAD_LOAD);
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }finally {
            // リトライ時は10分後
            if(is_retry){
                long next_ahead_load_time = System.currentTimeMillis() + FAILED_AHEAD_LOAD_INTERVAL;
//                setAlarmAheadLoad(FAILED_AHEAD_LOAD_INTERVAL);
                setAlarmAheadLoad(next_ahead_load_time);
            }else{
                // 先行取得再アラーム
                long trigger_at = this.calculateNextAheadLoad(HealthCheckPref.getAheadLoadTime());
                setAlarmAheadLoad(trigger_at);
            }
        }
    }

    private void updateHealthCheckPref(ScheduleHeaderObject sched_header){
        final int AHEAD_LOAD_DATE_MIN = 1;
        final int AHEAD_LOAD_DATE_MAX = 100;
        final int HEALTH_CHECK_INTERVAL_MIN = 600; // sec
        final int HEALTH_CHECK_INTERVAL_MAX = 3600; // sec
        final int DAY_OF_SECOND = 86400;

        // 先行取得日数
        if(sched_header.ahead_load_date != null){
            int ahead_load_date = HealthCheckPref.getAheadLoadDate();
            int value = sched_header.ahead_load_date;
            if(ahead_load_date != value){
                if(value >= AHEAD_LOAD_DATE_MIN && value <= AHEAD_LOAD_DATE_MAX){
                    HealthCheckPref.setAheadLoadDate(value);
                }
            }
        }

        // 先行取得時刻
        if(sched_header.ahead_load_time != null){
            int ahead_load_time = HealthCheckPref.getAheadLoadTime();
            int value = sched_header.ahead_load_time;
            if(ahead_load_time != value){
                if(value >= 0 && value <= DAY_OF_SECOND){
                    HealthCheckPref.setAheadLoadTime(value);
                    // 再アラーム
                    long trigger_at = calculateNextAheadLoad(value);
                    setAlarmAheadLoad(trigger_at);
                }
            }
        }

        // ヘルスチェックインターバル
        if(sched_header.heath_check_interval != null){
            int heath_check_interval = HealthCheckPref.getHealthCheckInterval();
            int value = sched_header.heath_check_interval;
            if(heath_check_interval != value){
                if(value >= HEALTH_CHECK_INTERVAL_MIN && value <= HEALTH_CHECK_INTERVAL_MAX){
                    HealthCheckPref.setHealthCheckInterval(value);
                }
            }
        }

        // 再生回数ログアップロード
        if(sched_header.upload_play_count != null){
            int upload_play_count = HealthCheckPref.getUploadPlayCount();
            int value = sched_header.upload_play_count;
            if(upload_play_count != value){
                HealthCheckPref.setUploadPlayCount(value);
            }
        }

        // リアルタイムチェック
        if(sched_header.real_time_check_interval != null){
            int real_time_check_interval = HealthCheckPref.getRealTimeCheckInterval();
            int value = sched_header.real_time_check_interval;
            if(real_time_check_interval != value){
                HealthCheckPref.setRealTimeCheckInterval(value);

                // 再アラーム
                intentRealtimeCheckSetRealtimeCheckAlarm();
            }
        }


        // 曜日時刻設定再起動
        if(sched_header.reboot_week_day != null && sched_header.reboot_week_time != null){
            boolean resetAlram = false;

            int reboot_week_day = HealthCheckPref.getRebootWeekDay();
            int value_day = sched_header.reboot_week_day;
            if(reboot_week_day != value_day){
                HealthCheckPref.setRebootWeekDay(value_day);
                resetAlram = true;
            }

            int reboot_week_time = HealthCheckPref.getRebootWeekTime();
            int value_time = sched_header.reboot_week_time;
            if(reboot_week_time != value_time){
                HealthCheckPref.setRebootWeekTime(value_time);
                resetAlram = true;
            }

            if(resetAlram){
                // 再アラーム
                intentUpdaterSetRebootAlarm();
            }
        }
    }

    private @Nullable String requestSchedule(int type) {

        String request_server_url = ServerUrlPref.getDeliveryServerUrl();
        if (request_server_url.length() == 0) {
            // delevery server url Noting
            return null;
        }

        String site_id = DefaultPref.getSiteId();
        String akey = DefaultPref.getAkey();

        int start_datetime = (int) Math.floor((double) System.currentTimeMillis() / (double) 1000);
        int offsetTime = TimeZone.getDefault().getOffset(Calendar.getInstance().getTimeInMillis()) / 1000;
        int end_datetime;
        if (type == SCHED_TYPE_AHEAD_LOAD) {
            int ahead_load_date = 1;
            if (HealthCheckPref.getAheadLoadDate() > 0) {
                ahead_load_date = HealthCheckPref.getAheadLoadDate();
            }
            end_datetime = start_datetime + (ahead_load_date * 86400);
        } else {
            end_datetime = start_datetime + 86400; //注: 86400秒 = 24時間
        }

        // postデータ作成
        HashMap<String, String> post = makePostData(site_id, start_datetime, end_datetime, type, offsetTime);

        String schedule_url = request_server_url + API_POPKEEPER_SCHEDULE + akey;

        return postRequest(schedule_url, post);
    }

    private void parseAndStoreScheduleXml(String xml_str, int sched_type){

        try {
            ScheduleParser sched_parser = new ScheduleParser();
            // デフォルトコンテンツ取得
            ScheduleObject default_obj = sched_parser.parseDefaultXml(xml_str);
            default_obj.info.sched_type = HealthCheckService.SCHED_TYPE_DEFAULT;
            // デフォルトコンテンツDB設定
            deleteAndInsertScheduleData(default_obj);

            // スケジュール取得
            ScheduleObject schedule_obj = sched_parser.parseScheduleXml(xml_str);
            schedule_obj.info.sched_type = sched_type;
            // スケジュールDB設定
            deleteAndInsertScheduleData(schedule_obj);
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
    }

    private void deleteAndInsertScheduleData(ScheduleObject sched_obj){
        ScheduleDbHelper dbh = null;
        try{
            dbh = new ScheduleDbHelper(this);
            SQLiteDatabase wdb = dbh.getWriterDb();

            // read + writeロックを取る
            wdb.beginTransaction();
            try{
                int sched_type = sched_obj.info.sched_type;

                String where = TABLE_SCHEDULE_INFO.SCHED_TYPE + " = " + sched_type;
                // delete table
                wdb.delete(ScheduleDbHelper.TABLE_SCHEDULE_INFO.getName(), where, null);
                wdb.delete(ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.getName(), where, null);
                wdb.delete(ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName(), where, null);
                wdb.delete(ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.getName(), where, null);
                wdb.delete(ScheduleDbHelper.TABLE_SCHEDULE_WEBVIEW.getName(), where, null);
                wdb.delete(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.getName(), null, null);
                wdb.delete(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP.getName(), null, null);
                wdb.delete(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP_INFO.getName(), null, null);


                // info
                {
                    ContentValues values = new ContentValues();

                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_INFO.SCHED_TYPE, sched_type);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_INFO.UPDATE_AT, sched_obj.info.update_at);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_INFO.START_TIME, sched_obj.info.start_time * 1000);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_INFO.END_TIME, sched_obj.info.end_time * 1000);

                    wdb.insert(ScheduleDbHelper.TABLE_SCHEDULE_INFO.getName(), null, values);
                }

                // program
                for (ScheduleObject.Program program : sched_obj.programs) {
                    ContentValues values = new ContentValues();

                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.SCHED_TYPE, sched_type);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST, program.st * 1000);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.UT, program.ut);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.PT, program.pt * 1000);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.PO, program.po);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.TCID, program.tcid);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.TCD, program.tcd);
                    wdb.insert(ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.getName(), null, values);
                }

                // content
                for (ScheduleObject.Content content : sched_obj.contents) {

                    ContentValues values = new ContentValues();
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.SCHED_TYPE, sched_type);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.ST, content.st * 1000);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.O, content.o);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.ID, content.id);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.D, content.d);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.T, content.t);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.U, content.u);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.TCID, content.tcid);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.TCD, content.tcd);

                    wdb.insert(ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName(), null, values);
                }

                // external
                for (ScheduleObject.External external : sched_obj.externals) {
                    ContentValues values = new ContentValues();

                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.SCHED_TYPE, sched_type);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.ID, external.id);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.F_ID, external.f_id);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.F_O, external.f_o);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.F_FNAME, external.f_name);

                    wdb.replace(ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.getName(), null, values);
                }

                // webview
                for (ScheduleObject.Webview webview : sched_obj.webviews) {
                    ContentValues values = new ContentValues();

                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_WEBVIEW.SCHED_TYPE, sched_type);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_WEBVIEW.ID, webview.id);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_WEBVIEW.W_URL, webview.w_url);

                    wdb.replace(ScheduleDbHelper.TABLE_SCHEDULE_WEBVIEW.getName(), null, values);
                }

                //緊急情報
                for (ScheduleObject.RescueContent rescueContent : sched_obj.rescue_content) {
                    ContentValues values = new ContentValues();
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.ID, rescueContent.id);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.T, rescueContent.type);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.D, rescueContent.duration);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.U, rescueContent.use);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.O, rescueContent.order);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.EXPIRE, rescueContent.expires);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.FILE_NAME, rescueContent.file_name);

                    wdb.replace(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.getName(), null, values);
                }

                for(ScheduleObject.RescueTelopInfo rescueTelopInfo : sched_obj.rescue_telop_info){
                    ContentValues values = new ContentValues();
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP_INFO.DIRECTION, rescueTelopInfo.direction);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP_INFO.ROTATE, rescueTelopInfo.rotate);
                    wdb.replace(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP_INFO.getName(), null, values);
                }

                for(ScheduleObject.RescueTelop rescueTelop : sched_obj.rescue_telop){
                    ContentValues values = new ContentValues();
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP.O, rescueTelop.order);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP.EXPIRE, rescueTelop.expires);
                    values.put(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP.TEXT, rescueTelop.text);
                    wdb.replace(ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP.getName(), null, values);
                }

                wdb.setTransactionSuccessful();
            }finally {
                wdb.endTransaction();
            }

        }finally {
            if(dbh != null){
                dbh.close();
            }
        }

    }

    private HashMap<String,String> makePostData(String site_id, int start_datetime, int end_datetime, int type, int offset_time) {
        HashMap<String,String> post = new HashMap<>();
        post.put("siteid", site_id);
        post.put("startdatetime", Integer.toString(start_datetime));
        post.put("enddatetime", Integer.toString(end_datetime));
        post.put("requesttype", Integer.toString(type));
        post.put("offsettime",Integer.toString(offset_time));
        post.put("touchuseflag", "1"); // タッチ対応フラグ
        post.put("imageuseflag", "1"); //静止画対応フラグ
        post.put("extuseflag", "1"); //外部コンテンツ対応フラグ
        post.put("webuseflag", "1"); // Web利用フラグ

        // プレイヤーバージョン
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
            if(packageInfo != null){
                post.put("playerver", packageInfo.versionName);
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }

        // WiFi電波強度
        Integer rssi = getWifiStrength();
        if(rssi != null) {
            String wifi_str = "W:" + rssi + "dBm";
            post.put("cninfo", wifi_str);
            Logging.info(getString(R.string.log_info_radio_wave_intensity) + wifi_str);
        }

        // 現再生中プレイリスト情報
        String remarks = makeRemarks();
        if(remarks != null) {
            post.put("remarks", remarks);
        }

        return post;
    }

    private @Nullable Integer getWifiStrength() {
        Integer ret = null;
        try {
            WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (manager != null && manager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
                WifiInfo wifi_info = manager.getConnectionInfo();
                if (wifi_info != null && getActiveNetworkType() == ConnectivityManager.TYPE_WIFI) {
                    ret = wifi_info.getRssi();
                }
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
        return ret;
    }

    private int getActiveNetworkType(){
        int ret = -1;
        try{
            ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
            if(cm != null) {
                NetworkInfo ninfo = cm.getActiveNetworkInfo();
                if(ninfo != null) {
                    ret = ninfo.getType();
                }
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
        return ret;
    }

    private @Nullable String makeRemarks(){
        String ret = null;

//        PlaylistDbHelper playlist_dbh = null;
        ViewlistDbHelper viewlist_dbh = null;
        try {
//            playlist_dbh = new PlaylistDbHelper(this);
//            SQLiteDatabase rdb = playlist_dbh.getReaderDb();

            viewlist_dbh = new ViewlistDbHelper(this);
            SQLiteDatabase rdb = viewlist_dbh.getReaderDb();

//            Integer sched_type = null;
//
//            {
//                String sql = "select " +
//                        PlaylistDbHelper.TABLE_PLAY_LIST_INFO.SCHED_TYPE +
//                        " from " + PlaylistDbHelper.TABLE_PLAY_LIST_INFO.getName();
//                try (Cursor cursor = rdb.rawQuery(sql, null)) {
//
//                    // 再生対象のスケジュールタイプ取得
//                    if (cursor.getCount() > 0) {
//                        cursor.moveToNext();
//                        sched_type = cursor.getInt(0);
//                    }
//                }
//
//                // スケジュールタイプが取れない時は再生対象なし
//                if (sched_type == null) {
//                    return null;
//                }
//            }

            boolean currentPlayViewIsDefault = AdmintApplication.getInstance().getIsPlayDefaultContent();
            boolean currentPlayViewIsEmergency = false;
            GetRescueDbData grdd = new GetRescueDbData();
            StringBuilder ids = new StringBuilder();
            String sql;
            String table_name;
            String id;
//            String o = "";
//            if(currentPlayViewIsDefault){
//                table_name = PlaylistDbHelper.TABLE_DEFAULT_LIST.getName();
//                o = PlaylistDbHelper.TABLE_DEFAULT_LIST.O;
//                id = PlaylistDbHelper.TABLE_DEFAULT_LIST.ID;
//
//            } else {
//                table_name = PlaylistDbHelper.TABLE_PLAY_LIST.getName();
//                o = PlaylistDbHelper.TABLE_PLAY_LIST.O;
//                id = PlaylistDbHelper.TABLE_PLAY_LIST.ID;
//            }

            table_name = ViewlistDbHelper.TABLE_VIEW_LIST.getName();
            id = ViewlistDbHelper.TABLE_VIEW_LIST.ID;

            String viewlist_o = ViewlistDbHelper.TABLE_VIEW_LIST.O;

            // 再生対象のID群を取得、サブIDの重複は除外（distinct o, id）
            String order = " order by "+ viewlist_o + " asc";
            sql = String.format("select distinct %s from %s" + order, id, table_name);

            {
                try (Cursor cursor = rdb.rawQuery(sql, null)) {

                    // メッセージ結合して返却値を作成
                    while (cursor.moveToNext()) {
                        //緊急配信を行っているかのチェック
                        if(grdd.checkPlaybackViewList(cursor.getInt(0), this)){
                            currentPlayViewIsEmergency = true;
                        }
                        ids.append(cursor.getInt(0));
                        ids.append(",");
                    }
                }
            }

            if (ids.length() > 0) {
                String list_str = ids.substring(0, ids.length() - 1);
                if (currentPlayViewIsDefault) {
                    ret = getString(R.string.net_logging_notice_show_default_content);
                } else  {
                    ret = getString(R.string.net_logging_notice_show_content);
                }

                //緊急情報があったらその情報を追加する
                String append_telop_str = "";
                if(currentPlayViewIsEmergency) {
                    ret = getString(R.string.net_log_notice_now_playback_rescue_content);
                } else {
                    if(grdd.getRescueTelopInfo(this) != -1){
                        append_telop_str = getString(R.string.net_log_notice_telop_playback_now);
                    }
                }

                ret = ret + list_str + append_telop_str;
            }
        }catch (Exception e){
           Logging.stackTrace(e);
        }finally {
//            if(playlist_dbh != null){
//                playlist_dbh.close();
//            }
            if(viewlist_dbh != null){
                viewlist_dbh.close();
            }
       }
        return ret;
    }

    private void debugWriteScheduleXml(String sched_xml, int sched_type){
        try {
            String file_pref = "UNKNOWN_";
            if (sched_type == HealthCheckService.SCHED_TYPE_HEALTH_CHECK) {
                file_pref = "HC_";
            } else if (sched_type == HealthCheckService.SCHED_TYPE_AHEAD_LOAD) {
                file_pref = "AD_";
            } else if (sched_type == HealthCheckService.SCHED_TYPE_SD_CARD) {
                file_pref = "SD_";
            }

            String file_name = file_pref + DateUtil.convDateTimeFileName(System.currentTimeMillis()) + ".xml";
            File file = new File(AdmintPath.getTemporaryDebugDir().getAbsolutePath() + File.separator + file_name);
            try (FileWriter fwriter = new FileWriter(file)) {
                fwriter.write(sched_xml);
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void intentUpdaterReboot(){
        Intent intent = new Intent(this, UpdaterService.class);
        intent.setAction(UpdaterService.ACTION_REBOOT);
        startService(intent);
    }
}

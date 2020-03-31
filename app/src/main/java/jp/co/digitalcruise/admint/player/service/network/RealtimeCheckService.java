package jp.co.digitalcruise.admint.player.service.network;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.annotation.Nullable;

import java.net.URL;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.CompatibleSdk;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.netutil.ContentInfo;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.pref.HealthCheckPref;
import jp.co.digitalcruise.admint.player.pref.RealtimeCheckPref;
import jp.co.digitalcruise.admint.player.pref.ServerUrlPref;

import static jp.co.digitalcruise.admint.player.BuildConfig.APPLICATION_ID;

public class RealtimeCheckService extends AbstractNetworkService {
    private static final String ACTION_PREFIX = APPLICATION_ID + ".RealtimeCheckService.";

    public static final String ACTION_SET_REALTIME_CHECK_ALARM = ACTION_PREFIX + "SET_REALTIME_CHECK_ALARM";
    public static final String ACTION_REALTIME_CHECK = ACTION_PREFIX + "REALTIME_CHECK";

    public RealtimeCheckService(){
        super(RealtimeCheckService.class.getName());
    }

    public RealtimeCheckService(String name) {
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

            if(ACTION_SET_REALTIME_CHECK_ALARM.equals(action)){
                actionSetRealtimeCheckAlarm();
            }else if(ACTION_REALTIME_CHECK.equals(action)){
                actionRealtimeCheck();
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionSetRealtimeCheckAlarm(){
        try {
            // ネットワークサービスがOFFの時
            if(!DefaultPref.getNetworkService()){
                return;
            }

            long interval = HealthCheckPref.getRealTimeCheckInterval() * 1000;

            if(interval > 0) {
                // リアルタイムチェックアラーム設定
                Intent makeIntent = new Intent(this, RealtimeCheckService.class);
                makeIntent.setAction(ACTION_REALTIME_CHECK);
                PendingIntent pintent = PendingIntent.getService(this, 0, makeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                long trigger_at_mills = System.currentTimeMillis() + interval;
                CompatibleSdk.setAlarmEx(this, trigger_at_mills, pintent);
            }else{
                // リアルタイムチェックキャンセル
                cancelRealtimeCheckAlarm();
            }

        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionRealtimeCheck(){
        try{
            // ネットワークサービスがOFFの時
            if(!DefaultPref.getNetworkService()){
                return;
            }

            // RTCファイルにアクセスして更新日時取得
            long server_modified = requestRealtimeCheck();
            // プリファレンスから取得
            long local_modified = RealtimeCheckPref.getRtcFileModified();

            if(server_modified != local_modified && server_modified > 0){
                if(server_modified > local_modified){
                    // ロギング
                    Logging.info(getString(R.string.log_info_detect_realtime_check));

                    // ヘルスチェック
                    Intent intent = new Intent(this, HealthCheckService.class);
                    intent.setAction(HealthCheckService.ACTION_RTC_HEALTH_CHECK);
                    startService(intent);
                }
                // modifiedをプリファレンス保存
                RealtimeCheckPref.setRtcFileModified(server_modified);
            }

        }catch(Exception e){
            Logging.stackTrace(e);
        }finally {
            // 次回アラームセット
            actionSetRealtimeCheckAlarm();
        }
    }

    private void cancelRealtimeCheckAlarm(){
        Intent makeIntent = new Intent(this, RealtimeCheckService.class);
        makeIntent.setAction(ACTION_REALTIME_CHECK);
        PendingIntent pintent = PendingIntent.getService(this, 0, makeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if(am == null){
            throw new RuntimeException("(AlarmManager) getSystemService(ALARM_SERVICE) return null");
        }
        am.cancel(pintent);
    }

    private long requestRealtimeCheck() {
        long ret = 0;
        try {
            final String URI_REALTIME = "realtime";

            // リアルタイムチェックサーバ
            String rtc_server = ServerUrlPref.getRtcServerUrl();

            String site_id = DefaultPref.getSiteId();
            String akey = DefaultPref.getAkey();

            String rtc_url = rtc_server + URI_APFILES + "/" + site_id + "/" + URI_REALTIME + "/" + akey + "?t=" + System.currentTimeMillis();
            URL url = new URL(rtc_url);

            // ネットワーク状態をログ
            loggingCheckActiveNetwork();

            ContentInfo cinfo = makeContentInfo();

            try{
                // リアルタイムチェックファイル情報取得
//                cinfo.execute(url);
                cinfo.executeContentInfo(url);
            }catch (Exception e){
                Logging.network_error(e);
            }

            ret = cinfo.getLastModified();
        }catch (Exception e){
            Logging.network_error(e);
            Logging.stackTrace(e);
        }
        return ret;
    }

}

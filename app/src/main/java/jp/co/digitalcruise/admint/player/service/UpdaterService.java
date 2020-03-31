package jp.co.digitalcruise.admint.player.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.CompatibleSdk;
import jp.co.digitalcruise.admint.player.component.define.DeviceDef;
import jp.co.digitalcruise.admint.player.component.define.UpdaterIntentDef;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.log.NetLog;
import jp.co.digitalcruise.admint.player.pref.HealthCheckPref;
import jp.co.digitalcruise.admint.player.pref.UpdaterPref;

import static jp.co.digitalcruise.admint.player.BuildConfig.APPLICATION_ID;
import static jp.co.digitalcruise.admint.player.component.parsexml.parser.ScheduleHeaderParser.REBOOT_FLAG_FRI;
import static jp.co.digitalcruise.admint.player.component.parsexml.parser.ScheduleHeaderParser.REBOOT_FLAG_MON;
import static jp.co.digitalcruise.admint.player.component.parsexml.parser.ScheduleHeaderParser.REBOOT_FLAG_SAT;
import static jp.co.digitalcruise.admint.player.component.parsexml.parser.ScheduleHeaderParser.REBOOT_FLAG_SUN;
import static jp.co.digitalcruise.admint.player.component.parsexml.parser.ScheduleHeaderParser.REBOOT_FLAG_THU;
import static jp.co.digitalcruise.admint.player.component.parsexml.parser.ScheduleHeaderParser.REBOOT_FLAG_TUE;
import static jp.co.digitalcruise.admint.player.component.parsexml.parser.ScheduleHeaderParser.REBOOT_FLAG_WED;

public class UpdaterService extends AbstractService{
    private static final String ACTION_PREFIX = APPLICATION_ID + ".UpdaterService.";

    // 再起動アラーム設定
    public static final String ACTION_SET_REBOOT_ALARM = ACTION_PREFIX + "SET_REBOOT_ALARM";
    // 再起動実行
    public static final String ACTION_REBOOT = ACTION_PREFIX + "REBOOT";
    // 手動再起動実行
    public static final String ACTION_REBOOT_MANUAL = ACTION_PREFIX + "REBOOT_MANUAL";
    // 端末時間更新をUpdaterに依頼
    public static final String ACTION_ADJUST_TIME = ACTION_PREFIX + "ADJUST_TIME";
    // Player起動時処理
    public static final String ACTION_PLAYER_LAUNCH = ACTION_PREFIX + "PLAYER_LAUNCH";

    public UpdaterService(){
        super(UpdaterService.class.getName());
    }

    public UpdaterService(String name) {
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

            if(ACTION_SET_REBOOT_ALARM.equals(action)){
                actionSetRebootAlarm();
            }else if(ACTION_ADJUST_TIME.equals(action)){
                actionAdjustTime();
            }else if(ACTION_REBOOT.equals(action)){
                actionReboot();
            }else if(ACTION_REBOOT_MANUAL.equals(action)){
                actionRebootManual();
            }else if(ACTION_PLAYER_LAUNCH.equals(action)){
                actionPlayerLaunch();
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionPlayerLaunch(){
        try{
            setRebootAlarm();
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionSetRebootAlarm(){
        try{
            setRebootAlarm();
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionReboot(){
        try{
            // ロギング
            NetLog.notice(getString(R.string.net_logging_notice_reboot_terminal));
            Logging.info(getString(R.string.log_info_start_reboot));

            // 端末再起動
            rebootStb(10000);
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionRebootManual(){
        try{
            // ロギング
            NetLog.notice(getString(R.string.net_logging_notice_reboot_terminal));
            Logging.info(getString(R.string.log_info_start_reboot_manual));

            // 端末再起動
            rebootStb(3000);
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionAdjustTime() {
        try {
            if (!Build.MODEL.equals(DeviceDef.GROOVA_BOX) && !Build.MODEL.equals(DeviceDef.GROOVA_STICK)) {

                Logging.info(getString(R.string.log_info_send_set_timestamp));
                
                Intent intent = new Intent(UpdaterIntentDef.ACTION_ADJUST_TIME);
                sendBroadcast(intent);

                // ↓2.xではディレイをかけていたが理由が謎なので即時broadcastに変更
//                Intent makeIntent = new Intent(UpdaterIntentDef.ACTION_ADJUST_TIME);
//                PendingIntent intent_adjust_time = PendingIntent.getBroadcast(this, 0, makeIntent, PendingIntent.FLAG_CANCEL_CURRENT);
//                long trigger_time = System.currentTimeMillis() + 60000;
//                CompatibleSdk.setAlarmEx(this,trigger_time,intent_adjust_time);
            }
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
    }

    private void setRebootAlarm(){

        // 2018/1/1
        final long REBOOT_ENABLE_MIN  = 1514732400000L;

        if(System.currentTimeMillis() > REBOOT_ENABLE_MIN){
            Calendar now = Calendar.getInstance();
            int week_int = now.get(Calendar.DAY_OF_WEEK);
            int date_diff = -1;

            int week_flag = HealthCheckPref.getRebootWeekDay();
            int week_time = HealthCheckPref.getRebootWeekTime();


            int cur_hms = (((now.get(Calendar.HOUR_OF_DAY) * 60) + now.get(Calendar.MINUTE)) * 60) + now.get(Calendar.SECOND);
            if(week_int == Calendar.SUNDAY) {
                if ((week_flag & REBOOT_FLAG_SUN) > 0 && week_time >= cur_hms) {
                    // 当日の現在時間以降
                    date_diff = 0;
                } else if ((week_flag & REBOOT_FLAG_MON) > 0) {
                    date_diff = 1;
                } else if ((week_flag & REBOOT_FLAG_TUE) > 0) {
                    date_diff = 2;
                } else if ((week_flag & REBOOT_FLAG_WED) > 0) {
                    date_diff = 3;
                } else if ((week_flag & REBOOT_FLAG_THU) > 0) {
                    date_diff = 4;
                } else if ((week_flag & REBOOT_FLAG_FRI) > 0) {
                    date_diff = 5;
                } else if ((week_flag & REBOOT_FLAG_SAT) > 0) {
                    date_diff = 6;
                } else if ((week_flag & REBOOT_FLAG_SUN) > 0) {
                    // 現在時刻を過ぎているので来週
                    date_diff = 7;
                }
            } else if(week_int == Calendar.MONDAY) {
                if ((week_flag & REBOOT_FLAG_MON) > 0 && week_time >= cur_hms) {
                    date_diff = 0;
                } else if ((week_flag & REBOOT_FLAG_TUE) > 0) {
                    date_diff = 1;
                } else if ((week_flag & REBOOT_FLAG_WED) > 0) {
                    date_diff = 2;
                } else if ((week_flag & REBOOT_FLAG_THU) > 0) {
                    date_diff = 3;
                } else if ((week_flag & REBOOT_FLAG_FRI) > 0) {
                    date_diff = 4;
                } else if ((week_flag & REBOOT_FLAG_SAT) > 0) {
                    date_diff = 5;
                } else if ((week_flag & REBOOT_FLAG_SUN) > 0) {
                    date_diff = 6;
                } else if ((week_flag & REBOOT_FLAG_MON) > 0) {
                    date_diff = 7;
                }
            } else if(week_int == Calendar.TUESDAY) {
                if ((week_flag & REBOOT_FLAG_TUE) > 0 && week_time >= cur_hms) {
                    date_diff = 0;
                } else if ((week_flag & REBOOT_FLAG_WED) > 0) {
                    date_diff = 1;
                } else if ((week_flag & REBOOT_FLAG_THU) > 0) {
                    date_diff = 2;
                } else if ((week_flag & REBOOT_FLAG_FRI) > 0) {
                    date_diff = 3;
                } else if ((week_flag & REBOOT_FLAG_SAT) > 0) {
                    date_diff = 4;
                } else if ((week_flag & REBOOT_FLAG_SUN) > 0) {
                    date_diff = 5;
                } else if ((week_flag & REBOOT_FLAG_MON) > 0) {
                    date_diff = 6;
                } else if ((week_flag & REBOOT_FLAG_TUE) > 0) {
                    date_diff = 7;
                }
            } else if(week_int == Calendar.WEDNESDAY) {
                if ((week_flag & REBOOT_FLAG_WED) > 0 && week_time >= cur_hms) {
                    date_diff = 0;
                } else if ((week_flag & REBOOT_FLAG_THU) > 0) {
                    date_diff = 1;
                } else if ((week_flag & REBOOT_FLAG_FRI) > 0) {
                    date_diff = 2;
                } else if ((week_flag & REBOOT_FLAG_SAT) > 0) {
                    date_diff = 3;
                } else if ((week_flag & REBOOT_FLAG_SUN) > 0) {
                    date_diff = 4;
                } else if ((week_flag & REBOOT_FLAG_MON) > 0) {
                    date_diff = 5;
                } else if ((week_flag & REBOOT_FLAG_TUE) > 0) {
                    date_diff = 6;
                } else if ((week_flag & REBOOT_FLAG_WED) > 0) {
                    date_diff = 7;
                }
            } else if(week_int == Calendar.THURSDAY) {
                if ((week_flag & REBOOT_FLAG_THU) > 0 && week_time >= cur_hms) {
                    date_diff = 0;
                } else if ((week_flag & REBOOT_FLAG_FRI) > 0) {
                    date_diff = 1;
                } else if ((week_flag & REBOOT_FLAG_SAT) > 0) {
                    date_diff = 2;
                } else if ((week_flag & REBOOT_FLAG_SUN) > 0) {
                    date_diff = 3;
                } else if ((week_flag & REBOOT_FLAG_MON) > 0) {
                    date_diff = 4;
                } else if ((week_flag & REBOOT_FLAG_TUE) > 0) {
                    date_diff = 5;
                } else if ((week_flag & REBOOT_FLAG_WED) > 0) {
                    date_diff = 6;
                } else if ((week_flag & REBOOT_FLAG_THU) > 0) {
                    date_diff = 7;
                }
            } else if(week_int == Calendar.FRIDAY) {
                if ((week_flag & REBOOT_FLAG_FRI) > 0 && week_time >= cur_hms) {
                    date_diff = 0;
                } else if ((week_flag & REBOOT_FLAG_SAT) > 0) {
                    date_diff = 1;
                } else if ((week_flag & REBOOT_FLAG_SUN) > 0) {
                    date_diff = 2;
                } else if ((week_flag & REBOOT_FLAG_MON) > 0) {
                    date_diff = 3;
                } else if ((week_flag & REBOOT_FLAG_TUE) > 0) {
                    date_diff = 4;
                } else if ((week_flag & REBOOT_FLAG_WED) > 0) {
                    date_diff = 5;
                } else if ((week_flag & REBOOT_FLAG_THU) > 0) {
                    date_diff = 6;
                } else if ((week_flag & REBOOT_FLAG_FRI) > 0) {
                    date_diff = 7;
                }
            } else if(week_int == Calendar.SATURDAY) {
                if ((week_flag & REBOOT_FLAG_SAT) > 0 && week_time >= cur_hms) {
                    date_diff = 0;
                } else if ((week_flag & REBOOT_FLAG_SUN) > 0) {
                    date_diff = 1;
                } else if ((week_flag & REBOOT_FLAG_MON) > 0) {
                    date_diff = 2;
                } else if ((week_flag & REBOOT_FLAG_TUE) > 0) {
                    date_diff = 3;
                } else if ((week_flag & REBOOT_FLAG_WED) > 0) {
                    date_diff = 4;
                } else if ((week_flag & REBOOT_FLAG_THU) > 0) {
                    date_diff = 5;
                } else if ((week_flag & REBOOT_FLAG_FRI) > 0) {
                    date_diff = 6;
                } else if ((week_flag & REBOOT_FLAG_SAT) > 0) {
                    date_diff = 7;
                }
            }

            if(week_flag > 0){
                Calendar next_cal = Calendar.getInstance();
                next_cal.set(Calendar.MILLISECOND, 0);
                next_cal.set(Calendar.SECOND, 0);
                next_cal.set(Calendar.MINUTE, 0);
                next_cal.set(Calendar.HOUR_OF_DAY, 0);
                next_cal.add(Calendar.SECOND, week_time);
                next_cal.add(Calendar.DATE, date_diff);
                long next_reboot_time = next_cal.getTimeInMillis();

                // ロギング
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss",  Locale.getDefault());
                Logging.info(getString(R.string.log_info_set_reboot_alarm_time) + ", reboot_time=" + sdf.format(next_reboot_time));

                // 再起動のアラーム設定
                Intent makeIntent = new Intent(this, UpdaterService.class);
                makeIntent.setAction(UpdaterService.ACTION_REBOOT);
                PendingIntent pintent = PendingIntent.getService(this, 0, makeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                CompatibleSdk.setAlarmEx(this,next_reboot_time,pintent);

                UpdaterPref.setRebootAlarmTime(next_reboot_time);

            }else{
                cancelRebootAlarm();
            }
        }else{
            // 時刻が合っていない可能性
            cancelRebootAlarm();
        }

    }

    private void cancelRebootAlarm(){
        Intent makeIntent = new Intent(this, UpdaterService.class);
        makeIntent.setAction(UpdaterService.ACTION_REBOOT);
        PendingIntent pintent = PendingIntent.getService(this, 0, makeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if(am != null){
            am.cancel(pintent);
            UpdaterPref.setRebootAlarmTime(0);
            Logging.info(getString(R.string.log_info_cancel_reboot_alarm_time));
        }else{
            throw new RuntimeException("AlarmManager.getSystemService(ALARM_SERVICE) return null");
        }
    }

    private void rebootStb(long delay_time){
        // toto 再起動 Intent Action
        final String GRV_TOTO_ACTION_REBOOT = "jp.co.grv.toto.ACTION_REBOOT";

        Intent intent;
        if(Build.MODEL.equals(DeviceDef.GROOVA_STICK) || Build.MODEL.equals(DeviceDef.GROOVA_BOX)) {
            intent = new Intent(GRV_TOTO_ACTION_REBOOT);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        } else {
            intent = new Intent(UpdaterIntentDef.ACTION_REBOOT);
        }

        if(delay_time > 0){
            // ディレイをかける（ロギングの時間等の理由で）
            PendingIntent pintent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            CompatibleSdk.setAlarmEx(this, System.currentTimeMillis() + delay_time, pintent);
        }else{
            // 即時再起動
            sendBroadcast(intent);
        }
    }
}

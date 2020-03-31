package jp.co.digitalcruise.admint.player.service;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.db.LogDbHelper;
import jp.co.digitalcruise.admint.player.db.PlayLogDbHelper;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;

import static jp.co.digitalcruise.admint.player.BuildConfig.APPLICATION_ID;

@SuppressLint("Registered")
public class LoggingService extends AbstractService {
    private LogDbHelper mLogDbHelper;
    private PlayLogDbHelper mPlayLogDbHelper;

    private static final String ACTION_PREFIX = APPLICATION_ID + ".LoggingService.";
    // ロギング
    public static final String ACTION_LOG = ACTION_PREFIX + "LOG";
    // 不要ログ削除
    public static final String ACTION_REDUCTION = ACTION_PREFIX + "REDUCTION";
    // プレイヤー起動時処理
    public static final String ACTION_PLAYER_LAUNCH = ACTION_PREFIX + "PLAYER_LAUNCH";
    // ログのクリア
    public static final String ACTION_LOG_CLEAR = ACTION_PREFIX + "CLEAR";
    // ログのファイル出力
    public static final String ACTION_LOG_OUTPUT_FILE = ACTION_PREFIX + "OUTPUT_FILE";
    // traceログ(ANRログ)出力
    public static final String ACTION_ANR_TRACE_OUTPUT_FILE = ACTION_PREFIX + "ANR_TRACE_OUTPUT_FILE";
    //再生ログの書き込み
    public static final String ACTION_LOGGING_PLAY_CONTENT = ACTION_PREFIX + "LOGGING_PLAY_COUNT";
    //タッチログの書き込み
    public static final String ACTION_LOGGING_TOUCH_CONTENT = ACTION_PREFIX + "LOGGING_TOUCH_CONTENT";

    public static final String INTENT_EXTRA_TIMESTAMP = "timestamp";
    public static final String INTENT_EXTRA_TAG = "tag";
    public static final String INTENT_EXTRA_TYPE = "type";
    public static final String INTENT_EXTRA_MSG = "msg";
    public static final String INTENT_EXTRA_LOG_STORAGE_PATH = "storage_path";

    public static final int LOG_TYPE_NOTICE = 0;
    public static final int LOG_TYPE_ERROR = 1;
    public static final int LOG_TYPE_INFO = 2;

    public static final String INTENT_EXTRA_LOG_CONTENT_ID = "id";
    public static final String INTENT_EXTRA_LOG_PATTERN_NAME = "pattern_name";
    public static final String INTENT_EXTRA_LOG_PAGE_NAME = "page_name";
    public static final String INTENT_EXTRA_LOG_OBJECT_NAME = "object_name";
    public static final String INTENT_EXTRA_DATETIME = "date_time";

    public LoggingService(){
        super(LoggingService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent == null || intent.getAction() == null){
            return;
        }

        mLogDbHelper = null;
        mPlayLogDbHelper = null;
        try{
            final String action = intent.getAction();


            // LoggingServiceはNoticeを出力しない（大量のログになるため）
//            Logging.notice(action);

            mLogDbHelper = new LogDbHelper(this);
            mPlayLogDbHelper = new PlayLogDbHelper(this);

            if(ACTION_LOG.equals(action)) {
                actionLog(intent);
            }else if (ACTION_PLAYER_LAUNCH.equals(action)){
                actionPlayerLaunch();
            }else if(ACTION_REDUCTION.equals(action)){
                actionReduction();
            }else if(ACTION_LOG_CLEAR.equals(action)){
                actionClear();
            } else if(ACTION_LOG_OUTPUT_FILE.equals(action)) {
                actionOutputLogFile();
            } else if(ACTION_ANR_TRACE_OUTPUT_FILE.equals(action)){
                actionTraceOutputFile();
            } else if(ACTION_LOGGING_PLAY_CONTENT.equals(action)){
                loggingPlay(intent);
            } else if(ACTION_LOGGING_TOUCH_CONTENT.equals(action)){
                loggingTouch(intent);
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }finally{
            if(mLogDbHelper != null){
                mLogDbHelper.close();
            }
            if(mPlayLogDbHelper != null){
                mPlayLogDbHelper.close();
            }
        }
    }

    private void actionTraceOutputFile(){
        try{
            traceOutPutFile();
        }catch (Exception e){
            Logging.stackTrace(e);
            sendToast(getString(R.string.toast_msg_output_log_error));
        }
    }

    private void actionLog(Intent intent){
        try{
            long timestamp = intent.getLongExtra(INTENT_EXTRA_TIMESTAMP, 0);
            int type = intent.getIntExtra(INTENT_EXTRA_TYPE, 0);
            if((type == LOG_TYPE_ERROR || type == LOG_TYPE_INFO || DefaultPref.getNoticeLog())){
                String tag = intent.getStringExtra(INTENT_EXTRA_TAG);
                String msg = intent.getStringExtra(INTENT_EXTRA_MSG);

                if(tag == null){
                    tag = "";
                }

                if(msg == null){
                    msg = "";
                }

                String datetime = getDatetimeStr(timestamp);

                mLogDbHelper.insertLog(timestamp, datetime, type, tag, msg);
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionPlayerLaunch(){
        try {
            //消す前にバックアップ取る
//            createBackUpLog();
            // ログを切り詰る
            reductionLog();
            // ログ切り詰めアラーム設定
            setAlarmReduction();
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionReduction(){
        try{
            //消す前にバックアップ取る
//            createBackUpLog();
            //ログの切りつめ
            reductionLog();
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionClear(){
        try{
            mLogDbHelper.initialize();
            sendToast(getString(R.string.toast_msg_clear_log_success));
        }catch(Exception e){
            Logging.stackTrace(e);
            sendToast(getString(R.string.toast_msg_clear_log_error));
        }
    }

    private void actionOutputLogFile(){
        try {
            if (outputLogFile()) {
                sendToast(getString(R.string.toast_msg_output_log_success));
                return;
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
        sendToast(getString(R.string.toast_msg_output_log_error));
    }

    private void traceOutPutFile (){
        final int RESULT_SUCCESS = 0;
        final int RESULT_FAILED_NO_OUTPUT_PLACE = 1;
        final int RESULT_FAILED_NO_INPUT_FILE = 2;

        int result = actionOutputAnrTraceLog();
        if(result == RESULT_SUCCESS) {
            sendToast(getString(R.string.toast_msg_output_log_success));
        } else if(result == RESULT_FAILED_NO_INPUT_FILE) {
            sendToast(getString(R.string.toast_msg_output_log_error_no_input_file));
        } else if(result == RESULT_FAILED_NO_OUTPUT_PLACE) {
            sendToast(getString(R.string.toast_msg_output_log_error_no_output_place));
        } else {
            sendToast(getString(R.string.toast_msg_output_log_error));
        }
    }

    private void reductionLog(){
//        final int REDUCTION_ROWS = 5000;
        final int REDUCTION_ROWS = 30000;

        SQLiteDatabase rdb = mLogDbHelper.getReadableDatabase();

        String sql = "select " + LogDbHelper.TABLE_LOG._ID + " from " + LogDbHelper.TABLE_LOG.getName() +
                " order by " + LogDbHelper.TABLE_LOG._ID + " desc " +
                " limit " + REDUCTION_ROWS + ", 1";

        try (Cursor cursor = rdb.rawQuery(sql, null)) {

            int target_id = 0;
            if (cursor.moveToNext()) {
                target_id = cursor.getInt(0);
            }

            if (target_id > 0) {
                mLogDbHelper.deleteLog(target_id);
                Logging.info(getString(R.string.log_info_reduction_log));
            }
        }
    }

    private boolean outputLogFile(){
        boolean ret = false;

        try{
            File out_dir = getSdDir();
            if(out_dir == null) {
                return false;
            }

            if(!out_dir.exists() || !out_dir.isDirectory()){
                return false;
            }

            // ファイル名生成
            Date date = new Date(System.currentTimeMillis());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss",  Locale.getDefault());
            String fname = sdf.format(date) + ".log";

            File out_file = new File(out_dir.getAbsolutePath() + "/" + fname);

            String sql = "select " +
                    LogDbHelper.TABLE_LOG.DATETIME + "," +
                    LogDbHelper.TABLE_LOG.TYPE + "," +
                    LogDbHelper.TABLE_LOG.TAG + "," +
                    LogDbHelper.TABLE_LOG.MSG +
                    " from " + LogDbHelper.TABLE_LOG.getName() +
                    " order by " + LogDbHelper.TABLE_LOG._ID + " desc ";

            SQLiteDatabase db = mLogDbHelper.getReadableDatabase();
            try(FileWriter fw = new FileWriter(out_file); Cursor cursor = db.rawQuery(sql, null)){
                try{
//                    fw.append("AdmintPlayer Log ").append(getDatetimeStr(System.currentTimeMillis())).append("\n");
                    fw.append("SignagePlayer Log ").append(getDatetimeStr(System.currentTimeMillis())).append("\n");
                    PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
                    fw.append("version:").append(packageInfo.versionName).append("\n");
                    String siteid = DefaultPref.getSiteId();
                    fw.append("siteid:").append(siteid).append("\n");
                    String terminalid = DefaultPref.getTerminalId();
                    fw.append("terminalid:").append(terminalid).append("\n");
                    String akey = DefaultPref.getAkey();
                    fw.append("akey:").append(akey).append("\n\n");
                }catch(Exception e){
                    Logging.stackTrace(e);
                }

                while (cursor.moveToNext()){
                    StringBuffer set_text = new StringBuffer();
                    try{
                        set_text.append("[").append(cursor.getString(0)).append("] ");
                        int type = Integer.parseInt(cursor.getString(1));

                        switch (type) {
                            case LOG_TYPE_NOTICE:
                                set_text.append(" NOTICE ");
                                break;
                            case LOG_TYPE_INFO :
                                set_text.append(" INFO ");
                                break;
                            case LOG_TYPE_ERROR:
                                set_text.append(" ERROR ");
                                break;
                            default:
                                set_text.append(" ");
                        }

                        set_text.append("[").append(cursor.getString(2)).append("] ");
                        set_text.append(cursor.getString(3));
                        set_text.append("\n");

                        fw.append(set_text);
                    }catch(Exception e){
                        Logging.stackTrace(e);
                    }
                }
                fw.flush();
                ret = true;
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
        return ret;
    }

    private void setAlarmReduction(){
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if(am != null){
            Intent intent = new Intent(this, LoggingService.class);
            intent.setAction(ACTION_REDUCTION);
            PendingIntent pintent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

//            am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, pintent);
            am.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, pintent);
        }else{
            throw new RuntimeException("(AlarmManager)getSystemService return null");
        }
    }

    private String getDatetimeStr(long timestamp){
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss(SSS)", Locale.getDefault());

        return sdf.format(date);
    }

    private int actionOutputAnrTraceLog() {
        int ret;

        final int RESULT_FAILED_NO_OUTPUT_PLACE = 1;
        final int RESULT_FAILED_NO_INPUT_FILE = 2;

        String path = "/data/anr/traces.txt";
        File in_file = new File(path);

        if(in_file.isFile()) {
            File out_dir = getSdDir();
            if(out_dir == null) {
                return RESULT_FAILED_NO_OUTPUT_PLACE;
            }

            if(!out_dir.exists() || !out_dir.isDirectory()){
                return RESULT_FAILED_NO_OUTPUT_PLACE;
            }

            // ファイル名生成
            Date date = new Date(System.currentTimeMillis());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss",  Locale.getDefault());
            String fname = sdf.format(date) + ".trace.log";

            File out_file = new File(out_dir.getAbsolutePath() + "/" + fname);
            ret = copyFile(in_file, out_file);
        } else{
            ret = RESULT_FAILED_NO_INPUT_FILE;
        }
        return ret;
    }

    private int copyFile(File in, File out) {
        FileInputStream fis  = null;
        FileOutputStream fos = null;
        int RESULT_SUCCESS = 0;
        final int RESULT_FAILED_OTHER_REASON = 3;

        int ret = RESULT_SUCCESS;
        try {
            fis = new FileInputStream(in);
            fos = new FileOutputStream(out);

            byte[] buf = new byte[1024];
            int i;
            try {
                while ((i = fis.read(buf)) != -1) {
                    fos.write(buf, 0, i);
                }
            } catch (IOException e) {
                Logging.stackTrace(e);
                ret = RESULT_FAILED_OTHER_REASON;
            }
        } catch (FileNotFoundException e) {
            Logging.stackTrace(e);
            ret = RESULT_FAILED_OTHER_REASON;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Logging.stackTrace(e);
                    ret = RESULT_FAILED_OTHER_REASON;
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Logging.stackTrace(e);
                    ret = RESULT_FAILED_OTHER_REASON;
                }
            }
        }

        return ret;
    }

    private void loggingPlay(Intent intent){
        try{
            final int id = intent.getIntExtra(INTENT_EXTRA_LOG_CONTENT_ID, 0);
            final long timestamp = intent.getLongExtra(INTENT_EXTRA_TIMESTAMP, 0);
            final String datetime = intent.getStringExtra(INTENT_EXTRA_DATETIME);


            SQLiteDatabase wdb = mPlayLogDbHelper.getWriterDb();

            int count = 0;
            // 再生回数取得
            {
                String where = " where " + PlayLogDbHelper.TABLE_PLAY_LOG.ID + " = " + id + " and " +
                        PlayLogDbHelper.TABLE_PLAY_LOG.TIMESTAMP + " = " + timestamp;
                String sql = "select " +  PlayLogDbHelper.TABLE_PLAY_LOG.COUNT +
                        " from " +  PlayLogDbHelper.TABLE_PLAY_LOG.getName() +
                        where;
                try(Cursor cursor = wdb.rawQuery(sql, null)){
                    if(cursor.moveToFirst()){
                        count = cursor.getInt(0);
                    }
                }
            }

            // カウントアップ
            count++;

            ContentValues values = new ContentValues();
            values.put(PlayLogDbHelper.TABLE_PLAY_LOG.ID, id);
            values.put(PlayLogDbHelper.TABLE_PLAY_LOG.TIMESTAMP, timestamp);
            values.put(PlayLogDbHelper.TABLE_PLAY_LOG.DATETIME, datetime);
            values.put(PlayLogDbHelper.TABLE_PLAY_LOG.COUNT, count);
            wdb.replace(PlayLogDbHelper.TABLE_PLAY_LOG.getName(), null, values);
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void loggingTouch(Intent intent){
        final int id = intent.getIntExtra(INTENT_EXTRA_LOG_CONTENT_ID, 0);
        final String pattern_name = intent.getStringExtra(INTENT_EXTRA_LOG_PATTERN_NAME);
        final String page_name = intent.getStringExtra(INTENT_EXTRA_LOG_PAGE_NAME);
        final String object_name = intent.getStringExtra(INTENT_EXTRA_LOG_OBJECT_NAME);
        final long timestamp = intent.getLongExtra(INTENT_EXTRA_TIMESTAMP, 0);
        final String datetime = intent.getStringExtra(INTENT_EXTRA_DATETIME);
        try{

            SQLiteDatabase wdb = mPlayLogDbHelper.getWriterDb();
            ContentValues values = new ContentValues();
            values.put(PlayLogDbHelper.TABLE_TOUCH_LOG.ID, id);
            values.put(PlayLogDbHelper.TABLE_TOUCH_LOG.TIMESTAMP, timestamp);
            values.put(PlayLogDbHelper.TABLE_TOUCH_LOG.DATETIME, datetime);
            values.put(PlayLogDbHelper.TABLE_TOUCH_LOG.PATTERN, pattern_name);
            values.put(PlayLogDbHelper.TABLE_TOUCH_LOG.PAGE, page_name);
            values.put(PlayLogDbHelper.TABLE_TOUCH_LOG.ACT, object_name);
            wdb.insert(PlayLogDbHelper.TABLE_TOUCH_LOG.getName(), null, values);
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    //ログを一部削除する前に保存
//    private void createBackUpLog(){
//
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault());
//        String file_name = sdf.format(System.currentTimeMillis()) + "TerminalLog.log";
//
//        File dir = new File(AdmintPath.getAplicationDir()+ File.separator + "BackupTerminalLog");
//
//        if(!dir.exists() || !dir.isDirectory()){
//            FileUtil.makeDir(dir);
//        }
//
//       deleteBackupLog(dir);
//
//        File backupLog = new File(dir + File.separator + file_name);
//
//        SQLiteDatabase rdb = mLogDbHelper.getReadableDatabase();
//
//        String sql = "select " +
//                LogDbHelper.TABLE_LOG.DATETIME + ", " +
//                LogDbHelper.TABLE_LOG.TYPE + ", " +
//                LogDbHelper.TABLE_LOG.TAG + ", " +
//                LogDbHelper.TABLE_LOG.MSG +
//                " from " + LogDbHelper.TABLE_LOG.getName() +
//                " order by " + LogDbHelper.TABLE_LOG._ID + " desc ";
//
//        try (Cursor cursor = rdb.rawQuery(sql, null); FileWriter writer = new FileWriter(backupLog, true)) {
//
//            StringBuffer stringBuffer = new StringBuffer();
//            while(cursor.moveToNext()){
//
//                stringBuffer.append("[").append(cursor.getString(0)).append("]  ");
//                int type = Integer.parseInt(cursor.getString(1));
//
//                switch (type) {
//                    case LoggingService.LOG_TYPE_INFO:
//                        stringBuffer.append("INFO");
//                        break;
//                    case LoggingService.LOG_TYPE_ERROR:
//                        stringBuffer.append("ERROR");
//                        break;
//                    case LoggingService.LOG_TYPE_NOTICE:
//                        stringBuffer.append("NOTICE");
//                        break;
//                    default:
//                        stringBuffer.append(" ");
//                        break;
//                }
//
//                stringBuffer.append("[").append(cursor.getString(2)).append("]  ");
//                stringBuffer.append(cursor.getString(3));
//                stringBuffer.append("\n");
//            }
//            writer.append(stringBuffer);
//        } catch (IOException e){
//            Logging.stackTrace(e);
//        }
//    }
//
//    private void deleteBackupLog(File dir){
//        long now_date = System.currentTimeMillis();
//        //対象ディレクトににファイルが存在している
//        if(dir.length() > 0){
//            for(File txt : dir.listFiles()){
//                long create_date = txt.lastModified();
//                long elapsed_time = (now_date - create_date)  / (1000 * 60 * 60 * 24 );
//                //作成から1週間以上経過しているファイルは削除
//                if(elapsed_time > 7){
//                    if(!txt.delete()){
//                        Logging.error(getString(R.string.log_failed_delete_backup_file, txt));
//                    }
//                }
//            }
//        }
//    }

}

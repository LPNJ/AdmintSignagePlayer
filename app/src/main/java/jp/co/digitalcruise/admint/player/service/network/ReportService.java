package jp.co.digitalcruise.admint.player.service.network;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.date.DateUtil;
import jp.co.digitalcruise.admint.player.component.file.AdmintPath;
import jp.co.digitalcruise.admint.player.component.file.ContentFile;
import jp.co.digitalcruise.admint.player.component.file.FileUtil;
import jp.co.digitalcruise.admint.player.component.file.ZipUtil;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.netutil.Uploader;
import jp.co.digitalcruise.admint.player.component.parsexml.object.ApiResultObject;
import jp.co.digitalcruise.admint.player.component.parsexml.parser.ApiResultParser;
import jp.co.digitalcruise.admint.player.db.LogDbHelper;
import jp.co.digitalcruise.admint.player.db.PlayLogDbHelper;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.pref.HealthCheckPref;
import jp.co.digitalcruise.admint.player.pref.ReportPref;
import jp.co.digitalcruise.admint.player.pref.ServerUrlPref;
import jp.co.digitalcruise.admint.player.service.LoggingService;

import static jp.co.digitalcruise.admint.player.BuildConfig.APPLICATION_ID;

public class ReportService extends AbstractNetworkService {
    private static final String ACTION_PREFIX = APPLICATION_ID + ".ReportService.";

    // 再生ログアップロード
    public static final String ACTION_UPLOAD_PLAY_LOG = ACTION_PREFIX + "UPLOAD_PLAY_LOG";

    // タッチログアップロード
    public static final String ACTION_UPLOAD_TOUCH_LOG = ACTION_PREFIX + "UPLOAD_TOUCH_LOG";

    // 端末ログアップロード
    public static final String ACTION_UPLOAD_TERMINAL_LOG = ACTION_PREFIX + "UPLOAD_TERMINAL_LOG";

    private static final String API_POPKEEPER_UPLOAD_PLAY_COUNT = API_POPKEEPER  + "uploadPlayCount" + PARAM_KEY;
    private static final String API_POPKEEPER_UPLOAD_FILE = API_POPKEEPER  + "uploadFile" + PARAM_KEY;

    public ReportService(){
        super(ReportService.class.getName());
    }

    public ReportService(String name) {
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

            if(ACTION_UPLOAD_PLAY_LOG.equals(action)){
                actionUploadPlayLog();
            }else if(ACTION_UPLOAD_TOUCH_LOG.equals(action)){
                actionUploadTouchLog();
            }else if(ACTION_UPLOAD_TERMINAL_LOG.equals(action)){
                actionUploadTerminalLog();
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionUploadPlayLog() {
        try {
            // ネットワークサービスがOFFの時
            if(!DefaultPref.getNetworkService()){
                return;
            }

            if(HealthCheckPref.getUploadPlayCount() != 1){
                return;
            }

            if (isUploadChance()) {

                // 前バージョンのプリファレンスが存在すれば移行
                onUpgradePlayLogPref();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                // 現在時刻(時分秒を切り捨て）
                long current_time = sdf.parse(sdf.format(new Date())).getTime();

                // 前回アップロード時刻
                long latest_upload_time = ReportPref.getPlayLogLatestUploadTime();

                if (current_time == latest_upload_time) {
                    // アップロード済
                    return;
                }

                // プレイログファイル生成
                File zip_file = outputPlayLogFile(current_time, sdf.format(current_time));


                // ロギング
                Logging.info(getString(R.string.log_info_start_upload_playlog));

                // ファイルアップロード
                if(uploadPlaylog(zip_file)) {
                    // アップロード完了
                    // ロギング
                    Logging.info(getString(R.string.log_info_finish_upload_playlog));

                    // DBレコード削除
                    deletePlayLogRecord(current_time);

                    // アップロード日更新
                    ReportPref.setPlayLogLatestUploadTime(current_time);
                }else{
                    // ロギング
                    Logging.error(getString(R.string.log_error_failed_upload_playlog));
                }

                // ファイル削除
                FileUtil.deleteFile(zip_file);
            }
        }catch (Exception e){
            // ロギング
            Logging.stackTrace(e);
        }
    }

    private void actionUploadTouchLog() {
        try {
            // ネットワークサービスがOFFの時
            if(!DefaultPref.getNetworkService()){
                return;
            }

            if (isUploadChance()) {

                // 前バージョンのプリファレンスが存在すれば移行
                onUpgradeTouchLogPref();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                // 現在時刻(時分秒を切り捨て）
                long current_time = sdf.parse(sdf.format(new Date())).getTime();

                // 前回アップロード時刻
                long latest_upload_time = ReportPref.getTouchLogLatestUploadTime();

                if (current_time == latest_upload_time) {
                    // アップロード済
                    return;
                }

                // タッチログファイル生成
                File zip_file = outputTouchLogFile();

                // アップロードファイルなし
                if(zip_file == null){
                    return;
                }

                // ロギング
                Logging.info(getString(R.string.log_info_start_upload_touchlog));

                // ファイルアップロード
                if(uploadTouchlog(zip_file)) {

                    // ロギング
                    Logging.info(getString(R.string.log_info_finish_upload_touchlog));

                    // DBレコード削除
                    deleteTouchLogRecord();

                    // 移行前ファイルがあれば削除
                    deleteOldtouchFile();

                    // アップロード日更新
                    ReportPref.setTouchLogLatestUploadTime(current_time);
                }else{
                    // ロギング
                    Logging.error(getString(R.string.log_error_failed_upload_touchlog));
                }

                // ファイル削除
                FileUtil.deleteFile(zip_file);
            }
        }catch (Exception e){
            // ロギング
            Logging.stackTrace(e);
        }
    }

    private void actionUploadTerminalLog() {
        final String TEMP_PATH = "terminal_log_temp";

        try {
            // ネットワークサービスがOFFの時
            if(!DefaultPref.getNetworkService()){
                return;
            }

            File temp_dir = new File(AdmintPath.getReportDir().getAbsolutePath() + File.separator + TEMP_PATH);

            // 作業ディレクトリが残っていれば削除
            if (temp_dir.isDirectory()) {
                FileUtil.deleteRecursive(temp_dir);
            }

            // デバッグログ出力
            File zip_file = outputLocalLog(temp_dir);

            // ロギング
            Logging.info(getString(R.string.log_info_start_upload_locallog));
            // ファイルアップロード
            if (uploadLocallog(zip_file)) {
                // ロギング
                Logging.info(getString(R.string.log_info_finish_upload_locallog));
            } else {
                // ロギング
                Logging.info(getString(R.string.log_error_failed_upload_locallog));
            }

            // 作業ディレクトリ削除
            if (temp_dir.isDirectory()) {
                FileUtil.deleteRecursive(temp_dir);
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private boolean isUploadChance(){
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        // 0 から 3までの乱数を生成
        int rand = (int) (Math.random() * 4);

        // アップロード時間が集中しないように散らすのが目的で
        // ２時以降、または（０時から１時５９分の間の時）４分の１の確率でアップロードを実行
        if (hour >= 2 || rand == 0) {
            return true;
        }else{
            return false;
        }
    }

    private void onUpgradePlayLogPref(){
        // 再生ログ
        try{
            SharedPreferences prefs = getSharedPreferences("ReportLogIntentService", Activity.MODE_PRIVATE);
            final String PREF_UPLOADED_PLAY_COUNT_DATE = "uploaded_play_count_date";
            String uploaded_playlog_date = prefs.getString(PREF_UPLOADED_PLAY_COUNT_DATE, null);

            // 旧プリファレンスの移行が未だ
            if(uploaded_playlog_date != null && ReportPref.getPlayLogLatestUploadTime() == 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date conv = sdf.parse(uploaded_playlog_date);

                // プリファレンス値移行
                if (conv.getTime() > 0) {
                    ReportPref.setPlayLogLatestUploadTime(conv.getTime());
                }
                prefs.edit().remove(PREF_UPLOADED_PLAY_COUNT_DATE).apply();
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void onUpgradeTouchLogPref(){
        // タッチログ
        try{
            SharedPreferences prefs = getSharedPreferences("jp.co.digitalcruise.admint.components.log.ReportLogIntentService", Activity.MODE_PRIVATE);
            final String PREF_UPLOADED_DATESTAMP = "uploaded_datestamp";
            String uploaded_touchlog_date = prefs.getString(PREF_UPLOADED_DATESTAMP, null);

            // 旧プリファレンスの移行が未だ
            if(uploaded_touchlog_date != null) {
                if (ReportPref.getTouchLogLatestUploadTime() == 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                    Date conv = sdf.parse(uploaded_touchlog_date);

                    // プリファレンス値移行
                    if (conv.getTime() > 0) {
                        ReportPref.setTouchLogLatestUploadTime(conv.getTime());
                    }
                }
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private File outputLocalLog(File temp_dir) throws IOException {
        final String COMPLESS_EXTENSION = ".zip";
        final String OUTPUT_DIR_PREFIX = "debug_";
        final String TERMINAL_LOG_FILE_NAME = "terminal.log";
        final String SETTING_INFO_FILE_NAME = "setting_info.log";
        final String TRACE_FILE_NAME = "traces.txt";
        final String NETWORK_TEST_FILE_NAME = "resultNetWorkTest.txt";
//        final String BACKUP_LOG_FILE_DIR = "BackupTerminalLog";

        // 作業ディレクトリ作成
        FileUtil.makeDir(temp_dir);

        // ログファイル出力ディレクトリ（zip対象ディレクトリ）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss(SSS)", Locale.getDefault());
        String dir_name = OUTPUT_DIR_PREFIX + DateUtil.convDateTimeFileName(System.currentTimeMillis());
        File out_dir = new File(temp_dir.getAbsolutePath() + File.separator + dir_name);
//        File out_dir = new File(temp_dir.getAbsolutePath() + File.separator + OUTPUT_DIR_PREFIX + sdf.format(new Date()));

        // ログファイル出力ディレクトリ作成
        FileUtil.makeDir(out_dir);

        // 端末ログ出力
        File terminal_log_file = new File(out_dir.getAbsolutePath() + File.separator + TERMINAL_LOG_FILE_NAME);
        outputTerminalLogFile(terminal_log_file);

        // 設定情報出力
        File setting_info_file = new File(out_dir.getAbsolutePath() + File.separator + SETTING_INFO_FILE_NAME);
        outputSettingInfo(setting_info_file);

        // ANR
        File trace_file = new File(out_dir.getAbsolutePath() + File.separator + TRACE_FILE_NAME);
        copyAnrFile(trace_file);

        // ネットワークテスト
        File networktest_file = new File(AdmintPath.getReportDir().getAbsolutePath() + File.separator + NETWORK_TEST_FILE_NAME);
        if(networktest_file.isFile()){
            File dest_file = new File(out_dir.getAbsolutePath() + File.separator + NETWORK_TEST_FILE_NAME);
            FileUtil.copyTransfer(networktest_file, dest_file);
        }

//        //バックアップログファイル
//        File temp_log_dir = new File(out_dir.getAbsolutePath() + File.separator + BACKUP_LOG_FILE_DIR);
//        File backup_log_dir = new File(AdmintPath.getAplicationDir() + File.separator + BACKUP_LOG_FILE_DIR);
//        if(backup_log_dir.exists() && backup_log_dir.isDirectory()){
//            if(temp_log_dir.mkdir()){
//                File[] list = backup_log_dir.listFiles();
//                for(File log_file : list){
//                    File dest_file = new File(temp_log_dir.getAbsolutePath()+File.separator+log_file.getName());
//                    FileUtil.copyTransfer(log_file, dest_file);
//                }
//            }
//        }


        File zip_file = new File(temp_dir.getAbsolutePath() + File.separator + OUTPUT_DIR_PREFIX + sdf.format(new Date()) + COMPLESS_EXTENSION);

        return complessLogDir(out_dir, zip_file);
    }

    private void deleteTouchLogRecord(){
        PlayLogDbHelper dbh = null;
        try {
            dbh = new PlayLogDbHelper(this);

            SQLiteDatabase wdb = dbh.getWriterDb();

            // delete record
            wdb.delete(PlayLogDbHelper.TABLE_TOUCH_LOG.getName(), null, null);

        }finally {
            if(dbh != null){
                dbh.close();
            }
        }
    }

    private void deletePlayLogRecord(long target_time){
        PlayLogDbHelper dbh = null;
        try {
            dbh = new PlayLogDbHelper(this);

            SQLiteDatabase wdb = dbh.getWriterDb();

            String where = PlayLogDbHelper.TABLE_PLAY_LOG.TIMESTAMP + " < " + target_time;

            // delete record
            wdb.delete(PlayLogDbHelper.TABLE_PLAY_LOG.getName(), where, null);

        }finally {
            if(dbh != null){
                dbh.close();
            }
        }
    }

    private @Nullable File outputTouchLogFile() throws IOException {
        final String TEMP_FILE_NAME = "touch.csv";
        final String OUTPUT_FILE_PREFIX = "touch_";
        final String OUTPUT_FILE_SUFFIX = ".csv";

        File temp_file = new File(AdmintPath.getReportDir().getAbsolutePath() + File.separator + TEMP_FILE_NAME);

        // ファイル出力
        if(!writeTouchLogFile(temp_file)){
            // ログデータなし
            return null;
        }

//        // ログデータなし
//        if(!existTouchLog()){
//            return null;
//        }

//        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss(SSS)", Locale.getDefault());

        // ファイル圧縮
        File zip_file = complessLogFile(temp_file, OUTPUT_FILE_PREFIX + DateUtil.convDateTimeFileName(System.currentTimeMillis()) + OUTPUT_FILE_SUFFIX);

        // 圧縮前ファイル削除
        FileUtil.deleteFile(temp_file);

        return zip_file;
    }


    private File outputPlayLogFile(long target_time, String date_str) throws IOException {
        final String TEMP_FILE_NAME = "play_count.tsv";
        final String OUTPUT_FILE_PREFIX = "play_count_"; // サーバ側も参照しているので値は変えないこと！
        final String OUTPUT_FILE_SUFFIX = ".tsv"; // サーバ側も参照しているので値は変えないこと！

        File temp_file = new File(AdmintPath.getReportDir().getAbsolutePath() + File.separator + TEMP_FILE_NAME);

        // ファイル出力
        writePlayLogFile(temp_file, target_time);

        // ファイル圧縮
        File zip_file = complessLogFile(temp_file, OUTPUT_FILE_PREFIX + date_str + OUTPUT_FILE_SUFFIX);

        // 圧縮前ファイル削除
        FileUtil.deleteFile(temp_file);

        return zip_file;
    }

    private File complessLogFile(File target_file, String file_name) throws IOException {
        final String COMPLESS_EXTENSION = ".zip";

        File dest_file = null;
        try{
            // ログファイルコピー
            dest_file = new File(target_file.getParent() + "/" + file_name);

            // コピー先ファイルがすでに存在すれば削除
            if(dest_file.isFile()){
                FileUtil.deleteFile(dest_file);
            }
            // ファイルコピー
            FileUtil.copyTransfer(target_file, dest_file);

            // 拡張子を.tsvから.zipに
            File zip_file = new File(dest_file.getParent() + "/" + ContentFile.splitFileExt(file_name) + COMPLESS_EXTENSION);

            // zipファイルオブジェクト作成
            ZipUtil.makeZip(dest_file, zip_file);

            return zip_file;
        }finally{
            // zipファイル作成後、コピー先ファイルは不要なので削除
            if(dest_file != null && dest_file.isFile()){
                FileUtil.deleteFile(dest_file);
            }
        }
    }

    private File complessLogDir(File target_dir, File zip_file) throws IOException {
        // zipファイルオブジェクト作成
        ZipUtil.makeZip(target_dir, zip_file);

        // 権限付与
//        FileUtil.chmodRecursive(zip_file);

        return zip_file;
    }

    private boolean migrateOldTouchFile(File new_temp_file) throws IOException {
        final String CURRENT_TOUCH_LOG_FILE_NAME = "current_touch.log"; // ver2までのファイル名
        final String OLD_FILE_PATH = "data/data/jp.co.digitalcruise.admint.player/files/report";
        File old_temp_file = new File(OLD_FILE_PATH + File.separator + CURRENT_TOUCH_LOG_FILE_NAME);
        // 旧ファイルが存在すれば移行
        if(old_temp_file.isFile()){

            FileUtil.copyTransfer(old_temp_file, new_temp_file);
            return true;
        }
        return false;
    }

    private void deleteOldtouchFile(){
        final String CURRENT_TOUCH_LOG_FILE_NAME = "current_touch.log"; // 2.xまでのファイル名
        final String OLD_FILE_PATH = "data/data/jp.co.digitalcruise.admint.player/files/report";

        File old_temp_file = new File(OLD_FILE_PATH + File.separator + CURRENT_TOUCH_LOG_FILE_NAME);
        // 旧ファイルが存在すれば移行
        if(old_temp_file.isFile()) {
            FileUtil.deleteFile(old_temp_file);
        }
    }

//    private boolean existTouchLog(){
//        PlayLogDbHelper dbh = null;
//        try{
//            dbh = new PlayLogDbHelper(this);
//            SQLiteDatabase rdb = dbh.getReaderDb();
//
//            long count = DatabaseUtils.queryNumEntries(rdb,PlayLogDbHelper.TABLE_TOUCH_LOG.getName());
//            if(count > 0){
//                return true;
//            }
//        }finally {
//            if(dbh != null){
//                dbh.close();
//            }
//        }
//        return false;
//    }


    private boolean writeTouchLogFile(File out_file) throws IOException {

        PlayLogDbHelper dbh = null;
        boolean is_write = false;
        try{
            dbh = new PlayLogDbHelper(this);
            SQLiteDatabase rdb = dbh.getReaderDb();

            String order = " order by " + PlayLogDbHelper.TABLE_TOUCH_LOG.TIMESTAMP + " asc";
            String sql = "select " + PlayLogDbHelper.TABLE_TOUCH_LOG.DATETIME + "," +
                    PlayLogDbHelper.TABLE_TOUCH_LOG.ID + "," +
                    PlayLogDbHelper.TABLE_TOUCH_LOG.PATTERN + "," +
                    PlayLogDbHelper.TABLE_TOUCH_LOG.PAGE + "," +
                    PlayLogDbHelper.TABLE_TOUCH_LOG.ACT +
                    " from " + PlayLogDbHelper.TABLE_TOUCH_LOG.getName() +
                    order;

            // ファイルが存在すれば削除
            if(out_file.isFile()){
                FileUtil.deleteFile(out_file);
            }

            // 旧タッチファイルがあれば移行
            boolean is_magrated = migrateOldTouchFile(out_file);

            // 移行ファイルがなければ新規作成
            if(!is_magrated) {
                FileUtil.createUtfFile(out_file);
            }

            boolean exists_touch_record = false;
            try (Cursor cursor = rdb.rawQuery(sql, null); FileWriter fw = new FileWriter(out_file, true)) {

                if(cursor.getCount() > 0) {
                    exists_touch_record = true;
                    while (cursor.moveToNext()) {
                        StringBuffer set_text = new StringBuffer();
                        set_text.append(cursor.getString(0));
                        set_text.append(",");
                        set_text.append(cursor.getInt(1));
                        set_text.append(",");
                        set_text.append(cursor.getString(2));
                        set_text.append(",");
                        set_text.append(cursor.getString(3));
                        set_text.append(",");
                        set_text.append(cursor.getString(4));
                        set_text.append("\n");
                        fw.append(set_text);
                    }
                    fw.flush();
                }
            }

            if(is_magrated || exists_touch_record){
                is_write = true;
            }

        }finally {
            if(dbh != null){
                dbh.close();
            }
        }

        return is_write;
//        return true;
    }

    private void writePlayLogFile(File out_file, long target_time) throws IOException {
        PlayLogDbHelper dbh = null;
        try{
            dbh = new PlayLogDbHelper(this);
            SQLiteDatabase db = dbh.getReaderDb();

            String where = " where " + PlayLogDbHelper.TABLE_PLAY_LOG.TIMESTAMP + " < " + target_time;
            String order = " order by " + PlayLogDbHelper.TABLE_PLAY_LOG.TIMESTAMP + " desc";
            String sql = "select " + PlayLogDbHelper.TABLE_PLAY_LOG.DATETIME + "," +
                    PlayLogDbHelper.TABLE_PLAY_LOG.ID + "," +
                    PlayLogDbHelper.TABLE_PLAY_LOG.COUNT +
                    " from " + PlayLogDbHelper.TABLE_PLAY_LOG.getName() +
                    where +
                    order;

            if(out_file.isFile()){
                FileUtil.deleteFile(out_file);
            }

            FileUtil.createFile(out_file);

            try (Cursor cursor = db.rawQuery(sql, null); FileWriter fw = new FileWriter(out_file)) {
                // データなし
                if(cursor.getCount() == 0){
                    return;
                }

                while (cursor.moveToNext()) {
                    StringBuffer set_text = new StringBuffer();
                    set_text.append(cursor.getString(0));
                    set_text.append("\t");
                    set_text.append(cursor.getInt(1));
                    set_text.append("\t");
                    set_text.append(cursor.getInt(2));
                    set_text.append("\n");
                    fw.append(set_text);
                }
                fw.flush();
            }
        }finally {
            if(dbh != null){
                dbh.close();
            }
        }
    }

    private boolean uploadLocallog(File zip_file) throws MalformedURLException {

        HashMap<String, String> post = new HashMap<>();
        post.put("siteid", DefaultPref.getSiteId());
        post.put("type", "1");

        String upload_url = ServerUrlPref.getUploadServerUrl() + API_POPKEEPER_UPLOAD_FILE + DefaultPref.getAkey();

        URL request_url = new URL(upload_url);

        return uploadLog(request_url, zip_file, post);
    }


    private boolean uploadPlaylog(File zip_file) throws MalformedURLException {

        HashMap<String, String> post = new HashMap<>();
        post.put("siteid", DefaultPref.getSiteId());
        post.put("type", "1");

        String upload_url = ServerUrlPref.getUploadServerUrl() + API_POPKEEPER_UPLOAD_PLAY_COUNT + DefaultPref.getAkey();

        URL request_url = new URL(upload_url);

        return uploadLog(request_url, zip_file, post);
    }

    private boolean uploadTouchlog(File zip_file) throws MalformedURLException {

        HashMap<String, String> post = new HashMap<>();
        post.put("siteid", DefaultPref.getSiteId());
        post.put("type", "0");

        String upload_url = ServerUrlPref.getUploadServerUrl() + API_POPKEEPER_UPLOAD_FILE + DefaultPref.getAkey();

        URL request_url = new URL(upload_url);

        return uploadLog(request_url, zip_file, post);
    }

    private boolean uploadLog(URL request_url, File zip_file, HashMap<String, String> post){
        boolean ret = false;
        Uploader uploader = makeUploader();

        try{
//            String request_result = uploader.upload(request_url, post, "upload", zip_file);
            String request_result = uploader.uploadWithOkHttp(request_url, post, "upload", zip_file);
            ApiResultObject xml_result = ApiResultParser.parseResultXml(request_result);
            if(ApiResultObject.STATUS_OK.equals(xml_result.status) && xml_result.code == 0){
                ret = true;
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
        return ret;
    }

    private void outputTerminalLogFile(File out_file) {
        LogDbHelper dbh = null;
        try (FileWriter fw = new FileWriter(out_file)) {
            // ログファイルヘッダ出力
            // 出力日付
//            fw.append("AdmintPlayer Log ");
            fw.append("SignagePlayer Log ");
            fw.append(DateUtil.convDateTimeFormal(System.currentTimeMillis()));
            fw.append("\n");

            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
            // version
            fw.append("version:");
            fw.append(packageInfo.versionName);
            fw.append("\n");

            // siteid
            String siteid = DefaultPref.getSiteId();
            fw.append("siteid:");
            fw.append(siteid);
            fw.append("\n");

            // terminalid
            String terminalid = DefaultPref.getTerminalId();
            fw.append("terminalid:");
            fw.append(terminalid);
            fw.append("\n");

            // akey
            String akey = DefaultPref.getAkey();
            fw.append("akey:");
            fw.append(akey);
            fw.append("\n\n");

            // DBのログを出力
            String sql = "select " +
                    LogDbHelper.TABLE_LOG.DATETIME + "," +
                    LogDbHelper.TABLE_LOG.TYPE + "," +
                    LogDbHelper.TABLE_LOG.TAG + "," +
                    LogDbHelper.TABLE_LOG.MSG +
                    " from " + LogDbHelper.TABLE_LOG.getName() +
                    " order by " + LogDbHelper.TABLE_LOG._ID + " desc";

            dbh = new LogDbHelper(this);
            SQLiteDatabase rdb = dbh.getReaderDb();

            try (Cursor cursor = rdb.rawQuery(sql, null)) {
                // カーソル取得
                while (cursor.moveToNext()) {
                    StringBuffer set_text = new StringBuffer();
                    set_text.append("[");
                    set_text.append(cursor.getString(0));
                    set_text.append("]");
                    int type = Integer.parseInt(cursor.getString(1));

                    if (type == LoggingService.LOG_TYPE_NOTICE) {
                        set_text.append(" NOTICE ");
                    } else if (type == LoggingService.LOG_TYPE_ERROR) {
                        set_text.append(" ERROR ");
                    } else if (type == LoggingService.LOG_TYPE_INFO) {
                        set_text.append(" INFO ");
                    } else {
                        set_text.append(" ");
                    }
                    set_text.append("[");
                    set_text.append(cursor.getString(2));
                    set_text.append("]");

                    set_text.append(cursor.getString(3));
                    set_text.append("\n");

                    fw.append(set_text);
                }
            }
            fw.flush();
        }catch (Exception e){
            Logging.stackTrace(e);
        }finally {
            if (dbh != null) {
                dbh.close();
            }
        }
    }

    private void outputSettingInfo(File out_file) {
        try(FileWriter fw = new FileWriter(out_file)){

//            fw.append("AdmintPlayer Setting Information ");
            fw.append("SignagePlayer Setting Information ");
            fw.append(DateUtil.convDateTimeFormal(System.currentTimeMillis()));
            fw.append("\n");

            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
            fw.append("version:");
            fw.append(packageInfo.versionName);
            fw.append("\n");

            String siteid = DefaultPref.getSiteId();
            fw.append("siteid:");
            fw.append(siteid);
            fw.append("\n");

            String terminalid = DefaultPref.getTerminalId();
            fw.append("terminalid:");
            fw.append(terminalid);
            fw.append("\n");

            String akey = DefaultPref.getAkey();
            fw.append("akey:");
            fw.append(akey);
            fw.append("\n");

            boolean is_net_service = DefaultPref.getNetworkService();
            fw.append("Net Service:");
            if(is_net_service) {
                fw.append("ON");
            } else {
                fw.append("OFF");
            }
            fw.append("\n");

            boolean is_boot_start = DefaultPref.getBootStart();
            fw.append("Boot Start:");
            if(is_boot_start) {
                fw.append("ON");
            } else {
                fw.append("OFF");
            }
            fw.append("\n");

            String user_storage = DefaultPref.getUserStorage();
            fw.append("User Storage Path:");
            fw.append(user_storage);
            fw.append("\n");

            boolean use_extra_storage = DefaultPref.getUseExtraStorage();
            fw.append("Use Extra Storage:");
            if(use_extra_storage) {
                fw.append("ON");
            } else {
                fw.append("OFF");
            }
            fw.append("\n");

            String path = DefaultPref.getSdcardDrive();
            fw.append("Extra Storage Path:");
            fw.append(path);
            fw.append("\n");

            boolean wifi_reset = DefaultPref.getWiFiReset();
            fw.append("WiFi Reset:");
            if(wifi_reset) {
                fw.append("ON");
            } else {
                fw.append("OFF");
            }
            fw.append("\n");

            boolean is_use_proxy;
            fw.append("Proxy Enabled:");
            is_use_proxy = DefaultPref.getProxyEnable();
            if(is_use_proxy) {
                fw.append("ON");
            } else {
                fw.append("OFF");
            }
            fw.append("\n");

            if(is_use_proxy) {
                String host = DefaultPref.getProxyHost();
                fw.append("Host:");
                fw.append(host);
                fw.append("\n");

                String port = DefaultPref.getProxyPort();
                fw.append("Port:");
                fw.append(port);
                fw.append("\n");

                String user = DefaultPref.getProxyUser();
                fw.append("User Name:");
                fw.append(user);
                fw.append("\n");

                String password = DefaultPref.getProxyPassword();
                fw.append("Password:");
                for(int i = 0; i < password.length(); i++) {
                    fw.append("*");
                }
                fw.append("\n");
            } else {
                fw.append("Host:");
                fw.append("N/A");
                fw.append("\n");

                fw.append("Port:");
                fw.append("N/A");
                fw.append("\n");

                fw.append("User Name:");
                fw.append("N/A");
                fw.append("\n");

                fw.append("Password:");
                fw.append("N/A");
                fw.append("\n");
            }

            boolean is_output_notice_log = DefaultPref.getNoticeLog();
            fw.append("Output Notice Log:");
            if(is_output_notice_log) {
                fw.append("ON");
            } else {
                fw.append("OFF");
            }
            fw.append("\n");

            boolean is_anr_reboot = DefaultPref.getCheckAnrReboot();
            fw.append("Check ANR Reboot:");
            if(is_anr_reboot) {
                fw.append("ON");
            } else {
                fw.append("OFF");
            }
            fw.append("\n");

        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    void copyAnrFile(File out_file){
        try{
            File anr_file = getAnrTracesFile();
            if(anr_file.isFile()){
                FileUtil.copyTransfer(anr_file, out_file);
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }
}

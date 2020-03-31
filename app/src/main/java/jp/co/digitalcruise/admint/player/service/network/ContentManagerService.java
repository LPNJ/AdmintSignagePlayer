package jp.co.digitalcruise.admint.player.service.network;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.file.AdmintPath;
import jp.co.digitalcruise.admint.player.component.file.ContentFile;
import jp.co.digitalcruise.admint.player.component.file.FileUtil;
import jp.co.digitalcruise.admint.player.component.file.ZipUtil;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.log.NetLog;
import jp.co.digitalcruise.admint.player.component.netutil.ContentInfo;
import jp.co.digitalcruise.admint.player.component.netutil.Downloader;
import jp.co.digitalcruise.admint.player.component.object.PlaylistObject;
import jp.co.digitalcruise.admint.player.component.parsexml.parser.TouchContentParser;
import jp.co.digitalcruise.admint.player.db.ErrorDbHelper;
import jp.co.digitalcruise.admint.player.db.PlaylistDbHelper;
import jp.co.digitalcruise.admint.player.db.ScheduleDbHelper;
import jp.co.digitalcruise.admint.player.db.ViewlistDbHelper;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.pref.HealthCheckPref;
import jp.co.digitalcruise.admint.player.pref.ServerUrlPref;
import jp.co.digitalcruise.admint.player.service.LoggingService;
import jp.co.digitalcruise.admint.player.service.PlaylistService;

import static jp.co.digitalcruise.admint.player.BuildConfig.APPLICATION_ID;

public class ContentManagerService extends AbstractNetworkService {
    private static final String ACTION_PREFIX = APPLICATION_ID + ".ContentManagerService.";

    // スケジュールされたコンテンツファイルダウンロード
    public static final String ACTION_DOWNLOAD_CONTENTS = ACTION_PREFIX + "DOWNLOAD_CONTENTS";

    // 拡張ストレージ読み込み
    public static final String ACTION_LOAD_EXTERNAL_STORAGE = ACTION_PREFIX + "LOAD_EXTERNAL_STORAGE";

    //スタンドアロン
    public static final String ACTION_LOAD_STAND_ALONE_SCHEDULE= ACTION_PREFIX + "STAND_ALONE_SCHEDULE";

    // 利用予定のないコンテンツファイル削除
    public static final String ACTION_DELETE_UNUSE_CONTENTS = ACTION_PREFIX + "DELETE_UNUSE_CONTENTS";

    // 利用予定のない外部コンテンツファイル削除
    public static final String ACTION_DELETE_UNUSE_EXTERNAL_CONTENTS = ACTION_PREFIX + "DELETE_UNUSE_EXTERNAL_CONTENTS";

    // プレイヤー起動時
    public static final String ACTION_PLAYER_LAUNCH = ACTION_PREFIX + "PLAYER_LAUNCH";

    // コンテンツ・素材ファイルクリア
    public static final String ACTION_CLEAR_CONTENTS = ACTION_PREFIX + "CLEAR_CONTENTS";

    // スケジュールタイプ
    public static final String INTENT_EXTRA_SCHEDULE_TYPE = "extra_schedule_type";

    public String ZIP_EXTENSION = ".zip";

    // 未使用コンテンツ削除済みフラグ
    private boolean mIsUnuseFileDeleted = false;

    // 現在再生対象コンテンツダウンロード通知ネットログ送信済フラグ
    private boolean mIsNetLogNoEnoughSpace = false;

    // 今回のシーケンスでダウンロード成功したコンテンツ存在
    private boolean mIsDownloadedContent = false;

    private ErrorDbHelper mErrorDbHelper = null;
    private ScheduleDbHelper mScheduleDbHelper = null;
    private PlaylistDbHelper mPlaylistDbHelper = null;
    private ViewlistDbHelper mViewlistDbHelper = null;

    public ContentManagerService() {
        super(ContentManagerService.class.getName());
    }

    public ContentManagerService(String name) {
        super(name);
    }

    private class DownloadFailedException extends RuntimeException {
        private long mTargetLength = 0;
        private long mDownloadedLength = 0;

        private DownloadFailedException(long target_len, long downloaded_len) {
            mTargetLength = target_len;
            mDownloadedLength = downloaded_len;
        }

        private long getTargetLength() {
            return mTargetLength;
        }

        private long getDownloadedLength() {
            return mDownloadedLength;
        }
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            if (intent == null || intent.getAction() == null) {
                return;
            }

            final String action = intent.getAction();

            Logging.notice(action);

            mErrorDbHelper = null;
            mScheduleDbHelper = null;
            mPlaylistDbHelper = null;
            mViewlistDbHelper = null;

            try {
                // DBオブジェクトは定常的につかうのでメンバ変数として生成
                // DB Helper生成
                mErrorDbHelper = new ErrorDbHelper(this);
                mScheduleDbHelper = new ScheduleDbHelper(this);
                mPlaylistDbHelper = new PlaylistDbHelper(this);
                mViewlistDbHelper = new ViewlistDbHelper(this);

                if (ACTION_DOWNLOAD_CONTENTS.equals(action)) {
                    mIsUnuseFileDeleted = false;
                    int sched_type = intent.getIntExtra(INTENT_EXTRA_SCHEDULE_TYPE, HealthCheckService.SCHED_TYPE_HEALTH_CHECK);
                    actionDownloadContents(sched_type);
                } else if (ACTION_LOAD_EXTERNAL_STORAGE.equals(action)) {
                    mIsUnuseFileDeleted = false;
                    actionLoadExternalStorage();
                } else if (ACTION_CLEAR_CONTENTS.equals(action)) {
                    actionClearContents();
                } else if (ACTION_DELETE_UNUSE_CONTENTS.equals(action)) {
                    actionDeleteUnuseContents();
                } else if(ACTION_DELETE_UNUSE_EXTERNAL_CONTENTS.equals(action)){
                    actionDeleteUnuseExternalContents();
                } else if (ACTION_PLAYER_LAUNCH.equals(action)){
                    actionPlayerLaunch();
                } else if(ACTION_LOAD_STAND_ALONE_SCHEDULE.equals(action)){
                    mIsUnuseFileDeleted = false;
                    actionLoadStandAloneSchedule();
                }
            } catch (Exception e) {
                // ロギング
                Logging.stackTrace(e);
            } finally {
                if (mErrorDbHelper != null) {
                    mErrorDbHelper.close();
                }

                if (mScheduleDbHelper != null) {
                    mScheduleDbHelper.close();
                }

                if (mPlaylistDbHelper != null) {
                    mPlaylistDbHelper.close();
                }

                if (mViewlistDbHelper != null) {
                    mViewlistDbHelper.close();
                }
            }
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
    }

    private void setAlarmUnuseExternalContents(){
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if(am != null){
            Intent intent = new Intent(this, LoggingService.class);
            intent.setAction(ACTION_DELETE_UNUSE_EXTERNAL_CONTENTS);
            PendingIntent pintent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

//            am.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis() + AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, pintent);
            am.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, pintent);
        }else{
            throw new RuntimeException("(AlarmManager)getSystemService return null");
        }
    }

    private void actionPlayerLaunch(){
        try{
            // 利用予定のない外部コンテンツを削除
            deleteUnusedExternalFiles();

            // リピートアラーム設定
            setAlarmUnuseExternalContents();
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionDeleteUnuseContents() {
        try {
            deleteUnuseFiles();
            sendToast(getString(R.string.toast_msg_delete_unused_contents_success));

        } catch (Exception e) {
            Logging.stackTrace(e);
            sendToast(getString(R.string.toast_msg_delete_unused_contents_error));
        }
    }

    private void actionDeleteUnuseExternalContents() {
        try {
            deleteUnusedExternalFiles();
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
    }

    private void actionClearContents() {
        try {
            // エラーDBクリア
//            clearErrorDb();

            // キャッシュディレクトリクリア
            FileUtil.deleteRecursive(AdmintPath.getTemporaryDir());

            // 素材ディレクトリクリア
            FileUtil.deleteRecursive(AdmintPath.getMaterialsDir());

            // コンテンツディレクトリクリア
            FileUtil.deleteRecursive(AdmintPath.getContentsDir());

            Logging.info(getString(R.string.log_info_deleted_all_contents));
            sendToast(getString(R.string.toast_msg_clear_contents_success));

        } catch (Exception e) {
            Logging.stackTrace(e);
            sendToast(getString(R.string.toast_msg_clear_contents_error));
        }
    }

//    private void clearErrorDb() {
//        SQLiteDatabase wdb = mErrorDbHelper.getWriterDb();
//        wdb.delete(ErrorDbHelper.TABLE_BLACK_LIST.getName(), null, null);
//        wdb.delete(ErrorDbHelper.TABLE_RE_DOWNLOAD.getName(), null, null);
//        wdb.delete(ErrorDbHelper.TABLE_VIEW_ERROR.getName(), null, null);
//    }

    private void actionLoadExternalStorage() {
        try {
            // 現在時刻
            long from_time_db = System.currentTimeMillis();

            // 展開対象のコンテンツID群(重複ID除外)取得
            LinkedHashMap<Integer, Integer> target_ids = listSchedulingContent(HealthCheckService.SCHED_TYPE_SD_CARD, from_time_db);
            // デフォルトコンテンツコンテンツID群(重複ID除外)取得
            LinkedHashMap<Integer, Integer> default_ids = listSchedulingContent(HealthCheckService.SCHED_TYPE_DEFAULT, 0);

            // リストをマージ
            for(Map.Entry<Integer, Integer> entry : default_ids.entrySet()){
                target_ids.put(entry.getKey(), entry.getValue());
            }

            // 拡張ストレージのコンテンツを展開
            extendExternalStorageContents(target_ids);

            sendToast(getString(R.string.toast_msg_sdcard_success));

            // プレイヤー起動
            intentPlaylistPlayerLaunch();

            // プレイリスト更新
            intentPlaylistRefreshPlaylist();

        } catch (Exception e) {
            Logging.stackTrace(e);
            sendToast(getString(R.string.toast_msg_sdcard_error));
        }
    }

    private void actionLoadStandAloneSchedule() {
        try {
            // 現在時刻
            long from_time_db = System.currentTimeMillis();

            // 展開対象のコンテンツID群(重複ID除外)取得
            LinkedHashMap<Integer, Integer> target_ids = listSchedulingContent(HealthCheckService.SCHED_TYPE_SD_CARD, from_time_db);
            // デフォルトコンテンツコンテンツID群(重複ID除外)取得
            LinkedHashMap<Integer, Integer> default_ids = listSchedulingContent(HealthCheckService.SCHED_TYPE_DEFAULT, 0);

            // リストをマージ
            for(Map.Entry<Integer, Integer> entry : default_ids.entrySet()){
                target_ids.put(entry.getKey(), entry.getValue());
            }

            // 拡張ストレージのコンテンツを展開
            extendStandAloneContents(target_ids);

            Intent intent = new Intent(this, PlaylistService.class);
            intent.setAction(PlaylistService.ACITON_SHOW_DIALOG);
            startService(intent);
//            sendToast(getString(R.string.toast_msg_stand_alone_success));

            // プレイヤー起動
            intentPlaylistPlayerLaunch();

            // プレイリスト更新
            intentPlaylistRefreshPlaylist();

        } catch (Exception e) {
            Logging.stackTrace(e);
            sendToast(getString(R.string.toast_msg_stand_alone_error));
        }
    }

    private void extendExternalStorageContents(LinkedHashMap<Integer, Integer> target_ids) throws IOException {
        final String CONTENTS_DIR = "contents";

        File sd_dir = AdmintPath.getSdDir();
        if(sd_dir == null){
            throw new RuntimeException("AdmintPath.getSdDir() return null");
        }

        File load_dir = new File(sd_dir.getAbsolutePath() + File.separator + AdmintPath.DIR_ADMINT_PLAYER + File.separator + CONTENTS_DIR);
        if (!load_dir.isDirectory()) {
            // ロギング
            return;
        }

        for (Map.Entry<Integer, Integer> entry : target_ids.entrySet()) {
            int id = entry.getKey();
            int t = entry.getValue();

            switch (t) {
                case HealthCheckService.CONTENT_TYPE_TOUCH:
                case HealthCheckService.CONTENT_TYPE_ORIGINAL:
                    File ready_file = ContentFile.getTouchContentReady(id);
                    // 準備完了ファイルが存在
                    if (ready_file.isFile()) {
                        continue;
                    }

                    // id.zipを展開
                    extendLocalNormalContent(load_dir, id);

                    // list.xmlが存在する
                    if (ContentFile.getTouchListFile(id).isFile()) {
                        extendLocalMaterial(load_dir, id);
                    }

                    // 素材が全てそろった
                    if (isTouchContentReady(id)) {
                        // 準備完了ファイル作成
                        FileUtil.createFile(ready_file);
                    }

                    break;
                case HealthCheckService.CONTENT_TYPE_EXTERNAL_MOVIE:
                case HealthCheckService.CONTENT_TYPE_EXTERNAL_PICTURE:
                case HealthCheckService.CONTENT_TYPE_WEBVIEW:
                    // デイリーは無視（ありえない）
                    // Webコンテンツは展開ファイルなし
                    break;
                default:
                    // 通常コンテンツ
                    extendLocalNormalContent(load_dir, id);
                    break;
            }
        }
    }

    private void extendStandAloneContents(LinkedHashMap<Integer, Integer> target_ids) throws IOException {
        final String CONTENTS_DIR = "contents";

        File sd_dir = AdmintPath.getAplicationDir();

        File load_dir = new File(sd_dir.getAbsolutePath() + File.separator + CONTENTS_DIR);
        if (!load_dir.isDirectory()) {
            // ロギング
            return;
        }

        for (Map.Entry<Integer, Integer> entry : target_ids.entrySet()) {
            int id = entry.getKey();
            int t = entry.getValue();

            switch (t) {
                case HealthCheckService.CONTENT_TYPE_TOUCH:
                    File ready_file = ContentFile.getTouchContentReady(id);
                    // 準備完了ファイルが存在
                    if (ready_file.isFile()) {
                        continue;
                    }

                    // id.zipを展開
                    extendLocalNormalContent(load_dir, id);

                    // list.xmlが存在する
                    if (ContentFile.getTouchListFile(id).isFile()) {
                        extendLocalMaterial(load_dir, id);
                    }

                    // 素材が全てそろった
                    if (isTouchContentReady(id)) {
                        // 準備完了ファイル作成
                        FileUtil.createFile(ready_file);
                    }

                    break;
                case HealthCheckService.CONTENT_TYPE_EXTERNAL_MOVIE:
                case HealthCheckService.CONTENT_TYPE_EXTERNAL_PICTURE:
                case HealthCheckService.CONTENT_TYPE_WEBVIEW:
                    // デイリーは無視（ありえない）
                    // Webコンテンツは展開ファイルなし
                    break;
                default:
                    // 通常コンテンツ
                    extendLocalNormalContent(load_dir, id);
                    break;
            }
        }
    }

    private void extendLocalMaterial(File load_dir, int id) {
        try {

            File list_xml = ContentFile.getTouchListFile(id);
            String xml_str = FileUtil.loadFile(list_xml.getAbsolutePath());
            LinkedHashMap<String, Integer> material_list = TouchContentParser.parseListXml(xml_str);

            for (Map.Entry<String, Integer> material : material_list.entrySet()) {
                // 素材ファイルオブジェクト生成
                File material_file;
//                File extend_dir;
                if (material.getValue() == 1) {
                    // 素材ディレクトリ内のファイル
                    material_file = new File(AdmintPath.getMaterialsDir().getAbsolutePath() + File.separator + material.getKey());
//                    extend_dir = AdmintPath.getMaterialsDir();
                } else {
                    // 自ディレクトリ内のファイル
                    material_file = new File(ContentFile.getContentDir(id).getAbsolutePath() + File.separator + material.getKey());
//                    extend_dir = ContentFile.getContentDir(id);
                }

                // ファイルが存在
                if (material_file.isFile()) {
                    continue;
                }

                // ファイル名から拡張子を取ってマテリアル名を取得
                String material_name = ContentFile.splitFileExt(material.getKey());

                File zip_file = new File(load_dir.getAbsolutePath() + File.separator + material_name + ZIP_EXTENSION);

                // zipファイルが存在しない
                if (!zip_file.isFile()) {
                    // ロギング
                    Logging.info(getString(R.string.log_error_failed_extend_content) + " (zip_file = " + zip_file.getAbsolutePath() + ")");
                    sendToast(getString(R.string.toast_msg_sdcard_content_extend_error) + "\n" + zip_file.getName());
                    continue;
                }

                if (!isEnoughExpandingSpace(zip_file.length())) {
                    Logging.info(getString(R.string.log_info_no_enough_storage) + " (" + getLogStorageStatusStr() + ")");
                    sendToast(getString(R.string.toast_msg_sdcard_content_extend_skip) + "\n" + zip_file.getName());
                    continue;
                }

                File extend_dir = new File(AdmintPath.getTemporaryMaterialsDir().getAbsolutePath() + File.separator + material_name);

                // 展開
                boolean is_unzip_success = extendZip(zip_file, extend_dir);


                // 展開が成功しかつメディアファイルが存在
                File extend_material_file = ContentFile.getMediaFile(extend_dir);

                boolean is_complated = false;
                if (is_unzip_success && extend_material_file != null) {
                    // ロギング 展開成功
                    FileUtil.renameFile(extend_material_file, material_file);

                    if(material_file.isFile()){
                        is_complated = true;
                    }
                }
                FileUtil.deleteRecursive(extend_dir);

                if(is_complated){
                    // ロギング
                    Logging.info(getString(R.string.log_info_success_extend_content) + " (material_name = " + material_name + ")");
                    sendToast(getString(R.string.toast_msg_sdcard_content_extend_success) + "\n" + zip_file.getName());
                }else{
                    // ロギング
                    Logging.info(getString(R.string.log_error_failed_extend_content) + " (material_name = " + material_name + ")");
                    sendToast(getString(R.string.toast_msg_sdcard_content_extend_error) + "\n" + zip_file.getName());
                }
            }
        } catch (Exception e) {
            // ロギング
            Logging.stackTrace(e);
        }
    }

    private void extendLocalNormalContent(File load_dir, int id) {

        File id_dir = ContentFile.getContentDir(id);
        if (id_dir.isDirectory()) {
            // ディレクトリが存在
            return;
        }


        File zip_file = new File(load_dir.getAbsolutePath() + File.separator + id + ZIP_EXTENSION);
        if (!zip_file.isFile()) {
            // ロギング
            Logging.info(getString(R.string.log_error_failed_extend_content) + " (zip_path = " + zip_file.getAbsolutePath() + ")");
            sendToast(getString(R.string.toast_msg_sdcard_content_extend_error) + "\n" + id);
            return;
        }

        if (!isEnoughExpandingSpace(zip_file.length())) {
            if (!mIsUnuseFileDeleted) {
                // 不要コンテンツファイル削除
                deleteUnuseFiles();
                // 一度のダウンロードシーケンス中に不要コンテンツ削除を行うのは一回のみ
                mIsUnuseFileDeleted = true;

                // 再びストレージ空き容量チェック
                if (!isEnoughExpandingSpace(zip_file.length())) {
                    Logging.info(getString(R.string.log_info_no_enough_storage) + " (" + getLogStorageStatusStr() + ")");
                    sendToast(getString(R.string.toast_msg_sdcard_content_extend_skip) + "\n" + id);
                    return;
                }
            }
        }

        if(extendZip(zip_file, id_dir)){
            // ロギング
            Logging.info(getString(R.string.log_info_success_extend_content) + " (id = " + id + ")");
            sendToast(getString(R.string.toast_msg_sdcard_content_extend_success) + "\n id = " + id);
        }else{
            // ロギング
            Logging.info(getString(R.string.log_error_failed_extend_content) + " (id = " + id + ")");
            sendToast(getString(R.string.toast_msg_sdcard_content_extend_error) + "\n id = " + id);
        }
    }

    private long getLatestScheduleTimestamp(int sched_type) {
        // 直近のスケジュール取得
        long latest_timestamp = 0;
        if (sched_type == HealthCheckService.SCHED_TYPE_HEALTH_CHECK) {
            latest_timestamp = HealthCheckPref.getLatestHealthCheckTime();
        } else if (sched_type == HealthCheckService.SCHED_TYPE_AHEAD_LOAD) {
            latest_timestamp = HealthCheckPref.getLatestHealthCheckTime();
        }
        return latest_timestamp;
    }

    private void actionDownloadContents(int sched_type) {

        // ネットワークサービスがOFFの時
        if(!DefaultPref.getNetworkService()){
            return;
        }

        try {
            // 直近のスケジュール取得
            long target_timestamp = getLatestScheduleTimestamp(sched_type);

            // viewerエラー処理
            deleteViewerrorContentsFile();

            // 現在時刻
            long from_time_db = System.currentTimeMillis();

            //緊急配信のダウンロード対象のコンテンツID群取得
            LinkedHashMap<Integer, Integer> emergency_ids = listEmergencyContent();

            // 対象スケジュールタイプのダウンロード対象のコンテンツID群(重複ID除外)取得
            LinkedHashMap<Integer, Integer> target_ids = listSchedulingContent(sched_type, from_time_db);

            // デフォルトコンテンツのダウンロード対象のコンテンツID群(重複ID除外)取得
            LinkedHashMap<Integer, Integer> default_ids = listSchedulingContent(HealthCheckService.SCHED_TYPE_DEFAULT, 0);

            // リストをマージ
            for (Map.Entry<Integer, Integer> entry : default_ids.entrySet()) {
                target_ids.put(entry.getKey(), entry.getValue());
            }

            // ダウンロード開始ネットログ
            netlogDownloading(sched_type, target_ids);
            isNeedDownloadRescueContent(emergency_ids);

            // ファイル群ダウンロード実行
            excecuteEmergencyContentDownload(emergency_ids);
            executeDownload(sched_type, target_ids, target_timestamp);
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
    }

    private void netlogDownloading(int sched_type, LinkedHashMap<Integer, Integer> target_ids){

        LinkedHashMap<Integer, Integer> downloading_ids = makeDownloadingContentIds(sched_type, target_ids);

        if(downloading_ids.isEmpty()){
           return;
        }

        boolean is_only_external = true;
        StringBuilder str_ids = new StringBuilder();

        for (Map.Entry<Integer, Integer> entry : downloading_ids.entrySet()) {
            int id = entry.getKey();
            int t = entry.getValue();

            // デイリーコンテンツ以外
            if(t != HealthCheckService.CONTENT_TYPE_EXTERNAL_MOVIE && t != HealthCheckService.CONTENT_TYPE_EXTERNAL_PICTURE){
                is_only_external = false;
            }
            str_ids.append(id);
            str_ids.append(",");
        }

        String str_list = "";
        if(str_ids.length() > 0) {
            str_list = "(" + getString(R.string.net_log_notice_content_id) + str_ids.substring(0, str_ids.length() - 1) + ")";
        }

        if(is_only_external){
            NetLog.notice(getString(R.string.net_log_notice_external_content_download) + str_list);
        }else{
            NetLog.notice(getString(R.string.net_log_notice_content_download) + str_list);
        }
    }

    private void isNeedDownloadRescueContent(LinkedHashMap<Integer, Integer> r_list){
        if(r_list == null){
            return;
        }
        StringBuilder download_content_list = new StringBuilder();
        for(Map.Entry<Integer, Integer> entry : r_list.entrySet()){
            int id = entry.getKey();
            if(ContentFile.getContentMediaFile(id) == null){
                download_content_list.append(id);
                download_content_list.append(",");
            }
        }

        String str_list;
        if(download_content_list.length() > 0){
            str_list = "(" + getString(R.string.net_log_notice_rescue_content_id) + download_content_list.substring(0, download_content_list.length() - 1) + ")";
            NetLog.notice(getString(R.string.net_log_notice_rescue_content_download) + str_list);
        }
    }

    private LinkedHashMap<Integer, Integer> makeDownloadingContentIds(int sched_type, LinkedHashMap<Integer, Integer> target_ids){

        LinkedHashMap<Integer, Integer> downloading_ids = new LinkedHashMap<>();

        for (Map.Entry<Integer, Integer> entry : target_ids.entrySet()) {
            int id = entry.getKey();
            int t = entry.getValue();
            switch (t) {
                case HealthCheckService.CONTENT_TYPE_TOUCH:
                case HealthCheckService.CONTENT_TYPE_ORIGINAL:
                    // タッチコンテンツ
                    // 準備完了ファイルが存在せず、かつデータファイルも存在しない
                    if(!ContentFile.getTouchContentReady(id).isFile() && !ContentFile.getTouchDataFile(id).isFile()){
                        downloading_ids.put(id, t);
                    }
                    break;
                case HealthCheckService.CONTENT_TYPE_EXTERNAL_MOVIE:
                case HealthCheckService.CONTENT_TYPE_EXTERNAL_PICTURE:
                    // デイリーコンテンツ
                    LinkedHashMap<Integer, String> external_list = listDownloadExternal(sched_type, id);

                    // 一つでもダウンロードを行うデイリーコンテンツファイルが存在する
                    if(existDownloadingExternal(id, external_list)){
                        downloading_ids.put(id, t);
                    }

                    break;
                case HealthCheckService.CONTENT_TYPE_WEBVIEW:
                    // Webコンテンツはダウンロードなし
                    break;
                default:
                    // 通常コンテンツ
                    // メディアファイルが存在しない
                    if(ContentFile.getContentMediaFile(id) == null){
                        downloading_ids.put(id, t);
                    }
                    break;
            }
        }

        return downloading_ids;
    }

    private boolean existDownloadingExternal(int id, LinkedHashMap<Integer, String> external_list){
        for (Map.Entry<Integer, String> entry : external_list.entrySet()) {
            int f_id = entry.getKey();
            File media_file = ContentFile.getExternalMediaFile(id, f_id);

            if(media_file == null){
                return true;
            }
        }
        return false;
    }

    private void excecuteEmergencyContentDownload(LinkedHashMap<Integer, Integer> emergency_ids){
        final String URI_RSC_EMERGENCY_CONTENTS = "rscfiles";
        // CDNサーバかDeliveryサーバURLを返す
        String select_server_url = getRscDownloadServer();
        if (select_server_url == null) {
            // どちらかのサーバURLが未定義ということは通信に一度も成功していない
            // ロギング
            Logging.error(getString(R.string.log_error_no_setting_download_server_url));
            return;
        }

        String rsc_server_url = select_server_url + URI_RSC_EMERGENCY_CONTENTS + File.separator;

        // ネットログ済フラグ 容量不足
        mIsNetLogNoEnoughSpace = false;
        // ダウンロードコンテンツが一つでも存在フラグ
        mIsDownloadedContent = false;

        int count = 0;
        int id = 0;
        for (Map.Entry<Integer, Integer> entry : emergency_ids.entrySet()) {
            // ダウンロード処理中にスケジュールの再取得が発生した
//            if (target_timestamp > 0 && target_timestamp != getLatestScheduleTimestamp(HealthCheckService.SCHED_TYPE_HEALTH_CHECK)) {
//                // ロギング
//                Logging.info(getString(R.string.log_info_change_schedule_donwloading));
//                break;
//            }

            try {
                id = entry.getKey();
                int t = entry.getValue();

                downloadRescueContent(rsc_server_url, id, resque_list.get(count));

                count++;
            } catch (Exception e) {
                Logging.stackTrace(e);
            }
        }
        if(mIsDownloadedContent){
            intentPlaylistDownloadContent(id);
        }
    }

    private void executeDownload(int sched_type, LinkedHashMap<Integer, Integer> target_ids, long target_timestamp) {
        final String URI_APFILES = "apfiles";
        final String URI_CONTENTS = "contents";
        final String URI_EXTERNAL_CONTENTS = "extfiles";

        // CDNサーバかDeliveryサーバURLを返す
        String select_server_url = getDownloadServer();
        if (select_server_url == null) {
            // どちらかのサーバURLが未定義ということは通信に一度も成功していない
            // ロギング
            Logging.error(getString(R.string.log_error_no_setting_download_server_url));
            return;
        }

        String download_server_url = select_server_url + URI_APFILES + "/" + DefaultPref.getSiteId() + "/" + URI_CONTENTS + "/";

        // デイリーコンテンツサーバ
        // 利用しないサイト環境によっては未定義かもしれない
        String external_server_url = ServerUrlPref.getExternalServerUrl() + URI_EXTERNAL_CONTENTS + "/";

        // ネットログ済フラグ 容量不足
        mIsNetLogNoEnoughSpace = false;
        // ダウンロードコンテンツが一つでも存在フラグ
        mIsDownloadedContent = false;

        for (Map.Entry<Integer, Integer> entry : target_ids.entrySet()) {
            // ダウンロード処理中にスケジュールの再取得が発生した
            if (target_timestamp > 0 && target_timestamp != getLatestScheduleTimestamp(sched_type)) {
                // ロギング
                Logging.info(getString(R.string.log_info_change_schedule_donwloading));
                break;
            }

            try {
                int id = entry.getKey();
                int t = entry.getValue();

                switch (t) {
                    case HealthCheckService.CONTENT_TYPE_TOUCH:
                    case HealthCheckService.CONTENT_TYPE_ORIGINAL:
                        // タッチコンテンツ
                        downloadTouchCountent(download_server_url, sched_type, id);
                        break;
                    case HealthCheckService.CONTENT_TYPE_EXTERNAL_MOVIE:
                    case HealthCheckService.CONTENT_TYPE_EXTERNAL_PICTURE:
                        // デイリーコンテンツ
                        downloadExternalContent(external_server_url, sched_type, id);
                        break;
                    case HealthCheckService.CONTENT_TYPE_WEBVIEW:
                        // Webコンテンツはダウンロードなし
                        break;
                    default:
                        // 通常コンテンツ
                        downloadNormalContent(download_server_url, sched_type, id);
                        break;
                }

            } catch (Exception e) {
                Logging.stackTrace(e);
            }
        }

        if(mIsDownloadedContent){
            intentPlaylistDownloadContent(0);
        }
    }

    private boolean extendZip(File zip, File output_dir) {
        final String CONTENT_EXTENDING_SUFFIX = ".extending";
        File extending_dir = new File(output_dir.getAbsolutePath() + CONTENT_EXTENDING_SUFFIX);

        // zipを展開
        try {
            // 展開中ディレクトリが残っていれば削除
            if (extending_dir.exists()) {
                FileUtil.deleteRecursive(extending_dir);
            }

            // 展開中ディレクトリ削除
            FileUtil.makeDir(extending_dir);

            // unzip
            ZipUtil.unZip(zip, extending_dir);

            // 展開後ディレクトリがあれば削除
            FileUtil.deleteRecursive(output_dir);

            // ディレクトリをリネーム
            FileUtil.renameFile(extending_dir, output_dir);

            return true;
        } catch (Exception e) {
            Logging.stackTrace(e);
            return false;
        }
    }

    private void deleteUnusedExternalFiles(){
        try {
            long start_time = System.currentTimeMillis();

            // playlist db writeロック
            mPlaylistDbHelper.getWriterDb().beginTransactionNonExclusive();

            // 利用予定中コンテンツID群取得
            LinkedHashMap<Integer, Integer> using_ids = listEnableContent();

            // 利用予定中デイリーID群取得
            LinkedHashMap<Integer, LinkedHashMap<Integer, String>> using_externals = listEnableExternal(using_ids);

            // 利用していないデイリーコンテンツ(f_id)削除
            deleteUnuseExternalFidFiles(using_externals);

            Logging.notice("deleteUnusedExternalFiles() processing time:" + (System.currentTimeMillis() - start_time));

            String storage_status = getLogStorageStatusStr();
            Logging.info(getString(R.string.log_info_deleted_unused_external_contents) + " (" + storage_status + ")");
        } finally {
            // writeロック解除
            mPlaylistDbHelper.getWriterDb().endTransaction();
        }
    }

    private void deleteUnuseFiles() {
        try {
            long start_time = System.currentTimeMillis();

            // playlist db writeロック
            mPlaylistDbHelper.getWriterDb().beginTransactionNonExclusive();

            // 利用予定中コンテンツID群取得
            LinkedHashMap<Integer, Integer> using_ids = listEnableContent();

            // 利用予定中デイリーID群取得
            LinkedHashMap<Integer, LinkedHashMap<Integer, String>> using_externals = listEnableExternal(using_ids);

            // 利用中素材名取得
            LinkedHashMap<String, String> using_materials = listEnableMaterial(using_ids);

            // 利用していないキャッシュ削除
            deleteUnuseCacheFiles(using_ids, using_externals, using_materials);

            // 利用していないコンテンツ削除
            deleteUnuseContentFiles(using_ids);

            // 利用していないデイリーコンテンツ削除
            deleteUnuseExternalFidFiles(using_externals);

//            // 利用していないコンテンツおよびデイリーファイル削除
//            deleteUnuseContentAndExternalFiles(using_ids, using_externals);

            // 利用していない素材削除
            deleteUnuseMaterialFiles(using_materials);

            Logging.notice("deleteUnuseFiles() processing time:" + (System.currentTimeMillis() - start_time));

            String storage_status = getLogStorageStatusStr();
            Logging.info(getString(R.string.log_info_deleted_unused_contents) + " (" + storage_status + ")");
            NetLog.notice(getString(R.string.net_log_notice_delete_unused_content) + " (" + storage_status + ")");
        } finally {
            // writeロック解除
            mPlaylistDbHelper.getWriterDb().endTransaction();
        }
    }

    private void deleteUnuseContentFiles(final LinkedHashMap<Integer, Integer> using_ids) {
        File contents_dir = AdmintPath.getContentsDir();
        File[] content_files = contents_dir.listFiles();

        // コンテンツディレクトリを走査
        for (File content : content_files) {
            if (content.isDirectory()) {
                int id = convContentId(content.getName());
                if (id > 0) {
                    // ディレクトリから取得したIDが利用予定リストに存在するか
                    Integer find = using_ids.get(id);
                    // 利用予定コンテンツファイルリストに存在しない
                    if (find == null) {
                        FileUtil.deleteRecursive(content);
                    }
                } else {
                    // コンテンツディレクトリにID名以外のファイルが混ざっている
                    // 展開失敗時のゴミディレクトリなので削除
                    Logging.info(getString(R.string.log_info_delete_non_id_dir) + " (dir = " + content.getName() + ")");
                    FileUtil.deleteRecursive(content);
                }
            }else if(content.isFile()){
                // contentsディレクトリ以下にはファイルはそもそも存在しないはずだが
                FileUtil.deleteFile(content);
            }
        }
    }

    private void deleteUnuseExternalFidFiles(final LinkedHashMap<Integer, LinkedHashMap<Integer, String>> using_externals) {
        File contents_dir = AdmintPath.getContentsDir();
        File[] content_files = contents_dir.listFiles();

        // コンテンツディレクトリを走査
        for (File content : content_files) {
            if (content.isDirectory()) {
                int id = convContentId(content.getName());
                if (id > 0) {
                    // 利用予定コンテンツファイルリストに存在しない
                    LinkedHashMap<Integer, String> external = using_externals.get(id);
                    // コンテンツIDに紐づいたデイリーが存在
                    if (external != null) {
                        // デイリーファイルを個別に削除
                        deleteContentExternalFiles(content, external);
                    }
                } else {
                    // コンテンツディレクトリにID名以外のファイルが混ざっている
                    // 展開失敗時のゴミディレクトリなので削除
                    Logging.info(getString(R.string.log_info_delete_non_id_dir) + " (dir = " + content.getName() + ")");
                    FileUtil.deleteRecursive(content);
                }
            }
        }
    }

    private void deleteUnuseMaterialFiles(LinkedHashMap<String, String> using_materials) {

        File material_dir = AdmintPath.getMaterialsDir();
        File[] material_files = material_dir.listFiles();

        // 素材ディレクトリを走査
        for (File material : material_files) {
            if (material.isFile()) {
                // ファイル名が利用予定の素材リストにない
                if (!using_materials.containsKey(material.getName())) {
                    FileUtil.deleteFile(material);
                }
            }
        }
    }

    private void deleteContentExternalFiles(File content_dir, final LinkedHashMap<Integer, String> in_externals) {

        File[] external_files = content_dir.listFiles();

        // コンテンツIDディレクトリを走査
        for (File external : external_files) {
            if (external.isDirectory()) {
                int f_id = convInt(external.getName());
                if (f_id > 0) {
                    String find = in_externals.get(f_id);
                    // サブIDが利用予定リストに存在しない
                    if (find == null) {
                        // デイリーファイル（サブディレクトリ）を削除
                        FileUtil.deleteRecursive(external);
                    }
                } else {
                    // サブディレクトリにID名以外のファイルが混ざっている
                    // おそらく展開失敗時のゴミディレクトリ
                    // 展開失敗時のゴミディレクトリなので削除
                    Logging.info(getString(R.string.log_info_delete_non_id_dir) + " (dir = " + external.getName() + ")");
                    FileUtil.deleteRecursive(external);
                }
            }else if(external.isFile()){
                // デイリーコンテンツディレクトリ直下にファイルは存在しないはず（全てディレクトリ）だが
                FileUtil.deleteFile(external);
            }
        }
    }

//    private void deleteUnuseExternalCacheFiles(final SparseArray<SparseArray<String>> target_externals){
//        File temp_dir = AdmintPath.getTemporaryDir();
//        File[] cache_files = temp_dir.listFiles();
//
//        for (File cache : cache_files) {
//            if (cache.isFile()) {
//                // キャッシュファイルがデイリーでかつ利用予定がなければ削除
//                checkAndDeleteUnuseExternalCache(target_externals, cache);
//            }
//        }
//    }

    private void deleteUnuseCacheFiles(final LinkedHashMap<Integer, Integer> using_ids, final LinkedHashMap<Integer, LinkedHashMap<Integer, String>> using_externals, final LinkedHashMap<String, String> using_materials) {

        File temp_dir = AdmintPath.getTemporaryDir();
        File[] cache_files = temp_dir.listFiles();

        for (File cache : cache_files) {

            if(cache.isDirectory()){
                if(AdmintPath.DIR_TEMP_CONTENTS.equals(cache.getName())){
                    checkAndDeleteUnuseContentsCache(using_ids);
                }else if(AdmintPath.DIR_TEMP_EXTERNALS.equals(cache.getName())){
                    checkAndDeleteUnuseExternalsCache(using_externals);
                }else if(AdmintPath.DIR_TEMP_MATERIALS.equals(cache.getName())){
                    checkAndDeleteUnuseMaterialsCache(using_materials);
                }else{
                    FileUtil.deleteRecursive(cache);
                }
            }else if(cache.isFile()){
                FileUtil.deleteFile(cache);
            }
        }
    }

    private void checkAndDeleteUnuseMaterialsCache(final LinkedHashMap<String, String> using_materials) {
        File temp_dir = AdmintPath.getTemporaryMaterialsDir();
        File[] cache_files = temp_dir.listFiles();

        for (File cache : cache_files) {

            if(cache.isFile()) {
                // 利用予定のある素材リストに存在しない
                if (!using_materials.containsValue(exclusionZipAndCacheExtension(cache.getName()))) {
                    // 利用予定がないので削除
                    FileUtil.deleteFile(cache);
                }
            }else if(cache.isDirectory()){
                FileUtil.deleteRecursive(cache);
            }
        }
    }

    private void checkAndDeleteUnuseContentsCache(final LinkedHashMap<Integer, Integer> using_ids) {

        File temp_dir = AdmintPath.getTemporaryContentsDir();
        File[] cache_files = temp_dir.listFiles();

        for (File cache : cache_files) {
            Boolean is_delete = true;
            if (cache.isFile()) {
                int content_id = convContentId(exclusionZipAndCacheExtension(cache.getName()));
                // リストに存在する時は利用予定あり
                Integer find = using_ids.get(content_id);
                if (find != null) {
                    is_delete = false;
                }
            }

            if (is_delete) {
                if(cache.isFile()) {
                    FileUtil.deleteFile(cache);
                }else if(cache.isDirectory()){
                    FileUtil.deleteRecursive(cache);
                }
            }
        }
    }

    private boolean existExternalFname(LinkedHashMap<Integer, String > fids, String fname){
        for(Map.Entry<Integer, String> entry : fids.entrySet()){
            if(fname.equals(entry.getValue())){
                return true;
            }
        }
        return false;
    }

    private void checkAndDeleteUnuseExternalsCache(final LinkedHashMap<Integer, LinkedHashMap<Integer, String>> using_externals) {

        File temp_dir = AdmintPath.getTemporaryExternalsDir();
        File[] cache_files = temp_dir.listFiles();

        for (File cache : cache_files) {
            Boolean is_delete = true;

            if (cache.isFile()) {
//                for (int i = 0; i < using_externals.size(); i++) {
                for(Map.Entry<Integer, LinkedHashMap<Integer, String>> entry : using_externals.entrySet()){
                    // 利用予定のあるデイリーコンテンツリストに存在する時はループを抜ける（削除しない）
                    String check_file = exclusionZipAndCacheExtension(cache.getName());
                    if (existExternalFname(entry.getValue(), check_file)) {
                        is_delete = false;
                        break;
                    }
                }
            }

            if (is_delete) {
                if(cache.isFile()) {
                    FileUtil.deleteFile(cache);
                }else if(cache.isDirectory()){
                    FileUtil.deleteRecursive(cache);
                }
            }
        }
    }

//    private boolean checkAndDeleteUnuseExternalCache(final SparseArray<SparseArray<String>> target_externals, File cache) {
//        boolean is_find = false;
//        for (int i = 0; i < target_externals.size(); i++) {
//            // 利用予定のあるデイリーコンテンツリストに存在する時はループを抜ける（削除しない）
//            Long f_id = convExternalFId(exclusionZipAndCacheExtension(cache.getName()));
//            if(f_id > 0) {
//                if (target_externals.valueAt(i).indexOfValue(f_id.toString()) > 0) {
//                    is_find = true;
//                    break;
//                }
//            }
//        }
//        if (is_find) {
//            return false;
//        } else {
//            // リストにみつからなかったので削除
//            FileUtil.deleteFile(cache);
//            return true;
//        }
//    }

    private int convContentId(String val) {
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            // Intに変換できない場合はコンテンツIDではない
            return 0;
        }
    }

//    private long convExternalFId(String val) {
//        try {
//            return Long.parseLong(val);
//        } catch (NumberFormatException e) {
//            // Intに変換できない場合はコンテンツIDではない
//            return 0;
//        }
//    }

    private String exclusionZipAndCacheExtension(String file_name) {
        final String EXCLUSION_STRING = ZIP_EXTENSION + Downloader.TMP_EXTENSION + "$";
        final Pattern EXCLUSION_PATTERN = Pattern.compile(EXCLUSION_STRING);

        Matcher matcher = EXCLUSION_PATTERN.matcher(file_name);
        return matcher.replaceAll("");
    }


    private void downloadTouchMaterials(String base_url, int id) throws IOException, NoSuchAlgorithmException, XmlPullParserException {

        File list_xml = ContentFile.getTouchListFile(id);

        // リストXML（素材ファイル）が存在
        if (list_xml.isFile()) {
            // XML パース
            String xml_str = FileUtil.loadFile(list_xml.getAbsolutePath());
            LinkedHashMap<String, Integer> material_list = TouchContentParser.parseListXml(xml_str);

            for (Map.Entry<String, Integer> material : material_list.entrySet()) {
                // 素材ファイルオブジェクト生成
                File material_file;
                if (material.getValue() == 1) {
                    // 素材ディレクトリ内のファイル
                    material_file = new File(AdmintPath.getMaterialsDir().getAbsolutePath() + File.separator + material.getKey());
                } else {
                    // 自ディレクトリ内のファイル
                    material_file = new File(ContentFile.getContentDir(id).getAbsolutePath() + File.separator + material.getKey());
                }

                // ファイルが存在
                if (material_file.isFile()) {
                    continue;
                }

                // ファイル名から拡張子を取ってマテリアル名を取得
                String material_name = ContentFile.splitFileExt(material.getKey());

                // ダウンロード
                File extended_file = downloadTouchMaterial(base_url, id, material_name);

                // ダウンロードと展開成功
                if (extended_file != null) {
                    // メディアファイルを移動
                    FileUtil.renameFile(extended_file, material_file);
                    // 展開後ディレクトリ（ファイル移動後の空ディレクトリ）削除
                    FileUtil.deleteRecursive(extended_file.getParentFile());
                }
            }
        }
    }

    private ArrayList<String> getNoExistMaterials(int id) {
        ArrayList<String> no_exist_materials = new ArrayList<>();

        try {
            File list_xml = ContentFile.getTouchListFile(id);

            // リストXML（素材ファイル）が存在
            if (list_xml.isFile()) {
                // XML パース
                String xml_str = FileUtil.loadFile(list_xml.getAbsolutePath());
                LinkedHashMap<String, Integer> material_list = TouchContentParser.parseListXml(xml_str);

                for (Map.Entry<String, Integer> material : material_list.entrySet()) {
                    File material_file;
                    if (material.getValue() == 1) {
                        // 素材ディレクトリ内のファイル
                        material_file = new File(AdmintPath.getMaterialsDir().getAbsolutePath() + File.separator + material.getKey());
                    } else {
                        // 自ディレクトリ内のファイル
                        material_file = new File(ContentFile.getContentDir(id).getAbsolutePath() + File.separator + material.getKey());
                    }

                    // 存在しないファイルをリストに追加
                    if (!material_file.isFile()) {
                        no_exist_materials.add(material_file.getName());
                    }
                }
            }
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
        return no_exist_materials;
    }

    private boolean isTouchContentReady(int id) {

        boolean exist_all_material = false;

        try {
            File list_xml = ContentFile.getTouchListFile(id);

            // リストXML（素材ファイル）が存在
            if (list_xml.isFile()) {
                // XML パース
                String xml_str = FileUtil.loadFile(list_xml.getAbsolutePath());
                LinkedHashMap<String, Integer> material_list = TouchContentParser.parseListXml(xml_str);

                // list_xmlが存在するのに素材が登録されていない
                if(material_list.size() == 0){
                    Logging.error(getString(R.string.log_error_no_exist_material_entry));
                    return false;
                }

                // 全てダウンロード済かディレクトリを再走査
                exist_all_material = true;
                for (Map.Entry<String, Integer> material : material_list.entrySet()) {
                    File material_file;
                    if (material.getValue() == 1) {
                        // 素材ディレクトリ内のファイル
                        material_file = new File(AdmintPath.getMaterialsDir().getAbsolutePath() + File.separator + material.getKey());
                    } else {
                        // 自ディレクトリ内のファイル
                        material_file = new File(ContentFile.getContentDir(id).getAbsolutePath() + File.separator + material.getKey());
                    }

                    // 一つでも素材が存在しなければfalse
                    if (!material_file.isFile()) {
                        exist_all_material = false;
                        break;
                    }
                }
            } else {
                exist_all_material = true;
            }
        } catch (Exception e) {
            exist_all_material = false;
            Logging.stackTrace(e);
        }
        return exist_all_material;
    }

    private void downloadExternalContent(String base_url, int sched_type, int id) throws MalformedURLException {
        // ダウンロード対象のデイリー群
        LinkedHashMap<Integer, String> external_list = listDownloadExternal(sched_type, id);

        // 展開先コンテンツディレクトリ
        File content_dir = ContentFile.getContentDir(id);
        if (!content_dir.isDirectory()) {
            // 存在しなければ作成
            FileUtil.makeDir(content_dir);
        }

        for (Map.Entry<Integer, String> entry : external_list.entrySet()) {

            int f_id = entry.getKey();

            // ブラックリストに登録されている
            if (isBlacklist(id, f_id, "")) {
                Logging.info(getString(R.string.log_info_skip_download_in_blacklist_content) + " (id = " + id + ", f_id = " + f_id + ")");
                continue;
            }

            // デイリーファイル取得
            File external_file = ContentFile.getExternalMediaFile(id, f_id);
            // デイリーファイル存在
            if (external_file != null) {
                continue;
            }

            // URL生成
            URL url = new URL(base_url + makeExternalUri(entry.getValue()));

            // キャッシュファイルサイズ取得（ログ用）
            String cache_file_name = Downloader.getCacheFileName(url);
            long cache_size = getCacheFileSize(cache_file_name, AdmintPath.getTemporaryExternalsDir());

            // ダウンロード
            File zip_file = null;
            try {
                // ロギング
                loggingDownloadStart(id, f_id, "", cache_size, 0);
                zip_file = download(url, AdmintPath.getTemporaryExternalsDir());
            } catch (DownloadFailedException e) {
                // ダウンロードエラー
                loggingDownloadError(id, f_id, "", e.getDownloadedLength(), 0);
                continue;
            }

            // 空き容量なし
            if (zip_file == null) {
                // ダウンロードエラー
                loggingDownloadError(id, f_id, "", cache_size, 0);
                continue;
            }

            // zipファイルサイズ取得（ログ用）
            long zip_file_size = zip_file.length();

            // 展開先ディレクトリ生成（サブディレクトリ）
            File sub_dir = new File(content_dir.getAbsolutePath() + File.separator + f_id);

            // 展開
            boolean is_unzip_success = extendZip(zip_file, sub_dir);

            // zipファイル削除
            FileUtil.deleteFile(zip_file);

            // 展開が成功しかつメディアファイルが存在
            if (is_unzip_success && ContentFile.getExternalMediaFile(id, f_id) != null) {
                loggingDownloadSuccess(id, f_id, "", zip_file_size, 0);

                // 現プレイリストコンテンツの時はダウンロード通知
                checkAndIntentPlaylistContent(sched_type, id);

                mIsDownloadedContent = true;
            } else {
                // ダウンロードリトライを検討
                updateReDownload(id, f_id, "");
                // ロギング
                loggingExtendError(id, f_id, "");
            }
        }
    }

    private void downloadTouchCountent(String base_url, int sched_type, int id) throws IOException, NoSuchAlgorithmException, XmlPullParserException {

        File ready_file = ContentFile.getTouchContentReady(id);

        // タッチコンテンツ再生準備ファイルが存在
        if (ready_file.isFile()) {
            return;
        }

        // ブラックリストに登録されている
        if (isBlacklist(id, 0, "")) {
            // ロギング
            Logging.info(getString(R.string.log_info_skip_download_in_blacklist_content) + " (id=" + id + ")");
            return;
        }

        // タッチデータファイルが存在しない（未ダウンロード）
        if (!ContentFile.getTouchDataFile(id).isFile()) {
            // URL生成
            URL url = new URL(base_url + makeContentUri(id));

            // キャッシュファイルサイズ取得（ログ用）
            String cache_file_name = Downloader.getCacheFileName(url);
            long cache_size = getCacheFileSize(cache_file_name, AdmintPath.getTemporaryContentsDir());

            File zip_file = null;
            // ダウンロード
            try {
                // ロギング
                loggingDownloadStart(id, 0, "", cache_size, 0);
                zip_file = download(url, AdmintPath.getTemporaryContentsDir());
            } catch (DownloadFailedException e) {
                // ダウンロードエラー
                loggingDownloadError(id, 0, "", e.getDownloadedLength(), 0);
                return;
            }

            // 空き容量なし
            if (zip_file == null) {
                // ダウンロードエラー
                loggingDownloadError(id, 0, "", cache_size, 0);
                return;
            }

            // zipファイルサイズ取得（ログ用）
            long zip_file_size = zip_file.length();

            // 展開
            boolean is_unzip_success = extendZip(zip_file, ContentFile.getContentDir(id));

            // zipファイル削除
            FileUtil.deleteFile(zip_file);

            // 展開が成功しかつメディアファイルが存在
            if (is_unzip_success && ContentFile.getTouchDataFile(id).isFile()) {
                loggingDownloadSuccessTouchContent(id, zip_file_size, getNoExistMaterials(id));

                // 現プレイリストコンテンツの時はダウンロード通知
                checkAndIntentPlaylistContent(sched_type, id);
            } else {
                // ダウンロードリトライを検討
                updateReDownload(id, 0, "");
                // ロギング
                loggingExtendError(id, 0, "");

                // 本体のダウンロードに失敗してるので素材リストも取得できてるはずはなく素材のダウンロード処理をスキップ
                return;
            }
        }

        // 素材ファイル群ダウンロード
        downloadTouchMaterials(base_url, id);

        if (isTouchContentReady(id)) {
            // 再生準備OK
            FileUtil.createFile(ready_file);
            mIsDownloadedContent = true;
        }

    }

//    private boolean isPlaylistContent(int sched_type, int id) {
//        long cur_st = currentProgramSt(sched_type);
//
//        if(cur_st > 0) {
//            String where = ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.ST + " = " + cur_st + " and " +
//                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.ID + " = " + id;
//            long count = DatabaseUtils.queryNumEntries(mPlaylistDbHelper.getReaderDb(), ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName(), where);
//            if (count > 0) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private long currentProgramSt(int sched_type) {
//        long cur_st = 0;
//        long cur_time = System.currentTimeMillis();
//
//        String where = " where " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.SCHED_TYPE + " = " + sched_type + ", " +
//                ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST + " <= " + cur_time + " and " +
//                "(" + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST + " + " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.PT + ") > " + cur_time;
//        String order = " order by " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST + " asc ";
//
//        String sql = "select " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST +
//                " from " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.getName() +
//                where +
//                order +
//                " limit 1";
//
//        SQLiteDatabase rdb = mScheduleDbHelper.getReaderDb();
//        try (Cursor cursor = rdb.rawQuery(sql, null)) {
//            if (cursor.moveToNext()) {
//                cur_st = cursor.getLong(0);
//            }
//        }
//        return cur_st;
//    }

    private boolean isPlaylistContent(int id){
        String where = PlaylistDbHelper.TABLE_PLAY_LIST.ID + " = " + id + " or " +
                PlaylistDbHelper.TABLE_PLAY_LIST.TCID + " = " + id;
        // 対象のコンテンツが現在再生中（プレイリスト）コンテンツに存在するか
        long count = DatabaseUtils.queryNumEntries(mPlaylistDbHelper.getReaderDb(), PlaylistDbHelper.TABLE_PLAY_LIST.getName(), where);

        if(count > 0){
            return true;
        }

        return false;
    }

//    private File searchMaterialFile(File dir, String find) {
//        if (dir.isDirectory()) {
//            File[] files = dir.listFiles();
//            if (files != null) {
//                for (File file : files) {
//                    if (file.getName().equals(find)) {
//                        return new File(file.getAbsolutePath());
//                    }
//                }
//            }
//        }
//        return null;
//    }

    private long getCacheFileSize(String file_name, File temp_dir) {
        File cache_file = new File(temp_dir.getAbsolutePath() + File.separator + file_name);

        if (cache_file.isFile()) {
            return cache_file.length();
        } else {
            return 0;
        }
    }
    private void downloadRescueContent(String base_url, int id, String rescue_file) throws MalformedURLException {
        // コンテンツメディアファイルが存在しない（ダウンロード対象）
        if (ContentFile.getContentMediaFile(id) == null) {
            // 対象コンテンツがブラックリスト入りしている
            if (isBlacklist(id, 0, "")) {
                // ロギング
                Logging.info(getString(R.string.log_info_skip_download_in_blacklist_content) + " (id = " + id + ")");
                return;
            }

            // URL生成
            URL url = new URL(base_url + makeExternalUri(rescue_file));

            // キャッシュファイルサイズ取得（ログ用）
            String cache_file_name = Downloader.getCacheFileName(url);
            long cache_size = getCacheFileSize(cache_file_name, AdmintPath.getTemporaryContentsDir());

            File zip_file;
            // ダウンロード
            try {
                // ロギング
                loggingDownloadStart(id, 0, "", cache_size, 1);
                zip_file = download(url, AdmintPath.getTemporaryContentsDir());
            } catch (DownloadFailedException e) {
                // ダウンロードエラー
                loggingDownloadError(id, 0, "", e.getDownloadedLength(), 1);
                return;
            }

            // 空き容量なし
            if (zip_file == null) {
                // ダウンロードエラー
                loggingDownloadError(id, 0, "", cache_size, 1);
                return;
            }

            // zipファイルサイズ取得（ログ用）
            long zip_file_size = zip_file.length();

            // 展開
            boolean is_unzip_success = extendZip(zip_file, ContentFile.getContentDir(id));

            // zipファイル削除
            FileUtil.deleteFile(zip_file);

            // 展開が成功しかつメディアファイルが存在
            if (is_unzip_success && ContentFile.getContentMediaFile(id) != null) {
                // ロギング展開成功
                loggingDownloadSuccess(id, 0, "", zip_file_size, 1);

                // 現プレイリストコンテンツの時はダウンロード通知
                checkAndIntentPlaylistContent(HealthCheckService.SCHED_TYPE_HEALTH_CHECK, id);

                mIsDownloadedContent = true;
            } else {
                // ダウンロードリトライを検討
                updateReDownload(id, 0, "");
                // ロギング
                loggingExtendError(id, 0, "");
            }
        }
    }

    private void downloadNormalContent(String base_url, int sched_type, int id) throws NoSuchAlgorithmException, MalformedURLException {
        // コンテンツメディアファイルが存在しない（ダウンロード対象）
        if (ContentFile.getContentMediaFile(id) == null) {
            // 対象コンテンツがブラックリスト入りしている
            if (isBlacklist(id, 0, "")) {
                // ロギング
                Logging.info(getString(R.string.log_info_skip_download_in_blacklist_content) + " (id = " + id + ")");
                return;
            }

            // URL生成
            URL url = new URL(base_url + makeContentUri(id));

            // キャッシュファイルサイズ取得（ログ用）
            String cache_file_name = Downloader.getCacheFileName(url);
            long cache_size = getCacheFileSize(cache_file_name, AdmintPath.getTemporaryContentsDir());

            File zip_file = null;
            // ダウンロード
            try {
                // ロギング
                loggingDownloadStart(id, 0, "", cache_size, 0);
                zip_file = download(url, AdmintPath.getTemporaryContentsDir());
            } catch (DownloadFailedException e) {
                // ダウンロードエラー
                loggingDownloadError(id, 0, "", e.getDownloadedLength(), 0);
                return;
            }

            // 空き容量なし
            if (zip_file == null) {
                // ダウンロードエラー
                loggingDownloadError(id, 0, "", cache_size, 0);
                return;
            }

            // zipファイルサイズ取得（ログ用）
            long zip_file_size = zip_file.length();

            // 展開
            boolean is_unzip_success = extendZip(zip_file, ContentFile.getContentDir(id));

            // zipファイル削除
            FileUtil.deleteFile(zip_file);

            // 展開が成功しかつメディアファイルが存在
            if (is_unzip_success && ContentFile.getContentMediaFile(id) != null) {
                // ロギング展開成功
                loggingDownloadSuccess(id, 0, "", zip_file_size, 0);

                // 現プレイリストコンテンツの時はダウンロード通知
                checkAndIntentPlaylistContent(sched_type, id);

                mIsDownloadedContent = true;
            } else {
                // ダウンロードリトライを検討
                updateReDownload(id, 0, "");
                // ロギング
                loggingExtendError(id, 0, "");
            }
        }
    }

    private void checkAndIntentPlaylistContent(int sched_type, int id){
        // ヘルスチェックのダウンロードの時のみ実行
        if(sched_type == HealthCheckService.SCHED_TYPE_HEALTH_CHECK) {
            // 現在放映予定のコンテンツのダウンロードが終わった時
            if (isPlaylistContent(id)) {
                // 現プレイリストのスケジュールタイプを取得
                int playing_schad_type = PlaylistObject.SCHEDULE_NOTHING;
                String sql = "select " + PlaylistDbHelper.TABLE_PLAY_LIST_INFO.SCHED_TYPE + " from " + PlaylistDbHelper.TABLE_PLAY_LIST_INFO.getName();
                SQLiteDatabase rdb = mPlaylistDbHelper.getReaderDb();
                try (Cursor cursor = rdb.rawQuery(sql, null)) {
                    if (cursor.moveToFirst()) {
                        playing_schad_type = cursor.getInt(0);
                    }
                }

                // 現プレイリストのスケジュールタイプがヘルスチェックの時、かつヘルスチェックのコンテンツダウンロードされた時
                if (playing_schad_type == HealthCheckService.SCHED_TYPE_HEALTH_CHECK) {
                    intentPlaylistDownloadContent(id);
                }
            }
        }
    }

    private void intentPlaylistDownloadContent(int id){
        Intent intent = new Intent(this, PlaylistService.class);
        intent.setAction(PlaylistService.ACTION_DOWNLOAD_CONTENT);
        intent.putExtra(PlaylistService.INTENT_EXTRA_DOWNLOAD_CONTENT_ID, id);
        startService(intent);
    }

    private void intentPlaylistPlayerLaunch(){
        Intent intent = new Intent(this, PlaylistService.class);
        intent.setAction(PlaylistService.ACTION_PLAYER_LAUNCH);
        startService(intent);
    }

    private void intentPlaylistRefreshPlaylist(){
        Intent intent = new Intent(this, PlaylistService.class);
        intent.setAction(PlaylistService.ACTION_REFRESH_PLAYLIST);
        startService(intent);
    }

    private File downloadTouchMaterial(String base_url, int id, String material_name) throws NoSuchAlgorithmException, MalformedURLException {
        // 対象コンテンツがブラックリスト入りしている
        if (isBlacklist(0, 0, material_name)) {
            // ロギング
            Logging.info(getString(R.string.log_info_skip_download_in_blacklist_content) + " (material_name = " + material_name + ")");
            return null;
        }

        // URL生成
        URL url = new URL(base_url + makeMaterialUri(id, material_name));

        // キャッシュファイルサイズ取得（ログ用）
        String cache_file_name = Downloader.getCacheFileName(url);
        long cache_size = getCacheFileSize(cache_file_name, AdmintPath.getTemporaryMaterialsDir());

        File zip_file = null;
        // ダウンロード
        try {
            // ロギング
            loggingDownloadStart(id, 0, material_name, cache_size, 0);
            zip_file = download(url, AdmintPath.getTemporaryMaterialsDir());
        } catch (DownloadFailedException e) {
            // ダウンロードエラー
            loggingDownloadError(id, 0, material_name, e.getDownloadedLength(), 0);
            return null;
        }

        // 空き容量なし
        if (zip_file == null) {
            // ロギング ダウンロードエラー
            loggingDownloadError(id, 0, material_name, cache_size, 0);
            return null;
        }

        // zipファイルサイズ取得（ログ用）
        long zip_file_size = zip_file.length();

        // 展開先ディレクトリ生成
//        File extend_dir = new File(AdmintPath.getMaterialsTempDir().getAbsolutePath() + File.separator + material_name);
        File extend_dir = new File(AdmintPath.getTemporaryMaterialsDir().getAbsolutePath() + File.separator + material_name);

        // 展開
        boolean is_unzip_success = extendZip(zip_file, extend_dir);

        // zipファイル削除
        FileUtil.deleteFile(zip_file);

        // 展開が成功しかつメディアファイルが存在
        File material_file = ContentFile.getMediaFile(extend_dir);
        if (is_unzip_success && material_file != null) {
            // ロギング 展開成功
            loggingDownloadSuccess(id, 0, material_name, zip_file_size, 0);
        } else {
            // ダウンロードリトライを検討
            updateReDownload(id, 0, material_name);
            // ロギング
            loggingExtendError(id, 0, material_name);
            return null;
        }
        return material_file;
    }

//    private File downloadAndExpandIdZip(URL url, File output_dir) {
//
//        File zip_file = null;
//        // ダウンロード実行
//        zip_file = download(url);
//
//        // 通常 Exceptionがdownloadからthrowされているはず
//        if(zip_file == null){
//            Logging.error(getString(R.string.log_error_download_unexpected));
//            return null;
//        }
//
//        // ダウンロード成功したら圧縮ファイルを展開
//        return  extendZip(zip_file, output_dir);
//    }

    private void updateReDownload(int id, int f_id, @NonNull String material_name) {
        final int DOWNLOAD_RETRY_MAX = 2;

        SQLiteDatabase wdb = mErrorDbHelper.getWriterDb();

        String where = ErrorDbHelper.TABLE_RE_DOWNLOAD.ID + " = " + id +
                " and " + ErrorDbHelper.TABLE_RE_DOWNLOAD.F_ID + " = " + f_id +
                " and " + ErrorDbHelper.TABLE_RE_DOWNLOAD.MATERIAL_NAME + " = " + DatabaseUtils.sqlEscapeString(material_name);

        // ダウンロードリトライ回数取得
        String sql = "select " + ErrorDbHelper.TABLE_RE_DOWNLOAD.RETRY_COUNT +
                " from " + ErrorDbHelper.TABLE_RE_DOWNLOAD.getName() +
                " where " + where;

        int retry_count = -1;
        try (Cursor cursor = wdb.rawQuery(sql, null)) {
            if (cursor.moveToNext()) {
                retry_count = cursor.getInt(0);
            }
        }

        // リトライではない
        if (retry_count == -1) {
            return;
        }

        // リトライカウントアップ
        if (retry_count < DOWNLOAD_RETRY_MAX) {
            ContentValues values = new ContentValues();
            values.put(ErrorDbHelper.TABLE_RE_DOWNLOAD.ID, id);
            values.put(ErrorDbHelper.TABLE_RE_DOWNLOAD.F_ID, f_id);
            values.put(ErrorDbHelper.TABLE_RE_DOWNLOAD.MATERIAL_NAME, material_name);
            values.put(ErrorDbHelper.TABLE_RE_DOWNLOAD.RETRY_COUNT, retry_count + 1);
            wdb.replace(ErrorDbHelper.TABLE_RE_DOWNLOAD.getName(), null, values);
        } else {
            // ブラックリスト入り
            // re_donwloadテーブルからレコード削除
            wdb.delete(ErrorDbHelper.TABLE_RE_DOWNLOAD.getName(), where, null);

            // black_listテーブルにレコード追加
            ContentValues values = new ContentValues();
            values.put(ErrorDbHelper.TABLE_BLACK_LIST.ID, id);
            values.put(ErrorDbHelper.TABLE_BLACK_LIST.F_ID, f_id);
            values.put(ErrorDbHelper.TABLE_BLACK_LIST.MATERIAL_NAME, material_name);
            wdb.replace(ErrorDbHelper.TABLE_BLACK_LIST.getName(), null, values);

            // ロギング
            loggingBlackList(id, f_id, material_name);
        }
    }

    private void loggingBlackList(int id, int f_id, @NonNull String material_name) {
        // ロギング
        String target_info = "";
        if (material_name.length() > 0) {
            target_info = " (material = " + material_name + ")";
        } else if (f_id > 0) {
            target_info = " (  " + getString(R.string.net_log_notice_content_id) + id + ", f_id = " + f_id + ")";
        } else {
            target_info = " ( " + getString(R.string.net_log_notice_content_id) + id + ")";
        }
        NetLog.warning(getString(R.string.net_log_warning_black_list_content) + target_info);
        Logging.error(getString(R.string.log_error_black_list_content) + target_info);
    }

    private boolean isBlacklist(int id, int f_id, String material_name) {
        boolean ret = false;

        SQLiteDatabase rdb = mErrorDbHelper.getReaderDb();

        String where = ErrorDbHelper.TABLE_BLACK_LIST.ID + " = " + id +
                " and " + ErrorDbHelper.TABLE_BLACK_LIST.F_ID + " = " + f_id +
                " and " + ErrorDbHelper.TABLE_BLACK_LIST.MATERIAL_NAME + " = " + DatabaseUtils.sqlEscapeString(material_name);

        if (DatabaseUtils.queryNumEntries(rdb, ErrorDbHelper.TABLE_BLACK_LIST.getName(), where) > 0) {
            ret = true;
        }

        return ret;
    }

    private boolean prepareDownloadSpace(URL url) {
        boolean ret = false;

        // ダウンロード対象ファイルサイズ取得
        long content_length = getContentLength(url);

        // ストレージ空き容量チェック
        if (isEnoughExpandingSpace(content_length)) {
            ret = true;
        } else {
            if (!mIsUnuseFileDeleted) {

                // 不要コンテンツファイル削除
                deleteUnuseFiles();
                // 一度のダウンロードシーケンス中に不要コンテンツ削除を行うのは一回のみ
                mIsUnuseFileDeleted = true;

                // 再びストレージ空き容量チェック
                if (isEnoughExpandingSpace(content_length)) {
                    ret = true;
                }
            }
        }
        return ret;
    }

    private @Nullable
    String getDownloadServer() {
        // 取得する順番に注意
        String server_url = ServerUrlPref.getCdnServerUrl();
        if (!server_url.equals("")) {
            return server_url;
        }

        server_url = ServerUrlPref.getDeliveryServerUrl();
        if (!server_url.equals("")) {
            return server_url;
        }

        return null;
    }

    private @Nullable
    String getRscDownloadServer(){

        String server_url = ServerUrlPref.getRscServerUrl();
        if(!server_url.equals("")){
            return server_url;
        }

        server_url = ServerUrlPref.getDeliveryServerUrl();
        if (!server_url.equals("")) {
            return server_url;
        }
        return null;
    }

    private String makeMaterialUri(int id, String no_ext_filename) throws NoSuchAlgorithmException {
        String uri = makeDownloadDirectoryUri(DefaultPref.getSiteId(), id);
        return uri + appendExtensionZip(no_ext_filename) + mekeDownloadUrlSuffix();
    }

    private String mekeDownloadUrlSuffix() {
        String PARAM_AKEY = "?akey=";
        return PARAM_AKEY + DefaultPref.getAkey();
    }

    private String makeContentUri(int id) throws NoSuchAlgorithmException {
        String uri = makeDownloadDirectoryUri(DefaultPref.getSiteId(), id);
        return uri + appendExtensionZip(Integer.toString(id)) + mekeDownloadUrlSuffix();
    }

    private String makeExternalUri(String f_name) {
        return appendExtensionZip(f_name) + mekeDownloadUrlSuffix();
    }

    private String appendExtensionZip(String val) {
        return val + ZIP_EXTENSION;
    }

    private boolean isEnoughExpandingSpace(long zip_length) {
        boolean ret = false;
        final double ZIP_EXTEND_RATIO = 2.5f;

        long comp_size = (long) (zip_length * ZIP_EXTEND_RATIO);
        if (FileUtil.getAvailableMemorySize(AdmintPath.getAplicationDir()) > comp_size) {
            ret = true;
        }
        return ret;
    }

    private long getContentLength(URL url) {
        long content_length = 0;
        try {
            ContentInfo cinfo = makeContentInfo();
//            cinfo.execute(url);
            cinfo.executeContentInfo(url);
            content_length = cinfo.getContentLength();
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
        return content_length;
    }

    private @Nullable File download(URL url, File download_dir) {
        final int MAX_DOWNLOAD_RETRY = 3;
        File downloaded_file = null;

        // 空き容量チェック、空きがなければ不要ファイル実行を試みる
        if (prepareDownloadSpace(url)) {
            Downloader downloader = null;
            // 複数回試行
            for (int i = 0; i < MAX_DOWNLOAD_RETRY; i++) {
                try {
                    // ネットワーク状態をログ
                    loggingCheckActiveNetwork();

                    // ダウンロード時実行
                    downloader = makeDownloader();
//                    downloaded_file = downloader.download(url, download_dir);
                    downloaded_file = downloader.downloadWithOkHttp(url, download_dir);
                    if(downloaded_file.isFile()){
                        // ダウンロード完了
                        break;
                    }
                } catch (Exception e) {
                    Logging.network_error(e);
                    Logging.stackTrace(e);
                }
            }

            // 規定回数ダウンロードを実行したが失敗した時は例外を投げる
            if (downloader != null && downloaded_file == null) {
                throw new DownloadFailedException(downloader.getContentLength(), downloader.getCacheLength());
            }
        } else {
            // 一度のヘルスチェックで容量不足を通知するのは一度だけ（ログ表示圧縮のため）
            if(!mIsNetLogNoEnoughSpace) {
                // ストレージ容量不足
                String status = getLogStorageStatusStr();
                Logging.info(getString(R.string.log_info_no_enough_storage) + " (" + status + ")");
                NetLog.warning(getString(R.string.net_log_warning_no_enough_storage) + " (" + status + ")");
                mIsNetLogNoEnoughSpace = true;
            }
        }

        return downloaded_file;
    }



    private LinkedHashMap<Integer, String> listDownloadExternal(int sched_type, int id) {

        LinkedHashMap<Integer, String> external_list = new LinkedHashMap<>();

        String order = " order by " + ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.F_O + " asc";
        String where = " where sched_type = " + sched_type + " and id = " + id;
        String sql = "select " + ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.F_ID + ", " +
                ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.F_FNAME +
                " from " + ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.getName() +
                where + order;

        SQLiteDatabase rdb = mScheduleDbHelper.getReaderDb();
        try (Cursor cursor = rdb.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                external_list.put(cursor.getInt(0), cursor.getString(1));
            }
        }

        return external_list;
    }

    private LinkedHashMap<Integer, Integer> listEnableContent() {

        long from_time_db = System.currentTimeMillis();

        // スケジュールコンテンツ
        // ダウンロード対象のコンテンツID群(重複ID除外)取得
//        LinkedHashMap<Integer, Integer> target_ids = listSchedulingContent(-1, from_time_db);
//
//        // デフォルトコンテンツ
//        {
//            // デフォルトコンテンツのダウンロード対象のコンテンツID群(重複ID除外)取得
//            LinkedHashMap<Integer, Integer> default_ids = listSchedulingContent(HealthCheckService.SCHED_TYPE_DEFAULT, 0);
//
//            // リストをマージ
//            for (Map.Entry<Integer, Integer> entry : default_ids.entrySet()) {
//                target_ids.put(entry.getKey(), entry.getValue());
//            }
//        }

        // 全スケジュールコンテンツ（デフォルトコンテンツ含む）
        LinkedHashMap<Integer, Integer> target_ids = listAllSchedulingContent(from_time_db);

        // プレイリストコンテンツ
        {
            // デフォルトコンテンツのダウンロード対象のコンテンツID群(重複ID除外)取得
            LinkedHashMap<Integer, Integer> playlist_ids = listPlaylistContent();

            // リストをマージ
            for (Map.Entry<Integer, Integer> entry : playlist_ids.entrySet()) {
                target_ids.put(entry.getKey(), entry.getValue());
            }
        }

        // プレイリストのデフォルトコンテンツ
        {
            // デフォルトコンテンツのダウンロード対象のコンテンツID群(重複ID除外)取得
            LinkedHashMap<Integer, Integer> defaultlist_ids = listDefaultlistContent();
            // リストをマージ
            for (Map.Entry<Integer, Integer> entry : defaultlist_ids.entrySet()) {
                target_ids.put(entry.getKey(), entry.getValue());
            }
        }

        // ビューリストコンテンツ
        {
            // デフォルトコンテンツのダウンロード対象のコンテンツID群(重複ID除外)取得
            LinkedHashMap<Integer, Integer> viewlist_ids = listViewlistContent();
            // リストをマージ
            for (Map.Entry<Integer, Integer> entry : viewlist_ids.entrySet()) {
                target_ids.put(entry.getKey(), entry.getValue());
            }
        }

        return target_ids;
    }

    private LinkedHashMap<Integer, Integer> listPlaylistContent() {
        LinkedHashMap<Integer, Integer> target_ids = new LinkedHashMap<>();

        String sql = "select " + PlaylistDbHelper.TABLE_PLAY_LIST.ID + ", " +
                PlaylistDbHelper.TABLE_PLAY_LIST.T + ", " +
                PlaylistDbHelper.TABLE_PLAY_LIST.TCID +
                " from " + PlaylistDbHelper.TABLE_PLAY_LIST.getName();

        SQLiteDatabase rdb = mPlaylistDbHelper.getReaderDb();
        Cursor cursor = null;
        try {
            cursor = rdb.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                target_ids.put(cursor.getInt(0), cursor.getInt(1));
                int tcid = cursor.getInt(2);

                // タッチコンテンツが紐づいている
                if (tcid > 0) {
                    target_ids.put(tcid, HealthCheckService.CONTENT_TYPE_TOUCH);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }

        return target_ids;
    }

    private LinkedHashMap<Integer, Integer> listDefaultlistContent() {
        LinkedHashMap<Integer, Integer> target_ids = new LinkedHashMap<>();

        String sql = "select " + PlaylistDbHelper.TABLE_DEFAULT_LIST.ID + ", " +
                PlaylistDbHelper.TABLE_DEFAULT_LIST.T + ", " +
                PlaylistDbHelper.TABLE_DEFAULT_LIST.TCID +
                " from " + PlaylistDbHelper.TABLE_DEFAULT_LIST.getName();

        SQLiteDatabase rdb = mPlaylistDbHelper.getReaderDb();
        Cursor cursor = null;
        try {
            cursor = rdb.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                target_ids.put(cursor.getInt(0), cursor.getInt(1));
                int tcid = cursor.getInt(2);

                // タッチコンテンツが紐づいている
                if (tcid > 0) {
                    target_ids.put(tcid, HealthCheckService.CONTENT_TYPE_TOUCH);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }

        return target_ids;
    }

    private LinkedHashMap<Integer, Integer> listViewlistContent() {
        LinkedHashMap<Integer, Integer> target_ids = new LinkedHashMap<>();

        String sql = "select " + ViewlistDbHelper.TABLE_VIEW_LIST.ID + ", " +
                ViewlistDbHelper.TABLE_VIEW_LIST.T + ", " +
                ViewlistDbHelper.TABLE_VIEW_LIST.TCID +
                " from " + ViewlistDbHelper.TABLE_VIEW_LIST.getName();


        SQLiteDatabase rdb = mViewlistDbHelper.getReaderDb();

        Cursor cursor = null;
        try {
            cursor = rdb.rawQuery(sql, null);
            while (cursor.moveToNext()) {
                target_ids.put(cursor.getInt(0), cursor.getInt(1));
                int tcid = cursor.getInt(2);

                // タッチコンテンツが紐づいている
                if (tcid > 0) {
                    target_ids.put(tcid, HealthCheckService.CONTENT_TYPE_TOUCH);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }
        }

        return target_ids;
    }

    private @NonNull LinkedHashMap<String, String> listEnableMaterial(final LinkedHashMap<Integer, Integer> target_ids) {
        LinkedHashMap<String, String> material_list = new LinkedHashMap<>();

        for (Map.Entry<Integer, Integer> entry : target_ids.entrySet()) {
            // タッチコンテンツの時
            if (entry.getValue() == HealthCheckService.CONTENT_TYPE_TOUCH) {
                try {
                    File list_xml = ContentFile.getTouchListFile(entry.getKey());
                    // リストXML（素材ファイル）が存在
                    if (list_xml.isFile()) {
                        // XML パース
                        String xml_str = FileUtil.loadFile(list_xml.getAbsolutePath());
                        LinkedHashMap<String, Integer> local_material_list = TouchContentParser.parseListXml(xml_str);

                        // 素材ファイルが存在する場合は素材(共有するしない関係なく)を追加
                        for (Map.Entry<String, Integer> material : local_material_list.entrySet()) {
                            // キャッシュファイル削除時用に拡張子を除外したファイル名も値として設定
                            material_list.put(material.getKey(), ContentFile.splitFileExt(material.getKey()));
                        }
                    }
                } catch (Exception e) {
                    // タッチコンテンツXMLパースエラー等の例外で全体の処理が止まるのを防止
                    Logging.stackTrace(e);
                }
            }
        }

        return material_list;
    }

    private @NonNull LinkedHashMap<Integer, LinkedHashMap<Integer, String>> listEnableExternal(final LinkedHashMap<Integer, Integer> target_ids) {
        LinkedHashMap<Integer, LinkedHashMap<Integer, String>> external_list = new LinkedHashMap<>();
        SQLiteDatabase rdb = mScheduleDbHelper.getReaderDb();

        // コンテンツIDのリスト作成（引数のリストコピー）
        for (Map.Entry<Integer, Integer> entry : target_ids.entrySet()) {
            // デイリーコンテンツのみ追加
            if (entry.getValue() == HealthCheckService.CONTENT_TYPE_EXTERNAL_MOVIE || entry.getValue() == HealthCheckService.CONTENT_TYPE_EXTERNAL_PICTURE) {
                external_list.put(entry.getKey(), new LinkedHashMap<>());
            }
        }

        // 作成したリストにデイリーコンテンツ追加
        for (Map.Entry<Integer, LinkedHashMap<Integer, String>> entry : external_list.entrySet()) {
            String where = " where " + ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.ID + " = " + entry.getKey();
            String sql = "select " + ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.F_ID + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.F_FNAME +
                    " from " + ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.getName() +
                    where;
            try (Cursor cursor = rdb.rawQuery(sql, null)) {
                while (cursor.moveToNext()) {
                    entry.getValue().put(cursor.getInt(0), cursor.getString(1));
                }
            }
        }

        return external_list;
    }

    private @NonNull LinkedHashMap<Integer, Integer> listAllSchedulingContent(long from_time_db){
        LinkedHashMap<Integer, Integer> target_ids = new LinkedHashMap<>();

        // デフォルトコンテンツID群(重複ID除外)取得
        LinkedHashMap<Integer, Integer> default_ids = listSchedulingContent(HealthCheckService.SCHED_TYPE_DEFAULT, from_time_db);
        // リストをマージ
        for(Map.Entry<Integer, Integer> entry : default_ids.entrySet()){
            target_ids.put(entry.getKey(), entry.getValue());
        }

        // ヘルスチェックコンテンツID群(重複ID除外)取得
        LinkedHashMap<Integer, Integer> healthcheck_ids = listSchedulingContent(HealthCheckService.SCHED_TYPE_HEALTH_CHECK, from_time_db);
        // リストをマージ
        for(Map.Entry<Integer, Integer> entry : healthcheck_ids.entrySet()){
            target_ids.put(entry.getKey(), entry.getValue());
        }

        // 先行取得コンテンツID群(重複ID除外)取得
        LinkedHashMap<Integer, Integer> aheadload_ids = listSchedulingContent(HealthCheckService.SCHED_TYPE_AHEAD_LOAD, from_time_db);
        // リストをマージ
        for(Map.Entry<Integer, Integer> entry : aheadload_ids.entrySet()){
            target_ids.put(entry.getKey(), entry.getValue());
        }

        // 拡張ストレージコンテンツID群(重複ID除外)取得
        LinkedHashMap<Integer, Integer> sdcard_ids = listSchedulingContent(HealthCheckService.SCHED_TYPE_SD_CARD, from_time_db);
        // リストをマージ
        for(Map.Entry<Integer, Integer> entry : sdcard_ids.entrySet()){
            target_ids.put(entry.getKey(), entry.getValue());
        }

        return target_ids;
    }

    ArrayList<String> resque_list = new ArrayList<>();
    //緊急コンテンツ
    private @NonNull LinkedHashMap<Integer, Integer> listEmergencyContent() {
        LinkedHashMap<Integer, Integer> emergency_ids = new LinkedHashMap<>();

            SQLiteDatabase rdb = mScheduleDbHelper.getReaderDb();

            //content table
            {
                String order = " order by " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.O;
                String sql = "select " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.ID + ", " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.T  + ", "
                        + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.FILE_NAME +  " from " +
                        ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.getName() + order;

                try(Cursor cursor = rdb.rawQuery(sql, null)){
                    while (cursor.moveToNext()){
                        emergency_ids.put(cursor.getInt(0), cursor.getInt(1));
                        resque_list.add(cursor.getString(2));
                    }
                } catch (Exception e){
                    Logging.stackTrace(e);
                }
            }

        return emergency_ids;
    }

    private @NonNull LinkedHashMap<Integer, Integer> listSchedulingContent(@IntRange(from = 0) int sched_type, long from_time_db) {
        LinkedHashMap<Integer, Integer> target_ids = new LinkedHashMap<>();
        SQLiteDatabase rdb = mScheduleDbHelper.getReaderDb();

        // コンテンツテーブル
        {
            String where;
            if(sched_type == HealthCheckService.SCHED_TYPE_DEFAULT) {
            // デフォルトコンテンツ
                where = " where " + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.SCHED_TYPE + " = " + sched_type;
            }else{
             // それ以外のコンテンツ
                where = " where " + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.SCHED_TYPE + " = " + sched_type + " and " +
                        "(" + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.ST + " + " +
                        ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.PT + ") > " + from_time_db;
            }
            String join = " left join " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.getName() + " on (" +
                    ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST + " = " +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.ST + ") " +
                    " and (" +
                    ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.SCHED_TYPE + " = " +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.SCHED_TYPE + ")";

            String order = " order by " +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.ST + " asc, " +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.O + " asc";

            String sql = "select " +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.ID + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.T + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.TCID +
                    " from " + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() +
                    join +
                    where +
                    order;

            try (Cursor cursor = rdb.rawQuery(sql, null)) {
                while (cursor.moveToNext()) {
                    target_ids.put(cursor.getInt(0), cursor.getInt(1));
                    int tcid = cursor.getInt(2);

                    // タッチコンテンツが紐づいている
                    if (tcid > 0) {
                        target_ids.put(tcid, HealthCheckService.CONTENT_TYPE_TOUCH);
                    }
                }
            }
        }

        // プログラムに紐づくタッチコンテンツ
        {
            String where;
            if (sched_type == HealthCheckService.SCHED_TYPE_DEFAULT) {
            // デフォルトコンテンツ
                where = " where " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.SCHED_TYPE + " = " + sched_type;
            } else {
            // それ以外のコンテンツ
                where = " where " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.SCHED_TYPE + " = " + sched_type + " and " +
                        "(" + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST + " + " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.PT + ") > " + from_time_db + " and " +
                        ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.TCID + " > 0";
            }
            String order = " order by " +
                    ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST + " asc";
            String sql = "select " +
                    ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.TCID +
                    " from " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.getName() +
                    where +
                    order;
            try (Cursor cursor = rdb.rawQuery(sql, null)) {
                while (cursor.moveToNext()) {
                    int tcid = cursor.getInt(0);
                    if (tcid > 0) {
                        target_ids.put(cursor.getInt(0), HealthCheckService.CONTENT_TYPE_TOUCH);
                    }
                }
            }
        }

        return target_ids;
    }

//    private long getCountPlaylistRecord(int id, int f_id) {
//        String where = "(" + PlaylistDbHelper.TABLE_PLAY_LIST.ID + " = " + id + " and " +
//                PlaylistDbHelper.TABLE_PLAY_LIST.F_ID + " = " + f_id + ") or " +
//                PlaylistDbHelper.TABLE_PLAY_LIST.TCID + " = " + id;
//        return DatabaseUtils.queryNumEntries(mPlaylistDbHelper.getReadableDatabase(), PlaylistDbHelper.TABLE_PLAY_LIST.getName(), where);
//    }

    private long getCountViewlistRecord(int id, int f_id) {
        String where = "(" + ViewlistDbHelper.TABLE_VIEW_LIST.ID + " = " + id + " and " +
                ViewlistDbHelper.TABLE_VIEW_LIST.F_ID + " = " + f_id + ") or " +
                ViewlistDbHelper.TABLE_VIEW_LIST.TCID + " = " + id;
        return DatabaseUtils.queryNumEntries(mViewlistDbHelper.getReadableDatabase(), ViewlistDbHelper.TABLE_VIEW_LIST.getName(), where);
    }

    private void deleteViewerrorContentsFile() {
        SQLiteDatabase wdb = mErrorDbHelper.getWriterDb();

        String sql;
        sql = "select " +
                ErrorDbHelper.TABLE_VIEW_ERROR.ID + "," +
                ErrorDbHelper.TABLE_VIEW_ERROR.F_ID +
                " from " + ErrorDbHelper.TABLE_VIEW_ERROR.getName() +
                " where " + ErrorDbHelper.TABLE_VIEW_ERROR.RETRY_COUNT + " >= " + ErrorDbHelper.VIEW_ERROR_RETRY_MAX;

        try (Cursor cursor = wdb.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                int f_id = cursor.getInt(1);

                // 再生対象となっていない場合はファイル削除
//                if (getCountPlaylistRecord(id, f_id) == 0 && getCountViewlistRecord(id, f_id) == 0) {
                if (getCountViewlistRecord(id, f_id) == 0) {
                    if (f_id > 0 && !ContentFile.getExternalDir(id, f_id).isDirectory()) {
                        continue;
                    } else if (!ContentFile.getContentDir(id).isDirectory()) {
                        continue;
                    }

                    // ロギング
                    String target_info;
                    if (f_id > 0) {
                        target_info = "(id = " + id + ", f_id" + f_id + ")";
                    } else {
                        target_info = "(id = " + id + ")";
                    }

                    // 対象コンテンツディレクトリ削除
                    deleteContentFile(id, f_id);

                    Logging.info(getString(R.string.log_error_delete_fraudulent_content) + target_info);

                    {
                        // view_errorレコード削除
                        String where = ErrorDbHelper.TABLE_VIEW_ERROR.ID + " = " + id + " and " + ErrorDbHelper.TABLE_VIEW_ERROR.F_ID + " = " + f_id;
                        wdb.delete(ErrorDbHelper.TABLE_VIEW_ERROR.getName(), where, null);
                    }

                    // re_downloadレコードの存在チェック
                    boolean exist_redownload = false;
                    {
                        String where = ErrorDbHelper.TABLE_RE_DOWNLOAD.ID + " = " + id + " and " + ErrorDbHelper.TABLE_RE_DOWNLOAD.F_ID + " = " + f_id;
                        if(DatabaseUtils.queryNumEntries(wdb, ErrorDbHelper.TABLE_RE_DOWNLOAD.getName(), where) > 0){
                            exist_redownload = true;
                        }
                    }

                    // レコードが存在すればインクリメント(またはブラックリスト入り）
                    if(exist_redownload){
                        updateReDownload(id, f_id, "");
                    }else{
                        // re_downloadレコード追加
                        ContentValues values = new ContentValues();
                        values.put(ErrorDbHelper.TABLE_RE_DOWNLOAD.ID, id);
                        values.put(ErrorDbHelper.TABLE_RE_DOWNLOAD.F_ID, f_id);
                        values.put(ErrorDbHelper.TABLE_RE_DOWNLOAD.MATERIAL_NAME, "");
                        values.put(ErrorDbHelper.TABLE_RE_DOWNLOAD.RETRY_COUNT, 0);
                        wdb.insert(ErrorDbHelper.TABLE_RE_DOWNLOAD.getName(), null, values);
                    }
                }
            }
        }
    }

    private void deleteContentFile(int id, int f_id) {
        File target_dir;
        if (f_id > 0) {
            target_dir = ContentFile.getExternalDir(id, f_id);
        } else {
            target_dir = ContentFile.getContentDir(id);
        }

        if (target_dir.isDirectory()) {
            FileUtil.deleteRecursive(target_dir);
        }
    }

    public String makeDownloadDirectoryUri(String siteid, int id) throws NoSuchAlgorithmException {
        final String MD5_CRYPT_KEY = "scdci";

        String source = id + MD5_CRYPT_KEY + siteid;
        String md5_str = getMd5(source);
        Integer idDir = (int) Math.floor((Math.floor((double) id / 100) + 1) * 100);
        return idDir.toString() + "/" + id + md5_str.substring(0, 10) + "/";
    }

    private String getMd5(String source) throws NoSuchAlgorithmException {
        StringBuilder hexString = new StringBuilder();
        if (source != null && source.length() > 0) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(source.getBytes());
            byte[] hash = md.digest();

            for (byte aHash : hash) {
                if ((0xff & aHash) < 0x10) {
                    hexString.append("0");
                    hexString.append(Integer.toHexString(0xFF & aHash));
//                    hexString.append("0" + Integer.toHexString((0xFF & aHash)));
                } else {
                    hexString.append(Integer.toHexString(0xFF & aHash));
                }
            }
        }
        return hexString.toString();
    }


    private void loggingDownloadStart(int id, int f_id, @NonNull String material_name, long cache_size, int infoflag) {
        if (material_name.length() > 0) {
            String log_suffix = " (material_name = " + material_name + ")";
            NetLog.notice(getString(R.string.net_log_notice_start_download) + log_suffix);
        } else {
            intentNetLogDownload(id, f_id, cache_size, NetLogService.DOWNLOAD_STATUS_START, infoflag);
        }
        if(infoflag == 1){
            loggingRescueDownload(id, getString(R.string.log_info_start_rescue_content_download));
        } else {
            loggingDownload(id, f_id, material_name, getString(R.string.log_info_start_download_content));
        }
    }

    private void loggingDownloadError(int id, int f_id, String material_name, long cache_size, int infoflag) {
        if (material_name.length() > 0) {
            String log_suffix = " (material_name = " + material_name + ")";
            NetLog.warning(getString(R.string.net_log_notice_error_download) + log_suffix);
            Logging.error(getString(R.string.log_error_failed_download_content) + log_suffix);
        } else {
            intentNetLogDownload(id, f_id, cache_size, NetLogService.DOWNLOAD_STATUS_ERROR, infoflag);
        }
        if(infoflag == 1){
            loggingRescueDownload(id, getString(R.string.log_info_failed_rescue_content_download));
        } else {
            loggingDownload(id, f_id, material_name, getString(R.string.log_error_failed_download_content));
        }
    }

    private void loggingDownloadSuccess(int id, int f_id, String material_name, long cache_size, int infoflag) {
        if (material_name.length() > 0) {
            String log_suffix = " (material_name = " + material_name + ")";
            NetLog.notice(getString(R.string.net_log_notice_finish_download) + log_suffix);
        } else {
            intentNetLogDownload(id, f_id, cache_size, NetLogService.DOWNLOAD_STATUS_SUCCESS, infoflag);
        }
        if(infoflag == 1){
            loggingRescueDownload(id, getString(R.string.log_info_success_rescue_content_download));
        } else {
            loggingDownload(id, f_id, material_name, getString(R.string.log_info_success_download_content));
        }
    }

    private void loggingDownloadSuccessTouchContent(int id, long cache_size, ArrayList<String> material_list) {
        Intent intent = getIntentNetLogDownload(id, 0, cache_size, NetLogService.DOWNLOAD_STATUS_SUCCESS, 0);
        intent.putStringArrayListExtra(NetLogService.INTENT_EXTRA_DOWNLOAD_MATERIALS, material_list);
        startService(intent);

        Logging.info(getString(R.string.log_info_success_download_content) + " (id = " + id + ")");
    }

    private void loggingExtendError(int id, int f_id, @NonNull String material_name) {

        String target_info;
        if (material_name.length() > 0) {
            target_info = " (material_name = " + material_name + ")";
        } else if (f_id > 0) {
            target_info = " (id = " + id + ",f_id = " + f_id + ")";
        } else {
            target_info = " (id = " + id + ")";
        }

        NetLog.notice(getString(R.string.net_log_warning_extend_failed) + target_info);
        Logging.error(getString(R.string.log_error_failed_extend_content) + target_info);
    }


    private void loggingDownload(int id, int f_id, String material_name, String msg) {
        String target_info;
        if (material_name.length() > 0) {
            target_info = " (material_name = " + material_name + ")";
        } else if (f_id > 0) {
            target_info = " (id = " + id + ", f_id = " + f_id + ")";
        } else {
            target_info = " (id=" + id + ")";
        }

        Logging.info(msg + target_info);

    }

    private void loggingRescueDownload(int id, String msg){
        Logging.info(msg + getString(R.string.log_info_rescue_content_id, String.valueOf(id)));
    }

    private void intentNetLogDownload(int id, int f_id, long cache_size, int type, int infoflag) {
        startService(getIntentNetLogDownload(id, f_id, cache_size, type, infoflag));
    }

    private Intent getIntentNetLogDownload(int id, int f_id, long cache_size, int type, int infoflag) {
        Intent intent = new Intent(this, NetLogService.class);
        intent.setAction(NetLogService.ACTION_LOG_DOWNLOAD);
        intent.putExtra(NetLogService.INTENT_EXTRA_DOWNLOAD_ID, id);
        intent.putExtra(NetLogService.INTENT_EXTRA_DOWNLOAD_F_ID, f_id);
        intent.putExtra(NetLogService.INTENT_EXTRA_DOWNLOAD_STATUS, type);
        intent.putExtra(NetLogService.INTENT_EXTRA_DOWNLOAD_BYTES, cache_size);
        intent.putExtra(NetLogService.INTENT_EXTRA_DOWNLOAD_INFOFLAG, infoflag);
        return intent;
    }
}
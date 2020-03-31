package jp.co.digitalcruise.admint.player.service;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;

import java.util.Collections;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.activity.viewer.ViewerActivity;
import jp.co.digitalcruise.admint.player.component.CompatibleSdk;
import jp.co.digitalcruise.admint.player.component.define.DeviceDef;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.object.PlayItem;
import jp.co.digitalcruise.admint.player.component.object.PlaylistObject;
import jp.co.digitalcruise.admint.player.component.rescue.GetRescueDbData;
import jp.co.digitalcruise.admint.player.db.PlaylistDbHelper;
import jp.co.digitalcruise.admint.player.db.ScheduleDbHelper;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.service.network.HealthCheckService;

import static jp.co.digitalcruise.admint.player.BuildConfig.APPLICATION_ID;

@SuppressLint("Registered")
public class PlaylistService extends AbstractService {
    private static final String ACTION_PREFIX = APPLICATION_ID + ".PlaylistService.";

    public static final String ACTION_REFRESH_PLAYLIST = ACTION_PREFIX + "REFRESH_PLAYLIST";

    public static final String ACTION_PLAYER_LAUNCH = ACTION_PREFIX + "PLAYER_LAUNCH";

    public static final String ACTION_CLEAR_PLAYLIST = ACTION_PREFIX + "CLEAR_PLAYLIST";

    public static final String ACTION_POST_HEALTH_CHECK = ACTION_PREFIX + "POST_HEALTH_CHECK";

    public static final String ACTION_DOWNLOAD_CONTENT = ACTION_PREFIX + "DOWNLOAD_CONTENT";

    public static final String ACTION_CHECK_UPDATE = "CHECK_UPDATE";

    public static final String ACITON_SHOW_DIALOG = "SHOW_DIALOG";

    public static final String INTENT_EXTRA_DOWNLOAD_CONTENT_ID = "DOWNLOAD_CONTENT_ID";


    static private Messenger mViewerMessenger;
    static private Messenger mSelfMessenger;


    public static final int HANDLE_MSG_CONNECT = 0;
    static private class PlaylistHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == HANDLE_MSG_CONNECT) {
                mViewerMessenger = msg.replyTo;
            }
        }
    }

    public PlaylistService() {
        super(PlaylistService.class.getName());
    }

    private ScheduleDbHelper mScheduleDbHelper = null;
    private PlaylistDbHelper mPlaylistDbHelper = null;

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            if (intent == null || intent.getAction() == null) {
                return;
            }

            mScheduleDbHelper = null;
            mPlaylistDbHelper = null;

            try {

                final String action = intent.getAction();

                Logging.notice(action);

                // DBオブジェクトは定常的につかうのでメンバ変数として生成
                // DB Helper生成
                mScheduleDbHelper = new ScheduleDbHelper(this);
                mPlaylistDbHelper = new PlaylistDbHelper(this);

                if (ACTION_REFRESH_PLAYLIST.equals(action)) {
                    actionRefreshPlaylist();
                }else if(ACTION_PLAYER_LAUNCH.equals(action)){
                    actionPlayerLaunch();
                }else if(ACTION_CLEAR_PLAYLIST.equals(action)){
                    actionClearPlaylist();
                }else if(ACTION_DOWNLOAD_CONTENT.equals(action)){
                    actionDownloadContent(intent);
                }else if(ACTION_POST_HEALTH_CHECK.equals(action)){
                    actionPostHealthCheck();
                }else if(ACTION_CHECK_UPDATE.equals(action)){
                    actionCheckUpdate();
                }else if(ACITON_SHOW_DIALOG.equals(action)){
                    sendMessageViewer(10, false);
                }
            } catch (Exception e) {
                // ロギング
            } finally {
                if(mScheduleDbHelper != null){
                    mScheduleDbHelper.close();
                }

                if(mPlaylistDbHelper != null){
                    mPlaylistDbHelper.close();
                }
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionDownloadContent(Intent intent){
        try {
            int id = intent.getIntExtra(PlaylistService.INTENT_EXTRA_DOWNLOAD_CONTENT_ID, 0);

            sendMessageViewer(ViewerActivity.HANDLE_MSG_CHECK_UPDATE, id);
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionClearPlaylist(){
        try{
            clearPlaylistDb();
            Logging.info(getString(R.string.log_info_all_clear_playlist));
            sendToast(getString(R.string.toast_msg_clear_playlist_success));
        }catch (Exception e){
            Logging.stackTrace(e);
            sendToast(getString(R.string.toast_msg_clear_playlist_error));
        }
    }

    private void actionPostHealthCheck(){
        try{
            // プレイリスト更新
            refreshPlaylist(true);

            // ダウンロード開始依頼
//            intentCManagerDownloadContent();
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionPlayerLaunch(){
        try {
            if (!isViewerForegraund()) {
                Intent intent = new Intent(this, ViewerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionRefreshPlaylist(){
        try{
            refreshPlaylist(false);
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionCheckUpdate(){
        try{
            if(isViewerForegraund()){
                sendMessageViewer(ViewerActivity.HANDLE_MSG_CHECK_UPDATE, 0);
            }
        } catch (Exception e){
            Logging.stackTrace(e);
        }
    }

//    private void intentCManagerDownloadContent(){
//        Intent intent = new Intent(this, ContentManagerService.class);
//        intent.setAction(ContentManagerService.ACTION_DOWNLOAD_CONTENTS);
//        startService(intent);
//    }

    private void clearPlaylistDb(){
        SQLiteDatabase wdb = mPlaylistDbHelper.getWriterDb();
        wdb.delete(PlaylistDbHelper.TABLE_PLAY_LIST.getName(), null, null);
        wdb.delete(PlaylistDbHelper.TABLE_PLAY_LIST_INFO.getName(), null, null);
        wdb.delete(PlaylistDbHelper.TABLE_DEFAULT_LIST.getName(), null, null);
        wdb.delete(PlaylistDbHelper.TABLE_DEFAULT_LIST_INFO.getName(), null, null);
    }

    private void refreshPlaylist(boolean isHealthCheck) {
        long next_time = 0;
        try {
            long cur_time = System.currentTimeMillis();

            // 取得中のスケジュールから現時刻の再生対象のスケジュールタイプを取得
            int sched_type = currentSchedType(cur_time);

            // スケジュールから新しいプレイリストを作成
            PlaylistObject new_play_list;
            GetRescueDbData rescue_data = new GetRescueDbData();

            if(rescue_data.checkEmergencyList(this)){
                //緊急配信ありますよ
                new_play_list = rescue_data.getEmergencyPlayList(this);
                boolean is_viewer_resart = isRestartViewer(new_play_list);
                deleteAndInsertPlaylist(new_play_list);
                sendMessageViewer(ViewerActivity.HANDLE_MSG_EMERGENCY, is_viewer_resart);
//                if(new_play_list.play_items.size() > 0){
//                } else {
////                    sendMessageViewer(ViewerActivity.HANDLE_MSG_POST_HEALTH_CHECK, true);
//                }

            } else {
//                Log.d("hoge", "normal content scheduling");
                new_play_list = getSchedulePlaylist(sched_type, cur_time);
                // 次回番組更新時刻
                next_time = (new_play_list.st + new_play_list.pt);

                // 時刻表で何もコンテンツを表示しない時間帯がある時はnext_timeが0になる（表示番組が存在しないので終わり時刻が算出できない）
                if(next_time == 0){
                    // 次の番組開始時刻を取得
                    next_time = getNextProgramTime(sched_type, cur_time);
                }

                // スケジュールからデフォルトコンテンツリストを作成
                PlaylistObject new_default_list = getSchedulePlaylist(HealthCheckService.SCHED_TYPE_DEFAULT, 0);

                // 処理の順序に注意、比較、DB更新、viewerへの再スタート指示
                // 現プレイリストと新プレイリストを比較
                boolean is_viewer_restart = isRestartViewer(new_play_list);

                // DB更新
                deleteAndInsertPlaylist(new_play_list);
                deleteAndInsertDefaultlist(new_default_list);

                // Viewerにメッセージ送信
                if(isHealthCheck){
                    sendMessageViewer(ViewerActivity.HANDLE_MSG_POST_HEALTH_CHECK, is_viewer_restart);
                } else {
                    postViewerHealthCheckSuccess(is_viewer_restart);
                }
            }


        }finally {
            // アラーム更新
            setAlarmRefreshPlaylist(next_time);
        }
    }

    private void setAlarmRefreshPlaylist(long next_time){
        // 再起動のアラーム設定
        Intent makeIntent = new Intent(this, PlaylistService.class);
        makeIntent.setAction(PlaylistService.ACTION_REFRESH_PLAYLIST);
        PendingIntent pintent = PendingIntent.getService(this, 0, makeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        if(next_time > 0) {
            CompatibleSdk.setAlarmEx(this, next_time, pintent);
        }else{
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if(am != null) {
                am.cancel(pintent);
            }else{
                throw new RuntimeException("(AlarmManager) getSystemService(Context.ALARM_SERVICE) return null");
            }
        }
    }

    private void sendMessageViewer(int what, Integer obj){
        try {
            if (isViewerForegraund() && mViewerMessenger != null) {
                Message msg = Message.obtain(null, what, obj);
                mViewerMessenger.send(msg);
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void sendMessageViewer(int what, Boolean obj){
        try {
            if (isViewerForegraund() && mViewerMessenger != null) {
                Message msg = Message.obtain(null, what, obj);
                mViewerMessenger.send(msg);
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void postViewerHealthCheckSuccess(boolean is_viewer_restart){
        if (is_viewer_restart) {
            Logging.notice("Viewer handler send msg HANDLE_MSG_PLAY_FIRST_FORCE");
            sendMessageViewer(ViewerActivity.HANDLE_MSG_PLAY_FIRST_FORCE, 0);
        } else {
            Logging.notice("Viewer handler send msg HANDLE_MSG_CHECK_UPDATE");
            sendMessageViewer(ViewerActivity.HANDLE_MSG_CHECK_UPDATE, 0);
        }
    }

    private void deleteAndInsertPlaylist(PlaylistObject new_play_list){

        if(new_play_list.sched_type == PlaylistObject.SCHEDULE_NOTHING){
            return;
        }

        SQLiteDatabase wdb = mPlaylistDbHelper.getWriterDb();

        // play_list_info
        {
            // レコード削除
            wdb.delete(PlaylistDbHelper.TABLE_PLAY_LIST_INFO.getName(), null, null);

            // レコード追加
            ContentValues values = new ContentValues();

            values.put(PlaylistDbHelper.TABLE_PLAY_LIST_INFO.SCHED_TYPE, new_play_list.sched_type);
            values.put(PlaylistDbHelper.TABLE_PLAY_LIST_INFO.UPDATE_AT, new_play_list.update_at);
            values.put(PlaylistDbHelper.TABLE_PLAY_LIST_INFO.ST, new_play_list.st);
            values.put(PlaylistDbHelper.TABLE_PLAY_LIST_INFO.PT, new_play_list.pt);
            values.put(PlaylistDbHelper.TABLE_PLAY_LIST_INFO.UT, new_play_list.ut);
            values.put(PlaylistDbHelper.TABLE_PLAY_LIST_INFO.PO, new_play_list.po);

            wdb.insert(PlaylistDbHelper.TABLE_PLAY_LIST_INFO.getName(), null, values);
        }

        // play_list
        // レコード削除
        wdb.delete(PlaylistDbHelper.TABLE_PLAY_LIST.getName(), null, null);

        // レコード追加
        for (PlayItem play_item : new_play_list.play_items) {
            ContentValues values = new ContentValues();

            values.put(PlaylistDbHelper.TABLE_PLAY_LIST.ID, play_item.id);
            values.put(PlaylistDbHelper.TABLE_PLAY_LIST.O, play_item.o);
            values.put(PlaylistDbHelper.TABLE_PLAY_LIST.D, play_item.d);
            values.put(PlaylistDbHelper.TABLE_PLAY_LIST.T, play_item.t);
            values.put(PlaylistDbHelper.TABLE_PLAY_LIST.TCID, play_item.tcid);
            values.put(PlaylistDbHelper.TABLE_PLAY_LIST.TCD, play_item.tcd);
            values.put(PlaylistDbHelper.TABLE_PLAY_LIST.F_ID, play_item.f_id);
            values.put(PlaylistDbHelper.TABLE_PLAY_LIST.F_O, play_item.f_o);
            values.put(PlaylistDbHelper.TABLE_PLAY_LIST.W_URL, play_item.w_url);

            wdb.insert(PlaylistDbHelper.TABLE_PLAY_LIST.getName(), null, values);
        }
    }

    private void deleteAndInsertDefaultlist(PlaylistObject new_default_list){

        if(new_default_list.sched_type == PlaylistObject.SCHEDULE_NOTHING){
            return;
        }

        SQLiteDatabase wdb = mPlaylistDbHelper.getWriterDb();

        // play_list_info
        {
            // レコード削除
            wdb.delete(PlaylistDbHelper.TABLE_DEFAULT_LIST_INFO.getName(), null, null);

            // レコード追加
            ContentValues values = new ContentValues();

            values.put(PlaylistDbHelper.TABLE_DEFAULT_LIST_INFO.SCHED_TYPE, new_default_list.sched_type);
            values.put(PlaylistDbHelper.TABLE_DEFAULT_LIST_INFO.UPDATE_AT, new_default_list.update_at);
            values.put(PlaylistDbHelper.TABLE_DEFAULT_LIST_INFO.ST, new_default_list.st);
            values.put(PlaylistDbHelper.TABLE_DEFAULT_LIST_INFO.PT, new_default_list.pt);
            values.put(PlaylistDbHelper.TABLE_DEFAULT_LIST_INFO.UT, new_default_list.ut);
            values.put(PlaylistDbHelper.TABLE_DEFAULT_LIST_INFO.PO, new_default_list.po);

            wdb.insert(PlaylistDbHelper.TABLE_DEFAULT_LIST_INFO.getName(), null, values);
        }

        // play_list
        // レコード削除
        wdb.delete(PlaylistDbHelper.TABLE_DEFAULT_LIST.getName(), null, null);

        // レコード追加
        for (PlayItem play_item : new_default_list.play_items) {
            ContentValues values = new ContentValues();

            values.put(PlaylistDbHelper.TABLE_DEFAULT_LIST.ID, play_item.id);
            values.put(PlaylistDbHelper.TABLE_DEFAULT_LIST.O, play_item.o);
            values.put(PlaylistDbHelper.TABLE_DEFAULT_LIST.D, play_item.d);
            values.put(PlaylistDbHelper.TABLE_DEFAULT_LIST.T, play_item.t);
            values.put(PlaylistDbHelper.TABLE_DEFAULT_LIST.TCID, play_item.tcid);
            values.put(PlaylistDbHelper.TABLE_DEFAULT_LIST.TCD, play_item.tcd);
            values.put(PlaylistDbHelper.TABLE_DEFAULT_LIST.F_ID, play_item.f_id);
            values.put(PlaylistDbHelper.TABLE_DEFAULT_LIST.F_O, play_item.f_o);
            values.put(PlaylistDbHelper.TABLE_DEFAULT_LIST.W_URL, play_item.w_url);

            wdb.insert(PlaylistDbHelper.TABLE_DEFAULT_LIST.getName(), null, values);
        }
    }

    private int currentSchedType(long cur_time){
        int cur_sched_type = PlaylistObject.SCHEDULE_NOTHING;
        SQLiteDatabase rdb = mScheduleDbHelper.getReaderDb();

        // スケジュールタイプ取得
        String order = " order by " + ScheduleDbHelper.TABLE_SCHEDULE_INFO.SCHED_TYPE + " asc";
        // SQL作成 order by 超重要
        String sql = "select " +
                ScheduleDbHelper.TABLE_SCHEDULE_INFO.SCHED_TYPE + "," +
                ScheduleDbHelper.TABLE_SCHEDULE_INFO.START_TIME + "," +
                ScheduleDbHelper.TABLE_SCHEDULE_INFO.END_TIME +
                " from " + ScheduleDbHelper.TABLE_SCHEDULE_INFO.getName() +
                order;

        try (Cursor cursor = rdb.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                int sched_type = cursor.getInt(0);
                long start_time = cursor.getLong(1);
                long end_time = cursor.getLong(2);
                if (start_time <= cur_time && end_time > cur_time) {
                    cur_sched_type = sched_type;
                    break;
                }
                else if (sched_type == HealthCheckService.SCHED_TYPE_DEFAULT) {
                    // ヘルスチェック、先行取得、拡張ストレージのどれも該当スケジュールが存在しない
                    cur_sched_type = sched_type;
                    break;
                }
            }
        }
        return cur_sched_type;
    }

    private PlaylistObject getCurrentPlaylist(){
        PlaylistObject playlist_obj = new PlaylistObject();
        SQLiteDatabase rdb = mPlaylistDbHelper.getReaderDb();

        // play_list_info
        {
            String sql = "select " +
                    PlaylistDbHelper.TABLE_PLAY_LIST_INFO.SCHED_TYPE + "," +
                    PlaylistDbHelper.TABLE_PLAY_LIST_INFO.ST + "," +
                    PlaylistDbHelper.TABLE_PLAY_LIST_INFO.UT + "," +
                    PlaylistDbHelper.TABLE_PLAY_LIST_INFO.PT + "," +
                    PlaylistDbHelper.TABLE_PLAY_LIST_INFO.PO +
                    " from " + PlaylistDbHelper.TABLE_PLAY_LIST_INFO.getName();

            try (Cursor cursor = rdb.rawQuery(sql, null)) {
                if (cursor.moveToNext()) {
                    playlist_obj.sched_type = cursor.getInt(0);
                    playlist_obj.st = cursor.getLong(1);
                    playlist_obj.ut = cursor.getLong(2);
                    playlist_obj.pt = cursor.getLong(3);
                    playlist_obj.po = cursor.getInt(4);
                }
            }
        }

        // play_list
        {
            String sql = "select " +
                    PlaylistDbHelper.TABLE_PLAY_LIST.ID + "," +
                    PlaylistDbHelper.TABLE_PLAY_LIST.O + "," +
                    PlaylistDbHelper.TABLE_PLAY_LIST.D + "," +
                    PlaylistDbHelper.TABLE_PLAY_LIST.T + "," +
                    PlaylistDbHelper.TABLE_PLAY_LIST.TCID + "," +
                    PlaylistDbHelper.TABLE_PLAY_LIST.TCD + "," +
                    PlaylistDbHelper.TABLE_PLAY_LIST.F_ID + "," +
                    PlaylistDbHelper.TABLE_PLAY_LIST.F_O + "," +
                    PlaylistDbHelper.TABLE_PLAY_LIST.W_URL +
                    " from " + PlaylistDbHelper.TABLE_PLAY_LIST.getName();

            try (Cursor cursor = rdb.rawQuery(sql, null)) {
                while (cursor.moveToNext()) {
                    PlayItem play_item = new PlayItem();
                    play_item.id = cursor.getInt(0);
                    play_item.o = cursor.getInt(1);
                    play_item.d = cursor.getInt(2);
                    play_item.t = cursor.getInt(3);
                    play_item.tcid = cursor.getInt(4);
                    play_item.tcd = cursor.getInt(5);
                    play_item.f_id = cursor.getInt(6);
                    play_item.f_o = cursor.getInt(7);
                    play_item.w_url = cursor.getString(8);

                    playlist_obj.play_items.add(play_item);
                }
            }
        }

        // o, f_o順でソート
        Collections.sort(playlist_obj.play_items);

        return playlist_obj;
    }

    private boolean isRestartViewer(PlaylistObject new_play_list){

        PlaylistObject cur_play_list = getCurrentPlaylist();

        // utの値が0でなく、かつ現・新リストのutの値が同じ時はビュアーの再起動は行わない
        if(new_play_list.ut != 0 && new_play_list.ut == cur_play_list.ut){
            return false;
        }

        // 現在のリストを元に比較
        for(PlayItem cur_item : cur_play_list.play_items){
            boolean is_match = false;
            for (PlayItem new_item : new_play_list.play_items){
                if(cur_item.id == new_item.id &&
                    cur_item.o == new_item.o &&
                    cur_item.d == new_item.d &&
                    cur_item.w_url.equals(new_item.w_url) &&
                    cur_item.tcid == new_item.tcid &&
                    cur_item.tcd == new_item.tcd
                ){
                    is_match = true;
                    break;
                }
            }

            // 一致コンテンツが存在しない
            if(!is_match){
                return true;
            }
        }

        // 新しいリストを元に比較
        for(PlayItem new_item : new_play_list.play_items){
            boolean is_match = false;
            for (PlayItem cur_item : cur_play_list.play_items){
                if(cur_item.id == new_item.id &&
                    cur_item.o == new_item.o &&
                    cur_item.d == new_item.d &&
                    cur_item.w_url.equals(new_item.w_url) &&
                    cur_item.tcid == new_item.tcid &&
                    cur_item.tcd == new_item.tcd
                ){
                    is_match = true;
                    break;
                }
            }

            // 一致コンテンツが存在しない
            if(!is_match){
                return true;
            }
        }

        return false;
    }

    private long getNextProgramTime(int sched_type, long cur_time){
        SQLiteDatabase rdb = mScheduleDbHelper.getReaderDb();

        if(sched_type == HealthCheckService.SCHED_TYPE_DEFAULT) {
            return 0;
        }

        // 現在時刻からみて次の番組開始の時刻を取得
        String where = " where " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.SCHED_TYPE + " = " + sched_type + " and " +
                ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST + " > " + cur_time;

        String order = " order by " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST + " asc";

        String sql = "select " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST +
                " from " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.getName() +
                where +
                order +
                " limit 1";

        try (Cursor cursor = rdb.rawQuery(sql, null)) {
            // 次の番組開始時刻を返す
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            }
        }

        // 取得したスケジュール範囲に現在時刻からみて次の番組が存在しない場合は、スケジュール範囲のENDの時刻を返す
        where = " where " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.SCHED_TYPE + " = " + sched_type;
        sql = "select " +
                ScheduleDbHelper.TABLE_SCHEDULE_INFO.END_TIME +
                " from " + ScheduleDbHelper.TABLE_SCHEDULE_INFO.getName() +
                where;
        try (Cursor cursor = rdb.rawQuery(sql, null)) {
            // 次の番組開始時刻を返す
            if (cursor.moveToNext()) {
                return cursor.getLong(0);
            }
        }

        // 取得したスケジュール範囲に現在時刻が存在しない
        return 0;
    }



    private PlaylistObject getSchedulePlaylist(int sched_type, long cur_time){

        PlaylistObject playlist_obj = new PlaylistObject();
        playlist_obj.sched_type = sched_type;

        // 再生対象のスケジュールなし
        if(sched_type == PlaylistObject.SCHEDULE_NOTHING){
            return playlist_obj;
        }

        SQLiteDatabase rdb = mScheduleDbHelper.getReaderDb();

        // 番組取得
        {
            String where;

            if(sched_type != HealthCheckService.SCHED_TYPE_DEFAULT) {
                where = " where " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.SCHED_TYPE + " = " + sched_type + " and " +
                        ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST + " <= " + cur_time + " and " +
                        "(" + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST + " + " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.PT + ") > " + cur_time;
            }else{
                where = " where " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.SCHED_TYPE + " = " + sched_type;
            }

            String order = " order by " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST + " asc ";

            String sql = "select " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST + "," +
                    ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.PT + "," +
                    ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.PO + "," +
                    ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.UT + "," +
                    ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.TCD + "," +
                    ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.TCID +
                    " from " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.getName() +
                    where +
                    order +
                    " limit 1";

            try (Cursor cursor = rdb.rawQuery(sql, null)) {
                if (cursor.moveToNext()) {
                    playlist_obj.st = cursor.getLong(0);
                    playlist_obj.pt = cursor.getLong(1);
                    playlist_obj.po = cursor.getInt(2);
                    playlist_obj.ut = cursor.getLong(3);
                    playlist_obj.tcd = cursor.getInt(4);
                    playlist_obj.tcid = cursor.getInt(5);
                } else {
                    // スケジュールは取得したが再生対象の番組が存在しない
                    return playlist_obj;
                }
            }
        }

        // コンテンツ取得
        {
             String where = " where " + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.SCHED_TYPE + " = " + sched_type + " and " +
                     ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.ST + " = " + playlist_obj.st + " and " +
                     ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.U + " = 1";

            String join =
                    " left join " + ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.getName() + " on (" +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.ID + " = " +
                    ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.ID + " and " +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.SCHED_TYPE + " = " +
                    ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.SCHED_TYPE + ") " +
                    " left join " + ScheduleDbHelper.TABLE_SCHEDULE_WEBVIEW.getName() + " on (" +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.ID + " = " +
                    ScheduleDbHelper.TABLE_SCHEDULE_WEBVIEW.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_WEBVIEW.ID + " and " +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.SCHED_TYPE + " = " +
                    ScheduleDbHelper.TABLE_SCHEDULE_WEBVIEW.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_WEBVIEW.SCHED_TYPE + ") ";

            String sql = "select " +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.ID + "," +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.O + "," +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.D + "," +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.T + "," +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.TCID + "," +
                    ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.TCD + "," +
                    ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.F_ID + "," +
                    ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_EXTERNAL.F_O + "," +
                    ScheduleDbHelper.TABLE_SCHEDULE_WEBVIEW.getName() + "." + ScheduleDbHelper.TABLE_SCHEDULE_WEBVIEW.W_URL +
                    " from " + ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName() +
                    join +
                   where;

            try (Cursor cursor = rdb.rawQuery(sql, null)) {
                while (cursor.moveToNext()) {
                    PlayItem play_item = new PlayItem();
                    play_item.id = cursor.getInt(0);
                    play_item.o = cursor.getInt(1);
                    play_item.d = cursor.getInt(2);
                    play_item.t = cursor.getInt(3);

                    if(!DefaultPref.getLimitlessContentMode()) {
                        // WebView対応端末ではない
                        if (play_item.t == HealthCheckService.CONTENT_TYPE_WEBVIEW && !DeviceDef.isValidWebview()) {
                            continue;
                            // Touch対応端末ではない
                        } else if (play_item.t == HealthCheckService.CONTENT_TYPE_TOUCH && !DeviceDef.isValidTouch()) {
                            continue;
                        }
                    }

                    // 番組にタッチが紐づいて、かつタッチ対応端末の時は番組タッチコンテンツを採用
                    if (playlist_obj.tcid > 0 && DeviceDef.isValidWebview()) {
                        play_item.tcid = playlist_obj.tcid;
                        play_item.tcd = playlist_obj.tcd;
                    } else {
                        play_item.tcid = cursor.getInt(4);
                        play_item.tcd = cursor.getInt(5);
                    }

                    // デイリーコンテンツ
                    if (play_item.t == HealthCheckService.CONTENT_TYPE_EXTERNAL_PICTURE || play_item.t == HealthCheckService.CONTENT_TYPE_EXTERNAL_MOVIE) {
                        if (!cursor.isNull(6) && !cursor.isNull(7)) {
                            play_item.f_id = cursor.getInt(6);
                            play_item.f_o = cursor.getInt(7);
                        } else {
                            // デイリーコンテンツが配信設定されているがサブIDが存在しない
                            continue;
                        }
                    }

                    // WebViewコンテンツ
                    if (play_item.t == HealthCheckService.CONTENT_TYPE_WEBVIEW) {
                        if (!cursor.isNull(8)) {
                            play_item.w_url = cursor.getString(8);
                        } else {
                            // WebViewコンテンツが配信設定されているが子(w)要素が存在しない（不具合の可能性）
                            continue;
                        }
                    }

                    playlist_obj.play_items.add(play_item);
                }
            }
        }

        // o, f_o順でソート
        Collections.sort(playlist_obj.play_items);

        return playlist_obj;
    }

    @Override
    public void onCreate() {
        try {
            super.onCreate();
            mSelfMessenger = new Messenger(new PlaylistHandler());
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    @Override
    public IBinder onBind(Intent i) {
        return mSelfMessenger.getBinder();
    }

}

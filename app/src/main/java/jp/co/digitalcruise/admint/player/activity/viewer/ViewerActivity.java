package jp.co.digitalcruise.admint.player.activity.viewer;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.net.Proxy;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.ArrayMap;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ricoh.view360.lib.PhotoSphereView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import jp.co.digitalcruise.admint.player.AdmintApplication;
import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.activity.AbstractAdmintActivity;
import jp.co.digitalcruise.admint.player.component.CompatibleSdk;
import jp.co.digitalcruise.admint.player.component.date.DateUtil;
import jp.co.digitalcruise.admint.player.component.define.DeviceDef;
import jp.co.digitalcruise.admint.player.component.file.ContentFile;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.log.NetLog;
import jp.co.digitalcruise.admint.player.component.object.PlayItem;
import jp.co.digitalcruise.admint.player.component.object.PlaylistObject;
import jp.co.digitalcruise.admint.player.component.rescue.GetRescueDbData;
import jp.co.digitalcruise.admint.player.db.ErrorDbHelper;
import jp.co.digitalcruise.admint.player.db.PlayLogDbHelper;
import jp.co.digitalcruise.admint.player.db.PlaylistDbHelper;
import jp.co.digitalcruise.admint.player.db.ViewlistDbHelper;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.pref.GroovaProxyPref;
import jp.co.digitalcruise.admint.player.pref.HealthCheckPref;
import jp.co.digitalcruise.admint.player.service.LoggingService;
import jp.co.digitalcruise.admint.player.service.PlaylistService;
import jp.co.digitalcruise.admint.player.service.network.HealthCheckService;
import jp.co.grv.toto.tvp.ReadyListener;
import jp.co.grv.toto.tvp.api.TVPlatform;
import jp.co.grv.toto.tvp.api.TVPropertyManager;

public class ViewerActivity extends AbstractAdmintActivity implements ServiceConnection, CustomDialogFragment.OnCustomDialogListener, ReadyListener {

    // 親レイアウト
    private FrameLayout mViewerLayout = null;
    private FrameLayout mTelopLayout = null;
    // ブランクレイアウト
    private FrameLayout mBlankLayout = null;
    // 静止画
    private ImageView mImageView = null;
    // THETA静止画
    private PhotoSphereView mThetaImageView = null;
    // 動画
    private ViewerSurfaceView mSurfaceView = null;
    // タッチコンテンツ
    private RelativeLayout mTouchView = null;
    // タッチコンテンツ本体
    private TouchContent mTouchContent = null;
    // WebView
    private ViewerWebView mViewerWebView = null;

    // 時刻表示
    private TextView mTimeTextView = null;

    //テロップ
    TelopSurfaceView mTelopView = null;

    // タッチコンテンツタッチ起動時刻
    // タッチコンテンツからのリターン時に参照
    private long mClickTouchTimestamp = 0;

    // メディアプレイヤー
    private MediaPlayer mMediaPlayer = null;
    // 無音プレイヤー（音飛び防止用にバックエンドで流し続ける）
    private MediaPlayer mSoundlessMediaPlayer = null;

    // 直近のビデオスケールサイズ
    private Point mCurrentScalePoint = new Point();

    // THETA初期化済フラグ
    private boolean mIsThetaInit = false;

    // メッセージハンドラ
    private Messenger mSelfMessenger = null;
    // ビューアハンドラ
    private ViewerHandler mViewerHandler = null;


    // プレイリストDB（再生予定のコンテンツ）
    private PlaylistDbHelper mPlaylistDbHelper = null;
    // ビューリストDB（実際に再生するコンテンツ）
    private ViewlistDbHelper mViewlistDbHelper = null;
    // エラーDB
    private ErrorDbHelper mErrorDbHelper = null;
    // 再生ログDB
    private PlayLogDbHelper mPlayLogDbHelper = null;

    // 現在のビューリストのインデックス（位置）
    private int mViewListIndex = 0;
    // ビューリスト（プレイリストのうち再生可能なリスト）
    private PlaylistObject mViewlist = null;
    // プレイリスト
    private PlaylistObject mPlaylist = null;

    private boolean isPendingDownload = false;

    // コンテンツのビュータイプ
    public static final int VIEW_TYPE_NOTING = 0;
    public static final int VIEW_TYPE_MOVIE = 1;
    public static final int VIEW_TYPE_PICTURE = 2;
    public static final int VIEW_TYPE_THETA = 3;
    public static final int VIEW_TYPE_TOUCH = 4;
    public static final int VIEW_TYPE_WEB = 5;
    public static final int VIEW_TYPE_ORIGINAL = 8;

    // 直前に再生した動画とで縦横サイズが変わった場合にTRUE
    // 縦横サイズが変わらない場合は高速切り替え可能な再生動画の変更処理が行える
    private boolean mIsScaleChange = false;

    // TRUEの時、再生中ビューリストの先頭に来たタイミングでリストを作り直す
    // 中断を伴わないプレイリストの変更、または現プレイリスト上のコンテンツのダウンロード完了時に通知されTRUEになる
    private boolean mCheckUpdate = false;

    //緊急配信が存在する時にtrueとする
    private boolean isEnableEmergencyContent = false;

    // ハンドラメッセージ種別
    public static final int HANDLE_MSG_PLAY_FIRST_FORCE = 0;
    public static final int HANDLE_MSG_CHECK_UPDATE = 1;
    public static final int HANDLE_MSG_NEXT_CONTENT = 2;
    public static final int HANDLE_MSG_CURRENT_CONTENT = 3;
    public static final int HANDLE_MSG_POST_HEALTH_CHECK = 4;
    public static final int HANDLE_MSG_SKIPPING_WEB_VIEW = 5;
    public static final int HANDLE_MSG_EMERGENCY = 119;
    public static final int HANDLE_MSG_VIEW_ERROR = 99;

    public static final int HANDLE_MSG_SHOW_DIALOG = 10;

    //check update handle
//    private static final String ACTION_PREFIX = APPLICATION_ID + ".PlayerBroadcastReceiver.";
    private static final String ACTION_PREFIX = "jp.co.digitalcruise.admint.player.activity.viewer.ViewerActivity.";
    public static final String ACTION_CHECK_UPDATE = ACTION_PREFIX + "CHECK_UPDATE";

    // コンテンツの描画開始時間
    // ハンドラからの描画処理が衝突しないよう時刻を保持
    private long mStartViewTimestamp = 0;

    // 静止画・THETA画像・WebViewタイマー
    private WaitThread mWaitThread = null;

    private boolean mCheckLookAhead = false;

    // レシーバー
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (intent == null || intent.getAction() == null) {
                    return;
                }

                // Viewerが起動中の時
                if (AdmintApplication.getInstance().getViewerStatus() == AdmintApplication.VIEWER_STATUS_FOREGROUND) {
                    if (Intent.ACTION_TIME_TICK.equals(intent.getAction())) {
                        // デフォルトコンテンツの時、またはビューリストが空の時
                        if (mViewlist.sched_type == HealthCheckService.SCHED_TYPE_DEFAULT || mViewlist.play_items.size() == 0) {
                            updateTimeTextView();
                        }
//                        if (mViewlist.sched_type == HealthCheckService.SCHED_TYPE_DEFAULT) {
//                            updateTimeTextView();
//                        } else if (mViewlist.play_items.size() == 0) {
//                            updateTimeTextView();
//                            sendSelfMessage(HANDLE_MSG_CHECK_UPDATE);
////                            checkUpdate(0);
//                        }
                    } else if(intent.getAction().equals(ACTION_CHECK_UPDATE)){
                        checkUpdate(0);
                    }
                }
            } catch (Exception e) {
                Logging.stackTrace(e);
            }
        }
    };

    static private class ViewerHandler extends Handler {
        private WeakReference<ViewerActivity> mActivity;

        ViewerHandler(ViewerActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        void destructor(){
            if(mActivity != null) {
                mActivity.clear();
                mActivity = null;
            }
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                if(mActivity == null || mActivity.get() == null){
                    Logging.notice("ViewerHandler not Activity Reference");
                    return;
                }
                if(AdmintApplication.getInstance().getViewerStatus() != AdmintApplication.VIEWER_STATUS_FOREGROUND){
                    Logging.notice("ViewerHandler ViewerActivity not Foreground");
                    return;
                }
                // ViewerActivityへの参照があり、かつViewerが起動中の時
                ViewerActivity my_activity = mActivity.get();
                boolean is_force;
                switch (msg.what) {
                    case HANDLE_MSG_PLAY_FIRST_FORCE:
                        if(DefaultPref.getDebugMode()){
                            Toast.makeText(my_activity,"Viewer Receive PLAY_FIRST_FORCE",Toast.LENGTH_SHORT).show();
                        }
                        my_activity.playFirstContentForce();
                        break;
                    case HANDLE_MSG_CHECK_UPDATE:
                        if(DefaultPref.getDebugMode()){
                            Toast.makeText(my_activity,"Viewer Receive CHECK_UPDATE",Toast.LENGTH_SHORT).show();
                        }

                        int id = 0;
                        if(msg.obj instanceof Integer){
                            id = (int)msg.obj;
                        }
                        my_activity.checkUpdate(id);
                        break;

                    case HANDLE_MSG_POST_HEALTH_CHECK:
                        if(DefaultPref.getDebugMode()){
                            Toast.makeText(my_activity,"Viewer Receive HANDLE_MSG_POST_HEALTH_CHECK",Toast.LENGTH_SHORT).show();
                        }

                        is_force = false;
                        if(msg.obj instanceof  Boolean){
                             is_force = (boolean)msg.obj;
                        }

                        my_activity.postHealthCheck(is_force);
                        break;
                    case HANDLE_MSG_NEXT_CONTENT:
                        if (msg.obj instanceof Long) {
                            long prev_timestamp = (long) msg.obj;
                            if (prev_timestamp == my_activity.mStartViewTimestamp) {
                                my_activity.playNextContent();
                            }
                        }
                        break;
                    case HANDLE_MSG_CURRENT_CONTENT:
                        if (msg.obj instanceof Long) {
                            long prev_timestamp = (long) msg.obj;
                            if (prev_timestamp == my_activity.mStartViewTimestamp) {
                                my_activity.stopTouchContent();
                                PlayItem cur_item = my_activity.getCurrentPlayItem();
                                my_activity.playFirstContent(cur_item);
                            }
                        }
                        break;
                    case HANDLE_MSG_VIEW_ERROR:
                        my_activity.playFirstContentForce();
                        break;

                    case HANDLE_MSG_SKIPPING_WEB_VIEW:
                        if(my_activity.viewTypeIsNotWebOnly() && !my_activity.mCheckLookAhead){
                            my_activity.playNextContent();
                        }

                    case HANDLE_MSG_EMERGENCY:
                        Logging.info(my_activity.getString(R.string.log_info_detected_rescue_content));
                        is_force = false;
                        if(msg.obj instanceof  Boolean){
                            is_force = (boolean)msg.obj;
                        }

                        //新規に緊急配信を検知した場合ダウンロードが終わったら切り替わるようにフラグ建て
                        //デフォルトコンテンツが流れている時もtrueにしましょう。でないとステータス出ちゃったりします
                        if(is_force || my_activity.mViewlist.sched_type == HealthCheckService.SCHED_TYPE_DEFAULT){
                            if(!my_activity.isEnableEmergencyContent){
                                my_activity.isPendingDownload = true;
                                my_activity.isEnableEmergencyContent = true;
                            } else {
                                my_activity.checkUpdate(0);
                            }
                        } else {
                            my_activity.checkUpdate(0);
                        }
                        break;

                    case HANDLE_MSG_SHOW_DIALOG:
                        my_activity.dialog();
                        break;
                }
            }catch (Exception e){
                Logging.stackTrace(e);
            }
        }
    }

    private class WaitThread extends Thread{
        Messenger mViewerMessenger;
        long mSleepTime = 0;
        long mStartViewTimestamp = 0;

        private WaitThread(Messenger handler){
            mViewerMessenger = handler;
        }

        private void setTimer(long sleep_time, long start_view_time){
            mSleepTime = sleep_time;
            mStartViewTimestamp = start_view_time;
        }

        @Override
        public void run(){
            try{
                Thread.sleep(mSleepTime);
                Message msg = Message.obtain(null, HANDLE_MSG_NEXT_CONTENT, mStartViewTimestamp);
                mViewerMessenger.send(msg);
            } catch (InterruptedException ignored) {

            } catch (RemoteException e) {
                Logging.stackTrace(e);
            }
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState){

        try {
            super.onCreate(savedInstanceState);
            if(DeviceDef.isGroova()){
                TVPlatform.getLifeCycleControl().onCreate(this);
            }
            Logging.notice("ViewerActivity onCreate(), getViewerStatus() = " + AdmintApplication.getInstance().getViewerStatus());
//            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            requestWindowFeature(Window.FEATURE_NO_TITLE);

        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    @Override
    protected void onDestroy(){
        try {
            super.onDestroy();
            if(DeviceDef.isGroova()){
                TVPlatform.getLifeCycleControl().onDestroy(this);
            }
            Logging.notice("ViewerActivity onDestroy(), getViewerStatus() = " + AdmintApplication.getInstance().getViewerStatus());
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    @Override
    protected void onResume() {
        try {
            super.onResume();

            //PlayerActivityにも適用するとリモコンでメニューが開けなくなるバグが出たため特別に対応
            if(Build.MODEL.equals(DeviceDef.PN_M_SERIES) || Build.MODEL.equals(DeviceDef.PN_B_SERIES)) {
                getWindow().getDecorView().setSystemUiVisibility(0x00000008);
            }

            if(DeviceDef.isGroova()){
                TVPlatform.getLifeCycleControl().onResume(this, this);
            }
            Logging.notice("ViewerActivity onResume(), getViewerStatus() = " + AdmintApplication.getInstance().getViewerStatus());
            Logging.info(getString(R.string.log_info_start_viewer));

            initialize();

            // Viewlist更新（作成）
            updateViewlist();

            // 先頭から再生開始
            playFirstContent(null);

            // ステータス表示用時刻更新レシーバー登録
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            registerReceiver(mReceiver, filter);

            IntentFilter update_filter = new IntentFilter();
            update_filter.addAction(ACTION_CHECK_UPDATE);
            registerReceiver(mReceiver, update_filter);

            // ボタン配置
            setButton();

        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    /**
     * Place a button on the view.
     */
    private void setButton(){

        Button button = findViewById(R.id.clearButton);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                Button loginButton = findViewById(R.id.login_btn_exec);
                loginButton.setEnabled(true);
                loginButton.setVisibility(View.VISIBLE);
                loginButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        loginButton.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });

        // ログインボタン
        // 押下時に認証ダイアログ表示要求通知を送信する。
        // 本来なら、ユーザー認証設定が有効なとき、かつ、ログアウト中のみログイン画面を表示するところまで考慮が必要。
//        Button loginButton = findViewById(R.id.login_btn_exec);
//        loginButton.setEnabled(true);
//        loginButton.setVisibility(View.VISIBLE);
//        loginButton.setOnClickListener(view -> {
//            Intent intent = new Intent("jp.co.ricoh.isdk.sdkservice.auth.DISPLAY_LOGIN_SCREEN");
//            getApplicationContext().sendBroadcast(intent, null);
//        });

    }

    @Override
    protected void onPause() {
        try {
            super.onPause();
            if(DeviceDef.isGroova()){
                TVPlatform.getLifeCycleControl().onPause(this);
            }
            Logging.notice("ViewerActivity onPause(), getViewerStatus() = " + AdmintApplication.getInstance().getViewerStatus());
            Logging.info(getString(R.string.log_info_finish_viewer));
            destory();

            unregisterReceiver(mReceiver);
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(DeviceDef.isGroova()){
            TVPlatform.getLifeCycleControl().onStart(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(DeviceDef.isGroova()){
            TVPlatform.getLifeCycleControl().onStop(this);
        }
    }

    @Override
    public void onReady() {
        try{
            TVPropertyManager propm = TVPlatform.getManager().getPropertyManager();
            String enabled = propm.getProperty("net.http_proxy.enable");
            String TOTO_PROXY_TRUE = "1";
            if(TOTO_PROXY_TRUE.equals(enabled)){
                String host = propm.getProperty("net.http_proxy.host");
                String port = propm.getProperty("net.http_proxy.port");
                String user = propm.getProperty("net.http_proxy.username");
                String password = propm.getProperty("net.http_proxy.password");

                GroovaProxyPref.setGroovaProxyEnable(true);
                GroovaProxyPref.setGroovaProxyHost(host);
                GroovaProxyPref.setGroovaProxyPort(port);
                GroovaProxyPref.setGroovaProxyUser(user);
                GroovaProxyPref.setGroovaProxyPassword(password);
            } else {
                GroovaProxyPref.setGroovaProxyEnable(false);
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            Logging.notice("ViewerActivity onTouchEvent(), getViewerStatus() = " + AdmintApplication.getInstance().getViewerStatus());

            if (mViewlist.play_items.size() > 0 && mClickTouchTimestamp == 0) {
                mClickTouchTimestamp = System.currentTimeMillis();
                PlayItem view_item = getCurrentPlayItem();
                if (view_item != null && view_item.tcid > 0 && view_item.view_type != VIEW_TYPE_TOUCH) {
                    startClickTouchContent(view_item);
                    return true;
                }
            }

        }catch (Exception e){
            Logging.stackTrace(e);
        }
        return false;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        Logging.notice("ViewerActivity onServiceConnected(), getViewerStatus() = " + AdmintApplication.getInstance().getViewerStatus());
        Messenger service_messenger = new Messenger(service);
        try {
            Message msg = Message.obtain(null, PlaylistService.HANDLE_MSG_CONNECT, null);
            msg.replyTo = mSelfMessenger;
            service_messenger.send(msg);
        } catch (RemoteException e) {
            Logging.stackTrace(e);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Logging.notice("ViewerActivity onServiceDisconnected(), getViewerStatus() = " + AdmintApplication.getInstance().getViewerStatus());
    }

    @Override
    public void onCustomDialogClick(int which) {
        mTouchContent.changePattern(mTouchContent.getTouchMenu(), mTouchContent.getPatternList().get(which).id);
    }

//    public void sendSelfMessage(int what) throws RemoteException {
//        Message msg = Message.obtain(null, what, 0);
//        mSelfMessenger.send(msg);
//    }

    private void checkUpdate(int id){
        if(mViewlist.play_items.size() == 0) {
            updateTimeTextView();
            updateViewlist();
            if (mViewlist.play_items.size() > 0) {
                playFirstContent(null);
            }
        }else if(mViewlist.sched_type == HealthCheckService.SCHED_TYPE_DEFAULT && mPlaylist.sched_type != HealthCheckService.SCHED_TYPE_DEFAULT){
            //現在デフォルトコンテンツが流れている。
            // ヘルスチェック等で再生するものは決まっているが、プレイリストの中で再生できるものが無い場合。
            boolean is_refresh = false;
            for(PlayItem play_item : mPlaylist.play_items){
                if(play_item.id == id){
                    is_refresh = true;
                }
            }

            if(is_refresh || isEnableEmergencyContent) {
                playFirstContentForce();
            }else{
                mCheckUpdate = true;
            }
        }else if(isPendingDownload && id > 0){
            playFirstContentForce();
        }else{
            //既に何かしらのコンテンツが流れている場合
            mCheckUpdate = true;
        }
        if(!isEnableEmergencyContent){
            updateScrollView();
        }
    }

    private void postHealthCheck(boolean is_force){
        PlaylistObject playlist = getCurrentPlaylist();
//        Toast.makeText(my_activity, "Now view schedule type is " + my_activity.mViewlist.sched_type, Toast.LENGTH_SHORT).show();
//        Toast.makeText(my_activity, "Now view play item is " + my_activity.mViewlist.play_items.size(), Toast.LENGTH_SHORT).show();

        //再生対象がデフォルトではないかつ今現在再生可能なコンテンツが存在しているかつ新しいプレイリストに再生するスケジュールが存在している場合
        //新しいプレイリストの中に今現在再生できるコンテンツが存在していないまたはすべてが不正なコンテンツである場合
        if(mViewlist.sched_type != HealthCheckService.SCHED_TYPE_DEFAULT && mViewlist.play_items.size() > 0 && playlist.play_items.size() > 0){
            if(!isAvailableList(playlist.play_items)|| isAllInvalidContent(playlist.play_items)) {
                isPendingDownload = true;
                mCheckUpdate = false;
                setNextUpdateCheckAlarm();
            } else {
                if(is_force) {
                    playFirstContentForce();
                } else {
                    mCheckUpdate = true;
                }
            }
        } else {
            //既にDL済のコンテンツがある場合
            if(is_force){
                playFirstContentForce();
            } else {
                mCheckUpdate = true;
            }
        }
    }

    private void setNextUpdateCheckAlarm(){
        int nextCheckTime = 1200000;
        // ヘルスチェックアラーム設定
        Intent makeIntent = new Intent();
        makeIntent.setAction(ACTION_CHECK_UPDATE);
        PendingIntent pintent = PendingIntent.getBroadcast(this, 0, makeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        long trigger_at_mills = System.currentTimeMillis() + nextCheckTime;
        CompatibleSdk.setAlarmEx(this,trigger_at_mills, pintent);

        // 次回ヘルスチェック日時設定
//        HealthCheckPref.setNextHealthCheckTime(trigger_at_mills);
    }

    private PlaylistObject getCurrentPlaylist(){
        PlaylistObject playlist_obj = new PlaylistObject();
        SQLiteDatabase rdb = mPlaylistDbHelper.getReaderDb();

        //テロップ更新チェック
        //緊急コンテンツが流れていない時だけ更新をかける
        if(!isEnableEmergencyContent){
            updateScrollView();
        }

        isEnableEmergencyContent = false;//初期化

        //緊急情報取得
        GetRescueDbData grdd = new GetRescueDbData();
        if(grdd.checkEmergencyList(this)){
            isEnableEmergencyContent = true;
            playlist_obj = grdd.getEmergencyPlayList(this);
            //コンテンツの順番に乱れが生じるのでソートは行わなずそのまま返す
            return playlist_obj;
        }


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

    private PlaylistObject getCurrentDefaultlist(){
        PlaylistObject defaultlist_obj = new PlaylistObject();
        SQLiteDatabase rdb = mPlaylistDbHelper.getReaderDb();

        // play_list_info
        {
            String sql = "select " +
                    PlaylistDbHelper.TABLE_DEFAULT_LIST_INFO.SCHED_TYPE + "," +
                    PlaylistDbHelper.TABLE_DEFAULT_LIST_INFO.ST + "," +
                    PlaylistDbHelper.TABLE_DEFAULT_LIST_INFO.UT + "," +
                    PlaylistDbHelper.TABLE_DEFAULT_LIST_INFO.PT + "," +
                    PlaylistDbHelper.TABLE_DEFAULT_LIST_INFO.PO +
                    " from " + PlaylistDbHelper.TABLE_DEFAULT_LIST_INFO.getName();

            try (Cursor cursor = rdb.rawQuery(sql, null)) {
                if (cursor.moveToNext()) {
                    defaultlist_obj.sched_type = cursor.getInt(0);
                    defaultlist_obj.st = cursor.getLong(1);
                    defaultlist_obj.ut = cursor.getLong(2);
                    defaultlist_obj.pt = cursor.getLong(3);
                    defaultlist_obj.po = cursor.getInt(4);
                }
            }
        }

        // play_list
        {
            String sql = "select " +
                    PlaylistDbHelper.TABLE_DEFAULT_LIST.ID + "," +
                    PlaylistDbHelper.TABLE_DEFAULT_LIST.O + "," +
                    PlaylistDbHelper.TABLE_DEFAULT_LIST.D + "," +
                    PlaylistDbHelper.TABLE_DEFAULT_LIST.T + "," +
                    PlaylistDbHelper.TABLE_DEFAULT_LIST.TCID + "," +
                    PlaylistDbHelper.TABLE_DEFAULT_LIST.TCD + "," +
                    PlaylistDbHelper.TABLE_DEFAULT_LIST.F_ID + "," +
                    PlaylistDbHelper.TABLE_DEFAULT_LIST.F_O + "," +
                    PlaylistDbHelper.TABLE_DEFAULT_LIST.W_URL +
                    " from " + PlaylistDbHelper.TABLE_DEFAULT_LIST.getName();

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

                    defaultlist_obj.play_items.add(play_item);
                }
            }
        }

        // o, f_o順でソート
        Collections.sort(defaultlist_obj.play_items);

        return defaultlist_obj;
    }

    private void makeViewlist(PlaylistObject play_list){

        isPendingDownload = false;

        mViewlist.sched_type = play_list.sched_type;
        mViewlist.po = play_list.po;
        mViewlist.pt = play_list.pt;
        mViewlist.st = play_list.st;
        mViewlist.tcd = play_list.tcd;
        mViewlist.tcid = play_list.tcid;
        mViewlist.ut = play_list.ut;

        mViewlist.play_items.clear();

        for(PlayItem pl_item : play_list.play_items){

            String media_path = null;
            if(pl_item.t == HealthCheckService.CONTENT_TYPE_WEBVIEW){
                if(pl_item.w_url.length() > 0){
                    media_path = pl_item.w_url;
                }
            }else if (!pl_item.media_path.equals("")){
                media_path = pl_item.media_path;
            } else {
                media_path = getMediaPath(pl_item.t, pl_item.id, pl_item.f_id);
            }

            if(media_path != null) {
                PlayItem vl_item = new PlayItem();
                vl_item.id = pl_item.id;
                vl_item.o = pl_item.o;
                vl_item.d = pl_item.d;
                vl_item.t = pl_item.t;
                vl_item.f_id = pl_item.f_id;
                vl_item.f_o = pl_item.f_o;
                vl_item.w_url = pl_item.w_url;
                vl_item.media_path = media_path;
                vl_item.view_type = getViewType(pl_item.t);

                if(pl_item.tcid > 0) {
                    String touch_media_path = getMediaPath(HealthCheckService.CONTENT_TYPE_TOUCH, pl_item.tcid, 0);
                    if(touch_media_path != null){
                        vl_item.touch_media_path = touch_media_path;
                        vl_item.tcd = pl_item.tcd;
                        vl_item.tcid = pl_item.tcid;
                    }
                }
                mViewlist.play_items.add(vl_item);
            }
        }
        // DB更新（DBの値は不要コンテンツ削除時に再生中のコンテンツを削除しないための判定に利用）
        deleteAndInsertVewilistData();
    }

    private void updateViewlist(){
        try {
            mCheckUpdate = false;

            // プレイリストを取得
            mPlaylist = getCurrentPlaylist();

            // プレイリストからビューリストを作成
            makeViewlist(mPlaylist);

            // 配信可能なリストが存在しない場合はデフォルトコンテンツ採用を検討
            if(mPlaylist.play_items.size() == 0 || mViewlist.play_items.size() == 0 && mViewlist.sched_type != HealthCheckService.SCHED_TYPE_DEFAULT){
                PlaylistObject default_list = getCurrentDefaultlist();
                // 有効なデフォルトコンテンツが存在
                if(isAvailableList(default_list.play_items)){
                    makeViewlist(default_list);
                    Logging.info(getString(R.string.log_info_viewer_no_exist_playlist_play_default));
                }else{
                    Logging.info(getString(R.string.log_info_viewer_no_exist_play_content));
                }
            }else{
                if(mViewlist.sched_type == HealthCheckService.SCHED_TYPE_HEALTH_CHECK) {
                    if(isEnableEmergencyContent){
                        Logging.info(getString(R.string.log_info_playback_rescue_contents));
                    } else {
                        Logging.info(getString(R.string.log_info_viewer_play_healthcheck_schedule));
                    }
                }else if(mViewlist.sched_type == HealthCheckService.SCHED_TYPE_AHEAD_LOAD){
                    Logging.info(getString(R.string.log_info_viewer_play_aheadload_schedule));
                }else if(mViewlist.sched_type == HealthCheckService.SCHED_TYPE_SD_CARD) {
                    Logging.info(getString(R.string.log_info_viewer_play_externalstorage_schedule));
                }
            }

        }catch (Exception e){
            // readロック中にDBを参照しロック時間が長い場合は例外が飛んでくるかも
            Logging.stackTrace(e);
        }
    }

    private void deleteAndInsertVewilistData(){

        SQLiteDatabase wdb = mViewlistDbHelper.getWriterDb();
        wdb.delete(ViewlistDbHelper.TABLE_VIEW_LIST.getName(), null, null);

        int view_order = 0;
        for (PlayItem view_item : mViewlist.play_items) {
            ContentValues values = new ContentValues();
            values.put(ViewlistDbHelper.TABLE_VIEW_LIST.ID, view_item.id);
            values.put(ViewlistDbHelper.TABLE_VIEW_LIST.O, view_order);
            values.put(ViewlistDbHelper.TABLE_VIEW_LIST.F_ID, view_item.f_id);
            values.put(ViewlistDbHelper.TABLE_VIEW_LIST.T, view_item.t);
            values.put(ViewlistDbHelper.TABLE_VIEW_LIST.TCID, view_item.tcid);
            wdb.insert(ViewlistDbHelper.TABLE_VIEW_LIST.getName(), null, values);
            view_order++;
        }
    }

    private @Nullable String getMediaPath(int t, int id, int f_id){
        File media_file = null;
        if(mErrorDbHelper.isValidContent(id, f_id)) {
            if (t == HealthCheckService.CONTENT_TYPE_MOVIE || t == HealthCheckService.CONTENT_TYPE_PICTURE || t == HealthCheckService.CONTENT_TYPE_THEATA) {
                media_file = ContentFile.getContentMediaFile(id);
            }else if (t == HealthCheckService.CONTENT_TYPE_EXTERNAL_MOVIE || t == HealthCheckService.CONTENT_TYPE_EXTERNAL_PICTURE) {
                media_file = ContentFile.getExternalMediaFile(id, f_id);
            }else if(t == HealthCheckService.CONTENT_TYPE_TOUCH){
                if(ContentFile.getTouchContentReady(id).isFile()) {
                    media_file = ContentFile.getTouchDataFile(id);
                }
            }

            if(media_file != null && media_file.isFile()){
                return media_file.getAbsolutePath();
            }
        }
        return null;
    }

    private void initialize() {

        setContentView(R.layout.viewer);
        mViewerLayout = findViewById(R.id.viewer_layout);
        mTelopLayout = findViewById(R.id.telop_layout);

        // オブジェクト作成
        mViewlist = new PlaylistObject();
        mPlaylist = new PlaylistObject();
        mPlaylistDbHelper = new PlaylistDbHelper(this);
        mViewlistDbHelper = new ViewlistDbHelper(this);
        mErrorDbHelper = new ErrorDbHelper(this);
        mPlayLogDbHelper = new PlayLogDbHelper(this);

        // メッセージハンドラ
        mViewerHandler = new ViewerHandler(this);
        mSelfMessenger = new Messenger(mViewerHandler);

        // ステータスコード
        createTimeTextView();

        // ブランク表示
        createBlankView();

        // 静止画
        createImageView();

        // 動画
        createVideoView();

        // タッチ
        createTouchConetntAndView();

        // THETA
        createThetaView();

        // WebView
        createWebView();

        // 無音MediaPlayer
        createSoundlessPlayer();

        // テロップビュー
        createScrollView();

        // Serviceに接続
        Intent intent = new Intent(this, PlaylistService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    private void destory(){
        // サービス切断
        unbindService(this);

        // メッセージハンドラ
        if(mViewerHandler != null){
            mViewerHandler.destructor();
            mViewerHandler = null;
        }
        mSelfMessenger = null;

        // タイマー停止
        cancelWaitThread();

        // オブジェクト破棄
        if(mPlaylistDbHelper != null) {
            mPlaylistDbHelper.close();
            mPlaylistDbHelper = null;
        }

        if(mViewlistDbHelper != null) {
            mViewlistDbHelper.close();
            mViewlistDbHelper = null;
        }

        if(mErrorDbHelper != null) {
            mErrorDbHelper.close();
            mErrorDbHelper = null;
        }

        if(mPlayLogDbHelper != null){
            mPlayLogDbHelper.close();
            mPlayLogDbHelper = null;
        }

        // 静止画
        if(mImageView != null){
            mImageView = null;
        }

        // 動画
        if(mMediaPlayer != null){
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if(mSurfaceView != null){
            mSurfaceView = null;
        }

        // タッチ
        // THETA
        if(mThetaImageView != null){
            if(mIsThetaInit) {
                mThetaImageView.stopContent();
                mIsThetaInit = false;
            }
            mThetaImageView = null;
        }

        // webview
        if(mViewerWebView != null && mViewerWebView.getPlayItem() != null) {
            mViewerWebView.stopLoading();
            mViewerWebView.onPause();
            mViewerWebView.clearCache(true);
            mViewerWebView.setWebViewClient(null);
            mViewerWebView.destroy();
            mViewerWebView = null;
        }

        if(mTouchView != null){
            if(mTouchContent != null){
                mTouchContent.cleanTouchObject();
                mTouchContent = null;
            }
            mTouchView = null;
        }

        // 無音MediaPlayer
        if(mSoundlessMediaPlayer != null){
            mSoundlessMediaPlayer.reset();
            mSoundlessMediaPlayer.release();
            mSoundlessMediaPlayer = null;
        }

        if(mViewerLayout.getChildCount() > 0){
            mViewerLayout.removeAllViews();
            mViewerLayout = null;
        }
    }

    private void cancelWaitThread(){
        if(mWaitThread != null){
            mWaitThread.interrupt();
        }
        mWaitThread = null;
    }

    private void createImageView(){
        final FrameLayout.LayoutParams IMAGE_VIEW_PARAM = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

        mImageView = new ImageView(this);
        mImageView.setBackgroundColor(Color.BLACK);
        mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mImageView.setLayoutParams(IMAGE_VIEW_PARAM);
    }

    private void createSoundlessPlayer() {
        try {
            mSoundlessMediaPlayer = new MediaPlayer();
            mSoundlessMediaPlayer.setOnErrorListener((mp, what, extra) -> false);

            mSoundlessMediaPlayer.setLooping(true);

            AssetFileDescriptor afdescripter = getAssets().openFd("silence.mp3");
            mSoundlessMediaPlayer.setDataSource(afdescripter.getFileDescriptor(), afdescripter.getStartOffset(), afdescripter.getLength());
            mSoundlessMediaPlayer.prepare();
            mSoundlessMediaPlayer.start();
        } catch (IOException e) {
            Logging.stackTrace(e);
        }
    }

    private void removeOtherView(View cur_view){
        for(int i = 0 ; i < mViewerLayout.getChildCount(); i++){
            if(mViewerLayout.getChildAt(i).getClass() != cur_view.getClass()){
                // Thetaはremove -> addの描画が終わるのに数秒かかるのでinvisible -> visibleの切り替えで高速化を図る
                if(mViewerLayout.getChildAt(i).getClass() == mThetaImageView.getClass()){
                    mThetaImageView.setVisibility(View.INVISIBLE);
                }else {
                    mViewerLayout.removeViewAt(i);
                }
            }
        }
    }

    private void orderTopStatusCode(){
        if(mViewerLayout.indexOfChild(mTimeTextView) != -1) {
            mViewerLayout.removeView(mTimeTextView);
        }
        mViewerLayout.addView(mTimeTextView);
        updateTimeTextView();
    }

    private void replaceViewToImage(){
        if(mViewerLayout.indexOfChild(mImageView) == -1){
            mViewerLayout.addView(mImageView);
        }
        removeOtherView(mImageView);
    }

    private void replaceViewToBlank(){
        if(mViewerLayout.getChildCount() > 0){
            mViewerLayout.removeAllViews();
        }
        mViewerLayout.addView(mBlankLayout);
        orderTopStatusCode();
    }

    private void removeAndAddViewToSurface(){
        if(mViewerLayout.indexOfChild(mSurfaceView) >= 0){
            mViewerLayout.removeView(mSurfaceView);
        }
        mViewerLayout.addView(mSurfaceView);
        removeOtherView(mSurfaceView);
    }


    private void replaceViewToSurface(){
        if(mViewerLayout.indexOfChild(mSurfaceView) == -1){
            mViewerLayout.addView(mSurfaceView);
        }
        removeOtherView(mSurfaceView);
    }

    private void replaceViewToThetaImage(){
        if(mViewerLayout.indexOfChild(mThetaImageView) == -1){
            mViewerLayout.addView(mThetaImageView);
        }
        removeOtherView(mThetaImageView);
    }

    private void replaceViewToWeb(){
        if(mViewerLayout.indexOfChild(mViewerWebView) == -1){
            mViewerLayout.addView(mViewerWebView);
        }
        removeOtherView(mViewerWebView);
    }

    private void replaceViewToTouchView(){
        if(mViewerLayout.indexOfChild(mTouchView) == -1){
            mViewerLayout.addView(mTouchView);
        }
        removeOtherView(mTouchView);
    }

    private void startWaitTimer(long wait_time){
        cancelWaitThread();
        mWaitThread = new WaitThread(mSelfMessenger);
        mWaitThread.setTimer(wait_time, mStartViewTimestamp);
        mWaitThread.start();
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void createWebView() {
//        mViewerWebView = AdmintApplication.getViewerWebViewInstance();

        mViewerWebView = new ViewerWebView(this);

        FrameLayout.LayoutParams WEB_VIEW_PARAM = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mViewerWebView.setLayoutParams(WEB_VIEW_PARAM);

        mViewerWebView.getSettings().setJavaScriptEnabled(true);

        // http://dayafterneet.blogspot.com/2011/08/androidwebview_23.html
        // キャッシュの設定（キャッシュサイズ変更はメソッドdeprecatedとなっているので定義しない）
// webではなくアプリケーションキャッシュなので設定に意味はない
//        mViewerWebView.getSettings().setAppCachePath(AdmintPath.getWebViewCahceDir().getAbsolutePath());
//        mViewerWebView.getSettings().setAppCacheEnabled(true);
//        mViewerWebView.getSettings().setAllowFileAccess(true);
//        mViewerWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        mViewerWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

        // Viewer起動時と終了時にキャッシュクリア
        mViewerWebView.clearCache(true);

        // プラグイン(Flash等)はOFF
        mViewerWebView.getSettings().setPluginState(WebSettings.PluginState.OFF);
//
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
////            mViewerWebView.setWebContentsDebuggingEnabled(true);
////        }
        mViewerWebView.getSettings().setDomStorageEnabled(true);

        //オートスケーリング
        mViewerWebView.getSettings().setUseWideViewPort(true);
        mViewerWebView.getSettings().setLoadWithOverviewMode(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mViewerWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        }

        if(!DefaultPref.getDebugMode()){
            mViewerWebView.setOnTouchListener((v, event) -> true);
        }else{
            mViewerWebView.setOnTouchListener((v, event) -> false);
        }

        // WebViewのプロキシ設定
        if (DefaultPref.getProxyEnable() && !DefaultPref.getProxyWebcontent()) {
            mViewerWebView.setProxy(DefaultPref.getProxyHost(), DefaultPref.getProxyUser(), DefaultPref.getProxyPassword());
            setSystemPropertyProxy();
        } else {
            mViewerWebView.resetProxy();
            resetSystemPropertyProxy();
        }

    }

    private void createTouchConetntAndView(){
        mTouchView = new RelativeLayout(this);
        mTouchView.setBackgroundColor(Color.WHITE);

        mTouchContent = new TouchContent(this);

        Point display_size = getRealSize();
        Point scale_size = new Point();
        if(display_size.x >= display_size.y) {
            scale_size.x = display_size.y * display_size.y / display_size.x;
            scale_size.y = display_size.y;
        } else {
            scale_size.x = display_size.x;
            scale_size.y = display_size.x * display_size.x / display_size.y;
        }
        mTouchContent.initialize(mSelfMessenger, display_size, scale_size,mTouchView, getFragmentManager());
    }

    private void loadTouchContent(int id, long duration, int what){
        mTouchContent.loadTouchContent(id, duration, mStartViewTimestamp, what);
    }

    private void createBlankView(){
        final FrameLayout.LayoutParams BLANK_VIEW_PARAM = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        mBlankLayout = new FrameLayout(this);
        mBlankLayout.setLayoutParams(BLANK_VIEW_PARAM);
        mBlankLayout.setKeepScreenOn(false);
        mBlankLayout.setBackgroundColor(Color.BLACK);
    }

    private void createThetaView(){
        final FrameLayout.LayoutParams THETA_VIEW_PARAM = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mThetaImageView = new PhotoSphereView(this);
        mThetaImageView.setLayoutParams(THETA_VIEW_PARAM);
        mIsThetaInit = false;
    }

    private void initThetaView(String path){
        if(!mIsThetaInit){
            File init_theta = new File(path);
            if(init_theta.isFile()) {
                Point p = getRealSize();

                mThetaImageView.initialize(init_theta.getAbsolutePath(), p.x / 2, p.y / 2);
                mThetaImageView.setDefaultCameraRotate(0.0f, 0.0f, 0.0f);
                if (p.x % 9 == 0 && 16 * (p.x / 9) == p.y) {
                    mThetaImageView.setCameraAngle(120.0f);
                } else {
                    mThetaImageView.setCameraAngle(160.0f);
                }
                mThetaImageView.setEnableTouch(false);
                mThetaImageView.startAutoRotationByRadian(0.15f);

                mIsThetaInit = true;
            }
        }
    }

    private void createVideoView(){
        final FrameLayout.LayoutParams SURFACE_VIEW_PARAM = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        mSurfaceView = new ViewerSurfaceView(this);
        mSurfaceView.setLayoutParams(SURFACE_VIEW_PARAM);

        mMediaPlayer = new MediaPlayer();
        mSurfaceView.setMediaPlayer(mMediaPlayer);

        mMediaPlayer.setOnCompletionListener(mp -> playNextContent());

        mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
            PlayItem error_item = getCurrentPlayItem();
            errorContent(error_item);

            // trueを返しOnCompletionListenerを呼ばないようにする
            // https://developer.android.com/reference/android/media/MediaPlayer.OnErrorListener
            return true;
        });

        mMediaPlayer.setLooping(false);

    }

    private void drawThetaImage(String media_path) {

        initThetaView(media_path);

        mThetaImageView.startAutoRotationByRadian(0.15f);
        mThetaImageView.changeContent(media_path);
        mThetaImageView.resetCamera();
        mThetaImageView.setVisibility(View.VISIBLE);
    }

    private void drawImage(String media_path){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(media_path, options);

        options.inJustDecodeBounds = false;
        options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight);
        Bitmap bm = BitmapFactory.decodeFile(media_path, options);
        mImageView.setImageBitmap(bm);
        mImageView.setBackgroundColor(Color.BLACK);
        mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mImageView.setVisibility(View.VISIBLE);
    }

    private int calculateInSampleSize(int width, int height) {
        final int IMAGE_MAX_SIZE = 1920;
        int in_sample = 1;

        int upper = height;
        if(height < width){
            upper = width;
        }

        double scale;
        if(upper > IMAGE_MAX_SIZE){
            scale = Math.ceil((float)upper / (float)IMAGE_MAX_SIZE);
            if(scale > 1){
                int power = 1;
                for(int i = 2; i < scale; i *= 2) {
                    power += 1;
                }
                in_sample = (int)Math.pow(2, power);
            }
        }

        return in_sample;
    }

    private void startMediaPlayer(boolean is_start_after_change){
        mSurfaceView.startMediaPlayer(is_start_after_change);
    }

    private void startMediaPlayerChangeFixSize(boolean is_start_after_change){
        mSurfaceView.startMediaPlayerChangeSize(mCurrentScalePoint.x, mCurrentScalePoint.y, is_start_after_change);
    }

    private void stopPlayingMedia(){

        cancelWaitThread();

        if(mViewerLayout.indexOfChild(mSurfaceView) != -1) {
            mMediaPlayer.reset();
        }else if(mViewerLayout.indexOfChild(mThetaImageView) != -1){
            mThetaImageView.stopContent();
        }else if(mViewerLayout.indexOfChild(mImageView) != -1){
            mImageView.setVisibility(View.INVISIBLE);
        }else if(mViewerLayout.indexOfChild(mTouchView) != -1){
            stopTouchContent();
        }

        // webviewは先読みしている可能性があるので、親レイアウトに登録されているか否かで判断できない
        if(mViewerWebView != null && mViewerWebView.getPlayItem() != null) {
            drawBlankWebView();
        }
    }

    private void stopTouchContent(){
        if(mTouchContent != null) {
            if (mTouchContent.getDialog() != null) {
                mTouchContent.getDialog().dismiss();
            }
            mTouchContent.cleanTouchObject();
        }
    }

    private void readyMediaPlayer(String media_path) throws IOException {
        try(FileInputStream fis = new FileInputStream(media_path)) {
            mMediaPlayer.setDisplay(null);
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(fis.getFD());
            mMediaPlayer.prepare();

            Point p = getScaleSize(mMediaPlayer);
            if (p.x != mCurrentScalePoint.x || p.y != mCurrentScalePoint.y) {
                mIsScaleChange = true;
//                mCurrentScalePoint = p;
            }else{
                mIsScaleChange = false;
            }
            mCurrentScalePoint = p;
        }
    }

    private Point getScaleSize(MediaPlayer mp){
        int video_width = mp.getVideoWidth();
        int video_height= mp.getVideoHeight();
        Point display_size = getRealSize();
        float x_scale = (float)display_size.x / (float)video_width;
        float y_scale = (float)display_size.y / (float)video_height;
        float scale;
        if(x_scale < y_scale){
            scale = x_scale;
        }else{
            scale = y_scale;
        }
        int new_width = (int)((float)video_width * scale);
        int new_height = (int)((float)video_height * scale);
        return new Point(new_width, new_height);
    }

    private void errorContent(PlayItem play_item){
        try {
            if(play_item != null){
                // エラーマーク
                updateViewError(play_item.id, play_item.f_id);
            }
            Message msg = Message.obtain(null, HANDLE_MSG_VIEW_ERROR, null);
            mSelfMessenger.send(msg);
        } catch (Exception e) {
            Logging.stackTrace(e);
            // リカバリ不能
            // 一度Activityを落としてRecoverServiceに復帰させてもらうのを待ってみる
            Logging.error(getString(R.string.log_error_finish_viewer_error));
            finish();
        }
    }

    private void playFirstContentForce(){
        stopPlayingMedia();
        updateViewlist();
        playFirstContent(null);
    }

    private void updateViewError(int id, int f_id){

        SQLiteDatabase wdb = mErrorDbHelper.getWriterDb();

        int retry_count = 0;
        // 対象コンテンツのエラー（リトライ）回数を取得

        String where =  " where " + ErrorDbHelper.TABLE_VIEW_ERROR.ID + " = " + id + " and " +
                ErrorDbHelper.TABLE_VIEW_ERROR.F_ID + " = " + f_id;

        String sql = "select " + ErrorDbHelper.TABLE_VIEW_ERROR.RETRY_COUNT +
                " from " +  ErrorDbHelper.TABLE_VIEW_ERROR.getName() +
                where;

        try (Cursor cursor = wdb.rawQuery(sql, null)) {
            if (cursor.moveToNext()) {
                retry_count = cursor.getInt(0);
            }
        }

        // カウントアップ
        retry_count++;

        ContentValues values = new ContentValues();
        values.put(ErrorDbHelper.TABLE_VIEW_ERROR.ID, id);
        values.put(ErrorDbHelper.TABLE_VIEW_ERROR.F_ID, f_id);
        values.put(ErrorDbHelper.TABLE_VIEW_ERROR.RETRY_COUNT, retry_count);
        // 2019/1/11 Fri
        // 重たいコンテンツ(100MBを超える)をダウンロード中にSQLのupdate処理が行われると
        // 端末のディスクが圧迫され、処理が進まなくなりANRが発生することがある。
        // Viewerのエラーはそうそう起こるものではないレアケースかつサービスに逃がすのは問題があるためUIスレッドで行う。
        // Errorが出てしまい問合せを受けた場合はエラーの出ない動画に差し替えることを伝える事。

        wdb.replace(ErrorDbHelper.TABLE_VIEW_ERROR.getName(), null, values);
    }

    private void drawBlankWebView(){
        mViewerWebView.stopLoading();
        mViewerWebView.loadUrl("about:blank");
        mViewerWebView.reload();
    }

    private void resetTouchContentDurationAlarm(){
        mTouchContent.resetDurationAlarm(mStartViewTimestamp);
    }

    private void playNextContent(){

        // タイマー等別スレッドでコールされることがあるのでブロック
        if(AdmintApplication.getInstance().getViewerStatus() != AdmintApplication.VIEWER_STATUS_FOREGROUND){
            return;
        }

        mStartViewTimestamp = System.currentTimeMillis();
        mClickTouchTimestamp = 0;

        PlayItem next_item = null;
        try {
            // タッチコンテンツのリスナー解除

            PlayItem cur_item = getCurrentPlayItem();
            next_item = moveNextPlayItem();

            if (cur_item == null || next_item == null) {
                // 中断を伴わないプレイリストの変更があったが再生可能なコンテンツが存在しない
                // ブランク表示
                replaceViewToBlank();
                return;
            }

            if(cur_item.view_type == VIEW_TYPE_WEB){
                drawBlankWebView();
            }

            if(cur_item.view_type == VIEW_TYPE_TOUCH && next_item.view_type != VIEW_TYPE_TOUCH){
                stopTouchContent();
            }

            // 再生ロギング
            loggingPlay(next_item.id);

            //次に再生するコンテンツのタイプが〇〇だったら
            switch (next_item.view_type) {
                case VIEW_TYPE_MOVIE:
                    readyMediaPlayer(next_item.media_path);
                    //現在流れているコンテンツのタイプ調べ
                    if (cur_item.view_type == VIEW_TYPE_MOVIE) {
                        if(DeviceDef.isGroova() && DefaultPref.getFastVideoChange()) {
                            // Groovaの高速切替対応
                            startMediaPlayerChangeFixSize(false);
                        }else{
                            //アスペクト比が変更されていたら
                            if (mIsScaleChange) {
                                removeAndAddViewToSurface();
                                startMediaPlayerChangeFixSize(true);
                            } else {
                                startMediaPlayer(false);
                            }
                        }
                    } else {
                        if (mIsScaleChange) {
                            removeAndAddViewToSurface();
                            startMediaPlayerChangeFixSize(true);
                        } else {
                            replaceViewToSurface();
                            startMediaPlayer(true);
                        }
                    }
                    break;
                case VIEW_TYPE_PICTURE:
                    if (cur_item.view_type == VIEW_TYPE_PICTURE) {
                        if (!cur_item.media_path.equals(next_item.media_path)) {//画像の切り替え判定
                            drawImage(next_item.media_path);
                        }
                    }else{
                        replaceViewToImage();
                        drawImage(next_item.media_path);
                    }
                    startWaitTimer(next_item.d * 1000);

                    break;
                case VIEW_TYPE_THETA:
                    if (cur_item.view_type == VIEW_TYPE_THETA) {
                        if (!cur_item.media_path.equals(next_item.media_path)) {
                            drawThetaImage(next_item.media_path);
                        }
                    }else{
                        replaceViewToThetaImage();
                        drawThetaImage(next_item.media_path);
                    }
                    startWaitTimer(next_item.d * 1000);

                    break;
                case VIEW_TYPE_TOUCH:
                    if(cur_item.view_type == VIEW_TYPE_TOUCH && next_item.id == cur_item.id){

                        resetTouchContentDurationAlarm();
                    }else{
                        if(cur_item.view_type == VIEW_TYPE_TOUCH){
                            stopTouchContent();
                        }else{
                            replaceViewToTouchView();
                        }
                        loadTouchContent(next_item.id, next_item.d * 1000, ViewerActivity.HANDLE_MSG_NEXT_CONTENT);
                    }
                    break;
                case VIEW_TYPE_WEB:
                    if(cur_item.view_type != VIEW_TYPE_WEB){
                        replaceViewToWeb();
                    }

                    // 先読みしているページを表示すると、（先読み）ロードのタイミングと実際のタイミングで矛盾が発生するのでやめる
    //                PlayItem preload_web_view = mViewerWebView.getPlayItem();
    //                if(preload_web_view == null || preload_web_view.id != next_item.id || !preload_web_view.media_path.equals(next_item.media_path)){
    //                    loadWebView(next_item);
    //                }
                    mCheckLookAhead = false;
                    loadWebView(next_item);

                    startWaitTimer(next_item.d * 1000);
                    break;

                case VIEW_TYPE_ORIGINAL :
                    if(cur_item.view_type == VIEW_TYPE_ORIGINAL && next_item.id == cur_item.id){
                        playFirstContent(cur_item);
                    } else {
                        if(cur_item.view_type == VIEW_TYPE_ORIGINAL){
                            stopTouchContent();
                        } else {
                            replaceViewToTouchView();
                        }
                        loadTouchContent(next_item.id, next_item.d * 1000, ViewerActivity.HANDLE_MSG_NEXT_CONTENT);
                    }
                    break;
            }

            // webviewの先読み
            if(cur_item.view_type == VIEW_TYPE_WEB && next_item.view_type != VIEW_TYPE_WEB){
                mCheckLookAhead = true;
                PlayItem next_web_view = getNextWebViewPlayItem();
                if (next_web_view != null) {
                    loadWebView(next_web_view);
                }
            }

            // ステータスコード表示
            if(mViewlist.sched_type == HealthCheckService.SCHED_TYPE_DEFAULT && !isEnableEmergencyContent) {
                orderTopStatusCode();
            }
        }catch (Exception e){
            Logging.stackTrace(e);
            errorContent(next_item);
        }
    }

    private void playFirstContent(@Nullable PlayItem resume_item){
        // タイマー等別スレッドでコールされることがあるのでブロック
        if(AdmintApplication.getInstance().getViewerStatus() != AdmintApplication.VIEWER_STATUS_FOREGROUND){
            return;
        }

        // スケジュールタイプによって表示位置が異なるので再作成
        createTimeTextView();

        mStartViewTimestamp = System.currentTimeMillis();
        mClickTouchTimestamp = 0;

        // パラメータ初期化
        mCurrentScalePoint.y = 0;
        mCurrentScalePoint.x = 0;

        PlayItem play_item = null;
        try {
            // 子Veiwをremove
            if(mViewerLayout.getChildCount() > 0){
                mViewerLayout.removeAllViews();
            }

            // resume_itemが存在する時はタッチコンテンツからのリターン
            if(resume_item == null) {
                play_item = moveFirstPlayItem();
            }else{
                play_item = resume_item;
            }

            if(play_item == null){
                // ブランク表示
                replaceViewToBlank();
                return;
            }

            // 再生ロギング
            loggingPlay(play_item.id);

            if (play_item.view_type == VIEW_TYPE_MOVIE) {
                // 動画
                readyMediaPlayer(play_item.media_path);
                replaceViewToSurface();
//                startMediaPlayer();
                startMediaPlayerChangeFixSize(true);
            } else if (play_item.view_type == VIEW_TYPE_PICTURE) {
                replaceViewToImage();
                drawImage(play_item.media_path);
                startWaitTimer(play_item.d * 1000);
            } else if (play_item.view_type == VIEW_TYPE_THETA) {
                replaceViewToThetaImage();
                drawThetaImage(play_item.media_path);
                startWaitTimer(play_item.d * 1000);
            } else if (play_item.view_type == VIEW_TYPE_TOUCH || play_item.view_type == VIEW_TYPE_ORIGINAL) {
                replaceViewToTouchView();
                loadTouchContent(play_item.id, play_item.d * 1000, ViewerActivity.HANDLE_MSG_NEXT_CONTENT);
            } else if(play_item.view_type == VIEW_TYPE_WEB){
                drawBlankWebView();
                replaceViewToWeb();
                mCheckLookAhead = false;
                loadWebView(play_item);
                startWaitTimer(play_item.d * 1000);
            }

            // webviewの先読み
            if(play_item.view_type != VIEW_TYPE_WEB){
                PlayItem next_web_view = getNextWebViewPlayItem();
                mCheckLookAhead = true;
                if (next_web_view != null) {
                    loadWebView(next_web_view);
                }
            }

            // ステータスコード表示
            if(mViewlist.sched_type == HealthCheckService.SCHED_TYPE_DEFAULT && !isEnableEmergencyContent) {
                orderTopStatusCode();
                AdmintApplication.getInstance().setIsPlayDefaultContent(true);

            } else {
                AdmintApplication.getInstance().setIsPlayDefaultContent(false);
            }

            // テロップ更新チェック
            updateScrollView();

        }catch (Exception e){
            Logging.stackTrace(e);
            errorContent(play_item);
        }
    }

    private void loadWebView(PlayItem view_item)
    {
        mViewerWebView.load(view_item, mViewerHandler);
    }

    private void startClickTouchContent(PlayItem view_item){
        mStartViewTimestamp = System.currentTimeMillis();

        stopPlayingMedia();
        replaceViewToTouchView();

        // ステータスコード表示
        if(mViewlist.sched_type == HealthCheckService.SCHED_TYPE_DEFAULT) {
            orderTopStatusCode();
        }

        // 再生ロギング
        loggingPlay(view_item.id);

        loadTouchContent(view_item.tcid, view_item.tcd * 1000, ViewerActivity.HANDLE_MSG_CURRENT_CONTENT);
    }

    private int getViewType(int t){
        if(t == HealthCheckService.CONTENT_TYPE_MOVIE || t == HealthCheckService.CONTENT_TYPE_EXTERNAL_MOVIE){
            return VIEW_TYPE_MOVIE;
        }else if(t == HealthCheckService.CONTENT_TYPE_PICTURE || t == HealthCheckService.CONTENT_TYPE_EXTERNAL_PICTURE){
            return VIEW_TYPE_PICTURE;
        }else if(t == HealthCheckService.CONTENT_TYPE_THEATA){
            return VIEW_TYPE_THETA;
        }else if(t == HealthCheckService.CONTENT_TYPE_TOUCH){
            return VIEW_TYPE_TOUCH;
        } else if(t == HealthCheckService.CONTENT_TYPE_ORIGINAL){
            return VIEW_TYPE_ORIGINAL;
        } else if (t == HealthCheckService.CONTENT_TYPE_WEBVIEW) {
            return VIEW_TYPE_WEB;
        }
        return VIEW_TYPE_NOTING;
    }

    private @Nullable PlayItem moveNextPlayItem(){
        if (mViewlist.play_items.size() == 0) {
            return null;
        }else if (mViewlist.play_items.size() <= mViewListIndex + 1) {
            if(mCheckUpdate){
                // mViewlist.play_items内容が置き換わるので継続処理は注意すること
                updateViewlist();
                if(mViewlist.play_items.size() == 0){
                    return null;
                }
            }

            //再生位置を初期化
            mViewListIndex = 0;
            return mViewlist.play_items.get(0);
        } else {
            //次のコンテンツへ進める
            mViewListIndex++;
            return mViewlist.play_items.get(mViewListIndex);
        }
    }

    private @Nullable PlayItem moveFirstPlayItem(){
        if(mViewlist.play_items.size() == 0){
            return null;
        }else {
            mViewListIndex = 0;
            return mViewlist.play_items.get(0);
        }
   }

    private @Nullable PlayItem getCurrentPlayItem(){

        if(mViewlist.play_items.size() == 0){
            return null;
        }

        if(mViewlist.play_items.size() > mViewListIndex){
            return mViewlist.play_items.get(mViewListIndex);
        }

        return null;
    }

    private @Nullable PlayItem getNextWebViewPlayItem(){
        int index = mViewListIndex + 1;
        for(int i = 0 ; i < mViewlist.play_items.size() ; i++){
            PlayItem check_item;
            if(index >= mViewlist.play_items.size()){
                index = 0;
            }
            check_item = mViewlist.play_items.get(index);
            if(check_item.view_type == VIEW_TYPE_WEB){
                return check_item;
            }
            index++;
        }
        return null;
    }

    private boolean viewTypeIsNotWebOnly(){
        boolean ret = false;
        for(PlayItem item : mViewlist.play_items){
            if(item.view_type != VIEW_TYPE_WEB){
                ret = true;
                break;
            }
        }
        return ret;
    }

//    private boolean isDefaultContentViewStatus(){
//        if(mViewlist != null && mViewlist.sched_type == HealthCheckService.SCHED_TYPE_DEFAULT){
//            return true;
//        }
//        return false;
//    }

    private void createTimeTextView(){
        final  ViewGroup.LayoutParams TIME_TEXTVIEW_PARAM = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        mTimeTextView =  new TextView(this, null, android.R.attr.textAppearanceMedium);
        mTimeTextView.setLayoutParams(TIME_TEXTVIEW_PARAM);
        mTimeTextView.setTextColor(Color.WHITE);

        Point size = getRealSize();
        int xpos = (int) (size.x * 0.025f);
        int ypos = size.y - (int) (size.y * 0.1f);

//        if (mViewlist != null && mViewlist.sched_type == HealthCheckService.SCHED_TYPE_DEFAULT && mViewlist.play_items.size() > 0) {
//        if(isDefaultContentViewStatus()){
        if(mViewlist.sched_type == HealthCheckService.SCHED_TYPE_DEFAULT){
            // デフォルトコンテンツ表示時はさらに表示位置を下にする
            ypos = size.y - (int) (size.y * 0.05f);
        }

        mTimeTextView.setTranslationX(xpos);
        mTimeTextView.setTranslationY(ypos);
        mTimeTextView.setShadowLayer(0.5f,0.5f,0.5f,Color.BLACK);
//        return mTimeTextView;
    }

    private void updateTimeTextView(){
        final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        if(mTimeTextView == null){
            return;
        }
        String status_str = FORMAT.format(new Date());

        String hc_time = "  (" + DateUtil.convDateTimeFormal(HealthCheckPref.getLatestHealthCheckTime()) + ")";
        status_str = status_str + "   Status: " + getStatusCode() + " " + hc_time;


        Point size = getRealSize();
        int ypos = size.y - (int) (size.y * 0.1f);
        int xpos = (int) (size.x * 0.025f);
        if(mViewlist.sched_type != HealthCheckService.SCHED_TYPE_DEFAULT){
            // デフォルトコンテンツ表示時はプレイリストの表示はしない（表示中コンテンツの邪魔になるから）
            String playlist = getStringPlaylist();
            if (playlist.length() > 0) {
                status_str += "\nPlaylist: " + playlist + "\n";
            }
        } else {
            // デフォルトコンテンツ表示時はさらに表示位置を下にする
            ypos = size.y - (int) (size.y * 0.05f);
        }
        mTimeTextView.setTranslationX(xpos);
        mTimeTextView.setTranslationY(ypos);
        mTimeTextView.setText(status_str);
    }

    private String getStringPlaylist(){
        StringBuilder sb = new StringBuilder();
        for (PlayItem play_item : mPlaylist.play_items) {
            sb.append(play_item.id);
            if (play_item.f_id > 0) {
                sb.append("(").append(play_item.f_id).append(")");
            }
            sb.append(",");
        }
        if(sb.length() > 0) {
            return sb.substring(0, sb.length() - 1);
        }
        return "";
    }

    private int getStatusCode(){
        final int STATUS_KITTING_ERROR = 99;
        final int STATUS_INVALID_TIME = 90;
        final int STATUS_ALL_INVALID_CONTENT = 60;
        final int STATUS_NO_ENABLE_CONTENT_HC_SUCCESS = 50;
        final int STATUS_NO_ENABLE_CONTENT_HC_FAILED = 40;
        final int STATUS_HEALTH_CHECK_SUCCESS = 30;
        final int STATUS_HEALTH_CHECK_FAILED = 20;
        final int STATUS_NETWORK_SERVICE_OFF = 10;

        // キッティング情報がない
        if(DefaultPref.getManagerUrl().length() == 0 || DefaultPref.getSiteId().length() == 0 || DefaultPref.getAkey().length() == 0){
            return STATUS_KITTING_ERROR;
        }

        // 時刻がおかしい
        if(!DateUtil.isValidCurrentTime()){
            return STATUS_INVALID_TIME;
        }

        // ネットワークサービスがOFF
        if(!DefaultPref.getNetworkService()){
            return STATUS_NETWORK_SERVICE_OFF;
        }

        // プレイリスト内のコンテンツ全てが不正
        if (isAllInvalidContent(mPlaylist.play_items)) {
            return STATUS_ALL_INVALID_CONTENT;
        }

        // ビューリストの有無に関わらないステータス
        // 直前のヘルスチェック成功
        if(HealthCheckPref.getIsPreviousHealthCheckSuccess()){
            if(mPlaylist.play_items.size() == 0){
                return STATUS_HEALTH_CHECK_SUCCESS;
            }else{
                return STATUS_NO_ENABLE_CONTENT_HC_SUCCESS;
            }
        // 直前のヘルスチェック失敗
        }else{
            if(mPlaylist.play_items.size() == 0){
                return STATUS_HEALTH_CHECK_FAILED;
            }else{
                return STATUS_NO_ENABLE_CONTENT_HC_FAILED;
            }
        }
    }

    private void loggingPlay(int id){
        if(HealthCheckPref.getUploadPlayCount() == 1  && !isEnableEmergencyContent){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            try {
                // 現在時刻(時分秒を切り捨て）
                long timestamp = sdf.parse(sdf.format(new Date())).getTime();
                String datetime = sdf.format(timestamp);
                Intent intent = new Intent(getApplicationContext(), LoggingService.class);
                intent.putExtra(LoggingService.INTENT_EXTRA_LOG_CONTENT_ID, id);
                intent.putExtra(LoggingService.INTENT_EXTRA_TIMESTAMP, timestamp);
                intent.putExtra(LoggingService.INTENT_EXTRA_DATETIME, datetime);
                intent.setAction(LoggingService.ACTION_LOGGING_PLAY_CONTENT);
                startService(intent);
            } catch (Exception e){
                Logging.stackTrace(e);
            }
        }
    }

    private boolean isAvailableList(@NonNull ArrayList<PlayItem> playitem_list){
        if(playitem_list.size() == 0){
            return false;
        }
        for (PlayItem play_item : playitem_list) {
            // 再生可能なメディアが存在すれば有効なリストとみなす
            //webViewはMediaPathによるチェックが不要のため、play_itemに格納されているタイプ(t)が合っていればtrueを返すようにする。
            if(getMediaPath(play_item.t, play_item.id, play_item.f_id) != null || play_item.t == HealthCheckService.CONTENT_TYPE_WEBVIEW){
                return true;
            }
        }
        return false;
    }

    private boolean isAllInvalidContent(ArrayList<PlayItem> play_items){

        if(play_items.size() == 0){
            return false;
        }
        for (PlayItem play_item : play_items) {
            if(mErrorDbHelper.isValidContent(play_item.id, play_item.f_id)){
                return false;
            }
        }

        // 全て不正コンテンツ
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        try {
            if(AdmintApplication.getInstance().getViewerStatus() == AdmintApplication.VIEWER_STATUS_FOREGROUND) {
                if(mViewlist != null && mViewlist.play_items.size() > 0) {
                    PlayItem view_item = getCurrentPlayItem();
                    if (view_item != null && view_item.view_type == VIEW_TYPE_TOUCH) {
                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            // タッチコンテンツを動かす
                            if (mViewerLayout.indexOfChild(mTouchView) != -1) {
                                mTouchContent.pressKeyBoard(event.getKeyCode());
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
        return super.dispatchKeyEvent(event);
    }


    private void resetSystemPropertyProxy(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Properties properties = System.getProperties();
            properties.remove("http.proxyHost");
            properties.remove("http.proxyPort");
            properties.remove("https.proxyHost");
            properties.remove("https.proxyPort");
        }
    }

    private void setSystemPropertyProxy() {

        String host = DefaultPref.getProxyHost();
        String port = DefaultPref.getProxyPort();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Context appContext = AdmintApplication.getInstance();

            Properties properties = System.getProperties();

            properties.setProperty("http.proxyHost", host);
            properties.setProperty("http.proxyPort", port);
            properties.setProperty("https.proxyHost", host);
            properties.setProperty("https.proxyPort", port);
//            System.setProperty("http.proxyHost", host);
//            System.setProperty("http.proxyPort", port);
//            System.setProperty("https.proxyHost", host);
//            System.setProperty("https.proxyPort", port);

            try {
                Class applictionCls = Class.forName("android.app.Application");
                Field loadedApkField = applictionCls.getField("mLoadedApk");
                loadedApkField.setAccessible(true);
                Object loadedApk = loadedApkField.get(appContext);
                @SuppressLint("PrivateApi") Class loadedApkCls = Class.forName("android.app.LoadedApk");
                Field receiversField = loadedApkCls.getDeclaredField("mReceivers");
                receiversField.setAccessible(true);
                ArrayMap receivers = (ArrayMap) receiversField.get(loadedApk);
                for (Object receiverMap : receivers.values()) {
                    for (Object rec : ((ArrayMap) receiverMap).keySet()) {
                        Class clazz = rec.getClass();
                        if (clazz.getName().contains("ProxyChangeListener")) {
                            Method onReceiveMethod = clazz.getDeclaredMethod("onReceive", Context.class, Intent.class);
                            Intent intent = new Intent(Proxy.PROXY_CHANGE_ACTION);

                            onReceiveMethod.invoke(rec, appContext, intent);
                        }
                    }
                }

            } catch (Exception e) {
                Logging.stackTrace(e);
            }
        }
    }

    private void dialog(){
        AlertDialog.Builder alert_dialog = new AlertDialog.Builder(this);
        alert_dialog.setCancelable(false);
        alert_dialog.setMessage(getString(R.string.download_end_stand_alone_content));
        alert_dialog.setPositiveButton("Close", null);

        AlertDialog dialog = alert_dialog.create();

        Handler handler = new Handler();
        Runnable r = dialog::dismiss;

        dialog.show();
        handler.postDelayed(r, 10000);

    }


    private void createScrollView(){

        int speed = 200;
        int text_h_scale;
        if(!Build.MODEL.equals(DeviceDef.HWLD_XM6502) && !Build.MODEL.equals(DeviceDef.MITACHI) && !DeviceDef.isGroova()){
            speed = (int) (speed * 1.5f);
        }
        Point size = getRealSize();//画面サイズ

        if(size.x > size.y){
            text_h_scale = size.y;
        } else {
            text_h_scale = size.x;
        }
        if(mTelopView == null){
            mTelopView = new TelopSurfaceView(this);

            mTelopView.setTextSize(text_h_scale / 10 - 25);
            mTelopView.setFps(30); // 秒間の描画回数
            mTelopView.setSpeed(speed); // 秒間に移動するピクセル数
            mTelopView.setBackgroundResource(R.color.colorBlack);

            final FrameLayout.LayoutParams param = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    size.y / 10
            );
            param.topMargin = text_h_scale / 10 * 9;
            mTelopView.setLayoutParams(param);
            mTelopView.bringToFront();
            mTelopView.setZOrderOnTop(true);
            mTelopView.setVisibility(View.INVISIBLE);
            mTelopLayout.addView(mTelopView);
        }

        // 初回テロップテキスト
        GetRescueDbData ggrd = new GetRescueDbData();
        StringBuilder telopText= ggrd.getTelopText(this);//テロップのテキスト
        boolean playback_rescue_content = ggrd.checkEmergencyList(this);//全画面の緊急配信情報があるか
        int rotation = ggrd.getRescueTelopInfo(this); //テロップの向き

        if(rotation != -1 && telopText != null && !playback_rescue_content){
            // テロップ表示回転
            rotateTelopView(rotation);
            // テロップテキスト追加
            mTelopView.updateText(telopText.toString()); // テキストサイズをセットしてから呼び出す
            mTelopView.setVisibility(View.VISIBLE);
        }
    }

    private void updateScrollView(){
        // 緊急情報
        GetRescueDbData grdd = new GetRescueDbData();
        StringBuilder telopText= grdd.getTelopText(this); // 緊急情報テキスト
        boolean playback_rescue_content = grdd.checkEmergencyList(this); // 非テロップ緊急情報の有無
        int rotation = grdd.getRescueTelopInfo(this); //テロップの向き

        if(telopText != null){
            if(!playback_rescue_content){
                //初回検知時のみロギング
                if(mTelopView.getVisibility() == View.INVISIBLE){
                    NetLog.notice(getString(R.string.net_log_notice_emergency_info_detected));
                    Logging.info(getString(R.string.log_info_detected_rescue_telop));
                }
                // テロップ表示回転
                rotateTelopView(rotation);
                // 今のロールが終わってからテロップテキスト追加
                mTelopView.updateText(telopText.toString());
                mTelopView.setVisibility(View.VISIBLE);
            } else {
                //全画面で表示する緊急情報がある場合は非表示
                //かつテキストの描画位置を初期化
                mTelopView.initPos();
                mTelopView.setVisibility(View.INVISIBLE);
            }
        } else {
            //期限切れで表示削除。位置を初期化しておかないと次回検知時に前回終了地点からテロップが開始してしまう
            mTelopView.initPos();
            mTelopView.setVisibility(View.INVISIBLE);
        }
    }

    private void rotateTelopView(int rotation) {
        Point size = getRealSize();//画面サイズ
        int side_margin = 0;
        final FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) mTelopView.getLayoutParams();
        switch(rotation) {
            case 270:
                param.width = size.y / 10;
                param.height = ViewGroup.LayoutParams.MATCH_PARENT;
//                param.leftMargin = size.x - size.y / 10;
                if(size.y > size.x){
                    side_margin = size.x / 10 * 9;
                } else {
                    side_margin = size.x - size.y / 10;
                }
                param.leftMargin = side_margin;
                param.topMargin = 0;
                break;
            case 90:
                if(size.x < size.y){
                    param.width = size.x / 10;
                } else {
                    param.width = size.y / 10;
                }
                param.height = ViewGroup.LayoutParams.MATCH_PARENT;
                param.leftMargin = 0;
                param.topMargin = 0;
                break;
            case 0:
            default:
                param.width = ViewGroup.LayoutParams.MATCH_PARENT;
                param.height = size.y /10;
                if(size.x > size.y){
                    //本体設定横向き
                    param.topMargin = size.y / 10 * 9;
                } else {
                    //本体設定縦向き
                    param.topMargin = (int) (size.y / 10 * 9.45f);
                }
                param.leftMargin = 0;
                break;
        }
        mTelopView.setAngle(rotation);
        mTelopView.setLayoutParams(param);
    }
}

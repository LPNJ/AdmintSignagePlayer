package jp.co.digitalcruise.admint.player;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import jp.co.digitalcruise.admint.player.activity.viewer.ViewerActivity;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.service.LoggingService;
import jp.co.digitalcruise.admint.player.service.PlaylistService;
import jp.co.digitalcruise.admint.player.service.RecoverService;
import jp.co.digitalcruise.admint.player.service.UpdaterService;
import jp.co.digitalcruise.admint.player.service.network.ContentManagerService;
import jp.co.digitalcruise.admint.player.service.network.NetApiService;

public class AdmintApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static AdmintApplication mApplication;

    public static final int VIEWER_STATUS_BACKGROUND = 0;
    public static final int VIEWER_STATUS_FOREGROUND = 1;

    private int mViewerStatus = VIEWER_STATUS_BACKGROUND;

    private boolean mIsPlayDefaultContent = false;

    public static long MEMORY_BOOT_TIME = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        mApplication = this;
        mViewerStatus = VIEWER_STATUS_BACKGROUND;
        registerActivityLifecycleCallbacks(this);
    }

    public static AdmintApplication getInstance(){
        return mApplication;
    }

//    public static ViewerWebView getViewerWebViewInstance(){
//        if(mViwerWebView == null){
//            mViwerWebView = new ViewerWebView(AdmintApplication.getInstance());
//        }
//        return mViwerWebView;
//    }

    public int getViewerStatus(){
        return mViewerStatus;
    }


    public static void launchApplication(){

        if(DefaultPref.getSiteId().equals("") && DefaultPref.getAkey().equals("")){
            return;
        }

        // Recoverサービス起動処理(Viewer起動、ANR検知アラーム)
        {
            Intent intent = new Intent(mApplication, RecoverService.class);
            intent.setAction(RecoverService.ACTION_PLAYER_LAUNCH);
            mApplication.startService(intent);
        }

        // Updaterサービス起動処理(再起動アラーム)
        {
            Intent intent = new Intent(mApplication, UpdaterService.class);
            intent.setAction(UpdaterService.ACTION_PLAYER_LAUNCH);
            mApplication.startService(intent);
        }

        // Loggingサービス起動処理(Log切り詰めアラーム)
        {
            Intent intent = new Intent(mApplication, LoggingService.class);
            intent.setAction(LoggingService.ACTION_PLAYER_LAUNCH);
            mApplication.startService(intent);
        }

        // ContentManagerサービス起動処理(未使用デイリーコンテンツ削除アラーム)
        {
            Intent intent = new Intent(mApplication, ContentManagerService.class);
            intent.setAction(ContentManagerService.ACTION_PLAYER_LAUNCH);
            mApplication.startService(intent);
        }

        if(DefaultPref.getNetworkService() && !DefaultPref.getStandAloneMode()) {
            // NetApiが各通信処理をシーケンスで実行
            // 1.NetApi GoForIt
            // 2.NetApi -> HealthCheck
            // 3.HealthCheck -> ContentManager AND Playlist
            // 4.ContentManager -> Playlist
            {
                Intent intent = new Intent(mApplication, NetApiService.class);
                intent.setAction(NetApiService.ACTION_PLAYER_LAUNCH);
                mApplication.startService(intent);
            }
        }else{
            // 通信サービスがOFF
            // 現スケジュールでプレイリストを更新
            {
                Intent intent = new Intent(mApplication, PlaylistService.class);
                intent.setAction(PlaylistService.ACTION_REFRESH_PLAYLIST);
                mApplication.startService(intent);
            }
        }

    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        if(activity.getClass().getName().equals(ViewerActivity.class.getName())) {
            mViewerStatus = VIEWER_STATUS_FOREGROUND;
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if(activity.getClass().getName().equals(ViewerActivity.class.getName())) {
            mViewerStatus = VIEWER_STATUS_BACKGROUND;
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    //プレイリストで何を流すかの情報setter
    public void setIsPlayDefaultContent(boolean val){
        mIsPlayDefaultContent = val;
    }
    //getter
    public boolean getIsPlayDefaultContent(){
        return mIsPlayDefaultContent;
    }
}

package jp.co.digitalcruise.admint.player;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.Toolbar;

import jp.co.digitalcruise.admint.player.activity.AbstractAdmintActivity;
import jp.co.digitalcruise.admint.player.activity.main.LogActivity;
import jp.co.digitalcruise.admint.player.activity.main.ScheduleActivity;
import jp.co.digitalcruise.admint.player.activity.main.SettingInfoActivity;
import jp.co.digitalcruise.admint.player.activity.main.SettingPreferenceActivity;
import jp.co.digitalcruise.admint.player.activity.main.networktest.NetworkTestActivity;
import jp.co.digitalcruise.admint.player.activity.register.DialogHandler;
import jp.co.digitalcruise.admint.player.activity.register.RegisterActivity;
import jp.co.digitalcruise.admint.player.component.define.DeviceDef;
import jp.co.digitalcruise.admint.player.component.file.AdmintPath;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.db.ErrorDbHelper;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.pref.GroovaProxyPref;
import jp.co.digitalcruise.admint.player.pref.RegisterPref;
import jp.co.digitalcruise.admint.player.service.PlaylistService;
import jp.co.digitalcruise.admint.player.service.UpdaterService;
import jp.co.digitalcruise.admint.player.service.network.HealthCheckService;
import jp.co.grv.toto.tvp.ReadyListener;
import jp.co.grv.toto.tvp.api.TVPlatform;
import jp.co.grv.toto.tvp.api.TVPropertyManager;

// PlayerActivityはAdmintHomeでActivity名およびパッケージを参照している
// 「jp.co.digitalcruise.admint.player.PlayerActivity」から変更しないこと！
public class PlayerActivity extends AbstractAdmintActivity implements ReadyListener {

    private static final String ACTION_PREFIX = BuildConfig.APPLICATION_ID + ".PlayerActivity.";
    public static final String ACTION_TOAST_MSG = ACTION_PREFIX + "TOAST_MSG";


    public static final String INTENT_EXTRA_TOAST_MSG = "msg";

    public static final String INTENT_EXTRA_ON_CREATE_TYPE = "on_create_type";
    public static final int INTENT_EXTRA_ON_CREATE_TYPE_BOOT_COMPLETE = 1;

    private static final float SHOW_MENU_LONGTAP_ZONE_X = 0.8f;
    private static final float SHOW_MENU_LONGTAP_ZONE_Y = 0.8f;

    private GestureDetector mGDetector = null;
    private ScaleGestureDetector mSGDetector = null;

    boolean mHookTouch = false;

    public BroadcastReceiver mReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            try{
                if(context != null && intent != null){
                    if(ACTION_TOAST_MSG.equals(intent.getAction())) {
                        String msg = intent.getStringExtra(INTENT_EXTRA_TOAST_MSG);
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    }
                }
            }catch(Exception e){
                Logging.stackTrace(e);
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        try {
            super.onCreate(savedInstanceState);
//            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

            if(DeviceDef.isGroova()){
                TVPlatform.getLifeCycleControl().onCreate(this);
            }

            // ロギング
            logBootStatus(getIntent());

            setContentView(R.layout.main);

            if(Build.VERSION.SDK_INT > 24){
                Toolbar toolbar = findViewById(R.id.toolbar);
                toolbar.inflateMenu(R.menu.main);
                setActionBar(toolbar);
            }
//
//            // Android 6, API 23以上でパーミッションの確認
//            if(Build.VERSION.SDK_INT >= 23) {
//                String[] permissions = {
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.READ_EXTERNAL_STORAGE,
//                        Manifest.permission.READ_PHONE_STATE
//                };
//                checkPermission(permissions, 1000);
//            }

            DefaultPref.setSdcardDrive(DeviceDef.getStoragePath());

            mGDetector = new GestureDetector(getApplicationContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent event) {
                    if (!mHookTouch) {
                        intentPlaylistPlayerLaunch();
                        mHookTouch = true;
                    }
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent event) {
                    if (!mHookTouch) {
                        try {
                            Point pt = getRealSize();
                            if ((event.getX() / (float) pt.x > SHOW_MENU_LONGTAP_ZONE_X) && (event.getY() / (float) pt.y > SHOW_MENU_LONGTAP_ZONE_Y)) {
                                if (getActionBar() != null) {
                                    getActionBar().show();
                                }
                            }
                        } catch (Exception e) {
                            Logging.stackTrace(e);
                        }
                    }
                }
            });

            mSGDetector = new ScaleGestureDetector(getApplicationContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                float mPinchScale = 1.0f;

                @Override
                public boolean onScaleBegin(ScaleGestureDetector detector) {
                    mPinchScale = 1.0f;
                    return super.onScaleBegin(detector);
                }

                @Override
                public void onScaleEnd(ScaleGestureDetector detector) {
                    if (!mHookTouch) {
                        if (mPinchScale < 0.3f) {
                            if (getActionBar() != null) {
                                getActionBar().show();
                            }
                        } else if (mPinchScale > 3.0f) {
                            Toast.makeText(PlayerActivity.this, getString(R.string.toast_msg_do_health_check), Toast.LENGTH_SHORT).show();
                            doHealthCheck();
                        }
                    }
                    super.onScaleEnd(detector);
                }

                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    mPinchScale = mPinchScale * detector.getScaleFactor();
                    return true;
                }
            });

            registReceiver();

            if(!DefaultPref.getRegisteredTerminal() && DefaultPref.getSiteId().equals("") && DefaultPref.getAkey().equals("")){
                if( !DefaultPref.getNotShowDialog()){
                    selectStartMode();
                } else {
                    if(DeviceDef.isGroova() && GroovaProxyPref.getCheckGroovaProxy() || !DeviceDef.isGroova()){
                        startup();
                    }
                }
            } else {
                DefaultPref.setRegisteredTerminal(true);
                if(DeviceDef.isGroova() && GroovaProxyPref.getCheckGroovaProxy() || !DeviceDef.isGroova()){
                    startup();
                }
            }

        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

//    public void checkPermission(final String[] permissions,final int request_code){
//        ActivityCompat.requestPermissions(this, permissions, request_code);
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//
//        for (int i = 0; i < permissions.length; i++) {
//            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
//                // パーミッションが許可された
//                Log.d("Permission", "Added Permission: " + permissions[i]);
//            } else {
//                // パーミッションが拒否された
//                Log.d("Permission", "Rejected Permission: " + permissions[i]);
//            }
//        }
//    }

    @Override
    protected void onResume(){
        try{
            super.onResume();

            if(DeviceDef.isGroova()){
                TVPlatform.getLifeCycleControl().onResume(this, this);
            }

            mHookTouch = false;
            if(getActionBar() != null) {
                getActionBar().hide();
            }

        }catch (Exception e){
            Logging.stackTrace(e);
        }

    }

    @Override
    protected void onDestroy(){
        try{
            unregisterReceiver(mReceiver);
            super.onDestroy();
            if(DeviceDef.isGroova()){
                TVPlatform.getLifeCycleControl().onDestroy(this);
            }
        } catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(DeviceDef.isGroova()){
            TVPlatform.getLifeCycleControl().onPause(this);
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
    public boolean onTouchEvent(MotionEvent event) {
        mSGDetector.onTouchEvent(event);
        mGDetector.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent){
        try{
            super.onNewIntent(intent);
            if(DefaultPref.getRegisteredTerminal()){
                startup();
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean ret;
        try{
            super.onCreateOptionsMenu(menu);
            MenuInflater inflater = getMenuInflater();
            if(Build.MODEL.equals(DeviceDef.STRIPE) || AdmintPath.isMaintenance()){
                inflater.inflate(R.menu.stripe_main, menu);
            } else {
                inflater.inflate(R.menu.main, menu);
            }
            ret = true;
        }catch(Exception e){
            ret = false;
            Logging.stackTrace(e);
        }
        return ret;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try{
            Intent intent;
            switch (item.getItemId()) {
                case R.id.menu_setting:
                    RegisterPref.setOpenProxySettingFromRegister(false);
                    intent = new Intent(getApplicationContext(), SettingPreferenceActivity.class);
                    startActivity(intent);
                    break;
                case R.id.menu_network_test:
                    intent = new Intent(getApplicationContext(), NetworkTestActivity.class);
                    startActivity(intent);
                    break;
                case R.id.menu_sdcard:
                    loadSdcard();
                    break;
                case R.id.menu_info:
                    Intent info_intent = new Intent(this, SettingInfoActivity.class);
                    startActivity(info_intent);
                    break;
                case R.id.menu_log:
                    intent = new Intent(this, LogActivity.class);
                    startActivity(intent);
                    break;
                case R.id.menu_schedule:
                    intent = new Intent(this, ScheduleActivity.class);
                    startActivity(intent);
                    break;
                case R.id.menu_license :
                    licenseView();
                    break;
                case R.id.menu_healthcheck:
                    doHealthCheck();
                    break;
                case R.id.menu_aheadload:
                    doAheadLoad();
                    break;
                case R.id.menu_reboot:
                    showRebootDialog();
                    break;
                case R.id.menu_register:
                    intent = new Intent(this, RegisterActivity.class);
                    startActivity(intent);
                    break;
                default:
                    break;
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
        return super.onOptionsItemSelected(item);
    }

    private void registReceiver(){
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_TOAST_MSG);
        registerReceiver(mReceiver, filter);
    }

    private void logBootStatus(Intent intent){

        if(intent != null) {
            // 手動起動
            if (intent.getCategories() != null) {
                for (String category : intent.getCategories()) {
                    if (category.equals("android.intent.category.LAUNCHER")) {
                        Logging.info(getString(R.string.log_info_launch_player_manual));
                        return;
                    }
                }
            }

            // BOOT_COMPLETED
            if (intent.getIntExtra(INTENT_EXTRA_ON_CREATE_TYPE, -1) == INTENT_EXTRA_ON_CREATE_TYPE_BOOT_COMPLETE) {
                intent.removeExtra(INTENT_EXTRA_ON_CREATE_TYPE); // ←これいる？
                Logging.info(getString(R.string.log_info_launch_player_boot_complated));
                return;
            }
        }

        // その他
        Logging.info(getString(R.string.log_info_launch_player_other));
    }

    private void showRebootDialog(){

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.menu_dialog_message_reboot).setTitle(R.string.menu_item_reboot_title);
            builder.setPositiveButton(R.string.menu_dialog_ok, (dialogInterface, i) -> {
                Intent reboot_intent = new Intent(getApplicationContext(), UpdaterService.class);
                reboot_intent.setAction(UpdaterService.ACTION_REBOOT_MANUAL);
                startService(reboot_intent);
                dialogInterface.dismiss();
            });

            builder.setNegativeButton(R.string.menu_dialog_cancel, (dialogInterface, i) -> dialogInterface.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void loadSdcard(){
        Toast.makeText(this,getString(R.string.toast_msg_sdcard_request), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, HealthCheckService.class);
        intent.setAction(HealthCheckService.ACTION_LOAD_EXTERNAL_STORAGE);
        startService(intent);
    }

    private void doHealthCheck(){
        try{

            Intent intent = new Intent(this, HealthCheckService.class);
            intent.setAction(HealthCheckService.ACTION_HEALTH_CHECK);
            startService(intent);

        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void doAheadLoad(){
        try{
            Intent intent = new Intent(this, HealthCheckService.class);
            intent.setAction(HealthCheckService.ACTION_AHEAD_LOAD);
            startService(intent);

        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void intentPlaylistPlayerLaunch(){
        // Viewer起動
        {
            Intent intent = new Intent(this, PlaylistService.class);
            intent.setAction(PlaylistService.ACTION_PLAYER_LAUNCH);
            startService(intent);
        }
    }

    private void startup(){

        // テストコード
//        DefaultPref.setManagerUrl("https://request.admint.jp/");
//        DefaultPref.setAkey("4507ADM12345");
//        DefaultPref.setSiteId("develop");

//        DefaultPref.setManagerUrl("https://request-local.admint.jp/");
//        DefaultPref.setAkey("6H5Q4nxF61gAC3CyCK5pttMjCbWtxq3W");
//        DefaultPref.setSiteId("devtouch2");

        // ブラックリスト以外のエラーコンテンツ履歴は削除
        resetErrorDb();

        // Viewer起動
        intentPlaylistPlayerLaunch();

        // 起動時処理（アラーム設定）
        AdmintApplication.launchApplication();
    }

    private void resetErrorDb(){

        ErrorDbHelper dbh = null;
        try{
            dbh = new ErrorDbHelper(this);
            SQLiteDatabase wdb = dbh.getWriterDb();
            wdb.delete(ErrorDbHelper.TABLE_VIEW_ERROR.getName(), null, null);
            wdb.delete(ErrorDbHelper.TABLE_RE_DOWNLOAD.getName(), null, null);
        }finally {
            if(dbh != null){
                dbh.close();
            }
        }
    }

    private void selectStartMode(){
        DialogHandler dialogHandler = new DialogHandler();
        dialogHandler.Confirm(this, getString(R.string.setting_key_dialog_title_text),"No", "Yes", RegistrationMode(), notUseRegistrationMode());
    }

    private Runnable RegistrationMode(){
        return () -> {
            //キッティング画面起動
            Intent intent = new Intent(this, RegisterActivity.class);
            this.startActivity(intent);
        };
    }
    private Runnable notUseRegistrationMode(){
        //いつもの起動
        return this::startup;
    }

    private void licenseView(){
        DialogHandler dialogHandler = new DialogHandler();
        dialogHandler.licenseViewer(this, getString(R.string.dialog_title_license), getString(R.string.dialog_button_ok));
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

            if(DeviceDef.isGroova() && !GroovaProxyPref.getCheckGroovaProxy()){
                startup();
            }
            GroovaProxyPref.setCheckGroovaProxy(true);


        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }
}

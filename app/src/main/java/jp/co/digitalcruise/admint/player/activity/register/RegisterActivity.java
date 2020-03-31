package jp.co.digitalcruise.admint.player.activity.register;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Xml;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.gson.Gson;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import jp.co.digitalcruise.admint.player.AdmintApplication;
import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.activity.AbstractAdmintActivity;
import jp.co.digitalcruise.admint.player.activity.main.SettingPreferenceActivity;
import jp.co.digitalcruise.admint.player.component.date.DateUtil;
import jp.co.digitalcruise.admint.player.component.define.DeviceDef;
import jp.co.digitalcruise.admint.player.component.define.UpdaterIntentDef;
import jp.co.digitalcruise.admint.player.component.file.AdmintPath;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.netutil.RequestApi;
import jp.co.digitalcruise.admint.player.db.LogDbHelper;
import jp.co.digitalcruise.admint.player.db.PlaylistDbHelper;
import jp.co.digitalcruise.admint.player.db.ScheduleDbHelper;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.pref.HealthCheckPref;
import jp.co.digitalcruise.admint.player.pref.NetWorkTestPref;
import jp.co.digitalcruise.admint.player.pref.RealtimeCheckPref;
import jp.co.digitalcruise.admint.player.pref.RecoverPref;
import jp.co.digitalcruise.admint.player.pref.RegisterPref;
import jp.co.digitalcruise.admint.player.pref.ReportPref;
import jp.co.digitalcruise.admint.player.pref.ServerUrlPref;
import jp.co.digitalcruise.admint.player.pref.UpdaterPref;
import jp.co.digitalcruise.admint.player.service.PlaylistService;
import jp.co.digitalcruise.admint.player.service.RecoverService;
import jp.co.digitalcruise.admint.player.service.UpdaterService;
import jp.co.digitalcruise.admint.player.service.network.HealthCheckService;


@SuppressLint("Registered")
public class RegisterActivity extends AbstractAdmintActivity {



    //local manage server
//    private final String MANAGE_SERVER_URL = "http://request-local.admint.jp/";
    //aws manage server
//    private static final  String MANAGE_SERVER_URL = "https://request.admintdev.jp/";

    private String MANAGE_SERVER_URL = "https://request.admint.jp/";

//    private final String MANAGE_SERVER_URL = "https://request.admint.jp/";

    private static final String API_REGIST = "Regist/";
    private static final String API_REGIST_ENTRY = API_REGIST + "entryM7";

    //valid models
    private static final String MODEL_F01D = "F-01D";

    //external storage path
    @SuppressLint("SdCardPath")
    private static final String EXTRA_STORAGE_PATH_F01D = "/sdcard/external_sd/";

    //layout parts
    private LinearLayout mLogTable = null;
    private ViewGroup.LayoutParams mCommonParams = null;

    //edit text
    private EditText mEditTerminalId = null;
    private EditText mEditSiteId = null;
    private EditText mExtraId = null;
    private boolean mTerminalIdNotNull = false;
    private boolean mExtraIdNotNull = false;

    //text view
    private TextView mIpAddress = null;
    private TextView mProxyServer = null;
    private TextView mDeviceDateTime = null;

    //checkbox
//    private CheckBox mCheckUpdateFlag = null;
    private CheckBox mStandAlone = null;

    //button
    private Button mBtnNetworkSetting = null;
    private Button mBtnNotUseProxy = null;
    private Button mBtnUseProxy = null;
    private Button mBtnReboot = null;
    private Button mBtnRegistServer = null;
    private Button mBtnSettingClear = null;

    RadioGroup mRbtnGroup = null;

    //terminal data
    private String mMacAddr = "";
    private String mTelNum = "";
    private String mModel = "";
    private String mModelVer = "";
    private String mExtraStorage = "";
    private boolean mValidModel = true;

    //xml data
    private static final String XML_TAG_RESULT = "result";
    private static final String XML_TAG_SITEID = "siteid";
    private static final String XML_TAG_AKEY = "akey";
    private static final String XML_TAG_STB_ID = "stbid";
    private static final String XML_DELIVERY_SERVER_URL = "delivery_server_url";

    int useContentViewLayout = 0;

    //update flag
    private int mUpdateFlag = 0;

    class XmlResult {
        static final String ATTR_MSG = "msg";
        static final String ATTR_STATUS = "status";
        static final String ATTR_CODE = "code";
        String msg = "";
        String status = "";
        int code = 0;
    }

    @SuppressLint("StaticFieldLeak")
    private class Task extends AsyncTask<String, Void, String> {
        private ArrayList<String> mErrorList = null;
        WeakReference<Activity> ref;
        int error_code = 0;

        Task(Activity activity){
            ref = new WeakReference<>(activity);
        }

        @Override
            protected String doInBackground(String... params) {
                URL url;
                String request_result = null;

                String terminal_id = params[0];
                String site_id = params[1];
                String update_flag = params[2];
                String extra_id = params[3];

                mErrorList = new ArrayList<>();

                try {
                    url = new URL(MANAGE_SERVER_URL + API_REGIST_ENTRY);
                } catch (Exception e) {
                    e.printStackTrace();
                    mErrorList.add(e.toString());
                    return null;
                }

                HashMap<String, String> post = new HashMap<>();

                //機番
                if (Build.MODEL.equals(DeviceDef.STRIPE)) {
                    if (extra_id != null && extra_id.length() > 0) {
                        post.put("extraid", extra_id);
                    }
                }

                // 端末IDあらため認証キー
                if (terminal_id != null && terminal_id.length() > 0) {
                    post.put("terminalid", terminal_id);
                }


                // SiteID
                if (site_id != null && site_id.length() > 0) {
                    post.put("siteid", site_id);
                }

                // MacAddr
                post.put("macaddr", mMacAddr);

                // 電話番号
                post.put("tel", mTelNum);

                // model
//				mModel = Build.MODEL;
                post.put("model", mModel);

                // model ver
                post.put("modelver", mModelVer);

                // android
                post.put("androidver", Build.VERSION.RELEASE);

                if (update_flag != null && update_flag.length() > 0) {
                    post.put("updateflag", update_flag);
                }

                try {
                    RequestApi api = new RequestApi();

                    if (DefaultPref.getProxyEnable()) {
                        api.setProxy(DefaultPref.getProxyHost(), Integer.parseInt(DefaultPref.getProxyPort()));
                        api.setProxyAuth(DefaultPref.getProxyUser(), DefaultPref.getProxyPassword());
                    }

                    request_result = api.okRequestPost(url, post);
//                    request_result = api.requestPost(url, post);

                    if (request_result == null || request_result.length() == 0) {
                        // toast
                        return null;
                    }

                    XmlResult result = parseResultXml(request_result);
                    if (result == null || result.code != 0) {
                        mErrorList.add(getString(R.string.log_info_registered_failed) + "\n");
                        if (result == null) {
                            mErrorList.add(getString(R.string.log_error_post_exception));
                        } else if (result.code == -1) {
                            mErrorList.add(getString(R.string.log_error_request_code_1));
                            error_code = -1;
                        } else if (result.code == -2) {
                            mErrorList.add(getString(R.string.log_error_request_code_2));
                            error_code = -1;
                        } else if (result.code == -3) {
                            mErrorList.add(getString(R.string.log_error_request_code_3));
                            mErrorList.add(mTelNum);
                            error_code = -2;
                        } else if (result.code == -4) {
                            mErrorList.add(getString(R.string.log_error_request_code_4));
                            error_code = -3;
                        } else if (result.code == -5) {
                            mErrorList.add(getString(R.string.log_error_request_code_5));
                            error_code = -3;
                        } else if (result.code == -6) {
                            mErrorList.add(getString(R.string.log_error_request_code_6));
                            error_code = -3;
                        } else if (result.code == -7) {
                            mErrorList.add(getString(R.string.log_error_request_code_7));
                            error_code = -3;
                        } else if (result.code == -8) {
                            mErrorList.add(getString(R.string.log_error_request_code_8));
                            mErrorList.add(mModel);
                            error_code = -4;
                        } else if (result.code == -9) {
                            mErrorList.add(getString(R.string.log_error_request_code_9));
                            error_code = -5;
                        } else if (result.code == -10) {
                            mErrorList.add(getString(R.string.log_error_request_code_10));
                            error_code = -10;
                        } else if (result.code == -11) {
                            mErrorList.add(getString(R.string.log_error_request_code_11));
                            error_code = -1;
                        }
//                        mErrorList.add(getString(R.string.log_error_request_false));
                    }
                } catch (Exception e) {
                    Logging.error(e.toString());
                    e.printStackTrace();
                    mErrorList.add(getString(R.string.log_error_post_exception));
                    mErrorList.add(getString(R.string.log_error_post_check_your_setting));
                }
                return request_result;
            }

            @Override
            protected void onPostExecute(String result) {
                final Activity activity = ref.get();
                if(activity != null){
                    if (mErrorList.isEmpty()) {
                        addLog(getString(R.string.log_notice_request_finish), 0);
                        if (result != null) {
                            parseEntryXml(result);
                        }
                    } else {
                        for (String err_string : mErrorList) {
                            addLog(err_string, 1);
                        }
//                        addLog(getString(R.string.log_error_entry_error), 1);
//                        addLog(getString(R.string.log_info_registered_failed), 1);
                        String e_msg = "";
                        switch (error_code){
                            case -1:
                                e_msg = getString(R.string.log_error_terminal_id_miss);
                                break;
                            case -2:
                                e_msg = getString(R.string.log_error_phone_num_miss);
                                break;
                            case -3:
                                e_msg = getString(R.string.log_error_site_id_miss);
                                break;
                            case -4:
                                e_msg = getString(R.string.log_error_model_miss);
                                break;
                            case -5:
                                e_msg = getString(R.string.log_error_stb_id_miss);
                                break;
                            case -10:
                                e_msg = getString(R.string.log_error_conflict_terminal_id);
                                break;
                            default:
                                e_msg = getString(R.string.log_error_connect_failed);

                        }
                        Logging.error(getString(R.string.log_info_registered_failed) + e_msg);
                    }
                    if(error_code == -10){
                        updateDialog();
                    }
                    mBtnRegistServer.setEnabled(true);
                }
            }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Logging.info(getString(R.string.log_info_registered_start));
        }
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        try{
            super.onCreate(savedInstanceState);
            //Software keyboard is hidden when the app launch
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

            int orientation = getResources().getConfiguration().orientation;

            //初期
            useContentViewLayout = R.layout.register_layout;

            //Set ContentView
            //Check maintenance mode
            switch (orientation) {
                case Configuration.ORIENTATION_LANDSCAPE:
                    if (isMaintenance()) {
                        if(Build.MODEL.equals(DeviceDef.STRIPE)){
                            useContentViewLayout = R.layout.register_layout_maintenance;
                            break;
                        } if(DeviceDef.isStandAloneInValid()){
                            useContentViewLayout = R.layout.register_layout_standalone_maintenance;
                            break;
                        } else {
                            useContentViewLayout = R.layout.register_layout_common_maintenance;
                            break;
                        }
                    } else {
                        if(Build.MODEL.equals(DeviceDef.STRIPE)) {
                            useContentViewLayout = R.layout.register_layout;
                            break;
                        } else if (DeviceDef.isStandAloneInValid()){
                            useContentViewLayout = R.layout.register_layout_standalone;
                            break;
                        } else {
                            useContentViewLayout = R.layout.register_layout_common;
                            break;
                        }
                    }

                case Configuration.ORIENTATION_PORTRAIT:
                    if (isMaintenance()) {
                        if(!Build.MODEL.equals(DeviceDef.STRIPE)){
                            if(DeviceDef.isStandAloneInValid()){
                                useContentViewLayout = R.layout.register_vertical_layout_standalone_maintenance;
                                break;
                            } else {
                                useContentViewLayout = R.layout.register_vertical_layout_common_maintenance;
                                break;
                            }
                        } else {
                            useContentViewLayout = R.layout.register_vertical_layout_maintenance;
                            break;
                        }
                    } else {
                        if(!Build.MODEL.equals(DeviceDef.STRIPE)){
                            if(DeviceDef.isStandAloneInValid()){
                                useContentViewLayout = R.layout.register_vertical_layout_standalone;
                                break;
                            } else {
                                useContentViewLayout = R.layout.register_vertical_layout_common;
                                break;
                            }
                        } else {
                            useContentViewLayout = R.layout.register_vertical_layout;
                            break;
                        }
                    }
            }
            setContentView(useContentViewLayout);

            initialize();

        } catch (Exception e){
            Logging.stackTrace(e);
        }

    }

    private void initialize() {

        //textView 各項目タイトル
        mIpAddress = findViewById(R.id.registerIpAddress);
        mProxyServer = findViewById(R.id.registerProxyServer);
        mDeviceDateTime = findViewById(R.id.deviceDateTime);

        //Text field at terminal id　認証キー
        mEditTerminalId = findViewById(R.id.editTerminalid);
        setPropEditText(mEditTerminalId);

        if(Build.MODEL.equals(DeviceDef.STRIPE)){
            //Text field at serial key 機番
            mExtraId = findViewById(R.id.editExtraKey);
            setPropEditText(mExtraId);
        }

        //Network setting button and ClickListener　ネットワーク設定
        mBtnNetworkSetting = findViewById(R.id.NetworkSetting);
        createNetworkSetting();

        //Proxy button and ClickListener　プロキシ
        mBtnNotUseProxy = findViewById(R.id.doNotUseProxy);
        mBtnUseProxy = findViewById(R.id.ProxySetting);
        createProxySetting();

        //Reboot button and ClickListener　再起動
        mBtnReboot = findViewById(R.id.rebootBtn);
        createRebootSetting();

        //Regist server button　サーバー登録
        mBtnRegistServer = findViewById(R.id.btnRegistServer);
        mBtnRegistServer.setFocusable(true);
        registSetting();

        mStandAlone = findViewById(R.id.StandAlone);
        if(Build.MODEL.equals(DeviceDef.PN_M_SERIES) || Build.MODEL.equals(DeviceDef.PN_B_SERIES)){
            mStandAlone.setChecked(true);
            DefaultPref.setStandAloneMode(true);
        }

        //Setup Log Table　ログ
        mLogTable = findViewById(R.id.layoutLog);

        //Layout params
        mCommonParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);

        //Get post values
        preSetting();

        //When using valid models at Check if the installed AdmintPlayer and AdmintUpdater　アプリはインストールされているか、対象のモデルか
//        if (!checkInstallApplication() || !mValidModel) {
//            mBtnRegistServer.setEnabled(false);
//        }

        //メンテナンスモードでないと表示しない
        if (isMaintenance()) {
            //Setting clear button
            mBtnSettingClear = findViewById(R.id.settingClear);
            settingClear();
            //Create text field at site id
            mEditSiteId = findViewById(R.id.editSiteid);
            setPropEditText(mEditSiteId);

            //Create Update flag
//            mCheckUpdateFlag = findViewById(R.id.cbxUpdateFlag);

            mRbtnGroup = findViewById(R.id.rBtnGroup);
            mRbtnGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                final String releaseURL = "https://request.admint.jp/";
                final String stagingURL = "https://request.admintstg.jp/";
                final String developURL = "https://request.admintdev.jp/";
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    switch(checkedId){
                        case R.id.rBtnRelease:
                            MANAGE_SERVER_URL = releaseURL;
                            break;
                        case R.id.rBtnStaging:
                            MANAGE_SERVER_URL = stagingURL;
                            break;
                        case R.id.rBtnDevelop:
                            MANAGE_SERVER_URL = developURL;
                            break;
                    }
                }
            });

        }
        //入力チェック
        createInputField();

    }

    @Override
    protected void onResume() {
        try{
            super.onResume();
            checkIpAddress();
            checkProxyState();

        } catch (Exception e){
            Logging.stackTrace(e);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setPropEditText(EditText edit) {
        InputFilter filter;
        filter = (source, start, end, dest, dstart, dend) -> {
            if (source.toString().matches("^[0-9a-zA-Z@¥.¥_¥¥-]+$")) {
                return source;
            } else {
                return "";
            }
        };
        InputFilter[] filters = new InputFilter[]{filter};
        edit.setFilters(filters);
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
    private void preSetting() {
        try {
            // MACアドレス
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            assert wm != null;
            WifiInfo wifiInfo = wm.getConnectionInfo();
            if (wifiInfo.getMacAddress() != null) {
                mMacAddr = wifiInfo.getMacAddress();
            }

            // 電話番号
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            assert tm != null;
            if (tm.getLine1Number() != null) {
                mTelNum = tm.getLine1Number();
            }

            // モデル名
            mModel = Build.MODEL;

            if (mModel.equals(MODEL_F01D)) {
                if (tm.getDeviceId() != null) {
                    mModelVer = tm.getDeviceId();
                    addLog("IMEI : " + mModelVer, 0);
                }
                mExtraStorage = EXTRA_STORAGE_PATH_F01D;
//                mCheckUpdateFlag.setEnabled(false);
            } else {
                mExtraStorage = DeviceDef.getStoragePath();
            }

            if (mExtraStorage.equals("")) {
                // キッティング対象外のモデル
                mValidModel = false;
                addLog(getString(R.string.log_error_no_target_model) + "(" + mModel + ")", 1);
            }

            TextView textModel;
            textModel = findViewById(R.id.textModelName);
            if (!mModel.equals(DeviceDef.STRIPE)) {
                textModel.setText(mModel);
            } else {
                textModel.setText(getString(R.string.text_lbl_model_stripe));
            }

        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), 1);
        }
    }

    private void postSetting() {
        try {
            if (mModel.equals(MODEL_F01D)) {
                setVolumeMin();
            } else {
                setVolumeMax();
            }
        } catch (Exception ignored) {

        }
    }

    private void requestServer() {

        addLog(getString(R.string.log_notice_request_start), 0);

        String param[] = new String[4];

        param[0] = mEditTerminalId.getText().toString();

        if (isMaintenance()) {
            param[1] = mEditSiteId.getText().toString();
        }

        if(mUpdateFlag == 1){
            param[2] = "1";
        } else {
            param[2] = "0";
        }
        
        if(Build.MODEL.equals(DeviceDef.STRIPE)){
            param[3] = mExtraId.getText().toString();
        }

        new Task(this).execute(param);
    }

    private XmlResult parseResultXml(String xmlstr) {
        XmlPullParser parser = Xml.newPullParser();
        XmlResult result = null;
        StringReader reader = null;
        try {
            reader = new StringReader(xmlstr);

            parser.setInput(reader);
            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tag;
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        tag = parser.getName();
                        if (XML_TAG_RESULT.equals(tag)) {
                            result = new XmlResult();
                            result.code = Integer.parseInt(parser.getAttributeValue(null, XmlResult.ATTR_CODE));
                            result.status = parser.getAttributeValue(null, XmlResult.ATTR_STATUS);
                            result.msg = parser.getAttributeValue(null, XmlResult.ATTR_MSG);
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                if (result != null) {
                    break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), 1);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                    addLog(e2.toString(), 1);
                }
            }
        }

        return result;
    }

    private void parseEntryXml(String xmlstr) {
        XmlPullParser parser = Xml.newPullParser();
        StringReader reader = null;
        String siteid = null;
        String stb_id = null;
        String akey = null;
        String delivery_server_url = null;

        boolean isError = false;

        try {
            reader = new StringReader(xmlstr);

            parser.setInput(reader);
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tag;

                if (eventType == XmlPullParser.START_TAG) {
                    tag = parser.getName();
                    if (XML_TAG_SITEID.equals(tag)) {
                        parser.next();
                        siteid = parser.getText();
                    } else if (XML_TAG_AKEY.equals(tag)) {
                        parser.next();
                        akey = parser.getText();
                    } else if (XML_DELIVERY_SERVER_URL.equals(tag)) {
                        parser.next();
                        delivery_server_url = parser.getText();
                    } else if (XML_TAG_STB_ID.equals(tag)) {
                        parser.next();
                        stb_id = parser.getText();
                    }
                }
                eventType = parser.next();
            }

        } catch (Exception e) {
            e.printStackTrace();
            addLog(e.toString(), 1);
            isError = true;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                    addLog(e2.toString(), 1);
                    isError = true;
                }
            }
        }

        if (isError) {
            addLog(getString(R.string.log_info_registered_failed), 1);
            Logging.error(getString(R.string.log_info_registered_failed));
        } else {
            registTerminal(siteid, akey, delivery_server_url, stb_id);
            addLog(getString(R.string.log_notice_entry_finish), 0);
        }
    }

    private void registTerminal(String siteid, String akey, String delivery_server_url, String stb_id) {
        String default_time_http_url = "http://ntp-a1.nict.go.jp/cgi-bin/jst";
        // ユーザ使用メモリパス
        File ext_memory = Environment.getExternalStorageDirectory();
        String user_strage = ext_memory.getAbsolutePath() + "/";

        // updaterに送信
        Intent updater_intent = new Intent(UpdaterIntentDef.ACTION_REGIST);
        updater_intent.putExtra(UpdaterIntentDef.EXTRA_SITE_ID, siteid);
        updater_intent.putExtra(UpdaterIntentDef.EXTRA_MANAGE_SERVER_URL, MANAGE_SERVER_URL);
        updater_intent.putExtra(UpdaterIntentDef.EXTRA_AKEY, akey);
        updater_intent.putExtra(UpdaterIntentDef.EXTRA_TERMINAL_ID, mEditTerminalId.getText().toString());
        updater_intent.putExtra(UpdaterIntentDef.EXTRA_DELIVERY_SERVER_URL, delivery_server_url);
        updater_intent.putExtra(UpdaterIntentDef.EXTRA_SDCARD_DRIVE, mExtraStorage);

        updater_intent.putExtra(UpdaterIntentDef.EXTRA_TIME_AUTO_SET, true);
        updater_intent.putExtra(UpdaterIntentDef.EXTRA_TIME_HTTP_URL, default_time_http_url);

        updater_intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        sendBroadcast(updater_intent);

        //write to preference
        DefaultPref.setTerminalId(mEditTerminalId.getText().toString());
        DefaultPref.setSiteId(siteid);
        DefaultPref.setStbId(stb_id);
        if(Build.MODEL.equals(DeviceDef.STRIPE)){
            DefaultPref.setExtraId(mExtraId.getText().toString());
        } else if(DeviceDef.isStandAloneInValid()) {
            DefaultPref.setStandAloneMode(mStandAlone.isChecked());
        }
        DefaultPref.setAkey(akey);
        DefaultPref.setManagerUrl(MANAGE_SERVER_URL);
        DefaultPref.setUserStorage(user_strage);
        DefaultPref.setSdcardDrive(mExtraStorage);
        DefaultPref.setWiFiReset(true);

        //登録した日付の保存
        saveTheRegisteredDay();

        Logging.info(getString(R.string.log_info_registered_complete));

        // ダイアログ
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.dialog_title_broadcast_complate);
        alert.setCancelable(false);
        if(Build.MODEL.equals(DeviceDef.STRIPE)){
            alert.setMessage(getString(R.string.dialog_msg_registration_complete, mExtraId.getText()));
        } else {
            alert.setMessage(getString(R.string.dialog_msg_registration_complete_common));
        }
        alert.setPositiveButton(getString(R.string.dialog_button_ok), (dialogInterface, i) -> finishRegistered());
        alert.show();
    }

    public void addLog(String msg, int type) {
        TextView view_item = new TextView(this);
        view_item.setText(msg);
        view_item.setTextAppearance(this, android.R.style.TextAppearance_Medium);
        if (type == 1) {
            view_item.setTextColor(Color.YELLOW);
        } else {
            view_item.setTextColor(Color.WHITE);
        }
        mLogTable.addView(view_item, mCommonParams);
    }

    private void setVolumeMax() {
        try {
            // 音量設定
            AudioManager audio_manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            // 最大音量にする
            assert audio_manager != null;
            int maxvol = audio_manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            audio_manager.setStreamVolume(AudioManager.STREAM_MUSIC, maxvol, 0);

//            addLog(getString(R.string.log_notice_volume_maximize), 0);

        } catch (Exception e) {
            e.printStackTrace();
            addLog("setVolumeMax() " + e.toString(), 1);
        }
    }

    private void setVolumeMin() {
        try {
            // 音量設定
            AudioManager audio_manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            // 最小音量にする
            assert audio_manager != null;
            audio_manager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);

//            addLog(getString(R.string.log_notice_volume_minimum), 0);
        } catch (Exception e) {
            e.printStackTrace();
            addLog("setVolumeMin() " + e.toString(), 1);
        }
    }

    private boolean checkInstallApplication() {
        boolean isInstallUpdater = false;



        PackageManager packageManager = getPackageManager();
        List<ApplicationInfo> applicationInfo = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo info : applicationInfo) {
            if (UpdaterIntentDef.UPDATER_APPLICATION_ID.equals(info.packageName)) {
                isInstallUpdater = true;
            }
        }

        if (!mModel.equals(MODEL_F01D)) {
            if (!isInstallUpdater) {
                addLog(getString(R.string.log_error_no_exist_updater), 1);
            }
        }

        return isInstallUpdater;
    }

    @SuppressLint("StringFormatInvalid")
    private void checkProxyState() {
        //プロキシ利用が有効かつホストとパスワードに値が入っていたら
        //ポートはデフォルトでが入っている
        if (DefaultPref.getProxyEnable() && !DefaultPref.getProxyHost().equals("") && !DefaultPref.getProxyPort().equals("0")) {
            mProxyServer.setText(getResources().getString(R.string.text_view_proxy_server, DefaultPref.getProxyHost(), DefaultPref.getProxyPort()));
        }
    }

    private void finishRegistered() {

        //端末登録が完了したフラグをオンに
        DefaultPref.setRegisteredTerminal(true);

        //スタンドアローンモードがオンになっていたらフラグ付与
        if(DeviceDef.isStandAloneInValid() && mStandAlone.isChecked()){
            DefaultPref.setHaveSetBeforeStandAloneMode(true);
        }

        //端末の登録が完了したのでアラームを起動する
        AdmintApplication.launchApplication();
        if(DefaultPref.getStandAloneMode()){
            Intent intent = new Intent(this, PlaylistService.class);
            intent.setAction(PlaylistService.ACTION_PLAYER_LAUNCH);
            startService(intent);
        }
        //Activityの終了
        RegisterActivity.this.finish();
    }


    private void createNetworkSetting() {

        mBtnNetworkSetting.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_SETTINGS);
            startActivity(intent);
        });

    }

    private void createProxySetting() {

        //Proxyを使用しない選択をした場合は設定のProxy使用の項目からチェックを外す
        mBtnNotUseProxy.setOnClickListener(v -> {
            if (DefaultPref.getProxyEnable()) {
                mProxyServer.setText("");
                DefaultPref.setProxyEnable(false);
                // updaterにも送信
                Intent updater_intent = new Intent(UpdaterIntentDef.ACTION_SETTING_UPDATE);
                updater_intent.putExtra(UpdaterIntentDef.EXTRA_SETTING_PROXY_ENABLE, DefaultPref.getProxyEnable());
                sendBroadcast(updater_intent);
            }
        });

        mBtnUseProxy.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), SettingPreferenceActivity.class);
            RegisterPref.setOpenProxySettingFromRegister(true);
            startActivity(intent);

        });

    }

    @SuppressLint("StringFormatInvalid")
    private void createRebootSetting() {
        String default_time_http_url = "http://ntp-a1.nict.go.jp/cgi-bin/jst";

        mDeviceDateTime.setText(getResources().getString(R.string.text_view_datetime, DateUtil.convDateTimeFormal(System.currentTimeMillis())));

        mBtnReboot.setOnClickListener(v -> {
            AlertDialog.Builder ad = new AlertDialog.Builder(this);
            ad.setTitle(getString(R.string.title_dialog_time_set));
            ad.setMessage(getString(R.string.text_dialog_time_set));
            ad.setPositiveButton(getString(R.string.dialog_button_ok),(dialog, which) -> {

                Intent auto_time_setting = new Intent(UpdaterIntentDef.ACTION_REGIST);
                auto_time_setting.putExtra(UpdaterIntentDef.EXTRA_TIME_AUTO_SET, true);
                auto_time_setting.putExtra(UpdaterIntentDef.EXTRA_TIME_HTTP_URL, default_time_http_url);
                sendBroadcast(auto_time_setting);

                Intent intent = new Intent(getApplicationContext(), UpdaterService.class);
                intent.setAction(UpdaterService.ACTION_REBOOT_MANUAL);
                intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                startService(intent);

            });
            ad.setNegativeButton(getString(R.string.dialog_button_cancel), null);
            ad.show();

        });

    }

    private void createInputField() {

        //端末ID入力時動作
        mEditTerminalId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                int textColor = Color.WHITE;

                // 入力文字数
                int txtLength = s.length();
                // 指定文字数オーバーで文字色を赤くする
                if (txtLength > 30) {
                    textColor = Color.RED;
                }
                mEditTerminalId.setTextColor(textColor);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mTerminalIdNotNull = s.length() > 0;
            }

            @Override
            public void afterTextChanged(Editable s) {
//                afterTextChangeCheck();
            }
        });

        //シリアルキー入力時動作
        if(Build.MODEL.equals(DeviceDef.STRIPE)) {
            mExtraId.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    int textColor = Color.WHITE;

                    // 入力文字数
                    int txtLength = s.length();
                    // 指定文字数オーバーで文字色を赤くする
                    if (txtLength > 30) {
                        textColor = Color.RED;
                    }
                    mExtraId.setTextColor(textColor);
                }


                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    mExtraIdNotNull = s.length() > 0;
                }

                @Override
                public void afterTextChanged(Editable s) {
//                    afterTextChangeCheck();
                }
            });
        }

        if (isMaintenance()) {
            mEditSiteId.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    int textColor = Color.WHITE;

                    // 入力文字数
                    int txtLength = s.length();
                    // 指定文字数オーバーで文字色を赤くする
                    if (txtLength > 30) {
                        textColor = Color.RED;
                    }
                    mEditSiteId.setTextColor(textColor);
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
//                    afterTextChangeCheck();
                }
            });
        }

    }

    private void registSetting() {

        if(!DefaultPref.getRegisteredTerminal()){
            mBtnRegistServer.setEnabled(true);
        } else {
            if(!isMaintenance()){
                mBtnRegistServer.setEnabled(false);
            } else {
                mBtnRegistServer.setEnabled(true);
            }
        }

        //Create an onClickListener for the submit button
        mBtnRegistServer.setOnClickListener(v -> {
            if(!mEditTerminalId.getText().toString().equals("") && mEditTerminalId.getText().toString().length() > 0){
                if(Build.MODEL.equals(DeviceDef.STRIPE) && !mExtraIdNotNull){
                    addLog(getString(R.string.log_error_extra_id_not_input), 1);
                } else {
                    mBtnRegistServer.setEnabled(false);
                    requestServer();
                    postSetting();
                }
            } else {
                addLog(getString(R.string.log_error_no_input_terminal_id), 1);
            }
        });
    }

    //EthernetのIPアドレス取得
    private String getEthernetIpAddress() {
        try {
            NetworkInterface ni = NetworkInterface.getByName("eth0");
            if (ni != null) {
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    String ipAddress = addresses.nextElement().getHostAddress();
                    if (ipAddress.matches("^(([1-9]?[0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([1-9]?[0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")) {
                        return ipAddress;
                    }
                }
            }
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
        return "";
    }

    //WiFiのIPアドレス取得
    private String getWiFiIpAddress() {

        String IpAddress;

        WifiInfo wifiInfo = null;
        WifiManager wifiManager = (WifiManager) AdmintApplication.getInstance().getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifiManager != null) {
            wifiInfo = wifiManager.getConnectionInfo();
        }

        assert wifiInfo != null;
        IpAddress = ((wifiInfo.getIpAddress()) & 0xFF) + "." + ((wifiInfo.getIpAddress() >> 8) & 0xFF) + "." + ((wifiInfo.getIpAddress() >> 16) & 0xFF) + "." + ((wifiInfo.getIpAddress() >> 24) & 0xFF);

        return IpAddress;
    }

    @SuppressLint("StringFormatInvalid")
    private void checkIpAddress() {
        String wifi = getWiFiIpAddress();
        String ethernet = getEthernetIpAddress();

        if (!wifi.equals("0.0.0.0") || !ethernet.equals("")) {
            if (!wifi.equals("0.0.0.0")) {
                mIpAddress.setText(getResources().getString(R.string.text_view_ip_address, wifi));
            } else {
                mIpAddress.setText(getResources().getString(R.string.text_view_ip_address, ethernet));
            }
        }
    }

    private void saveTheRegisteredDay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        ArrayList<String> array = new ArrayList<>();
        String latestActivationDay = DefaultPref.getRegisteredDay();
        Gson gson = new Gson();

        //前回の実行日付が存在しているか
        if (latestActivationDay != null) {

            //前回実行日付の取り出し
            array = gson.fromJson(latestActivationDay, (Type) List.class);
            //前回実行日付に今回の日付を足す
            array.add(sdf.format(System.currentTimeMillis()));
            //実行日付をJsonに変換してプリファレンスに格納する
            DefaultPref.setRegisteredDay(gson.toJson(array));

        } else {
            array.add(sdf.format(System.currentTimeMillis()));
            DefaultPref.setRegisteredDay(gson.toJson(array));
        }
    }

    //設定削除
    private void settingClear() {
        mBtnSettingClear.setOnClickListener(v -> {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle(getString(R.string.text_clear_setting_title));
            alertDialog.setMessage(getString(R.string.text_clear_setting_message));
            alertDialog.setPositiveButton(getString(R.string.dialog_button_ok), (dialog, which) -> {
                DefaultPref.clearDefaultPrefs();
                HealthCheckPref.clearHealthCheckPrefs();
                NetWorkTestPref.clearLatestCheckTime();
                RealtimeCheckPref.clearRtcFileModified();
                RecoverPref.clearAnrFileModified();
                ReportPref.clearLatestUploadTime();
                ServerUrlPref.clearServerUrlPrefs();
                UpdaterPref.clearRebootAlarmTime();
                try {
                    LogDbHelper logDbHelper = new LogDbHelper(this);
                    logDbHelper.initialize();
                    logDbHelper.close();
                } catch (Exception ignore) {}

                // updaterに送信
                Intent updater_intent = new Intent(UpdaterIntentDef.ACTION_REGIST);
                updater_intent.putExtra(UpdaterIntentDef.EXTRA_SITE_ID, "");
                updater_intent.putExtra(UpdaterIntentDef.EXTRA_MANAGE_SERVER_URL, "");
                updater_intent.putExtra(UpdaterIntentDef.EXTRA_AKEY, "");
                updater_intent.putExtra(UpdaterIntentDef.EXTRA_TERMINAL_ID, "");
                updater_intent.putExtra(UpdaterIntentDef.EXTRA_DELIVERY_SERVER_URL, "");
                updater_intent.putExtra(UpdaterIntentDef.EXTRA_SDCARD_DRIVE, "");
                updater_intent.putExtra(UpdaterIntentDef.EXTRA_TIME_AUTO_SET, true);
                updater_intent.putExtra(UpdaterIntentDef.EXTRA_TIME_HTTP_URL, "http://ntp-a1.nict.go.jp/cgi-bin/jst");

                Intent proxy_intent = new Intent(UpdaterIntentDef.ACTION_SETTING_UPDATE);
                proxy_intent.putExtra(UpdaterIntentDef.EXTRA_SETTING_PROXY_ENABLE, false);
                proxy_intent.putExtra(UpdaterIntentDef.EXTRA_SETTING_PROXY_HOST,"");
                proxy_intent.putExtra(UpdaterIntentDef.EXTRA_SETTING_PROXY_PASSWORD,"");
                proxy_intent.putExtra(UpdaterIntentDef.EXTRA_SETTING_PROXY_USER,"");
                proxy_intent.putExtra(UpdaterIntentDef.EXTRA_SETTING_PROXY_PORT,"");



                sendBroadcast(updater_intent);
                sendBroadcast(proxy_intent);

                //Contents delete
                File cdir_path = AdmintPath.getContentsDir();
                deleteContentDir(cdir_path);

                dropDataBases();

                // アラームの削除
                deleteAlarm();

                AlertDialog.Builder completeDialog = new AlertDialog.Builder(this);
                completeDialog.setMessage(getString(R.string.lbl_complete_device_initialize));
                completeDialog.setPositiveButton(getString(R.string.dialog_button_ok),null);
                completeDialog.show();

            });

            alertDialog.setNegativeButton(getString(R.string.dialog_button_cancel), null);

            alertDialog.show();
        });
    }

    private void deleteAlarm(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        //Playelist
        Intent playlistIntent = new Intent(getApplicationContext(), PlaylistService.class);
        PendingIntent playlistPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, playlistIntent, 0);

        //Recover
        Intent recoverIntent = new Intent(getApplicationContext(), RecoverService.class);
        PendingIntent recoverPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, recoverIntent, 0);

        //HealthCheck
        Intent healthIntent = new Intent(getApplicationContext(), HealthCheckService.class);
        PendingIntent healthPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, healthIntent, 0);

        playlistPendingIntent.cancel();
        recoverPendingIntent.cancel();
        healthPendingIntent.cancel();
        assert alarmManager != null;
        alarmManager.cancel(playlistPendingIntent);
        alarmManager.cancel(recoverPendingIntent);
        alarmManager.cancel(healthPendingIntent);
    }

    private void deleteContentDir(File file){

        // 存在しない場合は処理終了
        if (!file.exists()) {
            return;
        }

        // 対象がディレクトリの場合は再帰処理
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteContentDir(child);
            }
        }
        // 対象がファイルもしくは配下が空のディレクトリの場合は削除する
        file.delete();
    }

    private void dropDataBases(){
        try {
            PlaylistDbHelper playlistDbHelper = new PlaylistDbHelper(this);
            SQLiteDatabase pdb = playlistDbHelper.getReaderDb();
            playlistDbHelper.onDowngrade(pdb, 1, 1);
            playlistDbHelper.close();

            ScheduleDbHelper scheduleDbHelper = new ScheduleDbHelper(this);
            SQLiteDatabase sdb = scheduleDbHelper.getReaderDb();
            scheduleDbHelper.onDowngrade(sdb, 1, 1);
            scheduleDbHelper.close();
        } catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void updateDialog(){
        AlertDialog.Builder updateDialog = new AlertDialog.Builder(this);
        updateDialog.setTitle(getString(R.string.lbl_device_update_failed_title));
        updateDialog.setMessage(getString(R.string.lbl_device_update_failed_message));
        updateDialog.setPositiveButton(getString(R.string.dialog_button_yes), ((dialog, which) -> {
            //同意するとupdate flagを有効にした状態で再度登録を実行する
            mUpdateFlag = 1;
            if(!mEditTerminalId.getText().toString().equals("") && mEditTerminalId.getText().toString().length() > 0){
                mBtnRegistServer.setEnabled(false);
                requestServer();
                postSetting();
            } else {
                addLog(getString(R.string.log_error_no_input_terminal_id), 1);
            }
        }));

        updateDialog.setNegativeButton(getString(R.string.dialog_button_cancel), null);

        updateDialog.show();
    }

    public static boolean isMaintenance(){
        final String MAINTENANCE_KEY = "dci_maintenance";
        File key_dir = new File(DeviceDef.getStoragePath() + File.separator + MAINTENANCE_KEY);
        return key_dir.isDirectory();
    }
}

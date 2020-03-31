package jp.co.digitalcruise.admint.player.service.network;

import android.content.Intent;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.pref.ServerUrlPref;

import static jp.co.digitalcruise.admint.player.BuildConfig.APPLICATION_ID;

public class NetLogService extends AbstractNetworkService {
    private static final String ACTION_PREFIX = APPLICATION_ID + ".NetLogService.";

    // NOTICEログ
    public static final String ACTION_LOG_NOTICE = ACTION_PREFIX + "LOG_NOTICE";
    // WARNINGログ
    public static final String ACTION_LOG_WARNING = ACTION_PREFIX + "LOG_WARNING";
    // ダウンロードログ
    public static final String ACTION_LOG_DOWNLOAD = ACTION_PREFIX + "LOG_DOWNLOAD";
    // 旧プリファレンス値のURLでネットログ
    public static final String ACTION_LOG_OLD_DELIVERY_SERVER = ACTION_PREFIX + "LOG_OLD_DELIVERY_SERVER";

    // ログメッセージ
    public static final String INTENT_LOG_MESSAGE = "log_message";

    // エンドポイント
    private static final String API_POPKEEPER_NOTICE = API_POPKEEPER + "notice" + PARAM_KEY;
    private static final String API_POPKEEPER_LOG_DOWNLOAD = API_POPKEEPER + "downloadContentlog" + PARAM_KEY;

    public static final String INTENT_EXTRA_DOWNLOAD_ID = "download_id";
    public static final String INTENT_EXTRA_DOWNLOAD_F_ID = "download_f_id";
    public static final String INTENT_EXTRA_DOWNLOAD_MATERIALS = "download_materials";
    public static final String INTENT_EXTRA_DOWNLOAD_STATUS = "download_status";
    public static final String INTENT_EXTRA_DOWNLOAD_BYTES = "download_bytes";
    public static final String INTENT_EXTRA_DOWNLOAD_INFOFLAG = "info_flag";
    public static final int DOWNLOAD_STATUS_START = 1;
    public static final int DOWNLOAD_STATUS_ERROR = 2;
    public static final int DOWNLOAD_STATUS_SUCCESS = 3;

    private static final int LOG_LEVEL_NOTICE = 0;
    private static final int LOG_LEVEL_WARNING = 1;

    public NetLogService() {
        super(NetLogService.class.getName());
    }

    protected NetLogService(String name) {
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

            if(ACTION_LOG_NOTICE.equals(action)){
                actionLogNotice(intent);
            }else if(ACTION_LOG_WARNING.equals(action)){
                actionLogWarning(intent);
            }else if(ACTION_LOG_DOWNLOAD.equals(action)){
                actionLogDownload(intent);
            }else if(ACTION_LOG_OLD_DELIVERY_SERVER.equals(action)){
                actionLogOldDeliveryServer(intent);
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionLogDownload(Intent intent){
        try{
            // ネットワークサービスがOFFの時
            if(!DefaultPref.getNetworkService()){
                return;
            }

            netLoggingDownload(intent);
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionLogNotice(Intent intent){

        try {
            // ネットワークサービスがOFFの時
            if(!DefaultPref.getNetworkService()){
                return;
            }

            netLogging(LOG_LEVEL_NOTICE, intent.getStringExtra(INTENT_LOG_MESSAGE));
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionLogWarning(Intent intent){

        try {
            // ネットワークサービスがOFFの時
            if(!DefaultPref.getNetworkService()){
                return;
            }

            netLogging(LOG_LEVEL_WARNING, intent.getStringExtra(INTENT_LOG_MESSAGE));
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionLogOldDeliveryServer(Intent intent){

        try {
            // ネットワークサービスがOFFの時
            if(!DefaultPref.getNetworkService()){
                return;
            }

            netLoggingOldDeliveryServer(intent.getStringExtra(INTENT_LOG_MESSAGE));
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void netLoggingOldDeliveryServer(String msg){
        // 定義(preference)ファイルの変更によりバージョンアップ通知時に参照するURLが一時的に空になるため
        // このメソッドで通知する
        // 3.1以降(3.0が浸透したら）で不要になる予定
        try{
            String delivery_server_url = DefaultPref.getOldDeliveryServerUrl();

            if(delivery_server_url.length() == 0){
                Logging.error(getString(R.string.log_error_no_setting_delivery_server_url));
                return;
            }

            String akey = DefaultPref.getAkey();
            String site_id = DefaultPref.getSiteId();

            // postデータ設定
            HashMap<String,String> post = new HashMap<>();
            post.put("siteid", site_id);
            post.put("logtype", Integer.toString(LOG_LEVEL_NOTICE));
            post.put("logmsg", msg);

            // post実行
            String request_url = delivery_server_url + API_POPKEEPER_NOTICE + akey;
            postRequest(request_url, post);

        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }


    private void netLogging(int type, String msg){
        try{
            String delivery_server_url = ServerUrlPref.getDeliveryServerUrl();

            if(delivery_server_url.length() == 0){
                Logging.error(getString(R.string.log_error_no_setting_delivery_server_url));
                return;
            }

            String akey = DefaultPref.getAkey();
            String site_id = DefaultPref.getSiteId();

            // postデータ設定
            HashMap<String,String> post = new HashMap<>();
            post.put("siteid", site_id);
            post.put("logtype", Integer.toString(type));
            post.put("logmsg", msg);

            // post実行
            String request_url = delivery_server_url + API_POPKEEPER_NOTICE + akey;
            postRequest(request_url, post);

        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void netLoggingDownload(Intent intent){
        try{
            String delivery_server_url = ServerUrlPref.getDeliveryServerUrl();

            if(delivery_server_url.length() == 0){
                Logging.error(getString(R.string.log_error_no_setting_delivery_server_url));
                return;
            }

            int id = intent.getIntExtra(INTENT_EXTRA_DOWNLOAD_ID, 0);
            int f_id = intent.getIntExtra(INTENT_EXTRA_DOWNLOAD_F_ID, 0);
            int status = intent.getIntExtra(INTENT_EXTRA_DOWNLOAD_STATUS, 0);
            long bytes = intent.getLongExtra(INTENT_EXTRA_DOWNLOAD_BYTES, 0);
            int infoflag = intent.getIntExtra(INTENT_EXTRA_DOWNLOAD_INFOFLAG, 0);
            ArrayList<String> materials = intent.getStringArrayListExtra(INTENT_EXTRA_DOWNLOAD_MATERIALS);

            String akey = DefaultPref.getAkey();
            String site_id = DefaultPref.getSiteId();

            // postデータ設定
            HashMap<String,String> post = new HashMap<>();
            post.put("siteid", site_id);
            post.put("contentid", Integer.toString(id));
            post.put("status", Integer.toString(status));
            post.put("byte", String.valueOf(bytes));
            post.put("infoflag",String.valueOf(infoflag));

            if(materials != null && materials.size() > 0){
                for(int i = 0; i < materials.size(); i++){
                    post.put("commonid[" + i + "]", materials.get(i));
                }
            }

            if(f_id > 0) {
                post.put("subid", Integer.toString(f_id));
            } else {
                post.put("subid", "");
            }

            // post実行
            String request_url = delivery_server_url + API_POPKEEPER_LOG_DOWNLOAD + akey;
            postRequest(request_url, post);

        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }
}

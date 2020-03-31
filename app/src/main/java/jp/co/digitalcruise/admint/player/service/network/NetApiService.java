package jp.co.digitalcruise.admint.player.service.network;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.parsexml.object.GoForItObject;
import jp.co.digitalcruise.admint.player.component.parsexml.parser.GoForItParser;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.pref.ServerUrlPref;

import static jp.co.digitalcruise.admint.player.BuildConfig.APPLICATION_ID;

public class NetApiService extends AbstractNetworkService {

    private static final String ACTION_PREFIX = APPLICATION_ID + ".NetApiService.";

    // アプリ起動時
    public static final String ACTION_PLAYER_LAUNCH = ACTION_PREFIX + "PLAYER_LAUNCH";
    // 各サーバURL取得
    public static final String ACTION_GO_FOR_IT = ACTION_PREFIX + "GO_FOR_IT";

    private static final String API_GOFORIT = "GoForIt/";
    public static final String API_GOFORIT_GET_SERVER = API_GOFORIT + "GetServer";

    public NetApiService() {
        super(NetApiService.class.getName());
    }


    protected NetApiService(String name) {
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

            // ネットワークサービスがONの時
            if (ACTION_PLAYER_LAUNCH.equals(action)) {
                actionPlayerLaunch();
            } else if (ACTION_GO_FOR_IT.equals(action)) {
                actionGoForIt();
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionPlayerLaunch(){
        try {
            // ネットワークサービスがOFFの時
            if(!DefaultPref.getNetworkService()){
                return;
            }

            // 端末起動直後のアプリ起動の時ネットワーク接続できていない可能性がありネットワーク接続待ちを行う
            waitNetConnection();

            // 各サーバURL取得
            actionGoForIt();

            //24時間後にもう一度
            setGoForItAlarm();
            
            // ヘルスチェック
            intentHealthCheckPlayerLaunch();
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void actionGoForIt(){

        try{
            // ネットワークサービスがOFFの時
            if(!DefaultPref.getNetworkService()){
                return;
            }

            // 管理サーバURL
            String manage_url = DefaultPref.getManagerUrl();

            // ロギング
            Logging.info(getString(R.string.log_info_start_go_for_it));

            // サーバ情報取得
            String request_result = requestGoForIt(manage_url);

            // リクエスト成功
            if(request_result != null){
                // ロギング
                Logging.info(getString(R.string.log_info_success_go_for_it));

                // パース
                GoForItObject go_for_it = GoForItParser.parseGoForIt(request_result);

                // 取得した値をプリファレンスにセット
                setPrefGoForIt(go_for_it);
                
            }else{
                // リクエストエラー
                // ロギング
                Logging.info(getString(R.string.log_error_failed_go_for_it));
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void setGoForItAlarm(){
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NetApiService.class);
        intent.setAction(ACTION_GO_FOR_IT);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        assert alarmManager != null;
        alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis() + AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    private void intentHealthCheckPlayerLaunch(){
        Intent intent = new Intent(this, HealthCheckService.class);
        intent.setAction(HealthCheckService.ACTION_PLAYER_LAUNCH);
        startService(intent);
    }

    private void waitNetConnection(){
        try{
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if(cm != null){
                // ネットワーク接続を最長で10sec待つ
                for(int i = 0; i < 10 ; i++){
                    NetworkInfo net_info = cm.getActiveNetworkInfo();
                    if(net_info != null && net_info.isConnected()){
                        break;
                    }
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
    }

    private @Nullable String requestGoForIt(@Nullable String manage_url){
        String result = null;
        try{
            // 管理サーバURL
            // サイトID
            String site_id = DefaultPref.getSiteId();
            if(manage_url == null || manage_url.length() == 0 || site_id.length() == 0){
                return null;
            }

            // APIを設定
            String request_url = manage_url + API_GOFORIT_GET_SERVER;

            HashMap<String,String> post = new HashMap<>();
            post.put("siteid", site_id);

            result = postRequest(request_url, post);

        }catch(Exception e){
            Logging.stackTrace(e);
        }
        return result;
    }

    private void setPrefGoForIt(@NonNull GoForItObject go_for_it){

        String delivery_server_url = go_for_it.delivery_server_url;
        String cdn_server_url = go_for_it.cdn_server_url;
        String upload_server_url = go_for_it.upload_server_url;
        String rtc_server_url = go_for_it.rtc_server_url;
        String manage_server_url = go_for_it.manage_server_url;
        String external_sever_url = go_for_it.external_sever_url;
        String rsc_server_url = go_for_it.rsc_server_url;

        if(delivery_server_url != null){
            if(isEnableUrl(delivery_server_url)){
                if(delivery_server_url.charAt(delivery_server_url.length() - 1) != '/'){
                    delivery_server_url = delivery_server_url + "/";
                }

                String old_delivery_server_url = ServerUrlPref.getDeliveryServerUrl();
                // 変更があれば更新
                if(!delivery_server_url.equals(old_delivery_server_url)){
                    ServerUrlPref.setDeliveryServerUrl(delivery_server_url);
                }
            }
        }

        if(upload_server_url != null){
            if(isEnableUrl(upload_server_url)){
                if(upload_server_url.charAt(upload_server_url.length() - 1) != '/'){
                    upload_server_url = upload_server_url + "/";
                }

                String old_upload_server_url = ServerUrlPref.getUploadServerUrl();
                // 変更があれば更新
                if(!upload_server_url.equals(old_upload_server_url)){
                    ServerUrlPref.setUploadServerUrl(upload_server_url);
                }
            }
        }

        if(rtc_server_url != null){
            if(isEnableUrl(rtc_server_url)){
                if(rtc_server_url.charAt(rtc_server_url.length() - 1) != '/'){
                    rtc_server_url = rtc_server_url + "/";
                }

                String old_rtc_server_url = ServerUrlPref.getRtcServerUrl();
                // 変更があれば更新
                if(!rtc_server_url.equals(old_rtc_server_url)){
                    ServerUrlPref.setRtcServerUrl(rtc_server_url);
                }
            }
        }

        if(cdn_server_url != null){
            if(isEnableUrl(cdn_server_url)){
                if(cdn_server_url.charAt(cdn_server_url.length() - 1) != '/'){
                    cdn_server_url = cdn_server_url + "/";
                }

                String old_cdn_server_url = ServerUrlPref.getCdnServerUrl();
                // 変更があれば更新
                if(!cdn_server_url.equals(old_cdn_server_url)){
                    ServerUrlPref.setCdnServerUrl(cdn_server_url);
                }
            }else if(cdn_server_url.equals("")){
                // 設定がない場合は空でくる
                // ダウンロードサーバはcdnがなければdeliveryを使用するが、有効値をチェックしているとcdnの値がクリアできないので空文字の時はクリアする
                // ほとんどのケースで問題ないがcdnが存在しなくなった時用に対処する
                ServerUrlPref.setCdnServerUrl("");
            }
        }

        if(external_sever_url != null){
            if(isEnableUrl(external_sever_url)){
                if(external_sever_url.charAt(external_sever_url.length() - 1) != '/'){
                    external_sever_url = external_sever_url + "/";
                }

                String old_external_server_url = ServerUrlPref.getExternalServerUrl();
                // 変更があれば更新
                if(!external_sever_url.equals(old_external_server_url)){
                    ServerUrlPref.setExternalServerUrl(external_sever_url);
                }
            }
        }

        if(manage_server_url != null){
            updateManageServerUrl(manage_server_url);
        }

        if(rsc_server_url != null){
            if(isEnableUrl(rsc_server_url)){
                if(rsc_server_url.charAt(rsc_server_url.length() -1) != '/'){
                    rsc_server_url = rsc_server_url + "/";
                }
                String old_rsc_server_url = ServerUrlPref.getRscServerUrl();
                if(!rsc_server_url.equals(old_rsc_server_url)){
                    ServerUrlPref.setRscServerUrl(rsc_server_url);
                }
            }else if(rsc_server_url.equals("")){
                // 設定がない場合は空でくる
                // ダウンロードサーバはrscがなければdeliveryを使用するが、有効値をチェックしているとrscの値がクリアできないので空文字の時はクリアする
                // ほとんどのケースで問題ないがcdnが存在しなくなった時用に対処する
                ServerUrlPref.setRscServerUrl("");
            }
        }

    }


    private void updateManageServerUrl(@Nullable String new_url_str){

        try{
            String old_url_str = DefaultPref.getManagerUrl();
            if(new_url_str != null){
                if(!old_url_str.equals(new_url_str)){
                    // リクエストAPIが成功するか
                    if(requestGoForIt(new_url_str) != null){
                        DefaultPref.setManagerUrl(new_url_str);
                    }
                }
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private boolean isEnableUrl(@Nullable String url){
        boolean ret = false;
        try{
            if(url != null){
                String pattern = "\\b(https?|ftp)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
                Pattern patt = Pattern.compile(pattern);
                Matcher matcher = patt.matcher(url);
                ret = matcher.matches();
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
        return ret;
    }
}

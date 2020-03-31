package jp.co.digitalcruise.admint.player.service.network;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;

import java.net.URL;
import java.util.HashMap;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.define.DeviceDef;
import jp.co.digitalcruise.admint.player.component.file.AdmintPath;
import jp.co.digitalcruise.admint.player.component.file.FileUtil;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.netutil.ContentInfo;
import jp.co.digitalcruise.admint.player.component.netutil.Downloader;
import jp.co.digitalcruise.admint.player.component.netutil.RequestApi;
import jp.co.digitalcruise.admint.player.component.netutil.Uploader;
import jp.co.digitalcruise.admint.player.component.parsexml.object.ApiResultObject;
import jp.co.digitalcruise.admint.player.component.parsexml.parser.ApiResultParser;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.service.AbstractService;


abstract public class AbstractNetworkService extends AbstractService {

    public static final String API_POPKEEPER = "ScPopkeeper/";
    public static final String PARAM_KEY = "/akey:";
    public static final String URI_APFILES = "apfiles";

    protected AbstractNetworkService(String name) {
        super(name);
    }

    protected ContentInfo makeContentInfo(){
        ContentInfo cinfo = new ContentInfo();

        if(DefaultPref.getProxyEnable() && !DeviceDef.isGroova()){
            cinfo.setProxy(DefaultPref.getProxyHost(), convInt(DefaultPref.getProxyPort()));
            if(isProxyAuth()){
                cinfo.setProxyAuth(DefaultPref.getProxyUser(), DefaultPref.getProxyPassword());
            }
        }
        return cinfo;
    }

    protected Downloader makeDownloader(){
        Downloader loader = new Downloader();

        if(DefaultPref.getProxyEnable() && !DeviceDef.isGroova()){
            loader.setProxy(DefaultPref.getProxyHost(), convInt(DefaultPref.getProxyPort()));
            if(isProxyAuth()){
                loader.setProxyAuth(DefaultPref.getProxyUser(), DefaultPref.getProxyPassword());
            }
        }
        return loader;
    }

    protected RequestApi makeRequestApi(){
        RequestApi api = new RequestApi();
        if(DefaultPref.getProxyEnable() && !DeviceDef.isGroova()){
            api.setProxy(DefaultPref.getProxyHost(), convInt(DefaultPref.getProxyPort()));
            if(isProxyAuth()){
                api.setProxyAuth(DefaultPref.getProxyUser(), DefaultPref.getProxyPassword());
            }
        }
        return api;
    }

    protected Uploader makeUploader(){
        Uploader loader = new Uploader();
        if(DefaultPref.getProxyEnable() && !DeviceDef.isGroova()){
            loader.setProxy(DefaultPref.getProxyHost(), convInt(DefaultPref.getProxyPort()));
            if(isProxyAuth()){
                loader.setProxyAuth(DefaultPref.getProxyUser(), DefaultPref.getProxyPassword());
            }
        }
        return loader;
    }

//    protected String getPrefDeliveryServerUrl(){
//        return ServerUrlPref.getDeliveryServerUrl();
//    }
//
//    protected String getPrefSiteId(){
//        return DefaultPref.getSiteId();
//    }
//
//    protected String getPrefAkey(){
//        return DefaultPref.getAkey();
//    }

    private boolean isProxyAuth(){
        boolean ret = false;
        if(DefaultPref.getProxyEnable() && DefaultPref.getProxyUser().length() > 0){
            ret = true;
        }
        return ret;
    }

    protected void loggingCheckActiveNetwork(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo net_info;
        if (cm != null) {
            net_info = cm.getActiveNetworkInfo();
        } else {
            net_info = null;
        }

        boolean isConnected;
        String ext_msg;
        if (net_info != null) {
            if (!net_info.isAvailable()) {
                if (net_info.getState() == NetworkInfo.State.CONNECTED) {
                    ext_msg = "";
                    isConnected = true;
                } else {
                    ext_msg = "NetworkInfo.isAvailable() return false AND NetworkInfo.getState() return not CONNECTED";
                    isConnected = false;
                }
            } else if (!net_info.isConnectedOrConnecting()) {
                ext_msg = "NetworkInfo.isConnectedOrConnecting() return false";
                isConnected = false;
            } else {
                ext_msg = "Non Reason";
                isConnected = true;
            }
        } else {
            isConnected = false;
            ext_msg = "ConnectivityManager.getActiveNetworkInfo() return null";
        }

        if (!isConnected) {
            Logging.error(getString(R.string.log_error_failed_connect_network) + " (" + ext_msg + ")");
        }
    }

    protected @Nullable String postRequest(String url_path, HashMap<String, String> post){
//        notice(getString(R.string.log_notice_start_access_server));
        String result = null;
        URL url;
        RequestApi api = null;
        try{
            // androidが返却するネットワーク情報に誤りがある端末があり、通信処理はどういう状態であれ実行するが状態をログ
            loggingCheckActiveNetwork();

            url = new URL(url_path);
            api = makeRequestApi();

            String post_result = api.okRequestPost(url, post);
//           String post_result = api.requestPost(url, post);
            if(post_result != null) {
                ApiResultObject xml_result = ApiResultParser.parseResultXml(post_result);
                if (!ApiResultObject.STATUS_OK.equals(xml_result.status)) {
                    // サーバ通信は成功したが返却値がNG
                    Logging.error(getString(R.string.log_error_result_xml_return_ng) + ", code=" + xml_result.code + ", msg=" + xml_result.msg);
               } else {
                    result = post_result;
               }
            }
        }catch(Exception e){
            Logging.network_error(e);
            Logging.stackTrace(e);
        }

        return result;
    }

    protected int convInt(String val) {
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            // Intに変換できない場合はサブIDではない
            return 0;
        }
    }

    protected String getLogStorageStatusStr() {
        String msg = "";
        // 利用可能サイズ
        long avail = FileUtil.getAvailableMemorySize(AdmintPath.getAplicationDir());
        avail = avail / 1024 / 1024; // MB
        // トータルサイズ
        long total = FileUtil.getTotalMemorySize(AdmintPath.getAplicationDir());
        total = total / 1024 / 1024; // MB
        msg = "Storage size = " + Long.toString(avail) + "MB/" + Long.toString(total) + "MB(available/total)";
        return msg;
    }
}

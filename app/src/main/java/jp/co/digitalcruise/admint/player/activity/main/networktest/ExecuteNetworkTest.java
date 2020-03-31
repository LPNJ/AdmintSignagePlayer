package jp.co.digitalcruise.admint.player.activity.main.networktest;

import android.content.Context;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.netutil.RequestApi;
import jp.co.digitalcruise.admint.player.component.parsexml.object.ApiResultObject;
import jp.co.digitalcruise.admint.player.component.parsexml.parser.ApiResultParser;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.pref.ServerUrlPref;
import jp.co.digitalcruise.admint.player.service.network.HealthCheckService;

public class ExecuteNetworkTest extends AsyncTask<NetworkTestActivity.AsyncTaskArgument, Void, ExecuteNetworkTest.NetworkTestResult> {

    private CallBack callbacktask;

    static final int TYPE_PING_GATEWAY = 0;
    static final int TYPE_PING_PROXY = 1;
    static final int TYPE_HEALTH_CHECK = 3;

    class NetworkTestResult {

        boolean isResult = false;

        public String message = "";

        public void setResult(boolean result) {
            this.isResult = result;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }


    abstract static class CallBack {
        void callBack(NetworkTestResult result) {
        }

        void progressUpdateCallback(Void[] a) {
        }
    }

    void setOnCallBack(CallBack _cbj) {
        callbacktask = _cbj;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

    }

    @Override
    protected NetworkTestResult doInBackground(NetworkTestActivity.AsyncTaskArgument... params) {
        Context context = null;
        NetworkTestResult result = new NetworkTestResult();
        try {
            //nullCheck nullが来た場合はassertionErrorを表示
            assert params[0] != null;
            if (params[0].context != null) {
                context = params[0].context;
                String target = params[0].target;
                int type = params[0].type;

                if(type != TYPE_HEALTH_CHECK) {
                    if (target == null || target.length() <= 0) {
                        result.isResult = false;
                        result.message = context.getString(R.string.non_target_ip);
                        return result;
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Logging.stackTrace(e);
                }
                onProgressUpdate();

                //ヘルスチェックかpingか
                if (type == TYPE_HEALTH_CHECK) {

                    int typeHealthCheck = 0;
                    //ヘルスチェック
                    boolean healthCheck = requestHealthCheck(typeHealthCheck);

                    result.setResult(healthCheck);

                    return result;
                } else {
                    return ping(target);
                }
            }
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
        return result;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        callbacktask.progressUpdateCallback(values);
    }

    /**
     *
     * @param result
     */
    @Override
    protected void onPostExecute(NetworkTestResult result){
        callbacktask.callBack(result);
    }



    /**
     * 引数で指定されたIPアドレスに対してpingを実行し，その結果を返す
     *
     * @param ipAddressStr IPアドレス
     * @return pingの実行結果
     */
    private NetworkTestResult ping(String ipAddressStr){
        NetworkTestResult result = new NetworkTestResult();
        Runtime runtime = Runtime.getRuntime();
        Process proc = null;
        final String NO_MESSAGE = "NO MESSAGE";

        if(ipAddressStr != null && !ipAddressStr.equals("") && !"0.0.0.0".equals(ipAddressStr)){
            try {
                proc = runtime.exec("ping -c 5 " + ipAddressStr);
                proc.waitFor();
            } catch (Exception e) {
                Logging.stackTrace(e);
            }
        }
        int exitVal = -1;
        if(proc != null) {
            exitVal = proc.exitValue();
        }
        if(exitVal == 0) {
            result.setResult(true);
            InputStreamReader reader = new InputStreamReader(proc.getInputStream());
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[512];
            int read;
            try {
                while (0 <= (read = reader.read(buffer))) {
                    builder.append(buffer, 0, read);
                }
            } catch (IOException e) {
                Logging.stackTrace(e);
            }
            result.setMessage(builder.toString());
        } else {
            result.setResult(false);
            if(proc == null) {
                result.setMessage(NO_MESSAGE);
                return result;
            }
            InputStreamReader reader = new InputStreamReader(proc.getErrorStream());
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[512];
            int read;
            try {
                while (0 <= (read = reader.read(buffer))) {
                    builder.append(buffer, 0, read);
                }
            } catch (IOException e) {
                Logging.stackTrace(e);
            }
            result.setMessage(builder.toString());
        }
        return result;
    }

    private boolean requestHealthCheck(int type){
        boolean ret =false;
        try {
            String site_id = DefaultPref.getSiteId();
            String akey = DefaultPref.getAkey();
            String request_server_url = ServerUrlPref.getDeliveryServerUrl();
            int startDatetime = (int) Math.floor(System.currentTimeMillis() / (double) 1000);
            int endDatetime = startDatetime + 24 * 60 * 60;

            HashMap<String, String> post_data = new HashMap<>();
            post_data.put("siteid", site_id);
            post_data.put("startdatetime", Integer.toString(startDatetime));
            post_data.put("enddatetime", Integer.toString(endDatetime));
            post_data.put("requesttype", Integer.toString(type));

            String schedule_url = request_server_url + HealthCheckService.API_POPKEEPER_SCHEDULE + akey;


            URL url;
            RequestApi api;

            url = new URL(schedule_url);
            api = makeRequestApi();

            String post_result = api.okRequestPost(url, post_data);
//            String post_result = api.requestPost(url, post_data);
            if (post_result != null) {
                ApiResultObject xml_result = ApiResultParser.parseResultXml(post_result);
                if (ApiResultObject.STATUS_OK.equals(xml_result.status)) {
                    ret = true;
                }

            }
        }catch (Exception e) {
            Logging.stackTrace(e);
        }

        return ret;
    }

    private RequestApi makeRequestApi(){
        RequestApi api = new RequestApi();
        if(DefaultPref.getProxyEnable()){

            int port;
            try {
                port = Integer.parseInt(DefaultPref.getProxyPort());
            } catch (NumberFormatException e) {
                port = 0;
            }

            api.setProxy(DefaultPref.getProxyHost(), port);
            if(DefaultPref.getProxyEnable() && !DefaultPref.getProxyUser().equals("") && DefaultPref.getProxyUser().length() > 0){
                api.setProxyAuth(DefaultPref.getProxyUser(), DefaultPref.getProxyPassword());
            }
        }
        return api;
    }


}

package jp.co.digitalcruise.admint.player.component.netutil;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RequestApi extends AbstractNetUtil{

    private class ResponseHandlerApi implements ResponseHandler<String> {
        @Override
        public String handleResponse(HttpResponse response) throws RuntimeException, IOException {
            int res_code = response.getStatusLine().getStatusCode();
            switch (res_code) {
                case HttpStatus.SC_OK:
                    // レスポンスデータを文字列として取得する。
                    // byte[]として読み出したいときはEntityUtils.toByteArray()を使う。
                    return EntityUtils.toString(response.getEntity(), "UTF-8");
                default:
                    throw new NetUtilException(NetUtilException.ERROR_CODE_HTTP_RESPONS_ERROR, res_code, "Server Response : " + res_code);
            }
        }
    }

//    public @Nullable String requestPost(URL request_url, HashMap<String, String> post) throws IOException, RuntimeException {
//        String result = null;
//
//        HttpPost request = new HttpPost(request_url.toString());
//        request.setHeader("Accept-Encoding", "");
//        request.setHeader("Cache-Control", "no-cache");
//        request.setHeader("Connection", "close");
//
//        if(post != null && post.size() > 0){
//            ArrayList<NameValuePair> params = new ArrayList<>();
//            for(Map.Entry<String, String> e : post.entrySet()){
//                params.add(new BasicNameValuePair(e.getKey(), e.getValue()));
//            }
//            request.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
//        }
//
//        DefaultHttpClient httpClient = createHttpClient();
//
//        try{
//            result = httpClient.execute(request, new ResponseHandlerApi());
//        }finally{
//            httpClient.getConnectionManager().shutdown();
//        }
//
//        return result;
//    }

    public String okRequestPost(URL request_url, HashMap<String, String> post) throws IOException, RuntimeException{

        final FormBody.Builder formBodyBuilder = new FormBody.Builder();
        formBodyBuilder.add("param_name", "param_value");

        if(post != null && post.size() > 0){
            for(Map.Entry<String, String> e : post.entrySet()){
                formBodyBuilder.add(e.getKey(), e.getValue());
            }
        }


        final Request request = new Request.Builder()
                .url(request_url)
                .header("Accept-Encoding", "")
                .header("Cache-Control", "no-cache")
                .header("Connection", "close")
                .post(formBodyBuilder.build())
                .build();

        //TLSの指定とか
        OkHttpClient okHttpClient = createOkHttpClient(request_url);

        StringBuilder result = new StringBuilder();
//        result.append(response.body().string());

        Response response = okHttpClient.newCall(request).execute();
        assert response.body() != null;
        result.append(response.body().string());

//        Call call = okHttpClient.newCall(request);
//        call.enqueue(new Callback() {
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                try(ResponseBody responseBody = response.body()){
//                    assert responseBody != null;
//                    result.append(responseBody.string());
////                    System.out.println("append post data to result");
//                }
//            }
//
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                Logging.error(e.getMessage());
//            }
//        });
//
//        try{//resultに値を入れる処理が終わるのを待つために1.5秒ほど停止
//            Thread.sleep(2000);
//        } catch (Exception ignore){}

        return result.toString();
    }

//    public @Nullable String requestGet(URL request_url) throws IOException, RuntimeException {
//        String result = null;
//
//        HttpGet request = new HttpGet(request_url.toString());
//        request.setHeader("Accept-Encoding", "");
//
//        DefaultHttpClient httpClient = createHttpClient();
//
//        try{
//            result = httpClient.execute(request, new ResponseHandlerApi());
//        }finally{
//            httpClient.getConnectionManager().shutdown();
//        }
//        return result;
//    }
}

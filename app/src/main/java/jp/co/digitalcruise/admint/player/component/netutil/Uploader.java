package jp.co.digitalcruise.admint.player.component.netutil;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import jp.co.digitalcruise.admint.player.component.log.Logging;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Uploader extends AbstractNetUtil {

    protected class ResponseHandlerApi implements ResponseHandler<String> {
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

    private String getOkHttpResponse(Response response){
        int res_code = response.code();
        switch (res_code) {
            case OkHttpStatus.SC_OK:
                return response.message();
            default:
                throw new NetUtilException(NetUtilException.ERROR_CODE_HTTP_RESPONS_ERROR, res_code, "Server Response : " + res_code);
        }
    }

//    public String upload(URL request_url, HashMap<String, String> post, String post_name, File post_file) throws IOException, RuntimeException {
//
//        String result = null;
//        HttpPost request = new HttpPost(request_url.toString());
//        request.setHeader("Accept-Encoding", "");
//        request.setHeader("Cache-Control", "no-cache");
//        request.setHeader("Connection", "close");
//
//        MultipartEntityBuilder multiPartEntityBuilder = MultipartEntityBuilder.create();
//        multiPartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
//
//        ContentType textContentType = ContentType.create("text/plain", "UTF-8");
//        if(post != null && post.size() > 0){
//            for(Map.Entry<String, String> entry : post.entrySet()){
//                multiPartEntityBuilder.addTextBody(entry.getKey(), entry.getValue(), textContentType);
//            }
//        }
//
//        FileBody bin = new FileBody(post_file);
//        multiPartEntityBuilder.addPart(post_name, bin);
//
//        HttpEntity postEntity = multiPartEntityBuilder.build();
//        request.setEntity(postEntity);
//
//        DefaultHttpClient httpClient = createHttpClient();
//        ResponseHandlerApi respons = new ResponseHandlerApi();
//        try{
//            result = httpClient.execute(request, respons);
//        }finally{
//            httpClient.getConnectionManager().shutdown();
//        }
//
//        return result;
//    }

    public String uploadWithOkHttp(URL request_url, HashMap<String, String> post, String post_name, File post_file) throws IOException, RuntimeException {
        String result = null;
        Request.Builder requestBuilder = new Request.Builder()
                .url(request_url)
                .header("Accept-Encoding", "")
                .header("Cache-Control", "no-cache")
                .header("Connection", "close");


        MultipartBody.Builder multipartbody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM) ;
        if(post != null && post.size() > 0){
            for(Map.Entry<String, String> entry : post.entrySet()){
                multipartbody.addFormDataPart(entry.getKey(), entry.getValue());
            }
        }
        multipartbody.addFormDataPart(post_name, post_file.getName(), RequestBody.create(MediaType.parse("application/octet-stream"), post_file));
        Request request = requestBuilder.post(multipartbody.build()).build();

        OkHttpClient okHttpClient = createOkHttpClient(request_url);
        try{
            Response response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            {
                assert response.body() != null;
                result = response.body().string();
            }
        } catch (Exception e){
            Logging.stackTrace(e);
        }
        return  result;
    }
}

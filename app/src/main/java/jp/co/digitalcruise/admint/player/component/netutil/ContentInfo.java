package jp.co.digitalcruise.admint.player.component.netutil;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ContentInfo extends AbstractNetUtil{

    // ファイル最終更新日時
    private long mLastModified = 0;
    // ファイルサイズ
    private long mContentLength = 0;

    public long getLastModified(){
        return mLastModified;
    }

    public long getContentLength(){
        return mContentLength;
    }

//    public class ResponseHandlerHead implements ResponseHandler<Void> {
//        private long mHandleContentLength = 0;
//        private long mHandleModified = 0;
//
//        private long getHandleContentLength(){
//            return mHandleContentLength;
//        }
//
//        private long getHandleModified(){
//            return mHandleModified;
//        }
//
//        @Override
//        public Void handleResponse(HttpResponse response) {
//            int res_code = response.getStatusLine().getStatusCode();
//            switch (res_code) {
//                case HttpStatus.SC_OK:
//                case HttpStatus.SC_PARTIAL_CONTENT:
//                    Header length_header = response.getFirstHeader("Content-Length");
//                    if(length_header != null){
//                        mHandleContentLength = Long.parseLong(length_header.getValue());
//                    }
//
//                    Header date_header = response.getFirstHeader("Last-Modified");
//                    if(date_header != null){
//                        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US);
//                        try{
//                            Date lm_date = format.parse(date_header.getValue());
//                            mHandleModified = lm_date.getTime();
//                        }catch(java.text.ParseException e){
//                            throw new NetUtilException(NetUtilException.ERROR_CODE_UNDEFINED_ERROR, 0, e.toString());
//                        }
//                    }
//                    break;
//                default:
//                    throw new NetUtilException(NetUtilException.ERROR_CODE_HTTP_RESPONS_ERROR, res_code, "Server Response : " + res_code);
//            }
//            return null;
//        }
//    }

    private void setResponseModifiedData(Response response){
        int res_code = response.code();
        switch (res_code) {
            case OkHttpStatus.SC_OK:
            case OkHttpStatus.SC_PARTIAL_CONTENT:
                Headers length_header = response.headers();
                String length_header_value = length_header.get("Content-Length");
                if(length_header_value != null){
                    mContentLength = Long.parseLong(length_header_value);
                }
                Headers date_header = response.headers();
                String date_header_value = date_header.get("Last-Modified");
                if(date_header_value != null){
                    SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US);
                    try{
                        Date lm_date = format.parse(date_header_value);
                        mLastModified = lm_date.getTime();
                    }catch(java.text.ParseException e){
                        throw new NetUtilException(NetUtilException.ERROR_CODE_UNDEFINED_ERROR, 0, e.toString());
                    }
                }
                break;
            default:
                throw new NetUtilException(NetUtilException.ERROR_CODE_HTTP_RESPONS_ERROR, res_code, "Server Response : " + res_code);
        }
    }

//    public void execute(URL request_url) throws IOException {
//
//        DefaultHttpClient httpClient = createHttpClient();
//
//        try{
//            HttpHead request = new HttpHead(request_url.toString());
//            request.setHeader("Cache-Control", "no-cache");
//            request.setHeader("Connection", "close");
//
//            ResponseHandlerHead handler = new ResponseHandlerHead();
//            httpClient.execute(request, handler);
//
//            mLastModified = handler.getHandleModified();
//            mContentLength = handler.getHandleContentLength();
//        }finally{
//            httpClient.getConnectionManager().shutdown();
//        }
//    }

    public void executeContentInfo(URL request_url) throws IOException {

        OkHttpClient httpClient = createOkHttpClient(request_url);

        Request.Builder request = new Request.Builder()
                .url(request_url)
                .header("Cache-Control", "no-cache")
                .header("Connection", "close");

        Response response = httpClient.newCall(request.build()).execute();

        setResponseModifiedData(response);
    }
}

package jp.co.digitalcruise.admint.player.component.netutil;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

import jp.co.digitalcruise.admint.player.component.file.FileUtil;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Downloader extends AbstractNetUtil{

    // キャッシュファイル
    private File mTempFile = null;
    private long mContentLength = 0;

    public static final String TMP_EXTENSION = ".tmp";

//    public class ResponseHandlerBinaryFile implements ResponseHandler<Void> {
//
//        private File mOutputFile = null;
////        private long mHandleDownloaded = 0;
//        private long mHandleContentLength = 0;
//
//        private ResponseHandlerBinaryFile(File output_file){
//            mOutputFile = output_file;
//        }
//
//        @Override
//        public Void handleResponse(HttpResponse response) throws RuntimeException, IOException {
//            int res_code = response.getStatusLine().getStatusCode();
//            switch (res_code) {
//                case HttpStatus.SC_OK:
//                case HttpStatus.SC_PARTIAL_CONTENT:
//
//                    long temp_length = mOutputFile.length();
//                    mHandleContentLength = response.getEntity().getContentLength() + temp_length;
//                    InputStream in = response.getEntity().getContent();
//
//                    try (FileOutputStream fos = new FileOutputStream(mOutputFile, true);
//                         DataInputStream dis = new DataInputStream(in);
//                         DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos))) {
//
//                        byte[] buff = new byte[8192];
//                        int read_byte;
//
//                        while (-1 != (read_byte = dis.read(buff))) {
//                            dos.write(buff, 0, read_byte);
////                            mHandleDownloaded += read_byte;
//                        }
//                    }
//                    break;
//                case HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE:
//                    // DL再開時にファイルが置き換わった可能性
//                    // DL中のテンポラリファイルを削除
//                    String msg = "Server Response : " + res_code;
//
//                    FileUtil.deleteFile(mOutputFile);
//
//                    throw new NetUtilException(NetUtilException.ERROR_CODE_HTTP_RESPONS_ERROR, res_code, msg);
//                default:
//                    throw new NetUtilException(NetUtilException.ERROR_CODE_HTTP_RESPONS_ERROR, res_code, "Server Response : " + res_code);
//            }
//            return null;
//        }
//    }

    public long getCacheLength(){
        if(mTempFile != null && mTempFile.isFile()){
            return mTempFile.length();
        }else{
            return 0;
        }
    }

    public long getContentLength(){
        return mContentLength;
    }

    public static String getCacheFileName(URL target_url){
        return getFilenameFromUrl(target_url) + TMP_EXTENSION;
    }

//    public File download(URL request_url, File out_dir) throws IOException {
//
//        mContentLength = 0;
//
//        File out_file;
//        File temp_file;
//
//        // ファイル名取得
//        String content_name = getFilenameFromUrl(request_url);
//
//        // コンテンツファイル取得
//        out_file = new File(out_dir.getAbsolutePath() + File.separator + content_name);
//
//        // テンポラリファイル取得
//        temp_file = new File(out_dir.getAbsolutePath() + File.separator + content_name + TMP_EXTENSION);
//        mTempFile = temp_file;
//
//        // Download後のリネームファイルまで存在する場合
//        if(out_file.exists()){
//            // Download後解凍中に電源OFFの場合はこの分岐に入る
////            FileUtil.chmodRecursive(out_file);
//            if(temp_file.exists()){
//                FileUtil.deleteFile(temp_file);
//            }
//            return out_file;
//        }
//
//        long temp_length = 0;
//        if(temp_file.exists()){
//            temp_length = temp_file.length();
//        }else{
//            // 存在しない場合は作成
//            FileUtil.createFile(temp_file);
//        }
//
//        Request.Builder request = new Request.Builder()
//                .url(request_url)
//                .header("Accept-Encoding", "")
//                .header("Connection", "close");
//
//        if(temp_length > 0){
//            String prop = String.format (Locale.getDefault(), "bytes=%d-", temp_length);
//            request.header("Range", prop);
//        }
//
//        OkHttpClient okHttpClient = createOkHttpClient(request_url);
//
//        long content_length = 0;
//        Call call = okHttpClient.newCall(request.build());
//        Response response = call.execute();
//
//        try(ResponseBody body = response.body()){
//            int res_code = response.code();
//            content_length = getOkHttpContentLength(res_code, temp_file, body);
//        } catch (Exception e){
//            Logging.stackTrace(e);
//        }
//
//        mContentLength = content_length;
//
//        if(temp_file.exists() && temp_file.length() == content_length){
//            FileUtil.renameFile(temp_file, out_file);
//        }else{
//            // 異常終了の時は例外は発生しているはずなので、正常終了したにも関わらずこのブロックに入ることは通常ないはず
//            String msg = "";
//            if(temp_file.exists() && temp_file.length() > content_length){
//                msg = "too large content-length , temp length=" + temp_file.length() + ", file length=" + content_length;
//                FileUtil.deleteFile(temp_file);
//            }else if(temp_file.exists() && temp_file.length() < content_length){
//                msg = "file download resume downloaded/total:" + temp_file.length() + "/" + content_length;
//            }else if(!temp_file.exists()){
//                msg = "temp_file no exists";
//            }
//            throw new NetUtilException(NetUtilException.ERROR_CODE_DOWNLOAD_FILE_DIFFERENT_ERROR, 0, msg);
//        }
//
//        return out_file;
//    }


    public File downloadWithOkHttp(URL request_url, File out_dir) throws IOException {

        mContentLength = 0;

        File out_file;
        File temp_file;

        // ファイル名取得
        String content_name = getFilenameFromUrl(request_url);

        // コンテンツファイル取得
        out_file = new File(out_dir.getAbsolutePath() + File.separator + content_name);

        // テンポラリファイル取得
        temp_file = new File(out_dir.getAbsolutePath() + File.separator + content_name + TMP_EXTENSION);
        mTempFile = temp_file;

        // Download後のリネームファイルまで存在する場合
        if(out_file.exists()){
            // Download後解凍中に電源OFFの場合はこの分岐に入る
//            FileUtil.chmodRecursive(out_file);
            if(temp_file.exists()){
                FileUtil.deleteFile(temp_file);
            }
            return out_file;
        }

        long temp_length = 0;
        if(temp_file.exists()){
            temp_length = temp_file.length();
        }else{
            // 存在しない場合は作成
            FileUtil.createFile(temp_file);
        }

        Request.Builder request = new Request.Builder()
                .url(request_url)
                .header("Accept-Encoding", "")
                .header("Connection", "close");

        if(temp_length > 0){
            String prop = String.format (Locale.getDefault(), "bytes=%d-", temp_length);
            request.header("Range", prop);
        }

        OkHttpClient okHttpClient = createOkHttpClient(request_url);

        long content_length = 0;
        Call call = okHttpClient.newCall(request.build());
        Response response = call.execute();

        try(ResponseBody body = response.body()){
            int res_code = response.code();
            content_length = getOkHttpContentLength(res_code, temp_file, body);
        } catch (Exception e){
            Logging.stackTrace(e);
        }

        mContentLength = content_length;

        if(temp_file.exists() && temp_file.length() == content_length){
            FileUtil.renameFile(temp_file, out_file);
        }else{
            // 異常終了の時は例外は発生しているはずなので、正常終了したにも関わらずこのブロックに入ることは通常ないはず
            String msg = "";
            if(temp_file.exists() && temp_file.length() > content_length){
                msg = "too large content-length , temp length=" + temp_file.length() + ", file length=" + content_length;
                FileUtil.deleteFile(temp_file);
            }else if(temp_file.exists() && temp_file.length() < content_length){
                msg = "file download resume downloaded/total:" + temp_file.length() + "/" + content_length;
            }else if(!temp_file.exists()){
                msg = "temp_file no exists";
            }
            throw new NetUtilException(NetUtilException.ERROR_CODE_DOWNLOAD_FILE_DIFFERENT_ERROR, 0, msg);
        }

        return out_file;
    }

    private long getOkHttpContentLength(int res_code, File temp_file, ResponseBody response){
        long handleContentLength = 0;
        switch (res_code) {
            case OkHttpStatus.SC_OK:
            case OkHttpStatus.SC_PARTIAL_CONTENT:
                long temp_length = temp_file.length();
                handleContentLength = response.contentLength() + temp_length;
                InputStream in = response.byteStream();

                try (FileOutputStream fos = new FileOutputStream(temp_file, true);
                     DataInputStream dis = new DataInputStream(in);
                     DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos))) {

                    byte[] buff = new byte[8192];
                    int read_byte;

                    while (-1 != (read_byte = dis.read(buff))) {
                        dos.write(buff, 0, read_byte);
//                            mHandleDownloaded += read_byte;
                    }
                } catch (Exception e){
                    Logging.stackTrace(e);
                }
                break;
            case OkHttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE:
                // DL再開時にファイルが置き換わった可能性
                // DL中のテンポラリファイルを削除
                String msg = "Server Response : " + res_code;

                FileUtil.deleteFile(temp_file);

                throw new NetUtilException(NetUtilException.ERROR_CODE_HTTP_RESPONS_ERROR, res_code, msg);
            default:
                throw new NetUtilException(NetUtilException.ERROR_CODE_HTTP_RESPONS_ERROR, res_code, "Server Response : " + res_code);
        }
        return handleContentLength;
    }

    private static String getFilenameFromUrl(URL url){
        String[] p = url.getFile().split("/");
        String s = p[p.length-1];
        if(s.contains("?")){
            return s.substring(0, s.indexOf("?"));
        }
        return s;
    }
}

package jp.co.digitalcruise.admint.player.component.log;

import android.content.Context;
import android.content.Intent;

import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import jp.co.digitalcruise.admint.player.AdmintApplication;
import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.netutil.NetUtilException;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.service.LoggingService;

public class Logging {

    private static void logging(Long thread_id, String msg, int type){

        try{
            Context context = AdmintApplication.getInstance();

            Intent intent = new Intent(context, LoggingService.class);
            intent.setAction(LoggingService.ACTION_LOG);
            intent.putExtra(LoggingService.INTENT_EXTRA_MSG, msg);
            intent.putExtra(LoggingService.INTENT_EXTRA_TIMESTAMP, System.currentTimeMillis());
            intent.putExtra(LoggingService.INTENT_EXTRA_TAG, thread_id.toString());
            intent.putExtra(LoggingService.INTENT_EXTRA_TYPE, type);

            context.startService(intent);
        }catch (Exception e){
            stackTrace(e);
        }
    }

    public static void stackTrace(Exception e){
        try{
            Long thread_id = Thread.currentThread().getId();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace();
            e.printStackTrace(pw);
            pw.flush();
            logging(thread_id, sw.toString(), LoggingService.LOG_TYPE_ERROR);
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public static void info(String msg){
        Long thread_id = Thread.currentThread().getId();
        logging(thread_id, msg, LoggingService.LOG_TYPE_INFO);
    }

    public static void notice(String msg){
        if(DefaultPref.getNoticeLog()) {
            Long thread_id = Thread.currentThread().getId();
            logging(thread_id, msg, LoggingService.LOG_TYPE_NOTICE);
        }
    }

    public static void error(String msg){
        Long thread_id = Thread.currentThread().getId();
        logging(thread_id, msg, LoggingService.LOG_TYPE_ERROR);
    }

    public static void network_error(Exception e){

        String msg;
        if(e instanceof NoHttpResponseException){
            // 主な要因：サーバ側都合による接続断
            msg = AdmintApplication.getInstance().getString(R.string.log_error_http_failed_no_http_response);
        }else if(e instanceof SocketTimeoutException){
            // 主な要因：サーバレスポンス遅延
            msg = AdmintApplication.getInstance().getString(R.string.log_error_http_failed_socket_timeout);
        }else if(e instanceof ConnectTimeoutException){
            // 主な要因：サーバが接続を拒否またはサーバダウン
            msg = AdmintApplication.getInstance().getString(R.string.log_error_http_failed_connect_timeout);
        }else if(e instanceof SSLHandshakeException){
            // 主な要因：証明書エラー（信頼できないとJavaが判断）
            msg = AdmintApplication.getInstance().getString(R.string.log_error_http_failed_ssl_handshake);
        }else if(e instanceof UnknownHostException){
            // 主な要因：名前解決できない
            msg = AdmintApplication.getInstance().getString(R.string.log_error_http_failed_unknown_host);
        }else if(e instanceof SSLException){
            // 主な要因：SSL接続処理エラー（サーバとクライアントで時刻が極端に違う）
            msg = AdmintApplication.getInstance().getString(R.string.log_error_http_failed_ssl);
        } else if(e instanceof NetUtilException){
            msg = AdmintApplication.getInstance().getString(R.string.log_error_net_util) + " : " + e.getMessage();
        } else {
            msg = AdmintApplication.getInstance().getString(R.string.log_error_http_failed_exception);
        }

        Logging.error(msg);
    }

}

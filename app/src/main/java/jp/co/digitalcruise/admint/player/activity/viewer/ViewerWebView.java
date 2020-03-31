package jp.co.digitalcruise.admint.player.activity.viewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import jp.co.digitalcruise.admint.player.AdmintApplication;
import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.object.PlayItem;

public class ViewerWebView extends WebView {

    ViewerWebClient mViewerWebClient = null;

    private PlayItem mPlayItem = null;

    static Messenger self_messenger;

    static Handler web_handler;

    public void load(PlayItem view_item ,Handler handle){
        try {
            loadUrl(view_item.media_path);
            mPlayItem = view_item;
            web_handler = handle;
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    public PlayItem getPlayItem(){
        return mPlayItem;
    }

    public ViewerWebView(Context context) {
        super(context);
        try {
            mViewerWebClient = new ViewerWebClient();
            setWebViewClient(mViewerWebClient);
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    public void setProxy(String host, String user, String passwd) {
        mViewerWebClient.setProxy(host, user, passwd);
    }

    public void resetProxy() {
        mViewerWebClient.resetProxy();
    }

    static class ViewerWebClient extends WebViewClient {
        private String mProxyHost = "";
        private String mProxyUser = "";
        private String mProxyPasswd = "";


        private void setProxy(String host, String user, String passwd) {
            mProxyHost = host;
            mProxyUser = user;
            mProxyPasswd = passwd;
        }

        private void resetProxy() {
            mProxyHost = "";
            mProxyUser = "";
            mProxyPasswd = "";
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
            try {
                Log.e("WEBVIEW", "onReceivedHttpAuthRequest : " + host + " : " + realm);
                if (mProxyHost.length() > 0 && host != null && host.equals(mProxyHost)) {
                    handler.proceed(mProxyUser, mProxyPasswd);
                }
                super.onReceivedHttpAuthRequest(view, handler, host, realm);
            }catch (Exception e){
                Logging.stackTrace(e);
            }
        }

        // ページ読み込み開始時の処理
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
//            AdmintApplication.SKIPPING_ERROR_VIEW = false;
        }

        // ページ読み込み完了時の処理
        @Override
        public void onPageFinished(WebView view, String url) {
        }

        // ページ読み込みエラー時の処理
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String url) {
            Logging.error(AdmintApplication.getInstance().getString(R.string.log_error_webview_load_error) + ", error_code=" + errorCode + ", url=" + url);
            Message viewer_msg = Message.obtain(web_handler, ViewerActivity.HANDLE_MSG_SKIPPING_WEB_VIEW, 0);
            self_messenger = new Messenger(web_handler);
            try {
                self_messenger.send(viewer_msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            try {
                view.loadUrl(url);
                return false;
            }catch (Exception e){
                Logging.stackTrace(e);
                return true;
            }
        }
    }
}
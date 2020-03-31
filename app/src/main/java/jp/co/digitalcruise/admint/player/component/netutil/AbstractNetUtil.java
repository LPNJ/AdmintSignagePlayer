package jp.co.digitalcruise.admint.player.component.netutil;

import android.os.Build;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import jp.co.digitalcruise.admint.player.component.define.DeviceDef;
import jp.co.digitalcruise.admint.player.pref.GroovaProxyPref;
import okhttp3.Authenticator;
import okhttp3.Challenge;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;

public abstract class AbstractNetUtil {
    private final int PROXY_DEFAULT_PORT = 8080;

    private String mProxyHost = null;
    private int mProxyPort = PROXY_DEFAULT_PORT;
    private String mProxyUser = null;
    private String mProxyPassword = null;

    private boolean mUseProxy = false;
    private boolean mUseProxyAuth = false;


//    protected class BasicRetryHandler implements HttpRequestRetryHandler {
//
//        @Override
//        public boolean retryRequest(IOException exception, int executionCount, HttpContext context){
//            final int MAX_RETRY = 3;
//            if (executionCount > MAX_RETRY) {
//                return false;
//            }else if(exception instanceof NoHttpResponseException){
//                // 主な要因：サーバ側都合による接続断
//                return true;
//            }else if(exception instanceof SocketTimeoutException){
//                // 主な要因：サーバレスポンス遅延
//                return true;
//            }else if(exception instanceof ConnectTimeoutException){
//                // 主な要因：サーバが接続を拒否またはサーバダウン
//                return false;
//            }else if(exception instanceof SSLHandshakeException){
//                // 主な要因：証明書エラー（信頼できないとJavaが判断）
//                return false;
//            }else if(exception instanceof UnknownHostException){
//                // 主な要因：名前解決できない
//                return false;
//            }else if(exception instanceof SSLException){
//                // 主な要因：SSL接続処理エラー（サーバとクライアントで時刻が極端に違う）
//                return false;
//            }
//            return false;
//        }
//    }
//
//    private HttpParams makeHttpParam(){
//        HttpParams params = new BasicHttpParams();
//        HttpConnectionParams.setSocketBufferSize(params, 8192);
//        // 接続確立時のタイムアウト値(msec)
//        HttpConnectionParams.setConnectionTimeout(params, 10000);
//        // 接続後 データ取得時のタイムアウト値(msec)
//        HttpConnectionParams.setSoTimeout(params, 10000);
//        HttpProtocolParams.setContentCharset(params, "UTF-8");
//        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
//
//        return params;
//    }
//
//    DefaultHttpClient createHttpClient(){
//
//        DefaultHttpClient httpClient;
//
//        HttpParams params = makeHttpParam();
//        httpClient = new DefaultHttpClient(params);
//
//        httpClient.setHttpRequestRetryHandler(new BasicRetryHandler());
//
//        if(mUseProxy){
//            HttpHost proxy = new HttpHost(mProxyHost, mProxyPort);
//            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
//
//            if(mUseProxyAuth){
//                Credentials credentials = new UsernamePasswordCredentials(mProxyUser, mProxyPassword);
//                AuthScope scope = new AuthScope(mProxyHost, mProxyPort);
//                httpClient.getCredentialsProvider().setCredentials(scope, credentials);
//            }
//        }
//        return httpClient;
//    }

    OkHttpClient createOkHttpClient(URL request_url) {

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        okHttpClientBuilder.connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false);

        if(DeviceDef.isGroova()){
            if(GroovaProxyPref.getGroovaProxyEnable()){
                totoProxySetting();
            }
        }

        //プロキシ利用
        if(mUseProxy){
            InetSocketAddress socketAddress = new InetSocketAddress(mProxyHost, mProxyPort);
            okHttpClientBuilder.proxy(new Proxy(Proxy.Type.HTTP, socketAddress));
            if(mUseProxyAuth){
                Authenticator authenticator = (route, response) -> {
                    String credential = okhttp3.Credentials.basic(mProxyUser,mProxyPassword);

                    //Preemptive Authentication
                    for (Challenge challenge : response.challenges()) {
                        // If this is preemptive auth, use a preemptive credential.
                        if (challenge.scheme().equalsIgnoreCase("OkHttp-Preemptive")) {
                            return response.request().newBuilder()
                                    .header("Proxy-Authorization", credential)
                                    .build();
                        }
                    }
                    return null;
                };
                okHttpClientBuilder.proxyAuthenticator(authenticator);
            }
        }
        //TLSレベル
        if(request_url.getProtocol().equals("https")){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                try{
                    okHttpClientBuilder.connectionSpecs(Collections.singletonList(new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build()))
                            .sslSocketFactory(new TLSSocketFactory(), getTrustManager());
                } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e){
                    throw new RuntimeException(e);
                }
            }
        }
        return okHttpClientBuilder.build();
    }
    private X509TrustManager getTrustManager() throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
        }
        return (X509TrustManager) trustManagers[0];
    }

    public void setProxy(String proxy_host, int proxy_port){
        mUseProxy = true;

        if(proxy_host != null){
            mProxyHost = proxy_host;
        }else{
            mProxyHost = "";
        }

        if(proxy_port >= 0 && proxy_port < 65535){
            mProxyPort = proxy_port;
        }
    }

    public void setProxyAuth(String proxy_user, String proxy_password){
        mUseProxyAuth = true;

        if(proxy_user != null){
            mProxyUser = proxy_user;
        }else{
            mProxyUser = "";
        }

        if(proxy_password != null){
            mProxyPassword = proxy_password;
        }else{
            mProxyPassword = "";
        }
    }

    private void totoProxySetting(){
        mUseProxy = GroovaProxyPref.getGroovaProxyEnable();
        mProxyHost = GroovaProxyPref.getGroovaProxyHost();
        mProxyPort = Integer.parseInt(GroovaProxyPref.getGroovaProxyPort());
        mProxyUser = GroovaProxyPref.getGroovaProxyUser();
        mProxyPassword = GroovaProxyPref.getGroovaProxyPassword();
        if(mProxyUser.length() > 0){
            mUseProxyAuth = true;
        }
    }

//    public void setMaxRetry(int val){
//        if(val >= 0 && val < 10){
//            mMaxRetry = val;
//        }
//    }
}
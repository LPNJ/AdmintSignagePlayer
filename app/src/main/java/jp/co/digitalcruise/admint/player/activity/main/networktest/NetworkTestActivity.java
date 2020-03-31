package jp.co.digitalcruise.admint.player.activity.main.networktest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.define.DeviceDef;
import jp.co.digitalcruise.admint.player.component.file.AdmintPath;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.pref.NetWorkTestPref;
import jp.co.grv.toto.tvp.ReadyListener;
import jp.co.grv.toto.tvp.api.TVPlatform;
import jp.co.grv.toto.tvp.api.TVPlatformActivity;
import jp.co.grv.toto.tvp.api.TVPropertyManager;


@SuppressLint("Registered")
public class NetworkTestActivity extends TVPlatformActivity implements ReadyListener{


    private ExecuteNetworkTest ping2gateway = null;

    private ExecuteNetworkTest ping2proxy = null;

    private ExecuteNetworkTest execHealthCheck = null;

    private TextView ping2gatewayView = null;

    private TextView ping2proxyView = null;

    private TextView requestScheduleView = null;

    ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);

    private String gateway = "";

    boolean useProxy = false;

    private String proxyHost = "";
    private String proxyPort = "";
    private String proxyUser = "";
    private String proxyPassword = "";


    int networkType = -1;

    private Button execution;


    private TextView titleView = null;

    private TableLayout terminalInfoView = null;

    private LinearLayout layout = null;

    private String split_word = "split_word";

    //pingを送るときに使う
    class AsyncTaskArgument {
        Context context;
        String target;
        int type;

        AsyncTaskArgument(Context context, int type, String target) {
            this.context = context;
            this.target = target;
            this.type = type;
        }
    }

    ArrayList<String> netWorkTestData = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.network_test);
        titleView =  findViewById(R.id.execution_time);
        terminalInfoView =  findViewById(R.id.terminal_info);
        layout =  findViewById(R.id.result);

        execution =  findViewById(R.id.execution_test);
        execution.setOnClickListener(v -> execution());

    }

    @Override
    protected void onResume() {
        try{
            super.onResume();
            if(Build.MODEL.equals(DeviceDef.SHARP_BOX) || Build.MODEL.equals(DeviceDef.PN_M_SERIES) || Build.MODEL.equals(DeviceDef.PN_B_SERIES)){
                getWindow().getDecorView().setSystemUiVisibility(0x00000008);
            }
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }catch (Exception e){
            Logging.stackTrace(e);
        }

        //ネットワークテスト実行後放置し、Viewerが立ち上がった時に
        //実行結果と前回結果が重複して表示される対処
        terminalInfoView.removeAllViews();
        layout.removeAllViews();

        showLatestResult();

    }


    @Override
    public void onReady() {
        super.onReady();
        try{
            TVPropertyManager propm = TVPlatform.getManager().getPropertyManager();
            String enabled = propm.getProperty("net.http_proxy.enable");
            String TOTO_PROXY_TRUE = "1";
            if(TOTO_PROXY_TRUE.equals(enabled)){
                useProxy = true;
                proxyHost = propm.getProperty("net.http_proxy.host");
                proxyPort = propm.getProperty("net.http_proxy.port");
                proxyUser = propm.getProperty("net.http_proxy.username");
                proxyPassword = propm.getProperty("net.http_proxy.password");
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ping2gateway != null) {
            ping2gateway.cancel(true);
            ping2gateway = null;
        }
        if (ping2proxy != null) {
            ping2proxy.cancel(true);
            ping2proxy = null;
        }
        if (execHealthCheck != null) {
            execHealthCheck.cancel(true);
            execHealthCheck = null;
        }

    }


    /**
     * 直近のネットワークテストの結果を表示する。
     * ネットワークテストの結果とは
     * DefaultGateWay、プロキシサーバへのpingと
     * ヘルスチェックの結果の事。
     */
    private void showLatestResult(){
        try{
            LinearLayout result = findViewById(R.id.result);
            String latestExecutionTime = NetWorkTestPref.getLatestCheckTime();
            String title = getString(R.string.last_run_date_and_time) + ": " + latestExecutionTime;
            File latestTestResultFile = new File(AdmintPath.getReportDir().getAbsolutePath() + "/resultNetWorkTest.txt");

            boolean pingLog = false;

            if(!latestExecutionTime.equals("") && latestTestResultFile.exists()){
                titleView.setText(title);
                    BufferedReader showLatest = new BufferedReader(new FileReader(latestTestResultFile));
                    String line;
                    while((line = showLatest.readLine()) != null){
                        if(!line.contains(this.getString(R.string.last_run_date_and_time))){

                            if(line.contains("ping")){
                                pingLog = true;
                            }

                            String[] splitStr = line.split(" \t");

                            TableRow row = new TableRow(this);
                            TextView textView = new TextView(this);
                            textView.setPadding(0, 0, 20, 0);
                            TextView valueView = new TextView(this);
                            textView.setText(splitStr[0]);

                            if (splitStr.length > 1) {
                                valueView.setText(splitStr[1]);
                            }

                            if(line.contains(this.getString(R.string.ping_failed)) || line.contains(this.getString(R.string.could_not_get))
                                    || line.contains(this.getString(R.string.net_service_off)) || line.contains(this.getString(R.string.non_target_ip))
                                    || line.contains("Exception") || line.contains("unreachable")){
                                textView.setTextColor(Color.YELLOW);
                            }

                                row.addView(textView);
                                row.addView(valueView);
                            if(!pingLog){
                                terminalInfoView.addView(row, param);
                            } else {
                                result.addView(row, param);
                            }



                        }
                    }
            }
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    /*
     * ネットワークテストの実行から結果の表示まで
     */
    @SuppressLint("ApplySharedPref")
    private void execution(){
        try {
            terminalInfoView.removeAllViews();
            layout.removeAllViews();
            netWorkTestData.clear();

            execution.setEnabled(false);

            boolean netservice = DefaultPref.getNetworkService();

            SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
            String currentTime = timeFormat.format(System.currentTimeMillis());
            NetWorkTestPref.setLatestCheckTime(currentTime);
            String title = getString(R.string.last_run_date_and_time) + ": " + currentTime;
            titleView.setText(title);

            setTerminalInfo();

            ping2gatewayView = null;
            ping2proxyView = null;
            requestScheduleView = null;

            //ネットサービスがオンならpingを送る
            if(netservice){
                ping2gatewayView = new TextView(this);
                ping2gatewayView.setText(R.string.execute_ping_command_to_default_gateway);
                layout.addView(ping2gatewayView, param);
                ping2gateway();

                if(DefaultPref.getProxyEnable() || useProxy){
                    ping2proxyView = new TextView(NetworkTestActivity.this);
                    ping2proxyView.setText(R.string.execute_ping_command_to_proxy_server);
                    layout.addView(ping2proxyView, param);
                    ping2Proxy();
                }

                requestScheduleView = new TextView(NetworkTestActivity.this);
                requestScheduleView.setText(R.string.execute_health_check);
                Logging.info(getString(R.string.log_info_network_test_health_check));
                layout.addView(requestScheduleView, param);
                execHealthCheck();

            } else {
                ping2gatewayView = new TextView(this);
                ping2gatewayView.setText(R.string.net_service_off);
                layout.addView(ping2gatewayView, param);
                netWorkTestData.add(ping2gatewayView.getText()+"\n");
            }



        } catch (Exception e) {
            Logging.stackTrace(e);
        }
    }


    /*
      デフォルトゲートウェイへpingを送信する
     */
    private void ping2gateway(){
        if (ping2gateway != null) {
            ping2gateway.cancel(true);
            ping2gateway = null;
        }

        ping2gateway = new ExecuteNetworkTest();

        ping2gateway.setOnCallBack(new ExecuteNetworkTest.CallBack() {
            @Override
            void callBack(ExecuteNetworkTest.NetworkTestResult testResult) {
                super.callBack(testResult);

                try {
                    String result;
                    TextView textView = new TextView(NetworkTestActivity.this);
                    if (testResult.isResult) {
                        result = getString(R.string.execute_ping_command_to_default_gateway) + "  " + getString(R.string.ping_success);
                        ping2gatewayView.setText(result);

                    } else {
                        textView.setTextColor(Color.YELLOW);
                        ping2gatewayView.setTextColor(Color.YELLOW);
                        result = getString(R.string.execute_ping_command_to_default_gateway) + "  " + getString(R.string.ping_failed);
                        ping2gatewayView.setText(result);
                    }
                    textView.setPadding(20, 5, 0, 20);
                    textView.setText(testResult.message);
                    layout.addView(textView, 1,  param);
                    netWorkTestData.add(" \n");
                    netWorkTestData.add(result + "\n");
                    netWorkTestData.add(testResult.message + "\n");
                } catch (Exception e) {
                    Logging.stackTrace(e);
                }
            }

            @Override
            void progressUpdateCallback(Void[] a) {
                super.progressUpdateCallback(a);
                runOnUiThread(() -> ping2gatewayView.append("  " + getString(R.string.ping_executing) + "..."));
            }
        });
        AsyncTaskArgument params = new AsyncTaskArgument(this, ExecuteNetworkTest.TYPE_PING_GATEWAY, gateway);
        ping2gateway.execute(params);
    }

    /*
     * プロキシサーバへpingを送信する
     */
    private void ping2Proxy(){
        if (ping2proxy != null) {
            ping2proxy.cancel(true);
            ping2proxy = null;
        }

        ping2proxy = new ExecuteNetworkTest();

        ping2proxy.setOnCallBack(new ExecuteNetworkTest.CallBack() {
            @Override
            void callBack(ExecuteNetworkTest.NetworkTestResult testResult) {
                super.callBack(testResult);

                try {
                    String result;
                    TextView textView = new TextView(NetworkTestActivity.this);
                    if (testResult.isResult) {
                        result = getString(R.string.execute_ping_command_to_proxy_server) + "  " + getString(R.string.ping_success);
                        ping2proxyView.setText(result);
                    } else {
                        textView.setTextColor(Color.YELLOW);
                        ping2proxyView.setTextColor(Color.YELLOW);
                        result = getString(R.string.execute_ping_command_to_proxy_server) + "  " + getString(R.string.ping_failed);
                        ping2proxyView.setText(result);
                    }
                    textView.setPadding(20, 5, 0, 20);
                    textView.setText(testResult.message);
                    layout.addView(textView, 3,  param);
                    netWorkTestData.add("\n");
                    netWorkTestData.add(result + "\n");
                    netWorkTestData.add(testResult.message + "\n");
                } catch (Exception e) {
                    Logging.stackTrace(e);
                }
            }

            @Override
            void progressUpdateCallback(Void[] a) {
                super.progressUpdateCallback(a);
                runOnUiThread(() -> ping2proxyView.append("  " + getString(R.string.ping_executing) + "..."));
            }
        });
        AsyncTaskArgument params = new AsyncTaskArgument(this, ExecuteNetworkTest.TYPE_PING_PROXY, proxyHost);
        ping2proxy.execute(params);
    }

    /*
     * ヘルスチェックの実行
     */
    private void execHealthCheck(){
        if (execHealthCheck != null) {
            execHealthCheck.cancel(true);
            execHealthCheck = null;
        }

        execHealthCheck = new ExecuteNetworkTest();

        execHealthCheck.setOnCallBack(new ExecuteNetworkTest.CallBack() {
            @Override
            void callBack(ExecuteNetworkTest.NetworkTestResult testResult) {
                super.callBack(testResult);

                try {
                    String result;
                    TextView textView = new TextView(NetworkTestActivity.this);
                    if (testResult.isResult) {
                        result = getString(R.string.execute_health_check) + "  " + getString(R.string.ping_success);
                        Logging.info(getString(R.string.log_info_success_health_check));
                        requestScheduleView.setText(result);
                    } else {
                        textView.setTextColor(Color.YELLOW);
                        requestScheduleView.setTextColor(Color.YELLOW);
                        result = getString(R.string.execute_health_check) + "  " + getString(R.string.ping_failed);
                        Logging.info(getString(R.string.log_error_failed_health_check));
                        requestScheduleView.setText(result);
                    }
                    textView.setPadding(20, 5, 0, 20);
                    textView.setText(testResult.message);
                    layout.addView(textView, param);
                    netWorkTestData.add("\n");
                    netWorkTestData.add(result + "\n");
                    netWorkTestData.add(testResult.message);
                    makeResultNetworkTestFile(netWorkTestData);
                } catch (Exception e) {
                    Logging.stackTrace(e);
                }
                execution.setEnabled(true);
            }

            @Override
            void progressUpdateCallback(Void[] a) {
                super.progressUpdateCallback(a);
                runOnUiThread(() -> requestScheduleView.append("  " + getString(R.string.ping_executing) + "..."));
            }
        });
        AsyncTaskArgument params = new AsyncTaskArgument(this, ExecuteNetworkTest.TYPE_HEALTH_CHECK, "");
        execHealthCheck.execute(params);
    }

    /**
     * 端末情報の取得と表示
     */
    @SuppressLint("ApplySharedPref")
    private void setTerminalInfo(){
        try {
            //プリファレンス値取得
            if (!DeviceDef.isGroova()) {
                useProxy = DefaultPref.getProxyEnable();
                proxyHost = DefaultPref.getProxyHost();
                proxyPort = DefaultPref.getProxyPort();
                proxyUser = DefaultPref.getProxyUser();
                proxyPassword = DefaultPref.getProxyPassword();
            }

            TableRow row1 = new TableRow(this);
            TextView columnName1 = new TextView(this);
            columnName1.setPadding(0, 0, 10, 0);
            TextView columnValue1 = new TextView(this);
            columnName1.setText(R.string.terminal_id);
            String terminalId = DefaultPref.getTerminalId();
            columnValue1.setText(terminalId);
            row1.addView(columnName1);
            row1.addView(columnValue1);
            terminalInfoView.addView(row1, param);
            netWorkTestData.add(columnName1.getText() + split_word + columnValue1.getText());
            netWorkTestData.add("\n");

            TableRow extraRow = new TableRow(this);
            TextView extraName = new TextView(this);
            extraName.setPadding(0,0,10,0);
            TextView extraValue = new TextView(this);
            extraName.setText(R.string.extra_id);
            String extraid = DefaultPref.getExtraId();
            extraValue.setText(extraid);
            extraRow.addView(extraName);
            extraRow.addView(extraValue);
            terminalInfoView.addView(extraRow, param);
            netWorkTestData.add(extraName.getText() + split_word + extraValue.getText());
            netWorkTestData.add("\n");

            TableRow stbRow = new TableRow(this);
            TextView stbName = new TextView(this);
            stbName.setPadding(0,0,10,0);
            TextView stbValue = new TextView(this);
            stbName.setText(R.string.stb_id);
            String stb_id = DefaultPref.getStbId();
            stbValue.setText(stb_id);
            stbRow.addView(stbName);
            stbRow.addView(stbValue);
            terminalInfoView.addView(stbRow, param);
            netWorkTestData.add(stbName.getText() + split_word + stbValue.getText());
            netWorkTestData.add("\n");


            //ネットワーク種別の取得
            if (DeviceDef.isGroova()) {
                networkType = NetworkTypeIsEthernet(this);
            } else if (Build.MODEL.equals(DeviceDef.HWLD_XM6502) && isEthXm6502()) {
                networkType = ConnectivityManager.TYPE_ETHERNET;
            } else {
                networkType = getNetworkType(this);
            }

            TableRow row2 = new TableRow(this);
            TextView columnName2 = new TextView(this);
            columnName2.setPadding(0, 0, 10, 0);
            TextView columnValue2 = new TextView(this);
            columnName2.setText(R.string.network_type);
            String networkTypeName;
            if (networkType == ConnectivityManager.TYPE_WIFI) {
                networkTypeName = getString(R.string.wifi);
            } else if (networkType == ConnectivityManager.TYPE_ETHERNET) {
                networkTypeName = getString(R.string.ethernet);
            } else if (networkType == ConnectivityManager.TYPE_MOBILE) {
                networkTypeName = getString(R.string.mobile);
            } else {
                networkTypeName = "Other";
            }
            columnValue2.setText(networkTypeName);
            row2.addView(columnName2);
            row2.addView(columnValue2);
            terminalInfoView.addView(row2, param);
            netWorkTestData.add(columnName2.getText() + split_word + columnValue2.getText());
            netWorkTestData.add("\n");

            if (networkType == ConnectivityManager.TYPE_WIFI) {
                terminalInfoView = setWifiInfo();
            } else if (networkType == ConnectivityManager.TYPE_ETHERNET) {
                terminalInfoView = setEthernetInfo();
            }

            //Proxy use check
            TableRow proxyUse = new TableRow(this);
            TextView columnProxyUse = new TextView(this);
            columnProxyUse.setPadding(0, 0, 10, 0);
            TextView valueProxyUse = new TextView(this);
            columnProxyUse.setText(R.string.proxy);
            if (useProxy) {
                valueProxyUse.setText(R.string.proxy_use);
            } else {
                valueProxyUse.setText(R.string.proxy_unuse);
            }
            proxyUse.addView(columnProxyUse);
            proxyUse.addView(valueProxyUse);
            terminalInfoView.addView(proxyUse, param);
            netWorkTestData.add(columnProxyUse.getText() + split_word + valueProxyUse.getText());
            netWorkTestData.add("\n");



            if (useProxy) {
                TableRow proxyHostRow = new TableRow(this);
                TextView columnProxyHost = new TextView(this);
                columnProxyHost.setPadding(0, 0, 10, 0);
                TextView valueProxyHost = new TextView(this);
                columnProxyHost.setText(R.string.proxy_host);
                valueProxyHost.setText(proxyHost);
                proxyHostRow.addView(columnProxyHost);
                proxyHostRow.addView(valueProxyHost);
                terminalInfoView.addView(proxyHostRow, param);
                netWorkTestData.add(columnProxyHost.getText() + split_word + valueProxyHost.getText());
                netWorkTestData.add("\n");

                TableRow proxyPortRow = new TableRow(this);
                TextView columnProxyPort = new TextView(this);
                columnProxyPort.setPadding(0, 0, 10, 0);
                TextView valueProxyPort = new TextView(this);
                columnProxyPort.setText(R.string.proxy_port);
                valueProxyPort.setText(proxyPort);
                proxyPortRow.addView(columnProxyPort);
                proxyPortRow.addView(valueProxyPort);
                terminalInfoView.addView(proxyPortRow, param);
                netWorkTestData.add(columnProxyPort.getText() + split_word + valueProxyPort.getText());
                netWorkTestData.add("\n");

                TableRow proxyUserRow = new TableRow(this);
                TextView columnProxyUser = new TextView(this);
                columnProxyUser.setPadding(0, 0, 10, 0);
                TextView valueProxyUser = new TextView(this);
                columnProxyUser.setText(R.string.proxy_user_name);
                if (proxyUser == null || proxyUser.equals("") || proxyUser.equals("N/A")) {
                    if (proxyUser == null || proxyUser.equals("")){proxyUser = "N/A";}
                    valueProxyUser.setText("N/A");
                } else {
                    valueProxyUser.setText(proxyUser);
                }
                proxyUserRow.addView(columnProxyUser);
                proxyUserRow.addView(valueProxyUser);
                terminalInfoView.addView(proxyUserRow, param);
                netWorkTestData.add(columnProxyUser.getText() + split_word + valueProxyUser.getText());
                netWorkTestData.add("\n");

                TableRow proxyPasswordRow = new TableRow(this);
                TextView columnProxyPassword = new TextView(this);
                columnProxyPassword.setPadding(0, 0, 10, 0);
                TextView valueProxyPassword = new TextView(this);
                columnProxyPassword.setText(R.string.proxy_password);
                if (proxyPassword == null || proxyPassword.equals("") || proxyPassword.equals("N/A")) {
                    valueProxyPassword.setText("N/A");
                } else {
                    StringBuilder passwordMask = new StringBuilder();
                    for (int i = 0; i < proxyPassword.length(); i++) {
                        passwordMask.append("*");
                    }
                    valueProxyPassword.setText(passwordMask.toString());
                }
                proxyPasswordRow.addView(columnProxyPassword);
                proxyPasswordRow.addView(valueProxyPassword);
                terminalInfoView.addView(proxyPasswordRow, param);
                netWorkTestData.add(columnProxyPassword.getText() + split_word + valueProxyPassword.getText());
                netWorkTestData.add("\n");

            }


        } catch (Exception e) {
            Logging.stackTrace(e);
        }
    }

    /**
     * 使用している端末がHoneywld_XM6502だった場合に呼ばれる
     * Linuxのnetcfgコマンドを使い、Ethernetで繋がっているのかを調べる
     */
    private boolean isEthXm6502(){
        boolean re = false;
        String netcfgResult = executeCommand("netcfg");

        String[] results = netcfgResult.split("\n");

        for (String result : results) {
            if (result.startsWith("eth0") && result.contains("UP")) {
                re = true;
                break;
            }
        }

        return re;
    }

    /**
     * 送られてきたコマンドを実行する。
     * isEthXm6502とEthernet利用時のデフォルトゲートウェイ取得用
     */
    private String executeCommand(String command){
        StringBuilder resultBuilder = new StringBuilder();

        try {
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                resultBuilder.append(line);
                resultBuilder.append("\n");
            }
            reader.close();

            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                resultBuilder.append(line);
                resultBuilder.append("\n");
            }
            errorReader.close();
            process.waitFor();
        } catch (Exception e) {
            resultBuilder.append(getStackTraceString(e));
            resultBuilder.append("\n");
        }

        return resultBuilder.toString();
    }

    /**
     * 送られてきたコマンドを実行する。
     * DNS1,DNS2,Subnetのアドレスの取得に使用している
     */
    private String executeCommandSingleLine(String command){
        StringBuilder resultBuilder = new StringBuilder();

        try {
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                resultBuilder.append(line);
            }
            reader.close();

            process.waitFor();
        } catch (Exception e) {
            resultBuilder.append(getStackTraceString(e));
        }

        return resultBuilder.toString();
    }

    //Exceptionが出た時に表示する
    private String getStackTraceString(Exception e){
        String retString = "";

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(new StringWriter());
        e.printStackTrace(pw);
        pw.flush();

        retString = sw.toString();

        if(retString.equals("")){
            retString = e.getClass().getName() + " " + e.getMessage();
        }

        return retString;
    }

    /**
     * Wi-Fiで運用している時の端末情報の取得と表示
     */
    private TableLayout setWifiInfo(){
        WifiInfo wifiInfo = getWifiState(this);
        DhcpInfo dhcpInfo;
        if (wifiInfo != null) {
            dhcpInfo = getDhcpInfo(this);
        } else {
            dhcpInfo = null;
        }
        WifiConfiguration wifiConfiguration;
        if (wifiInfo != null && dhcpInfo == null) {
            wifiConfiguration = getWifiConfiguration(this, wifiInfo.getSSID());
        } else {
            wifiConfiguration = null;
        }

        String ssid;
        TableRow row3 = new TableRow(this);
        TextView columnName3 = new TextView(this);
        columnName3.setPadding(0, 0, 20, 0);
        TextView columnValue3 = new TextView(this);
        columnName3.setText(R.string.ssid);
        if (wifiInfo != null && wifiInfo.getSSID() != null) {
            ssid = wifiInfo.getSSID();
        } else {
            ssid = "";
        }
        if (ssid != null && !ssid.equals("")) {
            columnValue3.setText(ssid);
            row3.addView(columnName3);
            row3.addView(columnValue3);
            terminalInfoView.addView(row3, param);
            netWorkTestData.add(columnName3.getText() + split_word + columnValue3.getText());
            netWorkTestData.add("\n");


            TableRow row4 = new TableRow(this);
            TextView columnName4 = new TextView(this);
            columnName4.setPadding(0, 0, 20, 0);
            TextView columnValue4 = new TextView(this);
            columnName4.setText(R.string.ip_address);
            String ipAddress;
            ipAddress = formatIp
                    (wifiInfo.getIpAddress());
            columnValue4.setText(ipAddress);
            row4.addView(columnName4);
            row4.addView(columnValue4);
            terminalInfoView.addView(row4, param);
            netWorkTestData.add(columnName4.getText() + split_word + columnValue4.getText());
            netWorkTestData.add("\n");


            TableRow row5 = new TableRow(this);
            TextView columnName5 = new TextView(this);
            columnName5.setPadding(0, 0, 20, 0);
            TextView columnValue5 = new TextView(this);
            columnName5.setText(R.string.wifi_rssi);
            String rssi;
            rssi = String.valueOf(wifiInfo.getRssi());
            columnValue5.setText(rssi);
            row5.addView(columnName5);
            row5.addView(columnValue5);
            terminalInfoView.addView(row5, param);
            netWorkTestData.add(columnName5.getText() + split_word + columnValue5.getText());
            netWorkTestData.add("\n");


            TableRow row6 = new TableRow(this);
            TextView columnName6 = new TextView(this);
            columnName6.setPadding(0, 0, 20, 0);
            TextView columnValue6 = new TextView(this);
            columnName6.setText(R.string.default_gateway);
            String defaultGateway;
            if (dhcpInfo != null) {
                defaultGateway = formatIp(dhcpInfo.gateway);
            } else {
                defaultGateway = getGateway(wifiConfiguration);
            }
            columnValue6.setText(defaultGateway);
            gateway = defaultGateway;
            row6.addView(columnName6);
            row6.addView(columnValue6);
            terminalInfoView.addView(row6, param);
            netWorkTestData.add(columnName6.getText() + split_word + columnValue6.getText());
            netWorkTestData.add("\n");


            TableRow row7 = new TableRow(this);
            TextView columnName7 = new TextView(this);
            columnName7.setPadding(0, 0, 20, 0);
            TextView columnValue7 = new TextView(this);
            columnName7.setText(R.string.subnet_mask);
            String subnetMask = "";
            if(dhcpInfo != null){
                subnetMask = formatIp(dhcpInfo.netmask);
            }
            if(subnetMask.equals("") || subnetMask.equals("0.0.0.0")){
                subnetMask = getSubnetMask(ipAddress);
                if(subnetMask.equals("") || subnetMask.equals("0.0.0.0")){
                    columnValue7.setText(getString(R.string.could_not_get));
                    row7.addView(columnName7);
                    row7.addView(columnValue7);
                    terminalInfoView.addView(row7, param);
                } else {
                    columnValue7.setText(subnetMask);
                    row7.addView(columnName7);
                    row7.addView(columnValue7);
                    terminalInfoView.addView(row7, param);
                }
            } else {
                columnValue7.setText(subnetMask);
                row7.addView(columnName7);
                row7.addView(columnValue7);
                terminalInfoView.addView(row7, param);
            }
            netWorkTestData.add(columnName7.getText() + split_word + columnValue7.getText());
            netWorkTestData.add("\n");



            TableRow row8 = new TableRow(this);
            TextView columnName8 = new TextView(this);
            columnName8.setPadding(0, 0, 20, 0);
            TextView columnValue8 = new TextView(this);
            columnName8.setText(R.string.dns1);
            String dns1;
            if (dhcpInfo != null) {
                dns1 = formatIp(dhcpInfo.dns1);
            } else {
                dns1 = getDNS(wifiConfiguration);
            }
            columnValue8.setText(dns1);
            row8.addView(columnName8);
            row8.addView(columnValue8);
            terminalInfoView.addView(row8, param);
            netWorkTestData.add(columnName8.getText() + split_word + columnValue8.getText());
            netWorkTestData.add("\n");


//            if(!DeviceDef.isGroova()){
            if(Build.MODEL.equals(DeviceDef.SHARP_BOX) || Build.MODEL.equals(DeviceDef.SK_BOX) ||Build.MODEL.equals(DeviceDef.PN_M_SERIES) || Build.MODEL.equals(DeviceDef.PN_B_SERIES)){
                String dns2;
                if(dhcpInfo != null){
                    dns2 = formatIp(dhcpInfo.dns2);
                } else {
                    dns2 = executeCommand("getprop net.dns2");
                    dns2 = dns2.replaceAll("\n", "");
                }
                if(!dns2.equals("0.0.0.0") && !dns2.equals("")) {
                    TableRow row9 = new TableRow(this);
                    TextView columnName9 = new TextView(this);
                    columnName9.setPadding(0, 0, 20, 0);
                    TextView columnValue9 = new TextView(this);
                    columnName9.setText(R.string.dns2);
                    columnValue9.setText(dns2);
                    row9.addView(columnName9);
                    row9.addView(columnValue9);
                    terminalInfoView.addView(row9, param);
                    netWorkTestData.add(columnName9.getText() + split_word + columnValue9.getText());
                    netWorkTestData.add("\n");
                }
            }

        } else {
            columnValue3.setText(getString(R.string.could_not_get));
            row3.addView(columnName3);
            row3.addView(columnValue3);
            terminalInfoView.addView(row3, param);
            netWorkTestData.add(columnName3.getText() + split_word + columnValue3.getText());
            netWorkTestData.add("\n");

        }
        return terminalInfoView;
    }

    /**
     * Ethrenetで運用している時の端末情報の取得と表示
     */
    private TableLayout setEthernetInfo(){

        //IpAddress
        String ipAddress =getEthernetIpAddress();
        if(ipAddress.equals("") || ipAddress.equals("0.0.0.0")){
            ipAddress = "N/A";
        }

        //DefaultGateWay
        String ethGateway = getEthGateWay();
        if (!ethGateway.equals("0.0.0.0") && !ethGateway.equals("")) {
            gateway = ethGateway;
        } else {
            String gatewayCmd = "getprop dhcp.eth0.gateway";
            gateway =executeCommandSingleLine(gatewayCmd);
            if(gateway.equals("0.0.0.0") || gateway.equals("")){
                gateway = "N/A";
            }
        }

        //DNS1
        String dnsCmd = "getprop net.dns1";
        String dns1 = executeCommandSingleLine(dnsCmd);

        if (dns1.equals("0.0.0.0") || dns1.equals("")) {
            dns1 = "N/A";
        }

        //DNS2
        String dnsCmd2;
        String dns2= "";

        if(!DeviceDef.isGroova()){
            dnsCmd2 = "getprop dhcp.eth0.dns2";
            dns2 = executeCommandSingleLine(dnsCmd2);
            if(dns2.equals("0.0.0.0")){
                dnsCmd2 = "getprop net.dns2";
                dns2 = executeCommandSingleLine(dnsCmd2);
            }
        }
        if (dns2.equals("0.0.0.0") || dns2.equals("")) {
            dns2 = "N/A";
        }

        //MASK取得
        String mask = getSubnetMask(ipAddress);
        if (mask.equals("0.0.0.0") || mask.equals("")) {
            mask = "N/A";
        }


        //画面表示
        TableRow row4 = new TableRow(this);
        TextView columnName4 = new TextView(this);
        columnName4.setPadding(0, 0, 20, 0);
        TextView columnValue4 = new TextView(this);
        columnName4.setText(R.string.ip_address);
        columnValue4.setText(ipAddress);
        row4.addView(columnName4);
        row4.addView(columnValue4);
        terminalInfoView.addView(row4, param);
        netWorkTestData.add(columnName4.getText() + split_word + columnValue4.getText());
        netWorkTestData.add("\n");


        TableRow row5 = new TableRow(this);
        TextView columnName5 = new TextView(this);
        columnName5.setPadding(0, 0, 20, 0);
        TextView columnValue5 = new TextView(this);
        columnName5.setText(R.string.default_gateway);
        columnValue5.setText(gateway);
        row5.addView(columnName5);
        row5.addView(columnValue5);
        terminalInfoView.addView(row5, param);
        netWorkTestData.add(columnName5.getText() + split_word + columnValue5.getText());
        netWorkTestData.add("\n");


        if(!mask.equals("0.0.0.0") && !mask.equals("")) {
            TableRow row6 = new TableRow(this);
            TextView columnName6 = new TextView(this);
            columnName6.setPadding(0, 0, 20, 0);
            TextView columnValue6 = new TextView(this);
            columnName6.setText(R.string.subnet_mask);
            columnValue6.setText(mask);
            row6.addView(columnName6);
            row6.addView(columnValue6);
            terminalInfoView.addView(row6, param);
            netWorkTestData.add(columnName6.getText() + split_word + columnValue6.getText());
            netWorkTestData.add("\n");

        }

        TableRow row7 = new TableRow(this);
        TextView columnName7 = new TextView(this);
        columnName7.setPadding(0, 0, 20, 0);
        TextView columnValue7 = new TextView(this);
        columnName7.setText(R.string.dns1);
        columnValue7.setText(dns1);
        row7.addView(columnName7);
        row7.addView(columnValue7);
        terminalInfoView.addView(row7, param);
        netWorkTestData.add(columnName7.getText() + split_word + columnValue7.getText());
        netWorkTestData.add("\n");


//        if(!DeviceDef.isGroova()){
        if(Build.MODEL.equals(DeviceDef.SHARP_BOX) || Build.MODEL.equals(DeviceDef.SK_BOX) || Build.MODEL.equals(DeviceDef.PN_M_SERIES) || Build.MODEL.equals(DeviceDef.PN_B_SERIES)){
            if(!dns2.equals("N/A")) {
                TableRow row8 = new TableRow(this);
                TextView columnName8 = new TextView(this);
                columnName8.setPadding(0, 0, 20, 0);
                TextView columnValue8 = new TextView(this);
                columnName8.setText(R.string.dns2);
                columnValue8.setText(dns2);
                row8.addView(columnName8);
                row8.addView(columnValue8);
                terminalInfoView.addView(row8, param);
                netWorkTestData.add(columnName8.getText() + split_word + columnValue8.getText());
                netWorkTestData.add("\n");
            }
        }

        return terminalInfoView;
    }

    //16進数のIPアドレスを10進数、かつ４桁区切りに変換する
    private String formatIp(int ipAddress){
        return ((ipAddress) & 0xFF) + "." + ((ipAddress >> 8) & 0xFF) + "." + ((ipAddress >> 16) & 0xFF) + "." + ((ipAddress >> 24) & 0xFF);

    }

    /**
     * Ethernetでのデフォルトゲートウェイの取得
     */
    private String getEthGateWay(){
        String catResult = executeCommand("cat /proc/net/route");

        String [] results = catResult.split("\n");

        String gatewayServer = "0.0.0.0";

        for(String result : results){
            String[] details = result.split("\t");
            if(details.length > 2 && details[0].equals("eth0") && details[1].equals("00000000")){
                gatewayServer = getIpAddressStr(details[2]);
            }
        }

        return gatewayServer;
    }

    private String getIpAddressStr(String ipAddressConnectedStr){
        String retString;
        try {
            String addressStr1 = ipAddressConnectedStr.substring(6, 8);
            String addressStr2 = ipAddressConnectedStr.substring(4, 6);
            String addressStr3 = ipAddressConnectedStr.substring(2, 4);
            String addressStr4 = ipAddressConnectedStr.substring(0, 2);

            int address1 = Integer.parseInt(addressStr1, 16);
            int address2 = Integer.parseInt(addressStr2, 16);
            int address3 = Integer.parseInt(addressStr3, 16);
            int address4 = Integer.parseInt(addressStr4, 16);

            retString = address1 + "." + address2 + "." + address3 + "." + address4;
        } catch (Exception e) {
            retString = "0.0.0.0";
        }

        return retString;
    }

    //wi-fi用 Gateway取得
    private String getGateway(WifiConfiguration wifiConf) {
        Object objLinkProperties = getField(wifiConf, "linkProperties");
        if (objLinkProperties == null) return "";
        ArrayList arylstRoutes = (ArrayList) getDeclaredField(objLinkProperties, "mRoutes");
        Object objRouteInfo = arylstRoutes.get(0);
        InetAddress inetAddressGateway = (InetAddress) getDeclaredField(objRouteInfo, "mGateway");
        if (inetAddressGateway == null) return "";
        byte aryGateway[] = inetAddressGateway.getAddress();
        StringBuilder strGateway = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (strGateway.length() > 0) strGateway.append(".");
            if (aryGateway[i] >= 0) {
                strGateway.append(aryGateway[i]);
            } else {
                strGateway.append(aryGateway[i] + 256);
            }
        }
        return strGateway.toString();
    }

    //wi-fi用 DNS取得
    private String getDNS(WifiConfiguration wifiConf) {
        Object objLinkProperties = getField(wifiConf, "linkProperties");//WifiConfiguretionの全フィールドを取得している
        if (objLinkProperties == null) return "";
        StringBuilder strDNS = new StringBuilder();
        try {
            ArrayList<InetAddress> arylstDnses = (ArrayList<InetAddress>) getDeclaredField(objLinkProperties, "mDnses");
            byte aryDNS[] = arylstDnses.get(0).getAddress();
            for (int i = 0; i < 4; i++) {
                if (strDNS.length() > 0) strDNS.append(".");
                if (aryDNS[i] >= 0) {
                    strDNS.append(aryDNS[i]);
                } else {
                    strDNS.append(aryDNS[i] + 256);
                }
            }
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
        return strDNS.toString();
    }

    private Object getField(Object object, String strName) {
        Object objField = null;
        try {
            Field field = object.getClass().getField(strName);
            objField = field.get(object);
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
        return objField;
    }

    private Object getDeclaredField(Object object, String strName) {
        Object objField = null;
        try {
            Field field = object.getClass().getDeclaredField(strName);
            field.setAccessible(true);
            objField = field.get(object);
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
        return objField;
    }

    private String getSubnetMask(String ipAddress){
        String subnetMask = "";
        try {
            boolean findMask = false;
            Enumeration<NetworkInterface> networkInterfaceEnumeration =  NetworkInterface.getNetworkInterfaces();
            while(networkInterfaceEnumeration.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    if(("/"+ipAddress).equals(interfaceAddress.getAddress().toString())){
                        subnetMask = convertMask(interfaceAddress.getNetworkPrefixLength());
                        findMask = true;
                        break;
                    }
                }
                if(findMask){
                    break;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return subnetMask;
    }

    private String convertMask(Short cidrMask){
        String subNetMask = "";
        switch (cidrMask){
            case 1 :
                subNetMask = "128.0.0.0";
                break;
            case 2 :
                subNetMask = "192.0.0.0";
                break;
            case 3 :
                subNetMask = "224.0.0.0";
                break;
            case 4 :
                subNetMask = "240.0.0.0";
                break;
            case 5 :
                subNetMask = "248.0.0.0";
                break;
            case 6 :
                subNetMask = "252.0.0.0";
                break;
            case 7 :
                subNetMask = "254.0.0.0";
                break;
            case 8 :
                subNetMask = "255.0.0.0";
                break;
            case 9 :
                subNetMask = "255.128.0.0";
                break;
            case 10 :
                subNetMask = "255.192.0.0";
                break;
            case 11 :
                subNetMask = "255.224.0.0";
                break;
            case 12 :
                subNetMask = "255.240.0.0";
                break;
            case 13 :
                subNetMask = "255.248.0.0";
                break;
            case 14 :
                subNetMask = "255.252.0.0";
                break;
            case 15 :
                subNetMask = "255.254.0.0";
                break;
            case 16 :
                subNetMask = "255.255.0.0";
                break;
            case 17 :
                subNetMask = "255.255.128.0";
                break;
            case 18 :
                subNetMask = "255.255.192.0";
                break;
            case 19 :
                subNetMask = "255.255.224.0";
                break;
            case 20 :
                subNetMask = "255.255.240.0";
                break;
            case 21 :
                subNetMask = "255.255.248.0";
                break;
            case 22 :
                subNetMask = "255.255.252.0";
                break;
            case 23 :
                subNetMask = "255.255.254.0";
                break;
            case 24 :
                subNetMask = "255.255.255.0";
                break;
            case 25 :
                subNetMask = "255.255.255.128";
                break;
            case 26 :
                subNetMask = "255.255.255.192";
                break;
            case 27 :
                subNetMask = "255.255.255.224";
                break;
            case 28 :
                subNetMask = "255.255.255.240";
                break;
            case 29 :
                subNetMask = "255.255.255.248";
                break;
            case 30 :
                subNetMask = "255.255.255.252";
                break;
            case 31 :
                subNetMask = "255.255.255.254";
                break;
            case 32:
                subNetMask = "255.255.255.255";
                break;
            default :
        }
        return subNetMask;
    }


    /**
     * ネットワークのタイプを取得する
     * @param context Application Context
     * @return 正常に取得できた場合，戻り値は，
     * - {@link ConnectivityManager#TYPE_ETHERNET}
     * - {@link ConnectivityManager#TYPE_WIFI}
     * - {@link ConnectivityManager#TYPE_MOBILE}
     * のいずれか．処理に失敗した場合や，上記３つの値以外の接続方法であると判明した場合は-1が返る．
     */
    public static int getNetworkType(Context context){
        int type = -1;
        try{
            //あとで差し替え
            ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm != null && cm.getActiveNetworkInfo() != null) {
                type = cm.getActiveNetworkInfo().getType();
                if(type != ConnectivityManager.TYPE_ETHERNET && type != ConnectivityManager.TYPE_WIFI && type != ConnectivityManager.TYPE_MOBILE) {
                    type = -1;
                }
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
        return type;
    }

    public static int NetworkTypeIsEthernet(Context context) {
        final int ETH_STATE_ENABLED = 2;
        int eth_state = 0;
        try{
            @SuppressLint("WrongConstant") Object eth = context.getSystemService("ethernet");
            if(eth != null) {
                Method method = eth.getClass().getMethod("getEthState");
                eth_state = (Integer)method.invoke(eth);
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
        if(eth_state == ETH_STATE_ENABLED) {
            return ConnectivityManager.TYPE_ETHERNET;
        } else {
            return ConnectivityManager.TYPE_WIFI;
        }
    }

    /**
     * WiFiの接続状況を示す値を返却する
     * @param context Application Context
     * @return {@link WifiInfo}を参照
     */
    public static WifiInfo getWifiState(Context context) {
        WifiInfo wifiInfo = null;
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
            if(wifiManager != null) {
                wifiInfo = wifiManager.getConnectionInfo();
            }
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
        return wifiInfo;
    }

    public static DhcpInfo getDhcpInfo(Context context) {
        DhcpInfo dhcpInfo = null;
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
            if(wifiManager != null) {
                dhcpInfo = wifiManager.getDhcpInfo();
            }
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
        return dhcpInfo;
    }

    public static WifiConfiguration getWifiConfiguration(Context context, String ssid) {
        WifiConfiguration wifiConfiguration = null;
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
            if(wifiManager != null) {
                for(WifiConfiguration configuration : wifiManager.getConfiguredNetworks()) {
                    if(configuration.SSID.equals(ssid)) {
                        wifiConfiguration = configuration;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
        return wifiConfiguration;
    }

    /**
     * ネットワークインターフェース eth0 のIPアドレスを取得する
     *
     * @return IPアドレスを文字列で返却する．ネットワークがEthernet接続がどうかはチェックしない
     */
    public static String getEthernetIpAddress() {
        try {
            NetworkInterface ni = NetworkInterface.getByName("eth0");
            if (ni != null) {
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    String ipAddress = addresses.nextElement().getHostAddress();
                    if(ipAddress.matches("^(([1-9]?[0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([1-9]?[0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")) {
                        return ipAddress;
                    }
                }
            }
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
        return "";
    }

    private void makeResultNetworkTestFile(ArrayList<String> valueList) throws IOException {
        //create new File
        File newFile = new File(AdmintPath.getReportDir().getAbsolutePath() + "/resultNetWorkTest.txt");
        FileWriter fileWriter = new FileWriter(newFile);

        //ファイル作成時点の時刻取得
        long latestTestMillis = System.currentTimeMillis();
        SimpleDateFormat latestTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());

        String title = getString(R.string.last_run_date_and_time) + ": " + latestTime.format(latestTestMillis);

        fileWriter.append(title);
        fileWriter.append("\n");

        //write to file
//        for (String item : valueList){
//            fileWriter.append(item);
//        }
        String[]list;
        for(String line : valueList){
            list = line.split(split_word);
            if(list.length >= 1){
                for(String list_item : list){
                    if(list_item.equals(list[0])){
                        fileWriter.append(list_item);
                    } else {
                        fileWriter.append(" \t");
                        fileWriter.append(list_item);
                    }
                }
            } else {
                fileWriter.append(line);
            }
        }



        fileWriter.close();

    }

}

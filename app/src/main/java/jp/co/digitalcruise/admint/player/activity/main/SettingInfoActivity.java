package jp.co.digitalcruise.admint.player.activity.main;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.activity.AbstractAdmintActivity;
import jp.co.digitalcruise.admint.player.component.define.DeviceDef;
import jp.co.digitalcruise.admint.player.component.file.AdmintPath;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;

public class SettingInfoActivity extends AbstractAdmintActivity {

    private class TermInfo{
//        String name = null;
        String alias = null;
        String value = null;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        try{
            super.onCreate(savedInstanceState);

            createInfoView();


        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    @Override
    protected void onResume(){
        try{
            super.onResume();

        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void createInfoView(){
        try{
            setContentView(R.layout.register_info);
            //WrongViewCastはレイアウトXMLで定義されているViewが別の型にキャストされていないかを調べてくれる
            @SuppressLint("WrongViewCast") ScrollView parent_layout = findViewById(R.id.layout_info);

            TableLayout tb_layout = new TableLayout(this);
            setRegistInfo(tb_layout);

            // レイアウトにビュー追加
            ViewGroup.LayoutParams layout_param = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            parent_layout.addView(tb_layout, layout_param);

        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void setRegistInfo(TableLayout tb_layout){
        try{
            ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);

            // タイトル
            TableRow title_row = new TableRow(this);
            TextView title_value = new TextView(this);
            title_value.setText(getString(R.string.viewer_info_column_label_regist_info));
            title_row.addView(title_value);
            tb_layout.addView(title_row, param);

            ArrayList<TermInfo> item_list = getRegistInfo();

            for(TermInfo item : item_list){
                TableRow row = new TableRow(this);
                TextView value = new TextView(this);
                TextView alias = new TextView(this);
                alias.setPadding(0, 0, 10, 0);

                if(item.alias != null){
                    alias.setText(item.alias);
                }

                if(item.value != null){
                    value.setText(item.value);
                }

                row.addView(alias);
                row.addView(value);

                tb_layout.addView(row, param);
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }


    private ArrayList<TermInfo> getRegistInfo(){
//        final String PREF_DELIVERY_SERVER_URL = "delivery_server_url";

        ArrayList<TermInfo> item_list = new ArrayList<>();

        try{
            TermInfo info;

            PackageInfo packageInfo = getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_ACTIVITIES);
            String version = packageInfo.versionName;

            // version
            info = new TermInfo();
            info.alias = getString(R.string.viewer_info_row_label_version);
            info.value = version;
            item_list.add(info);

            // siteid
            info = new TermInfo();
            info.alias = getString(R.string.viewer_info_row_label_site_id);
            info.value = DefaultPref.getSiteId();
            item_list.add(info);

            //機番　iBASEのみ値入れる
            info = new TermInfo();
            info.alias = getString(R.string.viewer_info_row_label_extra_code);
            if(Build.MODEL.equals(DeviceDef.STRIPE)){
                info.value = DefaultPref.getExtraId();
            } else {
                info.value="";
            }
            item_list.add(info);


            //OSO用処理
            //端末管理番号
            info = new TermInfo();
            info.alias = getString(R.string.viewer_info_row_label_control_number);
            info.value = DefaultPref.getStbId();
            item_list.add(info);


            if(AdmintPath.isMaintenance()){
                // terminalid
                info = new TermInfo();
                info.alias = getString(R.string.viewer_info_row_label_terminal_id);
                info.value = DefaultPref.getTerminalId();
                item_list.add(info);


                // akey
                info = new TermInfo();
                info.alias = getString(R.string.viewer_info_row_label_akey);
                info.value = DefaultPref.getAkey();
                item_list.add(info);
            }

            //登録日
            //キッティングし直したら行を追加していく
            if(getRegisteredDay() != null){
                for(String value : getRegisteredDay()){
                    info = new TermInfo();
                    info.alias = getString(R.string.viewer_info_row_label_registered_day);
                    if(!value.equals(getRegisteredDay().get(0))){
                        info.alias = "";
                    }
                    info.value = value;
                    item_list.add(info);
                }
            } else {
                info = new TermInfo();
                info.alias = getString(R.string.viewer_info_row_label_registered_day);
                item_list.add(info);
            }

        }catch(Exception e){
            Logging.stackTrace(e);
        }

        return item_list;
    }

    //Get activation day for preference
    private ArrayList<String> getRegisteredDay(){
        Gson gson = new Gson();
        ArrayList<String> array;

        array = gson.fromJson(DefaultPref.getRegisteredDay(), (Type) List.class);

        return array;
    }
}

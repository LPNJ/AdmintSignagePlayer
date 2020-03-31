package jp.co.digitalcruise.admint.player.activity.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import java.util.List;

import jp.co.digitalcruise.admint.player.AdmintApplication;
import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.activity.main.dialog.OutputLogDialogPreference;
import jp.co.digitalcruise.admint.player.component.define.DeviceDef;
import jp.co.digitalcruise.admint.player.component.define.UpdaterIntentDef;
import jp.co.digitalcruise.admint.player.component.file.AdmintPath;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.pref.RegisterPref;
import jp.co.digitalcruise.admint.player.service.PlaylistService;
import jp.co.digitalcruise.admint.player.service.RecoverService;
import jp.co.digitalcruise.admint.player.service.network.HealthCheckService;

public class SettingPreferenceActivity extends PreferenceActivity {

    private static boolean mSendBroadcast = false;

    @Override
    protected void onPause(){
        super.onPause();

        if(mSendBroadcast){
            sendBroadCastSetting();
        }
        mSendBroadcast = false;
    }

    @Override
    public void onBuildHeaders(List<Header> target){
        try{
            boolean open_from_register = RegisterPref.getOpenProxySettingFromRegister();

            if(open_from_register){
                loadHeadersFromResource(R.xml.setting_hander_regist_proxy, target);
            } else if(AdmintPath.isMaintenance()){
                if(DeviceDef.isStandAloneInValid() && DefaultPref.getHaveSetBeforeStandAloneMode()){
                    loadHeadersFromResource(R.xml.setting_hander_stand_alone_maintenance, target);
                } else if (DeviceDef.isGroova()){
                    loadHeadersFromResource(R.xml.setting_hander_maintenance_groova, target);
                } else {
                    loadHeadersFromResource(R.xml.setting_hander_maintenance_no_touch, target);
                }
            } else {
                if(DeviceDef.isStandAloneInValid() && DefaultPref.getHaveSetBeforeStandAloneMode()){
                    loadHeadersFromResource(R.xml.setting_hander_standalone, target);
                } else if(DeviceDef.isGroova()){
                    loadHeadersFromResource(R.xml.setting_hander_groova, target);
                } else {
                    loadHeadersFromResource(R.xml.setting_hander_no_touch, target);
                }
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return PrefsSystem.class.getName().equals(fragmentName) ||
                PrefsProxy.class.getName().equals(fragmentName) ||
                PrefsData.class.getName().equals(fragmentName) ||
                PrefsLog.class.getName().equals(fragmentName) ||
                PrefsTouch.class.getName().equals(fragmentName) ||
                PrefsDevelop.class.getName().equals(fragmentName) ||
                PrefsStandAlone.class.getName().equals(fragmentName);
    }

    private void sendBroadCastSetting(){
        // updaterに送信
        Intent updater_intent = new Intent(UpdaterIntentDef.ACTION_SETTING_UPDATE);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        updater_intent.putExtra(UpdaterIntentDef.EXTRA_SETTING_PROXY_ENABLE, prefs.getBoolean(getString(R.string.setting_key_proxy_enable), false));
        updater_intent.putExtra(UpdaterIntentDef.EXTRA_SETTING_PROXY_HOST, prefs.getString(getString(R.string.setting_key_proxy_host), ""));
        updater_intent.putExtra(UpdaterIntentDef.EXTRA_SETTING_PROXY_PORT, prefs.getString(getString(R.string.setting_key_proxy_port), ""));
        updater_intent.putExtra(UpdaterIntentDef.EXTRA_SETTING_PROXY_USER, prefs.getString(getString(R.string.setting_key_proxy_user), ""));
        updater_intent.putExtra(UpdaterIntentDef.EXTRA_SETTING_PROXY_PASSWORD, prefs.getString(getString(R.string.setting_key_proxy_password), ""));

        sendBroadcast(updater_intent);
    }


    public static class PrefsSystem extends PreferenceFragment {

        public String DEVICE_MODEL;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            try{
                super.onCreate(savedInstanceState);

                DEVICE_MODEL = Build.MODEL;

                if(DefaultPref.getStandAloneMode()){
                    PreferenceManager.setDefaultValues(getActivity(), R.xml.setting_prefs_system_stand_alone, false);
                    addPreferencesFromResource(R.xml.setting_prefs_system_stand_alone);
                } else {
                    switch (DEVICE_MODEL) {
                        case DeviceDef.GROOVA_STICK:
                            PreferenceManager.setDefaultValues(getActivity(), R.xml.setting_prefs_system_groova_stick, false);
                            addPreferencesFromResource(R.xml.setting_prefs_system_groova_stick);
                            break;
                        case DeviceDef.GROOVA_BOX:
                            PreferenceManager.setDefaultValues(getActivity(), R.xml.setting_prefs_system_groova_box, false);
                            addPreferencesFromResource(R.xml.setting_prefs_system_groova_box);
                            break;
                        default:
                            PreferenceManager.setDefaultValues(getActivity(), R.xml.setting_prefs_system, false);
                            addPreferencesFromResource(R.xml.setting_prefs_system);
                            break;
                    }
                }



                EditTextPreference sdcard_drive = (EditTextPreference)findPreference(getString(R.string.setting_key_sdcard_drive));
                sdcard_drive.setOnPreferenceChangeListener((pref, newValue) -> true);

                CheckBoxPreference use_extra_storage = (CheckBoxPreference)findPreference(getString(R.string.setting_key_use_extra_storage));
                if(DEVICE_MODEL.equals(DeviceDef.TOSHIBA_SHARED_BOARD)) {
                    use_extra_storage.setSummary(getString(R.string.setting_list_summary_disable));
                    use_extra_storage.setEnabled(false);
                } else {
                    use_extra_storage.setOnPreferenceChangeListener((pref, newValue) -> {
                        try{
                            if((Boolean) newValue){
                                Logging.info(getString(R.string.log_info_menu_use_external_storage_on));
                                infoDialog(getString(R.string.log_info_menu_use_external_storage_on), getString(R.string.external_storage));
                            }else{
                                Logging.info(getString(R.string.log_info_menu_use_external_storage_off));
                                infoDialog(getString(R.string.log_info_menu_use_external_storage_off), getString(R.string.external_storage));
                            }
                        }catch(Exception e){
                            Logging.stackTrace(e);
                        }
                        return true;
                    });
                }

                if(!Build.MODEL.equals(DeviceDef.GROOVA_STICK) && !Build.MODEL.equals(DeviceDef.GROOVA_BOX)) {
                    CheckBoxPreference boot_start = (CheckBoxPreference)findPreference(getString(R.string.setting_key_boot_start));
                    boot_start.setOnPreferenceChangeListener((pref, newValue) -> {

                        if((Boolean) newValue){
                            Logging.info(getString(R.string.log_info_menu_auto_boot_on));
                            infoDialog(getString(R.string.log_info_menu_auto_boot_on), getString(R.string.auto_boot));
                        }else{
                            Logging.info(getString(R.string.log_info_menu_auto_boot_off));
                            infoDialog(getString(R.string.log_info_menu_auto_boot_off), getString(R.string.auto_boot));
                        }
                        return true;
                    });
                }

                CheckBoxPreference net_service = (CheckBoxPreference)findPreference(getString(R.string.setting_key_net_service));
                net_service.setOnPreferenceChangeListener((pref, newValue) -> {
                    if((Boolean) newValue){
                        Logging.info(getString(R.string.log_info_menu_net_service_on));
                        netServiceDialog(getString(R.string.log_info_menu_net_service_on));
//                        restartNetworkService();
                    }else{
                        Logging.info(getString(R.string.log_info_menu_net_service_off));
                        netServiceDialog(getString(R.string.log_info_menu_net_service_off));
                    }
                    return true;
                });

                CheckBoxPreference check_anr_and_reboot = (CheckBoxPreference)findPreference(getString(R.string.setting_key_check_anr_reboot));
                check_anr_and_reboot.setOnPreferenceChangeListener((preference, newValue) ->{

                    // ANR 監視サービスON / OFF
                    Intent intent = new Intent(getActivity(), RecoverService.class);
                    intent.setAction(RecoverService.ACTION_SET_CHECK_ANR_ALARM);
                    getActivity().startService(intent);

                    return true;
                });


            }catch(Exception e){
                Logging.stackTrace(e);
            }
        }

        private void infoDialog(String msg, String title){
            new AlertDialog.Builder(getActivity())
                    .setTitle(title)
                    .setMessage(msg)
                    .setPositiveButton(getString(R.string.dialog_button_ok), null)
                    .show();
        }

        private void netServiceDialog(String msg){
            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.setting_list_title_net_service))
                    .setMessage(msg + getString(R.string.please_reboot_application))
                    .setPositiveButton(getString(R.string.dialog_button_ok), null)
                    .show();
        }

    }


    public static class PrefsProxy extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            try{
                super.onCreate(savedInstanceState);

                PreferenceManager.setDefaultValues(getActivity(), R.xml.setting_prefs_proxy, false);
                addPreferencesFromResource(R.xml.setting_prefs_proxy);



                EditTextPreference proxy_host = (EditTextPreference)findPreference(getString(R.string.setting_key_proxy_host));
                proxy_host.setOnPreferenceChangeListener((pref, newValue) -> {
                    mSendBroadcast = true;
                    return true;
                });

                EditTextPreference proxy_port = (EditTextPreference)findPreference(getString(R.string.setting_key_proxy_port));
                proxy_port.setOnPreferenceChangeListener((pref, newValue) -> {
                    mSendBroadcast = true;
                    return true;
                });

                EditTextPreference proxy_user = (EditTextPreference)findPreference(getString(R.string.setting_key_proxy_user));
                proxy_user.setOnPreferenceChangeListener((pref, newValue) -> {
                    mSendBroadcast = true;
                    return true;
                });

                EditTextPreference proxy_password = (EditTextPreference)findPreference(getString(R.string.setting_key_proxy_password));
                proxy_password.setOnPreferenceChangeListener((pref, newValue) -> {
                    mSendBroadcast = true;
                    return true;
                });

                CheckBoxPreference proxy_enable = (CheckBoxPreference)findPreference(getString(R.string.setting_key_proxy_enable));
                proxy_enable.setOnPreferenceChangeListener((pref, newValue) -> {
                    try{
                        mSendBroadcast = true;
                        if((Boolean)newValue){
                            Logging.info(getString(R.string.proxy_setting_on));
                        }else{
                            Logging.info(getString(R.string.proxy_setting_off));
                        }
                    }catch(Exception e){
                        Logging.stackTrace(e);
                    }
                    return true;
                });

                CheckBoxPreference proxy_webcontent = (CheckBoxPreference)findPreference(getString(R.string.setting_key_proxy_webcontent));
                proxy_webcontent.setOnPreferenceChangeListener((pref, newValue) ->{
                    try{
                        mSendBroadcast = true;
                        if((Boolean)newValue){
                            Logging.info(getString(R.string.web_content_proxy_setting_on));
                        } else {
                            Logging.info(getString(R.string.web_content_proxy_setting_off));
                        }
                    }catch (Exception e){
                        Logging.stackTrace(e);
                    }
                    return true;
                });

                //Groovaではプロキシの設定は端末のネットワーク設定から行うのでアプリ内では設定できないようにする。
                if(Build.MODEL.equals(DeviceDef.GROOVA_STICK) || Build.MODEL.equals(DeviceDef.GROOVA_BOX)) {
                    proxy_enable.setSummary(getString(R.string.setting_list_summary_disable));
                    proxy_enable.setEnabled(false);
                    proxy_enable.setChecked(false);
                    proxy_host.setSummary(getString(R.string.setting_list_summary_disable));
                    proxy_host.setText("");
                    proxy_port.setSummary(getString(R.string.setting_list_summary_disable));
                    proxy_user.setSummary(getString(R.string.setting_list_summary_disable));
                    proxy_password.setSummary(getString(R.string.setting_list_summary_disable));
                    proxy_webcontent.setSummary(getString(R.string.setting_list_summary_disable));
                    proxy_webcontent.setEnabled(false);
                    proxy_webcontent.setChecked(false);
                }

            }catch(Exception e){
                Logging.stackTrace(e);
            }
        }
    }

    public static class PrefsData extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PreferenceManager.setDefaultValues(getActivity(), R.xml.setting_prefs_data, false);
            addPreferencesFromResource(R.xml.setting_prefs_data);
        }
    }

    public static class PrefsLog extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            try{
                super.onCreate(savedInstanceState);

                PreferenceManager.setDefaultValues(getActivity(), R.xml.setting_prefs_log, false);
                addPreferencesFromResource(R.xml.setting_prefs_log);

                OutputLogDialogPreference outputLogDialogPreference = (OutputLogDialogPreference)findPreference(getString(R.string.setting_key_output_log));
                if(Build.MODEL.equals(DeviceDef.TOSHIBA_SHARED_BOARD)) {
                    outputLogDialogPreference.setSummary(getString(R.string.setting_list_summary_disable));
                    outputLogDialogPreference.setEnabled(false);
                }
            }catch(Exception e){
                Logging.stackTrace(e);
            }
        }
    }

    public static class PrefsTouch extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            try{
                super.onCreate(savedInstanceState);

                //GroovaとSK_POPはタッチに対応してないので省く
                if(!Build.MODEL.equals(DeviceDef.GROOVA_STICK) && !Build.MODEL.equals(DeviceDef.GROOVA_BOX) && !Build.MODEL.equals(DeviceDef.SK_POP)) {
                    PreferenceManager.setDefaultValues(getActivity(), R.xml.setting_prefs_touch, false);
                    addPreferencesFromResource(R.xml.setting_prefs_touch);
                }
            }catch(Exception e){
                Logging.stackTrace(e);
            }
        }
    }


    public static class PrefsStandAlone extends PreferenceFragment{
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            try{
                super.onCreate(savedInstanceState);

                PreferenceManager.setDefaultValues(getActivity(), R.xml.setting_prefs_standalone, false);
                addPreferencesFromResource(R.xml.setting_prefs_standalone);

                CheckBoxPreference stand_alone_mode = (CheckBoxPreference)findPreference(getString(R.string.setting_key_stand_alone_mode));
                stand_alone_mode.setOnPreferenceChangeListener((pref, newValue) -> {
                    if((Boolean) newValue){
                        DefaultPref.setNetworkService(false);
                        cleanSchedule();
                        infoDialog(getString(R.string.dialog_stand_alone_off), getString(R.string.change_stand_alone));
                    }else{
                        DefaultPref.setNetworkService(true);
                        cleanSchedule();
                        infoDialog(getString(R.string.dialog_stand_alone_on), getString(R.string.change_stand_alone));
                    }
                    return true;
                });


            }catch (Exception e){
                Logging.stackTrace(e);
            }
        }

        private void infoDialog(String msg, String title){
            new AlertDialog.Builder(getActivity())
                    .setTitle(title)
                    .setMessage(msg)
                    .setPositiveButton(getString(R.string.dialog_button_ok), null)
                    .show();
        }

        private void cleanSchedule(){
            Context context = AdmintApplication.getInstance().getApplicationContext();
            // スケジュールクリア
            {
                Intent intent = new Intent(context, HealthCheckService.class);
                intent.setAction(HealthCheckService.ACTION_CLEAR_SCHEDULE);
                context.startService(intent);
            }

            {
                Intent intent = new Intent(context, PlaylistService.class);
                intent.setAction(PlaylistService.ACTION_CLEAR_PLAYLIST);
                context.startService(intent);
            }

        }

    }

    public static class PrefsDevelop extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            try{
                super.onCreate(savedInstanceState);
                PreferenceManager.setDefaultValues(getActivity(), R.xml.setting_prefs_develop, false);
                addPreferencesFromResource(R.xml.setting_prefs_develop);

                EditTextPreference user_storage = (EditTextPreference)findPreference(getString(R.string.setting_key_user_storage));
                user_storage.setOnPreferenceChangeListener((pref, newValue) -> true);

            }catch(Exception e){
                try {
                    Logging.stackTrace(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

}

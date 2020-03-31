package jp.co.digitalcruise.admint.player.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.io.File;
import jp.co.digitalcruise.admint.player.AdmintApplication;
import jp.co.digitalcruise.admint.player.PlayerActivity;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;

abstract public class AbstractService extends IntentService{
    public AbstractService(String name) {
        super(name);
    }

    protected File getSdDir(){
        File sd_dir = null;
        try{
            String prefs = DefaultPref.getSdcardDrive();
            sd_dir = getSdDir(prefs);
        }catch(Exception e){
            Logging.stackTrace(e);
        }
        return sd_dir;
    }

    @Nullable
    private File getSdDir(String pref_sd_path) {
        File sd_dir = null;
        try {
            File pref_sd_dir = null;
            if(pref_sd_path != null && pref_sd_path.length() > 0){
                pref_sd_dir = new File(pref_sd_path);
            }

            if(pref_sd_dir != null && pref_sd_dir.isDirectory()){
                sd_dir = pref_sd_dir;
            }
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
        return sd_dir;
    }

    protected @NonNull File getAnrTracesFile(){
        final String ANR_PATH = "/data/anr/traces.txt";
        return new File(ANR_PATH);
    }

    protected boolean isViewerForegraund(){
        return AdmintApplication.getInstance().getViewerStatus() == AdmintApplication.VIEWER_STATUS_FOREGROUND;
    }

    protected void sendToast(String msg){
        Intent intent = new Intent();
        intent.setAction(PlayerActivity.ACTION_TOAST_MSG);
        intent.putExtra(PlayerActivity.INTENT_EXTRA_TOAST_MSG, msg);
        getBaseContext().sendBroadcast(intent);
    }
}

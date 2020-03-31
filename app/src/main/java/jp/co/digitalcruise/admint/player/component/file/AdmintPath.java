package jp.co.digitalcruise.admint.player.component.file;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;

import jp.co.digitalcruise.admint.player.AdmintApplication;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;

public class AdmintPath {

    private static final String DIR_DCI = "DigitalCruise";
    public static final String DIR_ADMINT_PLAYER = "AdmintPlayer";
    private static final String DIR_CONTENTS = "contents";
    private static final String DIR_MATERIALS = "materials";
    private static final String DIR_TEMP = "temp";
    private static final String DIR_REPORT = "report";

    public static final String DIR_TEMP_CONTENTS = "contents";
    public static final String DIR_TEMP_MATERIALS = "materials";
    public static final String DIR_TEMP_EXTERNALS = "externals";
    private static final String DIR_TEMP_DEBUG = "debug";

    private static @Nullable File getSdDriveDir(){
        File sdcard_dir = null;
        String sdcard_drive = DefaultPref.getSdcardDrive();

        File temp_dir;
        if(sdcard_drive.length() > 0){
            temp_dir = new File(sdcard_drive);
            if(temp_dir.exists()){
                sdcard_dir = temp_dir;
            }
        }
        return sdcard_dir;
    }

    public static @NonNull File getAplicationDir(){
        File app_dir;
        Context context = AdmintApplication.getInstance();
        boolean use_extra_storage = DefaultPref.getUseExtraStorage();

        File extra_dir = null;
        if(use_extra_storage){
            extra_dir = getSdDriveDir();
        }

        if(extra_dir != null){
            app_dir = extra_dir;
        }else{
            String path = DefaultPref.getUserStorage();

            if(path.length() > 0){
                app_dir = new File(path);
            }else{
                app_dir = Environment.getExternalStorageDirectory();
            }
        }

        if(!app_dir.exists()){
            app_dir = context.getFilesDir();
        }else{
            File admint_dir = new File(app_dir.getAbsolutePath() + File.separator + DIR_DCI + File.separator + DIR_ADMINT_PLAYER);
            if(!admint_dir.exists() || !admint_dir.isDirectory()){
                FileUtil.makeDirs(admint_dir);
            }
            app_dir = admint_dir;
        }
        return app_dir;
    }


    public static @NonNull File getTemporaryDir(){
        File dir = new File(getAplicationDir().getAbsolutePath() + File.separator + DIR_TEMP);
        if(!dir.exists()){
            FileUtil.makeDir(dir);
        }
        return dir;
    }

    public static @NonNull File getTemporaryMaterialsDir(){
        File dir = new File(getTemporaryDir().getAbsolutePath() + File.separator + DIR_TEMP_MATERIALS);
        if(!dir.exists()){
            FileUtil.makeDir(dir);
        }
        return dir;
    }

    public static @NonNull File getTemporaryContentsDir(){
        File dir = new File(getTemporaryDir().getAbsolutePath() + File.separator + DIR_TEMP_CONTENTS);
        if(!dir.exists()){
            FileUtil.makeDir(dir);
        }
        return dir;
    }

    public static @NonNull File getTemporaryDebugDir(){
        File dir = new File(getTemporaryDir().getAbsolutePath() + File.separator + DIR_TEMP_DEBUG);
        if(!dir.exists()){
            FileUtil.makeDir(dir);
        }
        return dir;
    }

    public static @NonNull File getTemporaryExternalsDir(){
        File dir = new File(getTemporaryDir().getAbsolutePath() + File.separator + DIR_TEMP_EXTERNALS);
        if(!dir.exists()){
            FileUtil.makeDir(dir);
        }
        return dir;
    }

    public static @NonNull File getReportDir(){
        File dir = new File(getAplicationDir().getAbsolutePath() + File.separator + DIR_REPORT);
        if(!dir.exists()){
            FileUtil.makeDir(dir);
        }
        return dir;
    }

    public static @NonNull File getContentsDir(){
        File dir = new File(getAplicationDir().getAbsolutePath() + File.separator + DIR_CONTENTS);
        if(!dir.exists()){
            FileUtil.makeDir(dir);
        }
        return dir;
    }

    public static @NonNull File getMaterialsDir(){
        File dir = new File(getAplicationDir().getAbsolutePath() + File.separator + DIR_MATERIALS);
        if(!dir.exists()){
            FileUtil.makeDir(dir);
        }
        return dir;
    }

    private static @Nullable File getSdDir(String pref_sd_path) {
        File pref_sd_dir = null;
        if(pref_sd_path != null && pref_sd_path.length() > 0){
            pref_sd_dir = new File(pref_sd_path);
        }

        if(pref_sd_dir != null && pref_sd_dir.isDirectory()){
            return pref_sd_dir;
        }else{
            return null;
        }
    }

    public static @Nullable File getSdDir(){
        String pref_sd_path = DefaultPref.getSdcardDrive();
        return getSdDir(pref_sd_path);
    }

    public static boolean isMaintenance(){
        final String MAINTENANCE_KEY = "dci_maintenance";
        File dir = new File(DefaultPref.getSdcardDrive());
        if(dir.isDirectory()){
            File key_dir = new File(dir.getAbsolutePath() + File.separator + MAINTENANCE_KEY);
            if(key_dir.isDirectory()) {
                return true;
            }
        }
        return false;
    }

}

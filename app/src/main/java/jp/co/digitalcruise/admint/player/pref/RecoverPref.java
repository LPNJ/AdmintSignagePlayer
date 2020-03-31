package jp.co.digitalcruise.admint.player.pref;

import android.app.Activity;
import android.content.Context;

import jp.co.digitalcruise.admint.player.AdmintApplication;

public class RecoverPref {
    // プリファレンス（ファイル）名
    private static final String PREF_NAME = "recover";

    private static final String KEY_ANR_FILE_MODIFIED = "anr_file_modified";

    // traces.txt更新時刻
    public static long getAnrFileModified(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        return context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).getLong(KEY_ANR_FILE_MODIFIED,0);
    }

    public static void setAnrFileModified(long val){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().putLong(KEY_ANR_FILE_MODIFIED, val).apply();
    }

    public static void clearAnrFileModified(){
        Context context = AdmintApplication.getInstance().getApplicationContext();
        context.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE).edit().clear().apply();
    }

}

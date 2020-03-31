package jp.co.digitalcruise.admint.player.activity.main.dialog;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import jp.co.digitalcruise.admint.player.AdmintApplication;
import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.define.DeviceDef;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.service.LoggingService;

/**
 * Created by seki on 2016/08/24.
 *
 *
 */
public class OutputLogDialogPreference extends DialogPreference {

    private Context mContext = null;

    public OutputLogDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = AdmintApplication.getInstance().getApplicationContext();
        setDialogMessage(mContext.getString(R.string.setting_dialog_message_output_log));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        String storage = null;
        if (positiveResult) {
            if (Build.MODEL.equals(DeviceDef.SHARP_BOX)) {
                storage =DeviceDef.SHARP_USB;
            } else if (Build.MODEL.equals(DeviceDef.SK_BOX) || Build.MODEL.equals(DeviceDef.SK_POP)) {
                storage = DeviceDef.SK_USB;
            } else if (Build.MODEL.equals(DeviceDef.GROOVA_BOX) || Build.MODEL.equals(DeviceDef.GROOVA_STICK)) {
                storage = DeviceDef.GROOVA_USB;
            } else if( Build.MODEL.equals(DeviceDef.HWLD_XM6502)){
                storage = DeviceDef.XM6502_USB;
            } else if( Build.MODEL.equals(DeviceDef.STRIPE)){
                storage = DeviceDef.IBASE_USB;
            } else if( Build.MODEL.equals(DeviceDef.SHUTTLE)){
                storage = DeviceDef.SHUTTLE_USB;
            } else if( Build.MODEL.equals(DeviceDef.PN_M_SERIES) || Build.MODEL.equals(DeviceDef.PN_B_SERIES)){
                storage = DeviceDef.PN_M_SERIES_USB;
            } else {
                storage = DefaultPref.getSdcardDrive();
            }

            String key = getKey();

            String LOG_TYPE_KEY_LOG = mContext.getString(R.string.setting_key_output_log);
            String LOG_TYPE_KEY_ANR_TRACE = mContext.getString(R.string.setting_key_output_anr_trace_log);


            if (mContext != null) {
                Toast.makeText(mContext, mContext.getString(R.string.toast_msg_output_log_request), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(mContext, LoggingService.class);
                if (key.equals(LOG_TYPE_KEY_LOG)) {
                    intent.setAction(LoggingService.ACTION_LOG_OUTPUT_FILE);
                } else if(key.equals(LOG_TYPE_KEY_ANR_TRACE)) {
                    intent.setAction(LoggingService.ACTION_ANR_TRACE_OUTPUT_FILE);
                }
                intent.putExtra(LoggingService.INTENT_EXTRA_LOG_STORAGE_PATH, storage);
                mContext.startService(intent);
            }
        }
        super.onDialogClosed(positiveResult);
    }
}

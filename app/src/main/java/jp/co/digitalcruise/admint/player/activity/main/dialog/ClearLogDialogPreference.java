package jp.co.digitalcruise.admint.player.activity.main.dialog;

import android.content.Context;
import android.content.Intent;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.service.LoggingService;

public class ClearLogDialogPreference extends DialogPreference {

    private Context mContext = null;

    public ClearLogDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        this.setDialogMessage(R.string.setting_dialog_message_clear_log);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        try{
            if(positiveResult){
                if(mContext != null){
                    Toast.makeText(mContext, mContext.getString(R.string.toast_msg_clear_log_request), Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(mContext, LoggingService.class);
                    intent.setAction(LoggingService.ACTION_LOG_CLEAR);
                    mContext.startService(intent);
                }
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
        super.onDialogClosed(positiveResult);
    }
}

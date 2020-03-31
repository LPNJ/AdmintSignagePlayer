package jp.co.digitalcruise.admint.player.activity.main.dialog;

import android.content.Context;
import android.content.Intent;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.service.network.ReportService;

public class UploadTouchLogDialogPreference extends DialogPreference {
    public UploadTouchLogDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setDialogMessage(R.string.setting_dialog_message_upload_touch_log);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        try{
            if(positiveResult){
                Toast.makeText(getContext(), getContext().getString(R.string.toast_msg_upload_touch_log_request), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getContext(), ReportService.class);
                intent.setAction(ReportService.ACTION_UPLOAD_TOUCH_LOG);
                getContext().startService(intent);
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
        super.onDialogClosed(positiveResult);
    }
}

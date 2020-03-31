package jp.co.digitalcruise.admint.player.activity.main.dialog;

import android.content.Context;
import android.content.Intent;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.service.network.ContentManagerService;

public class DeleteUnusedContentsDialogPreference extends DialogPreference {
    public DeleteUnusedContentsDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setDialogMessage(R.string.setting_dialog_message_delete_unused_contents);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        try{
            if(positiveResult){
                    Toast.makeText(getContext(), getContext().getString(R.string.toast_msg_delete_unused_contents_request), Toast.LENGTH_LONG).show();
                {
                    Intent intent = new Intent(getContext(), ContentManagerService.class);
                    intent.setAction(ContentManagerService.ACTION_DELETE_UNUSE_CONTENTS);
                    getContext().startService(intent);
                }

            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }
        super.onDialogClosed(positiveResult);
    }
}

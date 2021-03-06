package jp.co.digitalcruise.admint.player.activity.main.dialog;

import android.content.Context;
import android.content.Intent;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.service.PlaylistService;
import jp.co.digitalcruise.admint.player.service.network.HealthCheckService;

public class ClearScheduleDialogPreference extends DialogPreference {
    public ClearScheduleDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setDialogMessage(R.string.setting_dialog_message_clear_schedule);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        try{
            if(positiveResult){
                Toast.makeText(getContext(), getContext().getString(R.string.toast_msg_clean_schedule_request), Toast.LENGTH_SHORT).show();
                // プレイリストクリア
                {
                    Intent intent = new Intent(getContext(), PlaylistService.class);
                    intent.setAction(PlaylistService.ACTION_CLEAR_PLAYLIST);
                    getContext().startService(intent);
                }

                // スケジュールクリア
                {
                    Intent intent = new Intent(getContext(), HealthCheckService.class);
                    intent.setAction(HealthCheckService.ACTION_CLEAR_SCHEDULE);
                    getContext().startService(intent);
                }
            }
        }catch(Exception e){
          Logging.stackTrace(e);
        }
        super.onDialogClosed(positiveResult);
    }
}

package jp.co.digitalcruise.admint.player.component.log;

import android.content.Context;
import android.content.Intent;

import jp.co.digitalcruise.admint.player.AdmintApplication;
import jp.co.digitalcruise.admint.player.service.network.NetLogService;

public class NetLog {
    private static void logging(String action, String msg){

        try{
            Context context = AdmintApplication.getInstance();

            Intent intent = new Intent(context, NetLogService.class);
            intent.setAction(action);
            intent.putExtra(NetLogService.INTENT_LOG_MESSAGE, msg);

            context.startService(intent);
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    public static void notice(String msg){
        logging(NetLogService.ACTION_LOG_NOTICE, msg);
    }

    public static void warning(String msg){
        logging(NetLogService.ACTION_LOG_WARNING, msg);
    }

}

package jp.co.digitalcruise.admint.player.component;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;

public class CompatibleSdk {

    public static void setAlarmEx(Context context, long trigger_at_mills, PendingIntent pintent){
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if(am != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC, trigger_at_mills, pintent);
            } else {
                am.set(AlarmManager.RTC, trigger_at_mills, pintent);
            }
        }else{
            throw new RuntimeException("(AlarmManager)getSystemService return null");
        }
    }
}

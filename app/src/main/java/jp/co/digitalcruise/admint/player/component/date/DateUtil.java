package jp.co.digitalcruise.admint.player.component.date;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtil {

    static public String convDateTimeFormal(long timestamp){
        final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        if(timestamp <= 0){
            return "0000-00-00 00:00:00";
        } else {
            Date date = new Date(timestamp);
            return FORMAT.format(date);
        }
    }

    static public String convDateTimeFileName(long timestamp){
        final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss(SSS)", Locale.getDefault());

        if(timestamp <= 0){
            return "0000-00-00-000000(000)";
        } else {
            Date date = new Date(timestamp);
            return FORMAT.format(date);
        }
    }

    static public boolean isValidCurrentTime(){
        final long crit_unixtime = 1483196400000L; // "2017/01/01 00:00:00"

        Date date = new Date(System.currentTimeMillis());

        Date criteria = new Date(crit_unixtime);
        // 比較元（左）と比較先（引数）を比べて比較元（2017-01-01）が値が大きい時は過去の日付
        if (criteria.compareTo(date) > 0) {
            // おかしい
            return false;
        }else{
            // 正しい
            return true;
        }
    }
}

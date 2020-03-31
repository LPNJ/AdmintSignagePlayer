package jp.co.digitalcruise.admint.player.component.parsexml.parser;

import android.support.annotation.NonNull;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;

import jp.co.digitalcruise.admint.player.component.parsexml.object.ScheduleHeaderObject;

import static java.lang.Integer.parseInt;

public class ScheduleHeaderParser {

    // reboot week day flag
    public static final int REBOOT_FLAG_SUN = 0x00000001;
    public static final int REBOOT_FLAG_MON = 0x00000002;
    public static final int REBOOT_FLAG_TUE = 0x00000004;
    public static final int REBOOT_FLAG_WED = 0x00000008;
    public static final int REBOOT_FLAG_THU = 0x00000010;
    public static final int REBOOT_FLAG_FRI = 0x00000020;
    public static final int REBOOT_FLAG_SAT = 0x00000040;


    public @NonNull
    ScheduleHeaderObject parseHeaderXml(String xml_str) throws IOException, XmlPullParserException {

        // header情報を取得
        ScheduleHeaderObject sched_header = parseHeaderBasicXml(xml_str);

        // reboot_settingは別のパース処理で取得
        ScheduleHeaderObject reboot_header =parseHeaderRebootXml(xml_str);
        sched_header.reboot_immediately = reboot_header.reboot_immediately;
        sched_header.reboot_week_day = reboot_header.reboot_week_day;
        sched_header.reboot_week_time = reboot_header.reboot_week_time;

        return sched_header;
    }

    private @NonNull
    ScheduleHeaderObject parseHeaderBasicXml(String xml_str) throws IOException, XmlPullParserException {
        final String XML_TAG_HEALTH_CHECK_INTERVAL = "health_check_interval";
        final String XML_TAG_AHEAD_LOAD_DATE = "ahead_load_date";
        final String XML_TAG_AHEAD_LOAD_TIME = "ahead_load_time";
        final String XML_TAG_REAL_TIME_CHECK_INTERVAL = "real_time_check_interval";
        final String XML_TAG_UPLOAD_TERMINAL_LOG = "upload_terminal_log";
        final String XML_TAG_UPLOAD_PLAY_COUNT = "upload_play_count";

        XmlPullParser parser;
        StringReader reader = null;
        ScheduleHeaderObject sched_header = new ScheduleHeaderObject();

        try{
            parser = Xml.newPullParser();
            reader = new StringReader(xml_str);
            parser.setInput(reader);

            boolean is_exit = false;

            int eventType = parser.getEventType();
            while(eventType != XmlPullParser.END_DOCUMENT){
                String tag;
                switch(eventType){
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        tag = parser.getName();
                        if(XML_TAG_HEALTH_CHECK_INTERVAL.equals(tag)){
                            sched_header.heath_check_interval = parseInt(parser.nextText());
                        }else if(XML_TAG_AHEAD_LOAD_DATE.equals(tag)){
                            sched_header.ahead_load_date = parseInt(parser.nextText());
                        }else if(XML_TAG_AHEAD_LOAD_TIME.equals(tag)){
                            sched_header.ahead_load_time = parseInt(parser.nextText());
                        }else if(XML_TAG_REAL_TIME_CHECK_INTERVAL.equals(tag)){
                            sched_header.real_time_check_interval = parseInt(parser.nextText());
                        }else if(XML_TAG_UPLOAD_PLAY_COUNT.equals(tag)){
                            sched_header.upload_play_count = parseInt(parser.nextText());
                        }else if(XML_TAG_UPLOAD_TERMINAL_LOG.equals(tag)){
                            sched_header.upload_terminal_log = parseInt(parser.nextText());
                        }else if(ScheduleParser.XML_TAG_SCHEDULE.equals(tag)) {
                            is_exit = true;
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                if(is_exit){
                    break;
                }
                eventType = parser.next();
            }
        }finally{
            if(reader != null){
                reader.close();
            }
        }

        return sched_header;
    }

    private @NonNull
    ScheduleHeaderObject parseHeaderRebootXml(String xml_str) throws IOException, XmlPullParserException {
        final String XML_TAG_REBOOT_SETTING = "reboot_setting";
        final String TAG_IMMEDIATELY = "immediately";
        final String TAG_REGULARLY = "regularly";
        final String TAG_WEEK = "week";
        final String ATTR_WEEK_TIME= "time";
        final String TAG_WEEK_SUN = "sun";
        final String TAG_WEEK_MON = "mon";
        final String TAG_WEEK_THE = "the";
        final String TAG_WEEK_WED = "wed";
        final String TAG_WEEK_THU = "thu";
        final String TAG_WEEK_FRI = "fri";
        final String TAG_WEEK_SAT = "sat";

        XmlPullParser parser;
        StringReader reader = null;
        ScheduleHeaderObject sched_header = new ScheduleHeaderObject();

        try{
            parser = Xml.newPullParser();
            reader = new StringReader(xml_str);
            parser.setInput(reader);

            Integer immediately = null;
            int week_flag = 0;
            Integer week_time = null;

            boolean is_parse_end = false;
            boolean find_reboot_setting = false;
            boolean find_regularly = false;
            boolean find_week = false;

            int eventType = parser.getEventType();
            while(eventType != XmlPullParser.END_DOCUMENT){
                String tag;
                switch(eventType){
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        tag = parser.getName();
                        if(XML_TAG_REBOOT_SETTING.equals(tag)){
                            find_reboot_setting = true;
                        }else if(find_reboot_setting){
                            if(TAG_IMMEDIATELY.equals(tag)){
                                immediately = parseInt(parser.nextText());
                            }else if(TAG_REGULARLY.equals(tag)){
                                find_regularly = true;
                            }else if(find_regularly){
                                if(TAG_WEEK.equals(tag)){
                                    week_time = parseInt(parser.getAttributeValue(null, ATTR_WEEK_TIME));
                                    find_week = true;
                                }else if(find_week){
                                    if(TAG_WEEK_SUN.equals(tag)){
                                        if(parseInt(parser.nextText()) == 1){
                                            week_flag = week_flag | REBOOT_FLAG_SUN;
                                        }
                                    }else if(TAG_WEEK_MON.equals(tag)){
                                        if(parseInt(parser.nextText()) == 1){
                                            week_flag = week_flag | REBOOT_FLAG_MON;
                                        }
                                    }else if(TAG_WEEK_THE.equals(tag)){
                                        if(parseInt(parser.nextText()) == 1){
                                            week_flag = week_flag | REBOOT_FLAG_TUE;
                                        }
                                    }else if(TAG_WEEK_WED.equals(tag)){
                                        if(parseInt(parser.nextText()) == 1){
                                            week_flag = week_flag | REBOOT_FLAG_WED;
                                        }
                                    }else if(TAG_WEEK_THU.equals(tag)){
                                        if(parseInt(parser.nextText()) == 1){
                                            week_flag = week_flag | REBOOT_FLAG_THU;
                                        }
                                    }else if(TAG_WEEK_FRI.equals(tag)){
                                        if(parseInt(parser.nextText()) == 1){
                                            week_flag = week_flag | REBOOT_FLAG_FRI;
                                        }
                                    }else if(TAG_WEEK_SAT.equals(tag)){
                                        if(parseInt(parser.nextText()) == 1){
                                            week_flag = week_flag | REBOOT_FLAG_SAT;
                                        }
                                    }
                                }
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        tag = parser.getName();
                        if(XML_TAG_REBOOT_SETTING.equals(tag)){
                            is_parse_end = true;
                        }
                        break;
                }
                if(is_parse_end){
                    break;
                }
                eventType = parser.next();
            }

            sched_header.reboot_immediately = immediately;
            if(week_time != null){
                sched_header.reboot_week_time = week_time;
                sched_header.reboot_week_day = week_flag;
            }
        }finally{
            if(reader != null){
                reader.close();
            }
        }

        return sched_header;
    }
}

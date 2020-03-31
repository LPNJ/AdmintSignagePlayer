package jp.co.digitalcruise.admint.player.component.parsexml.parser;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;

import jp.co.digitalcruise.admint.player.component.parsexml.object.ScheduleObject;
import jp.co.digitalcruise.admint.player.service.network.HealthCheckService;


public class ScheduleParser {
    // xml schedule
    public static final String XML_TAG_SCHEDULE = "schedule";
    private static final String XML_TAG_DEFAULT = "default";
    private static final String XML_TAG_DIRECT = "direct";
    private static final String XML_TAG_P = "p";
    private static final String XML_TAG_C = "c";
    private static final String XML_TAG_F = "f";
    private static final String XML_TAG_W = "w";

    private static final String XML_TAG_RES_INFO = "i";
    private static final String XML_TAG_TELOP = "telop";
    private static final String XML_TAG_T = "t";

    private class XmlSched{
        static final String ATTR_START = "start";
        static final String ATTR_END = "end";
    }

    private class XmlP{
        static final String ATTR_ST = "st";
        static final String ATTR_PT = "pt";
        static final String ATTR_PO = "po";
        static final String ATTR_UT = "ut";
        static final String ATTR_TCID = "tcid";
        static final String ATTR_TCD = "tcd";
    }

    private class XmlC{
        static final String ATTR_ID = "id";
        static final String ATTR_D = "d";
//        static final String ATTR_O = "o";
        static final String ATTR_T = "t";
        static final String ATTR_U = "u";
//        static final String ATTR_APP = "app";
        static final String ATTR_TCID = "tcid";
        static final String ATTR_TCD = "tcd";
    }

    private class XmlF {
        static final String ATTR_ID = "id";
//        static final String ATTR_EID = "eid";
    }

//    private class XmlW {
//        static final String ATTR_UP = "up";
//    }

    private int getXmlIntegerValue(String value){
        int ret = 0;
        try{
            ret = Integer.parseInt(value);
        }catch(Exception e){
            // nullや整数にならない場合は0を返す
        }
        return ret;
    }

    public ScheduleObject parseScheduleXml(String xml_str) throws IOException, XmlPullParserException {
        XmlPullParser parser;
        StringReader reader = null;
        ScheduleObject sched = new ScheduleObject();

        try {
            parser = Xml.newPullParser();
            reader = new StringReader(xml_str);
            parser.setInput(reader);

            boolean is_sched = false;
            boolean is_exit = false;

            // 番組
            int program_st = 0; // 開始時刻
            // コンテンツ
            int content_id = 0; // コンテンツID
            int content_o = 0;  // オーダー
            // デイリーコンテンツ
            int external_f_o = 0; // オーダー

            int event_type = parser.getEventType();
            while (event_type != XmlPullParser.END_DOCUMENT) {
                String tag;
                switch (event_type) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        tag = parser.getName();
                        if (XML_TAG_SCHEDULE.equals(tag)) {
                            // スケジュール情報
                            is_sched = true;
                            sched.info.update_at = System.currentTimeMillis();
                            sched.info.start_time = Integer.parseInt(parser.getAttributeValue(null, XmlSched.ATTR_START));
                            sched.info.end_time = Integer.parseInt(parser.getAttributeValue(null, XmlSched.ATTR_END));
                        } else if (is_sched) {
                            // 番組
                            if (XML_TAG_P.equals(tag)) {
                                program_st = Integer.parseInt(parser.getAttributeValue(null, XmlP.ATTR_ST));
                                int pt = Integer.parseInt(parser.getAttributeValue(null, XmlP.ATTR_PT));
                                int po = Integer.parseInt(parser.getAttributeValue(null, XmlP.ATTR_PO));
                                // 属性が存在しないケースを想定してgetXmlIntegerValue()関数を通す
                                int tcid = getXmlIntegerValue(parser.getAttributeValue(null, XmlP.ATTR_TCID));
                                int tcd = getXmlIntegerValue(parser.getAttributeValue(null, XmlP.ATTR_TCD));
                                int ut = getXmlIntegerValue(parser.getAttributeValue(null, XmlP.ATTR_UT));

                                ScheduleObject.Program program = sched.new Program();
                                program.st = program_st;
                                program.po = po;
                                program.pt = pt;
                                program.tcd = tcd;
                                program.tcid = tcid;
                                program.ut = ut;
                                sched.programs.add(program);
                            } else if (XML_TAG_C.equals(tag)) {
                                // コンテンツ
                                if (program_st > 0) {
                                    content_id = Integer.parseInt(parser.getAttributeValue(null, XmlC.ATTR_ID));
                                    int d = Integer.parseInt(parser.getAttributeValue(null, XmlC.ATTR_D));
                                    int o = content_o;
                                    int t = Integer.parseInt(parser.getAttributeValue(null, XmlC.ATTR_T));
                                    int u = Integer.parseInt(parser.getAttributeValue(null, XmlC.ATTR_U));
                                    // 属性が存在しないケースを想定してgetXmlIntegerValue()関数を通す
                                    int tcid = getXmlIntegerValue(parser.getAttributeValue(null, XmlC.ATTR_TCID));
                                    int tcd = getXmlIntegerValue(parser.getAttributeValue(null, XmlC.ATTR_TCD));

                                    ScheduleObject.Content content = sched.new Content();

                                    content.id = content_id;
                                    content.d = d;
                                    content.o = o;
                                    content.st = program_st;
                                    content.t = t;
                                    content.tcd = tcd;
                                    content.tcid = tcid;
                                    content.u = u;
                                    sched.contents.add(content);

                                    content_o++; // オーダー順はxmlのo項目は使用せずプログラムで採番する
                                }
                            } else if (XML_TAG_F.equals(tag)) {
                                // デイリーコンテンツ
                                if (content_id > 0) {
                                    int f_id = Integer.parseInt(parser.getAttributeValue(null, XmlF.ATTR_ID));
                                    String f_name = parser.nextText();

                                    ScheduleObject.External external = sched.new External();
                                    external.id = content_id;
                                    external.f_id = f_id;
                                    external.f_o = external_f_o;
                                    external.f_name = f_name;
                                    sched.externals.add(external);

                                    external_f_o++;
                                }
                            } else if (XML_TAG_W.equals(tag)) {
                                // Webview
                                if (content_id > 0) {
//                                    int w_up = Integer.parseInt(parser.getAttributeValue(null, XmlW.ATTR_UP));
                                    String w_url = parser.nextText();

                                    ScheduleObject.Webview webview = sched.new Webview();
                                    webview.id = content_id;
//                                    webview.w_up = w_up;
                                    webview.w_url = w_url;

                                    sched.webviews.add(webview);
                                }
                            }
                        }  else if (tag.equals(XML_TAG_RES_INFO)){
                            ScheduleObject.RescueContent rescue_content = sched.new RescueContent();
                            rescue_content.id = Integer.parseInt(parser.getAttributeValue(null, "id"));
                            rescue_content.type = parser.getAttributeValue(null, "t");
                            rescue_content.duration = parser.getAttributeValue(null, "d");
                            rescue_content.use = parser.getAttributeValue(null, "u");
                            rescue_content.order = parser.getAttributeValue(null, "o");
                            rescue_content.expires = Integer.parseInt(parser.getAttributeValue(null, "expire"));
                            event_type = parser.next();
                            if(event_type == XmlPullParser.TEXT){
                                rescue_content.file_name = parser.getText();
                            }
                            sched.rescue_content.add(rescue_content);
                        }  else if(tag.equals(XML_TAG_TELOP)){
                            ScheduleObject.RescueTelopInfo rescue_telop_info = sched.new RescueTelopInfo();
                            rescue_telop_info.direction = Integer.parseInt(parser.getAttributeValue(null, "direction"));
                            rescue_telop_info.rotate = Integer.parseInt(parser.getAttributeValue(null, "rotate"));
                            sched.rescue_telop_info.add(rescue_telop_info);
                        } else if (tag.equals(XML_TAG_T)) {
                            ScheduleObject.RescueTelop rescue_telop = sched.new RescueTelop();
                            rescue_telop.order = parser.getAttributeValue(null, "o");
                            rescue_telop.expires = Integer.parseInt(parser.getAttributeValue(null, "expire"));

                            event_type = parser.next();
                            if(event_type == XmlPullParser.TEXT){
                                rescue_telop.text = parser.getText();
                            }
                            sched.rescue_telop.add(rescue_telop);
                        }
                        break;
                    case XmlPullParser.TEXT:
                        break;
                    case XmlPullParser.END_TAG:
                        tag = parser.getName();
                        if (is_sched) {
                            if (XML_TAG_SCHEDULE.equals(tag)) {
                                is_exit = true;
                            } else if (XML_TAG_P.equals(tag)) {
                                program_st = 0;
                                content_o = 0;
                            } else if (XML_TAG_C.equals(tag)) {
                                content_id = 0;
                                external_f_o = 0;
                            }
                        }
                        break;
                }

                if (is_exit) {
                    break;
                }
                event_type = parser.next();
            }
        }finally {
            if(reader != null){
                reader.close();
            }
        }

        return sched;
    }

    public ScheduleObject parseDefaultXml(String xml_str) throws IOException, XmlPullParserException {
        XmlPullParser parser;
        StringReader reader = null;
        ScheduleObject sched = new ScheduleObject();

        try {
            parser = Xml.newPullParser();
            reader = new StringReader(xml_str);
            parser.setInput(reader);

            boolean is_default = false;
            boolean is_exit = false;

            // 番組
            int program_st = 0; // 開始時刻
            // コンテンツ
            int content_id = 0; // コンテンツID
            int content_o = 0;  // オーダー
            // デイリーコンテンツ
            int external_f_o = 0; // オーダー

            int event_type = parser.getEventType();
            while (event_type != XmlPullParser.END_DOCUMENT) {
                String tag;
                switch (event_type) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        tag = parser.getName();
                        if (XML_TAG_DEFAULT.equals(tag)) {
                            // スケジュール情報
                            is_default = true;
                            sched.info.sched_type = HealthCheckService.SCHED_TYPE_DEFAULT;
                            sched.info.update_at = System.currentTimeMillis();

                            int tcid = getXmlIntegerValue(parser.getAttributeValue(null, XmlP.ATTR_TCID));
                            int tcd = getXmlIntegerValue(parser.getAttributeValue(null, XmlP.ATTR_TCD));

                            ScheduleObject.Program program = sched.new Program();
                            program.st = 0;
                            program.po = 0;
                            program.pt = 0;
                            program.tcd = tcd;
                            program.tcid = tcid;
                            program.ut = 0;
                            sched.programs.add(program);

                        } else if (is_default) {
                            if (XML_TAG_C.equals(tag)) {
                                // コンテンツ
                                content_id = Integer.parseInt(parser.getAttributeValue(null, XmlC.ATTR_ID));
                                int d = Integer.parseInt(parser.getAttributeValue(null, XmlC.ATTR_D));
                                int o = content_o;
                                int t = Integer.parseInt(parser.getAttributeValue(null, XmlC.ATTR_T));
                                int u = Integer.parseInt(parser.getAttributeValue(null, XmlC.ATTR_U));
                                // 属性が存在しないケースを想定してgetXmlIntegerValue()関数を通す
                                int tcid = getXmlIntegerValue(parser.getAttributeValue(null, XmlC.ATTR_TCID));
                                int tcd = getXmlIntegerValue(parser.getAttributeValue(null, XmlC.ATTR_TCD));

                                ScheduleObject.Content content = sched.new Content();

                                content.id = content_id;
                                content.d = d;
                                content.o = o;
                                content.st = 0;
                                content.t = t;
                                content.tcd = tcd;
                                content.tcid = tcid;
                                content.u = u;
                                sched.contents.add(content);

                                content_o++; // オーダー順はxmlのo項目は使用せずプログラムで採番する
                            } else if (XML_TAG_W.equals(tag)) {
                                // Webview
                                if (content_id > 0) {
//                                    int w_up = Integer.parseInt(parser.getAttributeValue(null, XmlW.ATTR_UP));
                                    String w_url = parser.nextText();

                                    ScheduleObject.Webview webview = sched.new Webview();
                                    webview.id = content_id;
//                                    webview.w_up = w_up;
                                    webview.w_url = w_url;

                                    sched.webviews.add(webview);
                                }
                            }
                        }
                        break;
                    case XmlPullParser.TEXT:
                        break;
                    case XmlPullParser.END_TAG:
                        tag = parser.getName();
                        if (is_default) {
                            if (XML_TAG_DEFAULT.equals(tag)) {
                                is_exit = true;
                            } else if (XML_TAG_C.equals(tag)) {
                                content_id = 0;
                            }
                        }
                        break;
                }

                if (is_exit) {
                    break;
                }
                event_type = parser.next();
            }
        }finally {
            if(reader != null){
                reader.close();
            }
        }

        return sched;
    }

    public ScheduleObject parseDirectXml(String xml_str) throws IOException, XmlPullParserException {
        XmlPullParser parser;
        StringReader reader = null;
        ScheduleObject sched = new ScheduleObject();

        try {
            parser = Xml.newPullParser();
            reader = new StringReader(xml_str);
            parser.setInput(reader);

            boolean is_default = false;
            boolean is_exit = false;

            // 番組
            int program_st = 0; // 開始時刻
            // コンテンツ
            int content_id = 0; // コンテンツID
            int content_o = 0;  // オーダー
            // デイリーコンテンツ
            int external_f_o = 0; // オーダー

            int event_type = parser.getEventType();
            while (event_type != XmlPullParser.END_DOCUMENT) {
                String tag;
                switch (event_type) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        tag = parser.getName();
                        if (tag.equals(XML_TAG_DIRECT)) {
                            // スケジュール情報
                            is_default = true;
                            sched.info.sched_type = HealthCheckService.SCHED_TYPE_HEALTH_CHECK;
                            sched.info.update_at = System.currentTimeMillis();

                            int tcid = getXmlIntegerValue(parser.getAttributeValue(null, XmlP.ATTR_TCID));
                            int tcd = getXmlIntegerValue(parser.getAttributeValue(null, XmlP.ATTR_TCD));

                            ScheduleObject.Program program = sched.new Program();
                            program.st = 0;
                            program.po = 0;
                            program.pt = 0;
                            program.tcd = tcd;
                            program.tcid = tcid;
                            program.ut = 0;
                            sched.programs.add(program);

                        } else if (is_default) {
                            if (tag.equals(XML_TAG_C)) {
                                // コンテンツ
                                content_id = Integer.parseInt(parser.getAttributeValue(null, XmlC.ATTR_ID));
                                int d = Integer.parseInt(parser.getAttributeValue(null, XmlC.ATTR_D));
                                int o = content_o;
                                int t = Integer.parseInt(parser.getAttributeValue(null, XmlC.ATTR_T));
                                int u = Integer.parseInt(parser.getAttributeValue(null, XmlC.ATTR_U));
                                // 属性が存在しないケースを想定してgetXmlIntegerValue()関数を通す
                                int tcid = getXmlIntegerValue(parser.getAttributeValue(null, XmlC.ATTR_TCID));
                                int tcd = getXmlIntegerValue(parser.getAttributeValue(null, XmlC.ATTR_TCD));

                                ScheduleObject.Content content = sched.new Content();

                                content.id = content_id;
                                content.d = d;
                                content.o = o;
                                content.st = 0;
                                content.t = t;
                                content.tcd = tcd;
                                content.tcid = tcid;
                                content.u = u;
                                sched.contents.add(content);

                                content_o++; // オーダー順はxmlのo項目は使用せずプログラムで採番する
                            } else if (XML_TAG_W.equals(tag)) {
                                // Webview
                                if (content_id > 0) {
//                                    int w_up = Integer.parseInt(parser.getAttributeValue(null, XmlW.ATTR_UP));
                                    String w_url = parser.nextText();

                                    ScheduleObject.Webview webview = sched.new Webview();
                                    webview.id = content_id;
//                                    webview.w_up = w_up;
                                    webview.w_url = w_url;

                                    sched.webviews.add(webview);
                                }
                            }
                        }
                        break;
                    case XmlPullParser.TEXT:
                        break;
                    case XmlPullParser.END_TAG:
                        tag = parser.getName();
                        if (is_default) {
                            if (XML_TAG_DEFAULT.equals(tag)) {
                                is_exit = true;
                            } else if (XML_TAG_C.equals(tag)) {
                                content_id = 0;
                            }
                        }
                        break;
                }

                if (is_exit) {
                    break;
                }
                event_type = parser.next();
            }
        }finally {
            if(reader != null){
                reader.close();
            }
        }

        return sched;
    }

}

package jp.co.digitalcruise.admint.player.component.parsexml.parser;

import android.support.annotation.NonNull;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;

import jp.co.digitalcruise.admint.player.component.parsexml.object.GoForItObject;

public class GoForItParser {

    private static final String XML_TAG_DELIVERY_SERVER_URL = "delivery_server_url";
    private static final String XML_TAG_UPLOAD_SERVER_URL = "upload_server_url";
    private static final String XML_TAG_RTC_SERVER_URL = "rtc_server_url";
    private static final String XML_TAG_MANAGE_SERVER_URL = "manage_server_url";
    private static final String XML_TAG_EXTERNAL_SERVER_URL = "ext_server_url";
    private static final String XML_TAG_CDN_SERVER_URL = "cdn_server_url";
    private static final String XML_TAG_RSC_SERVER_URL = "rsc_server_url";

    public static GoForItObject parseGoForIt(@NonNull String xmlstr) throws XmlPullParserException, IOException {
        XmlPullParser parser;
        StringReader reader = null;

        GoForItObject go_for_it = new GoForItObject();

        try{
            parser = Xml.newPullParser();
            reader = new StringReader(xmlstr);
            parser.setInput(reader);

            int eventType = parser.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT){
                String tag;
                switch(eventType){
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        tag = parser.getName();
                        if(XML_TAG_DELIVERY_SERVER_URL.equals(tag)){
                            go_for_it.delivery_server_url = parser.nextText();
                        }else if(XML_TAG_UPLOAD_SERVER_URL.equals(tag)){
                            go_for_it.upload_server_url = parser.nextText();
                        }else if(XML_TAG_RTC_SERVER_URL.equals(tag)){
                            go_for_it.rtc_server_url = parser.nextText();
                        }else if(XML_TAG_MANAGE_SERVER_URL.equals(tag)){
                            go_for_it.manage_server_url = parser.nextText();
                        } else if(XML_TAG_EXTERNAL_SERVER_URL.equals(tag)) {
                            go_for_it.external_sever_url = parser.nextText();
                        } else if(XML_TAG_CDN_SERVER_URL.equals(tag)) {
                            go_for_it.cdn_server_url = parser.nextText();
                        } else if(XML_TAG_RSC_SERVER_URL.equals(tag)){
                            go_for_it.rsc_server_url = parser.nextText();
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = parser.next();
            }
        }finally{
            if(reader != null){
                reader.close();
            }
        }
        return go_for_it;
    }

}

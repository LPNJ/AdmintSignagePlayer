package jp.co.digitalcruise.admint.player.component.parsexml.parser;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

import jp.co.digitalcruise.admint.player.component.parsexml.object.ScheduleObject;

public class RescueFileParser {

    private final static String XML_TAG_FILE = "file";
    public ScheduleObject parseListXml(File xml_str) throws IOException, XmlPullParserException {
        XmlPullParser parser;
        StringReader reader = null;
        ScheduleObject sc_obj = new ScheduleObject();

        if(!xml_str.exists()){
            return sc_obj;
        }

        try{
            parser = Xml.newPullParser();
            reader = new StringReader(xmlReader(xml_str));
            parser.setInput(reader);

            int event_type = parser.getEventType();
            while(event_type != XmlPullParser.END_DOCUMENT){
                String tag;
                switch (event_type) {
                    case XmlPullParser.START_DOCUMENT:
                        break;

                    case XmlPullParser.START_TAG:
                        tag = parser.getName();
                        if(tag.equals(XML_TAG_FILE)){
                            ScheduleObject.RescueContentFile rescue_content_file = sc_obj.new RescueContentFile();
                            rescue_content_file.order = Integer.parseInt(parser.getAttributeValue(null, "o"));
                            event_type = parser.next();
                            if(event_type == XmlPullParser.TEXT){
                                rescue_content_file.content_name = parser.getText();
                            }
                            sc_obj.rescue_content_file.add(rescue_content_file);
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                event_type = parser.next();
            }
        } finally {
            if(reader != null){
                reader.close();
            }
        }
        return sc_obj;
    }

    private String xmlReader(File xml) throws IOException{
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(xml)))) {
            StringBuffer sb = new StringBuffer();
            int c;

            while ((c = br.read()) != -1) {
                sb.append((char) c);
            }
            return sb.toString();
        }
    }
}

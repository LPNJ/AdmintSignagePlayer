package jp.co.digitalcruise.admint.player.component.parsexml.parser;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;

public class TouchContentParser {
    public static LinkedHashMap<String, Integer> parseListXml(String xml_str) throws XmlPullParserException, IOException {
        final String XML_TAG_FILES = "files";
        final String XML_TAG_FILE = "file";
        final String XML_TAG_FILE_ATTR_COMMON = "common";

        LinkedHashMap<String, Integer> material_list = new LinkedHashMap<>();

        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new StringReader(xml_str));

        int eventType = parser.getEventType();
        boolean is_files = false;
        while(eventType != XmlPullParser.END_DOCUMENT){
            String tag;
            switch(eventType){
                case XmlPullParser.START_DOCUMENT:
                    break;
                case XmlPullParser.START_TAG:
                    tag = parser.getName();
                    if(XML_TAG_FILES.equals(tag)){
                        is_files = true;
                    } else if(XML_TAG_FILE.equals(tag)) {
                        if(is_files){
                            int common = Integer.parseInt(parser.getAttributeValue(null, XML_TAG_FILE_ATTR_COMMON));
                            String file_name = parser.nextText();

                            if(file_name != null && file_name.length() > 0) {
                                material_list.put(file_name, common);
                            }
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tag = parser.getName();
                    if(XML_TAG_FILES.equals(tag)){
                        is_files = false;
                    }
                    break;
            }
            eventType = parser.next();
        }

        return material_list;
    }

}

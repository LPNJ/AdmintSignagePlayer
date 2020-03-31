package jp.co.digitalcruise.admint.player.component.parsexml.parser;

import android.support.annotation.NonNull;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;

import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.parsexml.object.ApiResultObject;

public class ApiResultParser {

    public class XmlResult {
        static final String ATTR_MSG = "msg";
        static final String ATTR_STATUS = "status";
        static final String ATTR_CODE = "code";
    }

    static public @NonNull ApiResultObject parseResultXml(String xmlstr) throws IOException, XmlPullParserException {

        final String XML_TAG_RESULT = "result";

        XmlPullParser parser;
        ApiResultObject result = new ApiResultObject();
        StringReader reader = null;
        boolean exist_result = false;
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
                        if(XML_TAG_RESULT.equals(tag)){
                            result.code = Integer.parseInt(parser.getAttributeValue(null, XmlResult.ATTR_CODE));
                            result.status = parser.getAttributeValue(null, XmlResult.ATTR_STATUS);
                            result.msg = parser.getAttributeValue(null, XmlResult.ATTR_MSG);
                            exist_result = true;
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                if(exist_result){
                    break;
                }
                eventType = parser.next();
            }
        }finally{
            if(reader != null){
                try{
                    reader.close();
                }catch(Exception e){
                    Logging.stackTrace(e);
                }
            }
        }
        return result;
    }

}

package jp.co.digitalcruise.admint.player.component.parsexml.parser;

public class XmlUtil {
    public static int getInt(String value){
        int ret = 0;
        try{
            ret = Integer.parseInt(value);
        }catch(Exception e){
            // nullや整数にならない場合は0を返す
        }
        return ret;
    }

    public static long getLong(String value){
        long ret = 0;
        try{
            ret = Long.parseLong(value);
        }catch(Exception e){
            // nullや整数にならない場合は0を返す
        }
        return ret;
    }

}

package jp.co.digitalcruise.admint.player.component.netutil;

public class NetUtilException extends RuntimeException{
    static final int ERROR_CODE_HTTP_RESPONS_ERROR = 1;
    static final int ERROR_CODE_DOWNLOAD_FILE_DIFFERENT_ERROR  = 2;
    static final int ERROR_CODE_UNDEFINED_ERROR = 99;

    private int mErrorCode = 0;
    private int mSubCode = 0;
    private String mMessage = "No Message";

    NetUtilException(int code, int sub_code, String msg){
        mErrorCode = code;
        mSubCode = sub_code;
        mMessage = msg;
    }

    public int getErrorCode(){
        return mErrorCode;
    }

    public int getSubCode(){
        return mSubCode;
    }

    public String getMessage(){
        return mMessage;
    }

}

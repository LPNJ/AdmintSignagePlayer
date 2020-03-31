package jp.co.digitalcruise.admint.player.component.define;

import android.os.Build;

public class DeviceDef {
    public final static String SHARP_BOX = "SC_BX2";
    public final static String TOSHIBA_SHARED_BOARD = "Mozart";
    public final static String SK_BOX = "Sknet-Monopole_mini";
    public final static String SK_POP = "Sknet-Monopole7";
    public final static String GROOVA_STICK = "ITB-5070";
    public final static String GROOVA_BOX = "ITB-5076";
    public final static String HWLD_XM6502 = "Honeywld_XM6502";
    public final static String TOA_STICK = "Rockchip_Rk3328";
    public final static String STRIPE = "MBD101";
    public static String SHUTTLE = "NS02_Series";
    public static String PN_M_SERIES = "PN_M_series";
    public static String PN_B_SERIES = "PN_B_series";
    public static String MITACHI = "BLUECAT";

    //各USBメモリ,SDカードのパス
    public static String SHARP_USB = "/mnt/usb_storage/USB_DISK2";
    public static String SK_USB = "/mnt/usbhost1";
    public static String GROOVA_USB = "/mnt/usb_sda1";
    public static String XM6502_USB ="/storage/usb0";
    public static String SHUTTLE_USB = "/mnt/usb_storage/USB_DISK0/udisk0";
    public final static String IBASE_USB = "/storage/udisk1";
    public final static String PN_M_SERIES_USB = "/mnt/usb_storage/USB_DISK0";

    public static boolean isGroova(){
        return Build.MODEL.equals(GROOVA_STICK) || Build.MODEL.equals(GROOVA_BOX);
    }

    public static boolean isValidTouch(){
        // 端末追加時に検証ができないので否定ベースで判定
        return !Build.MODEL.equals(SK_BOX) && !Build.MODEL.equals(GROOVA_STICK) && !Build.MODEL.equals(GROOVA_BOX) && !Build.MODEL.equals(HWLD_XM6502);
    }

    public static boolean isValidWebview(){
        // 端末追加時に検証ができないので否定ベースで判定
        return !Build.MODEL.equals(TOSHIBA_SHARED_BOARD) && !Build.MODEL.equals(SK_BOX) && !Build.MODEL.equals(SK_POP) && !Build.MODEL.equals(GROOVA_STICK) && !Build.MODEL.equals(GROOVA_BOX);
    }

    public static boolean isValidTouchThetaMovie(){
        // タッチ対応端末かつ
        if(isValidTouch()) {
            // shared_boardでなくsk_popでないなら
            return !Build.MODEL.equals(TOSHIBA_SHARED_BOARD) && !Build.MODEL.equals(SK_POP);
        }
        return false;
    }

    public static boolean isStandAloneInValid(){
        return ( Build.MODEL.equals(SHARP_BOX) || Build.MODEL.equals(PN_M_SERIES) || Build.MODEL.equals(PN_B_SERIES));

    }

    public static String getStoragePath(){
        String ret = "";
        if(Build.MODEL.equals(SHARP_BOX)){
            ret = SHARP_USB;
        } else if(Build.MODEL.equals(SK_BOX) || Build.MODEL.equals(SK_POP)){
            ret = SK_USB;
        } else if(Build.MODEL.equals(HWLD_XM6502)){
            ret = XM6502_USB;
        } else if(isGroova()){
            ret = GROOVA_USB;
        } else if(Build.MODEL.equals(STRIPE)){
            ret = IBASE_USB;
        } else if(Build.MODEL.equals(PN_M_SERIES) || Build.MODEL.equals(PN_B_SERIES)){
            ret = PN_M_SERIES_USB;
        }

        return ret;
    }
}

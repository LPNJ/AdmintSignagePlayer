package jp.co.digitalcruise.admint.player.component.define;

public class UpdaterIntentDef {

    public static final String UPDATER_APPLICATION_ID = "jp.co.digitalcruise.admint.updater";

    private static final String UPDATER_ACTION_PREFIX = UPDATER_APPLICATION_ID + ".UpdaterBroadcastReceiver.";

    public static final String ACTION_SETTING_UPDATE = UPDATER_ACTION_PREFIX + "SETTING_UPDATE";

    public static final String EXTRA_SETTING_PROXY_ENABLE = "setting_proxy_enable";
    public static final String EXTRA_SETTING_PROXY_HOST = "setting_proxy_host";
    public static final String EXTRA_SETTING_PROXY_PORT = "setting_proxy_port";
    public static final String EXTRA_SETTING_PROXY_USER = "setting_proxy_user";
    public static final String EXTRA_SETTING_PROXY_PASSWORD = "setting_proxy_password";

    public static final String ACTION_REBOOT = UPDATER_ACTION_PREFIX + "REBOOT";

    public static final String ACTION_ADJUST_TIME = UPDATER_ACTION_PREFIX + "ADJUST_TIME";

    public static final String ACTION_REGIST = UPDATER_ACTION_PREFIX + "REGIST";

    public static final String EXTRA_TERMINAL_ID = "terminal_id";

    public static final String EXTRA_MANAGE_SERVER_URL ="manage_server_url";

    public static final String EXTRA_SITE_ID = "site_id";

    public static final String EXTRA_AKEY = "akey";

    public static final String EXTRA_SDCARD_DRIVE = "sdcard_drive";

    public static final String EXTRA_DELIVERY_SERVER_URL = "delivery_server_url";

    public static final String EXTRA_TIME_AUTO_SET = "time_auto_set";

    public static final String EXTRA_TIME_HTTP_URL = "time_http_url";

}

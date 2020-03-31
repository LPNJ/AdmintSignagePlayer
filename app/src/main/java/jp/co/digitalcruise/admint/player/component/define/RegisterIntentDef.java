package jp.co.digitalcruise.admint.player.component.define;

public class RegisterIntentDef {
    // レジスターアプリで参照しているので各値は変更しないこと！
    private static final String RECEIVER_PACKAGE_NAME = "jp.co.digitalcruise.admint.player";

    private static final String RECEIVER_CLASS_NAME = RECEIVER_PACKAGE_NAME + "." + "PlayerBroadcastReceiver";

    private static final String ACTION_PREFIX = RECEIVER_CLASS_NAME + ".";

    /**
     * Admint登録情報設定
     */
    public static final String ACTION_REGIST = ACTION_PREFIX + "REGIST";

    /**
     * 端末ID Type Of String
     */
    public static final String EXTRA_TERMINAL_ID = "terminal_id";

    /**
     * 管理サーバURL Type Of String
     */
    public static final String EXTRA_MANAGE_SERVER_URL = "manage_server_url";

    /**
     * サイトID Type Of String
     */
    public static final String EXTRA_SITE_ID = "site_id";

    /**
     * AKEY Type Of String
     */
    public static final String EXTRA_AKEY = "akey";

    /**
     * SDカード(外部メモリ）ルートパス Type Of String
     */
    public static final String EXTRA_SDCARD_DRIVE = "sdcard_drive";

    /**
     * データ配置パス Type Of String
     */
    public static final String EXTRA_USER_STORAGE = "user_storage";

    /**
     * 編集サーバ Type Of String
     */
    public static final String EXTRA_DELIVERY_SERVER_URL = "delivery_server_url";

    /**
     * 自動起動 Type Of Boolean
     */
    public static final String EXTRA_BOOT_START = "boot_start";

    /**
     * Proxy有効 Type Of Boolean
     */
    public static final String EXTRA_PROXY_ENABLE = "proxy_enable";

    public static final String EXTRA_WIFI_REBOOT = "wifi_service";

}

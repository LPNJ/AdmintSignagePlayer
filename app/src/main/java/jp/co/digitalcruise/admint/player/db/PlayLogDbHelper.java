package jp.co.digitalcruise.admint.player.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jp.co.digitalcruise.admint.player.component.log.Logging;

public class PlayLogDbHelper extends AbstractDbHelper {
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "PlayLog";

    private static final String TABLE_NAME_PLAY_COUNT = "play_count";
    private static final String TABLE_NAME_PLAY_LOG = "play_log";
    private static final String TABLE_NAME_TOUCH_LOG = "touch_log";

    public static class TABLE_PLAY_LOG {
        public static String getName(){
            return TABLE_NAME_PLAY_LOG;
        }
        public static final String TIMESTAMP = "timestamp";
        public static final String ID = "id";
        public static final String DATETIME = "datetime";
        public static final String COUNT = "count";
    }

    public static class TABLE_TOUCH_LOG {
        public static String getName(){
            return TABLE_NAME_TOUCH_LOG;
        }
        public static final String TIMESTAMP = "timestamp";
        public static final String DATETIME = "datetime";
        public static final String ID = "id";
        public static final String PATTERN = "pattern";
        public static final String PAGE = "page";
        public static final String ACT = "act";
    }

    public PlayLogDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            if (oldVersion == 1 && newVersion >= 2) {
                upgradeFrom1to2(db);
            }else{
                initializeLocal(db);
            }
        }catch(Exception e){
            initializeLocal(db);
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        initializeLocal(db);
    }

    private void createTables(SQLiteDatabase db) {
        try {
            String sql;
            //再生回数ログ
            sql = "create table if not exists "+ TABLE_NAME_PLAY_LOG + "(" +
                    TABLE_PLAY_LOG.TIMESTAMP + " integer not null," +
                    TABLE_PLAY_LOG.ID + " integer not null," +
                    TABLE_PLAY_LOG.DATETIME + " text," +
                    TABLE_PLAY_LOG.COUNT + " integer default 0," +
                    "unique(" + TABLE_PLAY_LOG.TIMESTAMP + "," + TABLE_PLAY_LOG.ID + ")" +
                    ")";
            db.execSQL(sql);

            //タッチ回数ログ
            sql = "create table if not exists "+ TABLE_NAME_TOUCH_LOG + "(" +
                    TABLE_TOUCH_LOG.TIMESTAMP + " integer not null," +
                    TABLE_TOUCH_LOG.DATETIME + " text," +
                    TABLE_TOUCH_LOG.ID + " integer not null," +
                    TABLE_TOUCH_LOG.PATTERN + " text," +
                    TABLE_TOUCH_LOG.PAGE + " text," +
                    TABLE_TOUCH_LOG.ACT + " text" +
                    ")";
            db.execSQL(sql);

        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void upgradeFrom1to2(SQLiteDatabase db){
        // 既存テーブルからのデータ移行

        // テーブル作成
        createTables(db);

        String sql;

        // play_countテーブルの存在チェック
        sql = "select count(*) from sqlite_master where type='table' and name='"+ TABLE_NAME_PLAY_COUNT + "'";
        Cursor pc_cursor = db.rawQuery(sql, null);
        int existPlayCount = pc_cursor.getCount();
        pc_cursor.close();

        if(existPlayCount > 0){
            // play_countテーブルが存在すればplay_logテーブルにデータ移行
            sql = "select datetime, content_id, count from " + TABLE_NAME_PLAY_COUNT;
            Cursor cursor = db.rawQuery(sql, null);

            // この形式で格納されている
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            while(cursor.moveToNext()){
                try {
                    Date date = sdf.parse(cursor.getString(0));

                    ContentValues values = new ContentValues();

                    values.put(TABLE_PLAY_LOG.TIMESTAMP, date.getTime()); // 日付文字列をUnixTime(msec)に変換し代入
                    values.put(TABLE_PLAY_LOG.ID, cursor.getInt(1));
                    values.put(TABLE_PLAY_LOG.DATETIME, cursor.getString(0));
                    values.put(TABLE_PLAY_LOG.COUNT, cursor.getInt(2));
                    db.insert(TABLE_NAME_PLAY_LOG, null, values);
                } catch (ParseException e) {
                    Logging.stackTrace(e);
                }
            }
            cursor.close();

            sql = "drop table if exists " + TABLE_NAME_PLAY_COUNT;
            db.execSQL(sql);
        }
    }

    private void initializeLocal(SQLiteDatabase db){
        try{
            db.execSQL("drop table if exists " + TABLE_NAME_PLAY_COUNT);
            db.execSQL("drop table if exists " + TABLE_NAME_PLAY_LOG);
            db.execSQL("drop table if exists " + TABLE_NAME_TOUCH_LOG);
        }catch(Exception e){
            Logging.stackTrace(e);
        }
        createTables(db);
    }
}

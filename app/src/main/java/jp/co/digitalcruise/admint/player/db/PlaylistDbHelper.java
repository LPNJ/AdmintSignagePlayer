package jp.co.digitalcruise.admint.player.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import jp.co.digitalcruise.admint.player.component.log.Logging;

public class PlaylistDbHelper extends AbstractDbHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Playlist";

    private static final String TABLE_NAME_PLAY_LIST_INFO = "play_list_info";
    private static final String TABLE_NAME_PLAY_LIST = "play_list";
    private static final String TABLE_NAME_DEFAULT_LIST_INFO = "default_list_info";
    private static final String TABLE_NAME_DEFAULT_LIST = "default_list";

    public static class TABLE_PLAY_LIST_INFO{
        public static String getName(){
            return TABLE_NAME_PLAY_LIST_INFO;
        }
        public static final String UPDATE_AT = "update_at";
        public static final String SCHED_TYPE = "sched_type";
        public static final String ST = "st";
        public static final String UT = "ut";
        public static final String PT = "pt";
        public static final String PO = "po";
    }

    public static class TABLE_PLAY_LIST{
        public static String getName(){
            return TABLE_NAME_PLAY_LIST;
        }
        public static final String ID = "id";
        public static final String O = "o";
        public static final String F_ID = "f_id";
        public static final String F_O = "f_o";
        public static final String T = "t";
        public static final String W_URL = "w_url";
//        public static final String W_UP = "w_up";
        public static final String D = "d";
        public static final String TCID = "tcid";
        public static final String TCD = "tcd";
    }

    public static class TABLE_DEFAULT_LIST_INFO{
        public static String getName(){
            return TABLE_NAME_DEFAULT_LIST_INFO;
        }
        public static final String UPDATE_AT = "update_at";
        public static final String SCHED_TYPE = "sched_type";
        public static final String ST = "st";
        public static final String UT = "ut";
        public static final String PT = "pt";
        public static final String PO = "po";
    }

    public static class TABLE_DEFAULT_LIST{
        public static String getName(){
            return TABLE_NAME_DEFAULT_LIST;
        }
        public static final String ID = "id";
        public static final String O = "o";
        public static final String F_ID = "f_id";
        public static final String F_O = "f_o";
        public static final String T = "t";
        public static final String W_URL = "w_url";
        //        public static final String W_UP = "w_up";
        public static final String D = "d";
        public static final String TCID = "tcid";
        public static final String TCD = "tcd";
    }

    public PlaylistDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        initializeLocal(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        initializeLocal(db);
    }

    private void createTables(SQLiteDatabase db) {

        try {
            String sql;
            // プレイリスト情報
            sql = "create table if not exists " + TABLE_NAME_PLAY_LIST_INFO + "(" +
                    TABLE_PLAY_LIST_INFO.UPDATE_AT + " integer not null," +
                    TABLE_PLAY_LIST_INFO.SCHED_TYPE + " integer not null," +
                    TABLE_PLAY_LIST_INFO.ST + " integer not null," +
                    TABLE_PLAY_LIST_INFO.UT + " integer not null," +
                    TABLE_PLAY_LIST_INFO.PT + " integer not null," +
                    TABLE_PLAY_LIST_INFO.PO + " integer not null" +
                    ")";
            db.execSQL(sql);

            // プレイリスト
            sql = "create table if not exists " + TABLE_NAME_PLAY_LIST + "(" +
                    TABLE_PLAY_LIST.ID + " integer not null," +
                    TABLE_PLAY_LIST.O + " integer not null," +
                    TABLE_PLAY_LIST.F_ID + " integer default 0," +
                    TABLE_PLAY_LIST.F_O + " integer not null," +
                    TABLE_PLAY_LIST.T + " integer not null," +
                    TABLE_PLAY_LIST.W_URL + " text," +
//                    TABLE_PLAY_LIST.W_UP + " integer default 0," +
                    TABLE_PLAY_LIST.D + " integer not null, " +
                    TABLE_PLAY_LIST.TCID + " integer not null," +
                    TABLE_PLAY_LIST.TCD + " integer not null" +
                    ")";
            db.execSQL(sql);


            // デフォルトリスト情報
            sql = "create table if not exists " + TABLE_NAME_DEFAULT_LIST_INFO + "(" +
                    TABLE_PLAY_LIST_INFO.UPDATE_AT + " integer not null," +
                    TABLE_PLAY_LIST_INFO.SCHED_TYPE + " integer not null," +
                    TABLE_PLAY_LIST_INFO.ST + " integer not null," +
                    TABLE_PLAY_LIST_INFO.UT + " integer not null," +
                    TABLE_PLAY_LIST_INFO.PT + " integer not null," +
                    TABLE_PLAY_LIST_INFO.PO + " integer not null" +
                    ")";
            db.execSQL(sql);

            // デフォルトリスト
            sql = "create table if not exists " + TABLE_NAME_DEFAULT_LIST + "(" +
                    TABLE_PLAY_LIST.ID + " integer not null," +
                    TABLE_PLAY_LIST.O + " integer not null," +
                    TABLE_PLAY_LIST.F_ID + " integer default 0," +
                    TABLE_PLAY_LIST.F_O + " integer not null," +
                    TABLE_PLAY_LIST.T + " integer not null," +
                    TABLE_PLAY_LIST.W_URL + " text," +
//                    TABLE_PLAY_LIST.W_UP + " integer default 0," +
                    TABLE_PLAY_LIST.D + " integer not null, " +
                    TABLE_PLAY_LIST.TCID + " integer not null," +
                    TABLE_PLAY_LIST.TCD + " integer not null" +
                    ")";
            db.execSQL(sql);
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void initializeLocal(SQLiteDatabase db){
        try{
            db.execSQL("drop table if exists " + TABLE_NAME_PLAY_LIST_INFO);
            db.execSQL("drop table if exists " + TABLE_NAME_PLAY_LIST);
            db.execSQL("drop table if exists " + TABLE_NAME_DEFAULT_LIST_INFO);
            db.execSQL("drop table if exists " + TABLE_NAME_DEFAULT_LIST);
        }catch(Exception e){
            Logging.stackTrace(e);
        }
        createTables(db);
    }
}
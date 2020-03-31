package jp.co.digitalcruise.admint.player.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import jp.co.digitalcruise.admint.player.component.log.Logging;

public class ScheduleDbHelper extends AbstractDbHelper {

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "Schedule";

    private static final String TABLE_NAME_SCHEDULE_INFO = "schedule_info";
    private static final String TABLE_NAME_SCHEDULE_PROGRAM = "schedule_program";
    private static final String TABLE_NAME_SCHEDULE_CONTENT = "schedule_content";
    private static final String TABLE_NAME_SCHEDULE_EXTERNAL = "schedule_external";
    private static final String TABLE_NAME_SCHEDULE_WEBVIEW = "schedule_webview";
    private static final String TABLE_NAME_SCHEDULE_RESCUE_CONTENT = "schedule_rescue_content";
    private static final String TABLE_NAME_SCHEDULE_RESCUE_TELOP = "schedule_rescue_telop";
    private static final String TABLE_NAME_SCHEDULE_RESCUE_TELOP_INFO = "schedule_rescue_telop_info";

    public static class TABLE_SCHEDULE_INFO{
        public static String getName(){
            return TABLE_NAME_SCHEDULE_INFO;
        }
        public static final String SCHED_TYPE = "sched_type";
        public static final String UPDATE_AT = "update_at";
        public static final String START_TIME = "start_time";
        public static final String END_TIME = "end_time";
    }


    public static class TABLE_SCHEDULE_PROGRAM {
        public static String getName(){
            return TABLE_NAME_SCHEDULE_PROGRAM;
        }
        public static final String SCHED_TYPE = "sched_type";
        public static final String ST = "st";
        public static final String UT = "ut";
        public static final String PT = "pt";
        public static final String PO = "po";
        public static final String TCID = "tcid";
        public static final String TCD = "tcd";
    }

    public static class TABLE_SCHEDULE_CONTENT {
        public static String getName(){
            return TABLE_NAME_SCHEDULE_CONTENT;
        }
        public static final String SCHED_TYPE = "sched_type";
        public static final String ST = "st";
        public static final String O = "o";
        public static final String ID = "id";
        public static final String D = "d";
        public static final String T = "t";
        public static final String U = "u";
        public static final String TCID = "tcid";
        public static final String TCD = "tcd";
    }

    public static class TABLE_SCHEDULE_EXTERNAL {
        public static String getName(){
            return TABLE_NAME_SCHEDULE_EXTERNAL;
        }
        public static final String SCHED_TYPE = "sched_type";
        public static final String ID = "id";
        public static final String F_ID = "f_id";
        public static final String F_O = "f_o";
        public static final String F_FNAME = "f_fname";
    }

    public static class TABLE_SCHEDULE_WEBVIEW {
        public static String getName(){
            return TABLE_NAME_SCHEDULE_WEBVIEW;
        }
        public static final String SCHED_TYPE = "sched_type";
        public static final String ID = "id";
        public static final String W_URL = "w_url";
//        public static final String W_UP = "w_up";
    }

    public static class TABLE_SCHEDULE_RESCUE_CONTENT{
        public static String getName(){return TABLE_NAME_SCHEDULE_RESCUE_CONTENT;}
        public static final String SCHED_TYPE = "sched_type";
        public static final String ID = "id";
        public static final String T = "t";
        public static final String D = "d";
        public static final String U = "u";
        public static final String O ="o";
        public static final String EXPIRE = "expire";
        public static final String FILE_NAME = "file_name";
    }

    public static class TABLE_SCHEDULE_RESCUE_TELOP{
        public static String getName(){return TABLE_NAME_SCHEDULE_RESCUE_TELOP;}
        public static final String O = "o";
        public static final String EXPIRE = "expire";
        public static final String TEXT = "text";
    }

    public static class TABLE_SCHEDULE_RESCUE_TELOP_INFO{
        public static String getName(){return TABLE_NAME_SCHEDULE_RESCUE_TELOP_INFO;}
        public static final String DIRECTION = "direction";
        public static final String ROTATE = "rotate";
    }

    public ScheduleDbHelper(Context context){
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
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){
        initializeLocal(db);
    }


    private void createTables(SQLiteDatabase db){
        String sql;
        // スケジュール取得情報
        sql = "create table if not exists " + TABLE_NAME_SCHEDULE_INFO + "(" +
                TABLE_SCHEDULE_INFO.SCHED_TYPE + " integer not null," +
                TABLE_SCHEDULE_INFO.UPDATE_AT + " integer not null," +
                TABLE_SCHEDULE_INFO.START_TIME + " integer not null," +
                TABLE_SCHEDULE_INFO.END_TIME + " integer not null," +
                "unique("+ TABLE_SCHEDULE_INFO.SCHED_TYPE + ")" +
                ")";
        db.execSQL(sql);

        // 番組
        sql = "create table if not exists " + TABLE_NAME_SCHEDULE_PROGRAM + "(" +
                TABLE_SCHEDULE_PROGRAM.SCHED_TYPE + " integer not null," +
                TABLE_SCHEDULE_PROGRAM.ST + " integer not null," +
                TABLE_SCHEDULE_PROGRAM.UT + " integer not null," +
                TABLE_SCHEDULE_PROGRAM.PT + " integer not null," +
                TABLE_SCHEDULE_PROGRAM.PO + " integer not null," +
                TABLE_SCHEDULE_PROGRAM.TCID + " integer not null," +
                TABLE_SCHEDULE_PROGRAM.TCD + " integer not null," +
                "unique(" + TABLE_SCHEDULE_PROGRAM.SCHED_TYPE + "," + TABLE_SCHEDULE_PROGRAM.ST + ")" +
                ")";
        db.execSQL(sql);

        // コンテンツ
        sql ="create table if not exists " + TABLE_NAME_SCHEDULE_CONTENT + "(" +
                TABLE_SCHEDULE_CONTENT.SCHED_TYPE + " integer not null," +
                TABLE_SCHEDULE_CONTENT.ST + " integer not null," +
                TABLE_SCHEDULE_CONTENT.O + " integer not null," +
                TABLE_SCHEDULE_CONTENT.ID + " integer not null," +
                TABLE_SCHEDULE_CONTENT.D + " integer not null," +
                TABLE_SCHEDULE_CONTENT.T + " integer not null," +
                TABLE_SCHEDULE_CONTENT.U + " integer not null," +
                TABLE_SCHEDULE_CONTENT.TCID + " integer not null," +
                TABLE_SCHEDULE_CONTENT.TCD + " integer not null," +
                "unique("+ TABLE_SCHEDULE_CONTENT.SCHED_TYPE + "," + TABLE_SCHEDULE_CONTENT.ST + ","+ TABLE_SCHEDULE_CONTENT.O + ")" +
                ")";
        db.execSQL(sql);

        // デイリーコンテンツ
        sql = "create table if not exists " + TABLE_NAME_SCHEDULE_EXTERNAL + "(" +
                TABLE_SCHEDULE_EXTERNAL.SCHED_TYPE + " integer not null," +
                TABLE_SCHEDULE_EXTERNAL.ID + " integer not null," +
                TABLE_SCHEDULE_EXTERNAL.F_ID + " integer not null," +
                TABLE_SCHEDULE_EXTERNAL.F_O + " integer not null," +
                TABLE_SCHEDULE_EXTERNAL.F_FNAME + " text not null," +
                "unique(" + TABLE_SCHEDULE_EXTERNAL.SCHED_TYPE + "," + TABLE_SCHEDULE_EXTERNAL.ID +"," + TABLE_SCHEDULE_EXTERNAL.F_ID + "," + TABLE_SCHEDULE_EXTERNAL.F_O + ")" +
                ")";
        db.execSQL(sql);

        // webview
        sql = "create table if not exists " + TABLE_NAME_SCHEDULE_WEBVIEW + "(" +
                TABLE_SCHEDULE_WEBVIEW.SCHED_TYPE + " integer not null," +
                TABLE_SCHEDULE_WEBVIEW.ID + " integer not null," +
                TABLE_SCHEDULE_WEBVIEW.W_URL + " text not null," +
//                TABLE_SCHEDULE_WEBVIEW.W_UP + " integer default 0," +
                "unique(" + TABLE_SCHEDULE_WEBVIEW.SCHED_TYPE + "," + TABLE_SCHEDULE_WEBVIEW.ID + ")" +
                ")";
        db.execSQL(sql);

        //緊急情報
        sql = "create table if not exists " + TABLE_NAME_SCHEDULE_RESCUE_CONTENT + "(" +
                TABLE_SCHEDULE_RESCUE_CONTENT.ID + " integer not null, "+
                TABLE_SCHEDULE_RESCUE_CONTENT.T + " integer not null, " +
                TABLE_SCHEDULE_RESCUE_CONTENT.D + " integer not null, " +
                TABLE_SCHEDULE_RESCUE_CONTENT.U + " integer not null, " +
                TABLE_SCHEDULE_RESCUE_CONTENT.O + " integer not null, " +
                TABLE_SCHEDULE_RESCUE_CONTENT.EXPIRE + " integer not null, " +
                TABLE_SCHEDULE_RESCUE_CONTENT.FILE_NAME + " text not null, " +
                "unique(" + TABLE_SCHEDULE_RESCUE_CONTENT.ID + ", " + TABLE_SCHEDULE_RESCUE_CONTENT.FILE_NAME + ")" +
                ")";
        db.execSQL(sql);

        sql = "create table if not exists " + TABLE_NAME_SCHEDULE_RESCUE_TELOP_INFO + "(" +
                TABLE_SCHEDULE_RESCUE_TELOP_INFO.DIRECTION + " integer not null, " +
                TABLE_SCHEDULE_RESCUE_TELOP_INFO.ROTATE + " integer not null" +
                ")";
        db.execSQL(sql);

        sql = "create table if not exists " + TABLE_NAME_SCHEDULE_RESCUE_TELOP + "(" +
                TABLE_SCHEDULE_RESCUE_TELOP.O + " integer not null," +
                TABLE_SCHEDULE_RESCUE_TELOP.EXPIRE + " integer not null, " +
                TABLE_SCHEDULE_RESCUE_TELOP.TEXT + " text not null" +
                ")";
        db.execSQL(sql);

    }

    private void initializeLocal(SQLiteDatabase db){
        try{
            db.execSQL("drop table if exists " + TABLE_NAME_SCHEDULE_INFO);
            db.execSQL("drop table if exists " + TABLE_NAME_SCHEDULE_PROGRAM);
            db.execSQL("drop table if exists " + TABLE_NAME_SCHEDULE_CONTENT);
            db.execSQL("drop table if exists " + TABLE_NAME_SCHEDULE_EXTERNAL);
            db.execSQL("drop table if exists " + TABLE_NAME_SCHEDULE_WEBVIEW);
            db.execSQL("drop table if exists " + TABLE_NAME_SCHEDULE_RESCUE_CONTENT);
            db.execSQL("drop table if exists " + TABLE_NAME_SCHEDULE_RESCUE_TELOP_INFO);
            db.execSQL("drop table if exists " + TABLE_NAME_SCHEDULE_RESCUE_TELOP);
        }catch(Exception e){
            Logging.stackTrace(e);
        }
        createTables(db);
    }

}

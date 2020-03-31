package jp.co.digitalcruise.admint.player.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import jp.co.digitalcruise.admint.player.component.log.Logging;

public class ViewlistDbHelper extends AbstractDbHelper {

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "Viewlist";

//    private static final String TABLE_NAME_VIEW_LIST_INFO = "view_list_info";
    private static final String TABLE_NAME_VIEW_LIST = "view_list";

//    public static class TABLE_VIEW_LIST_INFO{
//        public static String getName(){
//            return TABLE_NAME_VIEW_LIST_INFO;
//        }
//        public static final String SCHED_TYPE = "sched_type";
//        public static final String UT = "ut";
//        public static final String PO = "po";
//    }

//    public static class TABLE_VIEW_LIST{
//        public static String getName(){
//            return TABLE_NAME_VIEW_LIST;
//        }
//        public static final String ID = "id";
//        public static final String O = "o";
//        public static final String F_ID = "f_id";
//        public static final String F_O = "f_o";
//        public static final String T = "t";
//        public static final String W_URL = "w_url";
//        public static final String W_UP = "w_up";
//        public static final String D = "d";
//        public static final String TCID = "tcid";
//        public static final String TCD = "tcd";
//        public static final String ENABLE = "enable";
//        public static final String TOUCH_ENABLE = "touch_enable";
//    }

    public static class TABLE_VIEW_LIST{
        public static String getName(){
            return TABLE_NAME_VIEW_LIST;
        }
        public static final String ID = "id";
        public static final String O = "o";
        public static final String F_ID = "f_id";
        public static final String T = "t";
        public static final String TCID = "tcid";
    }


    public ViewlistDbHelper(Context context) {
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

    private void createTables(SQLiteDatabase db) {
        String sql;
//        // ビューリスト情報
//        sql = "create table if not exists " + TABLE_NAME_VIEW_LIST_INFO + "(" +
//                TABLE_VIEW_LIST_INFO.SCHED_TYPE + " integer not null," +
//                TABLE_VIEW_LIST_INFO.UT + " integer not null," +
//                TABLE_VIEW_LIST_INFO.PO + " integer not null" +
//                ")";
//        db.execSQL(sql);
//
//        // ビューリスト
//        sql = "create table if not exists " + TABLE_NAME_VIEW_LIST + "(" +
//                TABLE_VIEW_LIST.ID + " integer not null," +
//                TABLE_VIEW_LIST.O + " integer not null," +
//                TABLE_VIEW_LIST.F_ID + " integer default 0," +
//                TABLE_VIEW_LIST.F_O + " integer not null," +
//                TABLE_VIEW_LIST.T + " integer not null," +
//                TABLE_VIEW_LIST.W_URL + " text," +
//                TABLE_VIEW_LIST.W_UP + " integer default 0," +
//                TABLE_VIEW_LIST.D + " integer not null, " +
//                TABLE_VIEW_LIST.TCID + " integer not null," +
//                TABLE_VIEW_LIST.TCD + " integer not null," +
//                TABLE_VIEW_LIST.ENABLE + " integer not null," +
//                TABLE_VIEW_LIST.TOUCH_ENABLE + " integer default 0" +
//                ")";

        // ビューリスト
        sql = "create table if not exists " + TABLE_NAME_VIEW_LIST + "(" +
                TABLE_VIEW_LIST.ID + " integer not null," +
                TABLE_VIEW_LIST.O + " integer not null," +
                TABLE_VIEW_LIST.F_ID + " integer default 0," +
                TABLE_VIEW_LIST.T + " integer not null," +
                TABLE_VIEW_LIST.TCID + " integer not null" +
                ")";

        db.execSQL(sql);
    }

    private void initializeLocal(SQLiteDatabase db){
        try{
//            db.execSQL("drop table if exists " + TABLE_NAME_VIEW_LIST_INFO);
            db.execSQL("drop table if exists " + TABLE_NAME_VIEW_LIST);
        }catch(Exception e){
            Logging.stackTrace(e);
        }
        createTables(db);
    }
}
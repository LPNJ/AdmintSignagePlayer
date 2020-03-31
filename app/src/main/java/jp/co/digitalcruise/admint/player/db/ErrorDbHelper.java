package jp.co.digitalcruise.admint.player.db;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import jp.co.digitalcruise.admint.player.component.log.Logging;

public class ErrorDbHelper extends AbstractDbHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Error";

    private static final String TABLE_NAME_VIEW_ERROR = "view_error";
    private static final String TABLE_NAME_RE_DOWNLOAD = "re_download";
    private static final String TABLE_NAME_BLACK_LIST = "black_list";

    // リトライは２回（なので再生試行自体は初回表示を入れて３回）
    public static final int VIEW_ERROR_RETRY_MAX = 3;

    public static class TABLE_VIEW_ERROR {
        public static String getName(){
            return TABLE_NAME_VIEW_ERROR;
        }
        public static final String ID = "id";
        public static final String F_ID = "f_id";
        public static final String RETRY_COUNT = "retry_count";
    }

    public static class TABLE_RE_DOWNLOAD {
        public static String getName(){
            return TABLE_NAME_RE_DOWNLOAD;
        }
        public static final String ID = "id";
        public static final String F_ID = "f_id";
        public static final String MATERIAL_NAME = "material_name";
        public static final String RETRY_COUNT = "retry_count";
    }

    public static class TABLE_BLACK_LIST {
        public static String getName(){
            return TABLE_NAME_BLACK_LIST;
        }
        public static final String ID = "id";
        public static final String F_ID = "f_id";
        public static final String MATERIAL_NAME = "material_name";
    }

    public ErrorDbHelper(Context context) {
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
            // コンテンツ再生エラー
            sql = "create table if not exists " + TABLE_NAME_VIEW_ERROR + "(" +
                    TABLE_VIEW_ERROR.ID + " integer not null," +
                    TABLE_VIEW_ERROR.F_ID + " integr not null," +
                    TABLE_VIEW_ERROR.RETRY_COUNT + " integer not null," +
                    "unique(" + TABLE_VIEW_ERROR.ID + "," + TABLE_VIEW_ERROR.F_ID + ")" +
                    ")";
            db.execSQL(sql);

            //ダウンロード再試行
            sql = "create table if not exists " + TABLE_NAME_RE_DOWNLOAD + "(" +
                    TABLE_RE_DOWNLOAD.ID + " integer not null," +
                    TABLE_RE_DOWNLOAD.F_ID + " integer not null," +
                    TABLE_RE_DOWNLOAD.MATERIAL_NAME + " text," +
                    TABLE_RE_DOWNLOAD.RETRY_COUNT + " integer not null," +
                    "unique(" + TABLE_RE_DOWNLOAD.ID + "," + TABLE_RE_DOWNLOAD.F_ID + "," + TABLE_RE_DOWNLOAD.MATERIAL_NAME + ")" +
                    ")";

            db.execSQL(sql);

            //ブラックリスト
            sql = "create table if not exists " + TABLE_NAME_BLACK_LIST + "(" +
                    TABLE_BLACK_LIST.ID + " integer not null," +
                    TABLE_BLACK_LIST.F_ID + " integer not null," +
                    TABLE_BLACK_LIST.MATERIAL_NAME + " text," +
                    "unique(" + TABLE_BLACK_LIST.ID + "," + TABLE_BLACK_LIST.F_ID + "," + TABLE_BLACK_LIST.MATERIAL_NAME +")" +
                    ")";
            db.execSQL(sql);
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void initializeLocal(SQLiteDatabase db){
        try{
            db.execSQL("drop table if exists " + TABLE_NAME_VIEW_ERROR);
            db.execSQL("drop table if exists " + TABLE_NAME_RE_DOWNLOAD);
            db.execSQL("drop table if exists " + TABLE_NAME_BLACK_LIST);
        }catch(Exception e){
            Logging.stackTrace(e);
        }
        createTables(db);
    }

    public boolean isValidContent(int id, int f_id){
        SQLiteDatabase rdb = getReaderDb();

        // ViewError
        {
            String where =  ErrorDbHelper.TABLE_VIEW_ERROR.ID + " = " + id + " and " +
                    ErrorDbHelper.TABLE_VIEW_ERROR.F_ID + " = " + f_id + " and " +
                    ErrorDbHelper.TABLE_VIEW_ERROR.RETRY_COUNT + " >= " + VIEW_ERROR_RETRY_MAX;
            long count = DatabaseUtils.queryNumEntries(rdb, ErrorDbHelper.TABLE_VIEW_ERROR.getName(), where);
            if(count > 0){
                return false;
            }
        }

        // BlackList
        {
            String where = ErrorDbHelper.TABLE_BLACK_LIST.ID + " = " + id + " and " +
                    ErrorDbHelper.TABLE_BLACK_LIST.F_ID + " = " + f_id;
            long count = DatabaseUtils.queryNumEntries(rdb, ErrorDbHelper.TABLE_BLACK_LIST.getName(), where);
            if(count > 0){
                return false;
            }
        }
        return true;
    }

}

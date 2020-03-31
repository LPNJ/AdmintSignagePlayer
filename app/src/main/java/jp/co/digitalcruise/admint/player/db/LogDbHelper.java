package jp.co.digitalcruise.admint.player.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import jp.co.digitalcruise.admint.player.component.log.Logging;

public class LogDbHelper extends AbstractDbHelper{
    private static final int DATABASE_VERSION = 3;
    private static final String DATABASE_NAME = "Log";

    private static final String TABLE_NAME_LOG = "log";

    public static class TABLE_LOG{
        public static String getName(){
            return TABLE_NAME_LOG;
        }
        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DATETIME = "datetime";
        public static final String TYPE = "type";
        public static final String TAG = "tag";
        public static final String MSG = "msg";

    }

    public LogDbHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    private void createTables(SQLiteDatabase db ){
        //log
        String sql_log = "create table " + TABLE_NAME_LOG + " (" +
                TABLE_LOG._ID + " integer primary key autoincrement, " +
                TABLE_LOG.TIMESTAMP + " integer default 0, " +
                TABLE_LOG.DATETIME + " text, " +
                TABLE_LOG.TYPE + " integer default 0, " +
                TABLE_LOG.TAG + " text, " +
                TABLE_LOG.MSG + " text" +
                ")";
        db.execSQL(sql_log);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        initializeLocal(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        initializeLocal(db);
    }

    public void insertLog(long timestamp, String datetime, int type, String tag, String msg){
        SQLiteDatabase db = getWriterDb();

        ContentValues values = new ContentValues();
        values.put(TABLE_LOG.TIMESTAMP, timestamp);
        values.put(TABLE_LOG.DATETIME, datetime);
        values.put(TABLE_LOG.TYPE, type);
        values.put(TABLE_LOG.TAG, tag);
        values.put(TABLE_LOG.MSG, msg);

        db.insert(TABLE_LOG.getName(), null, values);
    }

    public void deleteLog(int _id){
        SQLiteDatabase db = getWriterDb();
        String where = TABLE_LOG._ID + " < " + _id;
        db.delete(TABLE_NAME_LOG, where, null);
    }

    public void initialize(){
        initializeLocal(getWriterDb());
    }

    private void initializeLocal(SQLiteDatabase db ){
        try{
            db.execSQL("drop table if exists " + TABLE_NAME_LOG);
        }catch(Exception e){
            Logging.stackTrace(e);
        }
        createTables(db);
    }
}

package jp.co.digitalcruise.admint.player.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

abstract public class AbstractDbHelper extends SQLiteOpenHelper {

    private SQLiteDatabase mWritableDatabase = null;
    private SQLiteDatabase mReadableDatabase = null;

    public AbstractDbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public SQLiteDatabase getReaderDb(){
        if(mReadableDatabase == null){
            mReadableDatabase = this.getReadableDatabase();
        }
        return mReadableDatabase;
    }

    public SQLiteDatabase getWriterDb(){
        if(mWritableDatabase == null){
            mWritableDatabase = this.getWritableDatabase();
        }

        return mWritableDatabase;
    }

    @Override public void close(){
        if(mReadableDatabase != null){
            mReadableDatabase.close();
            mReadableDatabase = null;
        }

        if(mWritableDatabase != null){
            mWritableDatabase.close();
            mWritableDatabase = null;
        }
        super.close();
    }
}

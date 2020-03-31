package jp.co.digitalcruise.admint.player.component.rescue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jp.co.digitalcruise.admint.player.component.CompatibleSdk;
import jp.co.digitalcruise.admint.player.component.file.AdmintPath;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.object.PlayItem;
import jp.co.digitalcruise.admint.player.component.object.PlaylistObject;
import jp.co.digitalcruise.admint.player.component.parsexml.object.ScheduleObject;
import jp.co.digitalcruise.admint.player.component.parsexml.parser.RescueFileParser;
import jp.co.digitalcruise.admint.player.db.ScheduleDbHelper;
import jp.co.digitalcruise.admint.player.service.PlaylistService;
import jp.co.digitalcruise.admint.player.service.network.HealthCheckService;

public class GetRescueDbData {

    //配信する緊急情報のコンテンツを取得した後PlaylistObjectを生成して返す。
    //ID取得→contentsディレクトリの中にあるIDのディレクトリからlist.xmlを読み取る
    //その中に書かれているコンテンツの情報をPlayItemに入れ込む
    public PlaylistObject getEmergencyPlayList(Context context){

        ScheduleDbHelper rdh = null;
        PlaylistObject obj;
        try{
            rdh = new ScheduleDbHelper(context);

            obj = new PlaylistObject();
            obj.sched_type = HealthCheckService.SCHED_TYPE_HEALTH_CHECK;

            SQLiteDatabase rdb = rdh.getReaderDb();

            //Expireの値と桁を合わせる為に1000で割る
            long now_time = (System.currentTimeMillis() / 1000);

            String order = " order by " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.O + " , " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.EXPIRE;
            String where  = " where "  + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.EXPIRE + " > " + now_time;
            String sql = "select " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.ID + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.O + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.D + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.T + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.EXPIRE + " from " +
                    ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.getName() + where + order;

            try(Cursor cursor = rdb.rawQuery(sql, null)){
                while (cursor.moveToNext()){
                    //file name and order parse
                    File list_xml = new File( AdmintPath.getContentsDir() + File.separator + cursor.getInt(0) + File.separator + "list.xml");
                    RescueFileParser parser = new RescueFileParser();
                    ScheduleObject res_file_list = parser.parseListXml(list_xml);
                    for(ScheduleObject.RescueContentFile file : res_file_list.rescue_content_file){
                        PlayItem playItem = new PlayItem();
                        playItem.id = cursor.getInt(0);
                        playItem.o = file.order;
                        playItem.d = cursor.getInt(2);
                        playItem.t = cursor.getInt(3);
                        playItem.media_path = AdmintPath.getContentsDir() + File.separator + cursor.getInt(0) + File.separator + file.content_name;
                        obj.play_items.add(playItem);
                    }
                    long expire = cursor.getLong(4);
                    setRefreshAlarm(expire, context);
                }
            } catch (XmlPullParserException e) {
                Logging.stackTrace(e);
            } catch (IOException e) {
                Logging.stackTrace(e);
            }
        } finally {
            if(rdh != null){
                rdh.close();
            }
        }
        return obj;
    }

    //流すべき緊急情報はありますか？
    public boolean checkEmergencyList(Context context){
        ScheduleDbHelper rdh = null;
        PlaylistObject obj;
        boolean ret = false;
        try{
            rdh = new ScheduleDbHelper(context);

            obj = new PlaylistObject();
            obj.sched_type = HealthCheckService.SCHED_TYPE_HEALTH_CHECK;

            SQLiteDatabase rdb = rdh.getReaderDb();

            long now_time = (System.currentTimeMillis() / 1000);

            String order = " order by " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.O;
            String where  = " where "  + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.EXPIRE + " > " + now_time;
            String sql = "select " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.ID + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.O + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.D + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.T + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.EXPIRE + " from " +
                    ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.getName() + where + order;

            try(Cursor cursor = rdb.rawQuery(sql, null)){
                if (cursor.moveToNext()) {
                    ret = true;
                }
            }
        } finally {
            if(rdh != null){
                rdh.close();
            }
        }
        return ret;
    }

    //このidの緊急情報流してますかー？
    public boolean checkPlaybackViewList(int id, Context context){
        boolean ret = false;
        ScheduleDbHelper sdh = null;
        try{
            sdh = new ScheduleDbHelper(context);

            SQLiteDatabase rdb = sdh.getReaderDb();

            long now_time = (System.currentTimeMillis() / 1000);

            String where  = " where " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.ID + " == " + id + " AND " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.EXPIRE + " > " + now_time;
            String sql = "select " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.ID +
                    " from " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.getName() + where;

            try(Cursor cursor = rdb.rawQuery(sql, null)){
                if (cursor.moveToNext()) {
                    if(id == cursor.getInt(0)){
                        ret = true;
                    }
                }
            }
        } finally {
            if(sdh != null){
                sdh.close();
            }
        }
        return ret;
    }

    public StringBuilder getEmergencyScheduleContents(Context context){
        ScheduleDbHelper rdh = null;
        PlaylistObject obj;
        StringBuilder builder = new StringBuilder();
        try{
            rdh = new ScheduleDbHelper(context);

            obj = new PlaylistObject();
            obj.sched_type = HealthCheckService.SCHED_TYPE_HEALTH_CHECK;

            SQLiteDatabase rdb = rdh.getReaderDb();

            //Expireの値と桁を合わせる為に1000で割る
            long now_time = (System.currentTimeMillis() / 1000);

            String order = " order by " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.O + " , " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.EXPIRE;
            String where  = " where "  + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.EXPIRE + " > " + now_time;
            String sql = "select " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.O + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.ID + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.D + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.EXPIRE + " from " +
                    ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.getName() + where + order;

            try(Cursor cursor = rdb.rawQuery(sql, null)) {
                while (cursor.moveToNext()) {
                    builder.append(cursor.getInt(0)).append(",");
                    builder.append(cursor.getInt(1)).append(",");
                    builder.append(cursor.getInt(2)).append("sec,");
                    builder.append(cursor.getLong(3)).append(":");
                }
            }
        } finally {
            if(rdh != null){
                rdh.close();
            }
        }
        return builder;
    }

    //テロップがどの向きで表示されるべきなのかの情報を取得
    public int getRescueTelopInfo(Context context){

        int rotation = -1;

        ScheduleDbHelper resdb = null;
        try{
            resdb = new ScheduleDbHelper(context);
            SQLiteDatabase rdb = resdb.getReaderDb();

            String sql = "select " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP_INFO.ROTATE + " from " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP_INFO.getName();

            try(Cursor cursor = rdb.rawQuery(sql, null)){
                while (cursor.moveToNext()){
                    rotation = cursor.getInt(0);
                }
            }
        } finally {
            if(resdb != null){
                resdb.close();
            }
        }
        return rotation;
    }

    //テロップに表示するテキストの取得
    public StringBuilder getTelopText(Context context){
        ScheduleDbHelper resdb = null;
        StringBuilder telop_text;
        try {
            resdb = new ScheduleDbHelper(context);
            SQLiteDatabase rdb = resdb.getReaderDb();
            ArrayList<String> telop_list = new ArrayList<>();
            long expire;
            long now = System.currentTimeMillis() / 1000;

            String order = " order by " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP.O;
            String where = " where " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP.EXPIRE + " > " + now;
            String telop_data_sql = "select " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP.TEXT + ", "
                    + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP.EXPIRE + " from " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP.getName()
                    + where
                    + order;

            try (Cursor cursor = rdb.rawQuery(telop_data_sql, null)) {
                while (cursor.moveToNext()) {
                    expire = cursor.getLong(1);
                    //二重チェック
                    if (expire > now) {
                        telop_list.add(cursor.getString(0));
                    }
                    setRefreshAlarm(expire, context);
                }
            }

            //表示できるものが無いのでさよなら
            if (telop_list.size() == 0) {
                return null;
            }

            //取得したテロップが複数あった場合はつなげて表示する
            telop_text = new StringBuilder();
            for (String item : telop_list) {
                telop_text.append(item);
                telop_text.append(" ").append(" ").append(" ").append(" ");
            }
        } finally {
            if(resdb != null){
                resdb.close();
            }
        }
        return telop_text;
    }

    //テロップや緊急コンテンツが終了した際に作動するアラームを設定する
    //時間が来たらPlaylistをリフレッシュ
    private void setRefreshAlarm(long expire, Context context){
        Intent intent = new Intent(context, PlaylistService.class);
        intent.setType(String.valueOf(expire));
        intent.setAction(PlaylistService.ACTION_REFRESH_PLAYLIST);
        PendingIntent pending = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        CompatibleSdk.setAlarmEx(context, expire * 1000, pending);
    }

//    private void setTelopAlarm(long expire, Context context){
//        Intent intent = new Intent(context, PlaylistService.class);
//        intent.setType(String.valueOf(expire));
//        intent.setAction(PlaylistService.ACTION_CHECK_UPDATE);
//        PendingIntent pending = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
//
//        CompatibleSdk.setAlarmEx(context, expire * 1000, pending);
//    }

//    //まだ実行されていないアラームのキャンセル
//    //途中で緊急情報が無くなった時に実行するそんな雰囲気
//    public void cancelRefreshAlarm(Context context){
//        Intent intent = new Intent(context, PlaylistService.class);
//
//        ScheduleDbHelper resdb = null;
//        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
//        try {
//            resdb = new ScheduleDbHelper(context);
//            SQLiteDatabase rdb = resdb.getReaderDb();
//            long expire;
//            long now = System.currentTimeMillis() / 1000;
//
//            String order = " order by " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.O;
//            String where = " where " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.EXPIRE + " > " + now;
//            String telop_data_sql = "select " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_CONTENT.EXPIRE + " from " + ScheduleDbHelper.TABLE_SCHEDULE_RESCUE_TELOP.getName()
//                    + where
//                    + order;
//
//            try (Cursor cursor = rdb.rawQuery(telop_data_sql, null)) {
//                while (cursor.moveToNext()) {
//                    expire = cursor.getLong(0);
//                    intent.setType(String.valueOf(expire));
//                    PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
//
//                    pendingIntent.cancel();
//                    assert alarmManager != null;
//                    alarmManager.cancel(pendingIntent);
//
//                }
//            }
//        } finally {
//            if(resdb != null){
//                resdb.close();
//            }
//        }
//
//
//    }

}

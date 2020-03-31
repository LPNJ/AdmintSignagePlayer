package jp.co.digitalcruise.admint.player.activity.main;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.activity.AbstractAdmintActivity;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.component.rescue.GetRescueDbData;
import jp.co.digitalcruise.admint.player.db.ErrorDbHelper;
import jp.co.digitalcruise.admint.player.db.ScheduleDbHelper;
import jp.co.digitalcruise.admint.player.pref.HealthCheckPref;
import jp.co.digitalcruise.admint.player.pref.UpdaterPref;
import jp.co.digitalcruise.admint.player.service.network.HealthCheckService;

import static jp.co.digitalcruise.admint.player.service.network.HealthCheckService.CONTENT_TYPE_EXTERNAL_MOVIE;
import static jp.co.digitalcruise.admint.player.service.network.HealthCheckService.CONTENT_TYPE_EXTERNAL_PICTURE;
import static jp.co.digitalcruise.admint.player.service.network.HealthCheckService.CONTENT_TYPE_MOVIE;
import static jp.co.digitalcruise.admint.player.service.network.HealthCheckService.CONTENT_TYPE_ORIGINAL;
import static jp.co.digitalcruise.admint.player.service.network.HealthCheckService.CONTENT_TYPE_PICTURE;
import static jp.co.digitalcruise.admint.player.service.network.HealthCheckService.CONTENT_TYPE_THEATA;
import static jp.co.digitalcruise.admint.player.service.network.HealthCheckService.CONTENT_TYPE_TOUCH;
import static jp.co.digitalcruise.admint.player.service.network.HealthCheckService.CONTENT_TYPE_WEBVIEW;

public class ScheduleActivity extends AbstractAdmintActivity {

    private class ViewingTask extends AsyncTask<Void, Void, LinearLayout[]>{
        private Context mContext;
        private ScrollView mScrollView;
        ViewingTask(Context context,  ScrollView scrollView){
            mContext = context;
            mScrollView = scrollView;
        }

        private void setHeaderInfo(LinearLayout tb_layout, ViewGroup.LayoutParams param) {

            try {
                TextView value = new TextView(mContext);
                value.setSingleLine(false);

                // ヘルスチェック情報取得
                long latest_server_check_time = System.currentTimeMillis();
                long next_health_check = HealthCheckPref.getNextHealthCheckTime();
                long next_ahead = HealthCheckPref.getNextAheadLoadTime();
                int get_ahead_date = HealthCheckPref.getAheadLoadDate();
                int get_ahead_time = HealthCheckPref.getAheadLoadTime();
                int health_check_interval = HealthCheckPref.getHealthCheckInterval();
                long reboot_time = UpdaterPref.getRebootAlarmTime();

                String label_next_health_check_time = getDatetimeString(next_health_check);
                String label_next_ahead_load_time = getDatetimeString(next_ahead);
                String label_ahead_load_time = getTimeString(get_ahead_time);

                String label_latest_server_check_time = getDatetimeString(latest_server_check_time);
                String label_reboot_alarm_time = getDatetimeString(reboot_time);

                String set_text =
                        getString(R.string.schedule_label_latest_server_check_time) + " " + label_latest_server_check_time+ "\n" +
                                getString(R.string.schedule_label_next_health_check_time) + " " + label_next_health_check_time + "\n" +
                                getString(R.string.schedule_label_health_check_interval) + " " + health_check_interval + "\n" +
                                getString(R.string.schedule_label_next_ahead_load_time) + " " + label_next_ahead_load_time + "\n" +
                                getString(R.string.schedule_label_ahead_load_date) + " " + get_ahead_date + "\n" +
                                getString(R.string.schedule_label_ahead_load_time) + " " + label_ahead_load_time + "\n" +
                                getString(R.string.schedule_label_reboot_alarm_time) + " " + label_reboot_alarm_time + "\n";

                value.setText(set_text);
                tb_layout.addView(value, param);
            } catch (Exception e) {
                Logging.stackTrace(e);
            }
        }

        @Override
        protected LinearLayout[] doInBackground(Void... params) {
            LinearLayout layout[] = new LinearLayout[6];
            try{

                LinearLayout rescue_layout = createRescueLayout();
                LinearLayout health_check_layout = createScheduleLayout(HealthCheckService.SCHED_TYPE_HEALTH_CHECK);
                LinearLayout ahead_layout = createScheduleLayout(HealthCheckService.SCHED_TYPE_AHEAD_LOAD);
                LinearLayout sdcard_layout = createScheduleLayout(HealthCheckService.SCHED_TYPE_SD_CARD);
                LinearLayout default_layout = createScheduleLayout(HealthCheckService.SCHED_TYPE_DEFAULT);
                LinearLayout blacklist_layout = createBlacklistLayout();

                layout[0] = rescue_layout;
                layout[1] = health_check_layout;
                layout[2] = ahead_layout;
                layout[3] = sdcard_layout;
                layout[4] = default_layout;
                layout[5] = blacklist_layout;
            }catch(Exception e ){
                Logging.stackTrace(e);
            }
            return layout;
        }

        @Override
        protected void onPostExecute(LinearLayout[] layout) {
            try {
                LinearLayout parent_layout = new LinearLayout(getApplicationContext());

                ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);

                parent_layout.setOrientation(LinearLayout.VERTICAL);

                setHeaderInfo(parent_layout, param);

                for (LinearLayout aLayout : layout) {
                    if (aLayout != null) {
                        parent_layout.addView(aLayout, param);
                    }
                }

                if (mScrollView != null) {

                    mScrollView.removeAllViews();

                    param = new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                    mScrollView.addView(parent_layout, param);
                }
            } catch (Exception e) {
                Logging.stackTrace(e);
            }
        }

        private LinearLayout createRescueLayout() {
            String rescue_title_text = "Emergency contents";

            LinearLayout rescue_schedule_layout = new LinearLayout(mContext);
            ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            rescue_schedule_layout.setOrientation(LinearLayout.VERTICAL);

            GetRescueDbData rescue_data = new GetRescueDbData();

            LinearLayout rescue_list_title = new LinearLayout(mContext);
            TextView title_text = new TextView(mContext);
            title_text.setTextColor(Color.YELLOW);
            title_text.setTypeface(Typeface.DEFAULT_BOLD);
            title_text.setText(rescue_title_text);
            rescue_list_title.addView(title_text);
            rescue_schedule_layout.addView(rescue_list_title, param);

            StringBuilder builder = rescue_data.getEmergencyScheduleContents(ScheduleActivity.this);

            TableLayout rescue_tb_layout = new TableLayout(mContext);


            if(rescue_data.checkEmergencyList(mContext)){

                String[] list = builder.toString().split(":");//複数の緊急コンテンツがある場合に働く
                for(String item_list : list){
                    TableRow rescue_tb_row = new TableRow(mContext);
                    String[] item = item_list.split(",");//１コンテンツの情報 order, id, duration, expireの順に分割
                    for(int i = 0; i <= item.length-1; i++){
                        TextView content_info = new TextView(mContext);
                        content_info.setPadding(0,0,10,0);
                        if(i == 3){
                            long expire_time = Long.parseLong(item[3]) * 1000;
                            StringBuffer expire = new StringBuffer(getDatetimeString(expire_time));
                            content_info.setText(expire);//expireだけフォーマットを変換しているので指定追加
                        } else {
                            content_info.setText(item[i]);
                        }
                        rescue_tb_row.addView(content_info);
                    }
                    rescue_tb_layout.addView(rescue_tb_row);
                }
            } else {
                TextView content_info = new TextView(mContext);
                content_info.setText(R.string.no_data);
                rescue_schedule_layout.addView(content_info);
            }
            rescue_schedule_layout.addView(rescue_tb_layout, param);
            Space sp = new Space(mContext);
            rescue_schedule_layout.addView(sp, new LinearLayout.LayoutParams(1, 20));
            return rescue_schedule_layout;
        }

        private LinearLayout createScheduleLayout(int sched_type) {

            LinearLayout sched_layout;

            sched_layout = new LinearLayout(mContext);
            ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            sched_layout.setOrientation(LinearLayout.VERTICAL);

            ScheduleDbHelper dbh = null;
            SQLiteDatabase db = null;

            String p_tbl = ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.getName();
            String c_tbl = ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.getName();

            LinearLayout sched_type_title = new LinearLayout(mContext);
            TextView title_text = new TextView(mContext);
            title_text.setTextColor(Color.YELLOW);
            title_text.setTypeface(Typeface.DEFAULT_BOLD);
            String title_str;
            String scheduleType;
            switch (sched_type) {
                case HealthCheckService.SCHED_TYPE_HEALTH_CHECK:
                    scheduleType = "sched_type=" + HealthCheckService.SCHED_TYPE_HEALTH_CHECK;
                    title_str = "Schedule Health Check";
                    break;
                case HealthCheckService.SCHED_TYPE_AHEAD_LOAD:
                    scheduleType = "sched_type=" + HealthCheckService.SCHED_TYPE_AHEAD_LOAD;
                    title_str = "Schedule Ahead Load";
                    break;
                case HealthCheckService.SCHED_TYPE_SD_CARD:
                    scheduleType = "sched_type=" + HealthCheckService.SCHED_TYPE_SD_CARD;
                    title_str = "Schedule SD Card";
                    break;
                default:
                    scheduleType = "sched_type=" + HealthCheckService.SCHED_TYPE_DEFAULT;
                    title_str = "Schedule Default";
                    break;
            }

//            Cursor c_cursor = null;

            dbh = new ScheduleDbHelper(mContext);
            db = dbh.getReadableDatabase();

            String p_build = ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.ST + " + " + ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.PT + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_PROGRAM.TCID;

            String i_build = ScheduleDbHelper.TABLE_SCHEDULE_INFO.START_TIME + ", " +
                    ScheduleDbHelper.TABLE_SCHEDULE_INFO.END_TIME;

            StringBuffer sb;
//            String p_sql = "select st , st + pt, tcid  from " + p_tbl + " where " + scheduleType + " order by st, po";
            String p_sql = "select " + p_build + " from " + p_tbl + " where " + scheduleType + " order by st, po";
            sched_layout.addView(sched_type_title, param);
            sched_type_title.addView(title_text);
//            String s_sql = "select start_time, end_time from " + ScheduleDbHelper.TABLE_SCHEDULE_INFO.getName() + " where " + scheduleType;
            String s_sql = "select " + i_build + " from " + ScheduleDbHelper.TABLE_SCHEDULE_INFO.getName() + " where " + scheduleType;

            try (Cursor p_cursor = db.rawQuery(p_sql, null); Cursor s_cursor = db.rawQuery(s_sql, null)) {
                if (s_cursor.getCount() > 0) {
                    s_cursor.moveToFirst();
                    sb = new StringBuffer(title_str + "(" + getDatetimeString(s_cursor.getLong(0)) + " - " + getDatetimeString(s_cursor.getLong(1)) + ")");
                    title_text.setText(sb);
                } else {
                    title_text.setText(title_str);
                }
                // program カーソル取得
                if (p_cursor.getCount() == 0) {
                    LinearLayout data_none = new LinearLayout(mContext);
                    TextView none_text = new TextView(mContext);
                    none_text.setText(R.string.no_data);
                    data_none.addView(none_text);
                    sched_layout.addView(data_none, param);

                    Space sp = new Space(mContext);
                    sched_layout.addView(sp, new LinearLayout.LayoutParams(1, 30));

                } else {
                    //番組が登録されていたら
                    while (p_cursor.moveToNext() && !this.isCancelled()) {
                        long st = p_cursor.getLong(0);

                        LinearLayout prg_row1 = new LinearLayout(mContext);

                        TextView prg_time = new TextView(mContext);

                        prg_time.setPadding(0, 0, 20, 0);
                        prg_time.setTypeface(Typeface.DEFAULT_BOLD);

                        if (st > 0) {
                            sb = new StringBuffer(getDatetimeString(st) + " - " + getDatetimeString(p_cursor.getLong(1)));
                        } else {
                            sb = new StringBuffer("indefinite");
                        }
                        prg_time.setText(sb);
                        prg_row1.addView(prg_time);

                        TextView prg_tcid = new TextView(mContext);
                        prg_tcid.setSingleLine(false);
                        sb = new StringBuffer("tcid:" + p_cursor.getString(2));
                        prg_tcid.setText(sb);
                        prg_row1.addView(prg_tcid);

                        sched_layout.addView(prg_row1, param);

                        TableLayout tb_layout = new TableLayout(mContext);

//                        String c_sql = "select  o, id, d, t, u, tcid from " + c_tbl + " where st = " + st + " AND " + scheduleType + " order by st, o, id";
                        String c_build = ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.O + ", " +
                                ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.ID + ", " +
                                ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.D + ", " +
                                ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.T + ", " +
                                ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.U + ", " +
                                ScheduleDbHelper.TABLE_SCHEDULE_CONTENT.TCID;
                        String c_sql = "select " + c_build + " from " + c_tbl + " where st = " + st + " AND " + scheduleType + " order by st, o, id";

                        try (Cursor c_cursor = db.rawQuery(c_sql, null)) {
//                            String c_sql = "select  o, id, d, t, u, tcid from " + c_tbl + " where st = " + st + " AND " + scheduleType + " order by st, o, id";

                            // program カーソル取得
//                            c_cursor = db.rawQuery(c_sql, null);

                            while (c_cursor.moveToNext() && !this.isCancelled()) {
                                TableRow c_row = new TableRow(mContext);

                                // o
                                TextView o_column = new TextView(mContext);
                                o_column.setPadding(0, 0, 10, 0);
                                o_column.setText(c_cursor.getString(0));
                                c_row.addView(o_column);

                                // id
                                TextView id_column = new TextView(mContext);
                                id_column.setPadding(0, 0, 10, 0);
                                id_column.setText(c_cursor.getString(1));
                                c_row.addView(id_column);

                                // d
                                TextView d_column = new TextView(mContext);
                                d_column.setPadding(0, 0, 10, 0);
                                int d_sec = c_cursor.getInt(2);
                                sb = new StringBuffer(d_sec + "sec");
                                d_column.setText(sb);
                                c_row.addView(d_column);

                                // t
                                TextView t_column = new TextView(mContext);
                                t_column.setPadding(0, 0, 10, 0);
                                String str_type;

                                switch (c_cursor.getInt(3)) {
                                    case CONTENT_TYPE_MOVIE:
                                        str_type = "movie";
                                        break;
                                    case CONTENT_TYPE_PICTURE:
                                        str_type = "image";
                                        break;
                                    case CONTENT_TYPE_TOUCH:
                                        str_type = "touch";
                                        break;
                                    case CONTENT_TYPE_THEATA:
                                        str_type = "theta_image";
                                        break;
                                    case CONTENT_TYPE_WEBVIEW:
                                        str_type = "web";
                                        break;
                                    case CONTENT_TYPE_ORIGINAL:
                                        str_type = "original";
                                        break;
                                    case CONTENT_TYPE_EXTERNAL_MOVIE:
                                        str_type = "daily_movie";
                                        break;
                                    case CONTENT_TYPE_EXTERNAL_PICTURE:
                                        str_type = "daily_image";
                                        break;
                                    default:
                                        str_type = "unknown";
                                }

                                t_column.setText(str_type);
                                c_row.addView(t_column);

                                // u
                                TextView u_column = new TextView(mContext);
                                u_column.setPadding(0, 0, 10, 0);
                                String str_use = "use";
                                if (c_cursor.getInt(4) == 0) {
                                    str_use = "not_use";
                                }
                                u_column.setText(str_use);
                                c_row.addView(u_column);


                                // tcid
                                TextView tcid_column = new TextView(mContext);
                                tcid_column.setPadding(0, 0, 10, 0);
                                sb = new StringBuffer("tcid:" + c_cursor.getString(5));
                                tcid_column.setText(sb);
                                c_row.addView(tcid_column);

                                tb_layout.addView(c_row, param);
                            }
                        } catch (Exception e) {
                            Logging.stackTrace(e);
                        }
                        sched_layout.addView(tb_layout, param);
                        Space sp = new Space(mContext);
                        sched_layout.addView(sp, new LinearLayout.LayoutParams(1, 20));

                    }
                }
            } catch (Exception e) {
                Logging.stackTrace(e);
            } finally {
                if (dbh != null) {
                    dbh.close();
                }
            }

            return sched_layout;
        }

        private String getDatetimeString(long timestamp) {

            if (timestamp <= 0) {
                return Long.toString(timestamp);
            }

            Date date = new Date(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            return sdf.format(date);
        }

        private String getTimeString(int time) {
            String ret = "";
            try {
                if (time < 0) {
                    ret = "UnSetting";
                } else {
                    int hour = (int) Math.floor((double) time / 3600f);
                    int min = (int) Math.floor((time % 3600) / 60);

                    String str_hour = Integer.toString(hour);
                    if (hour < 10) {
                        str_hour = "0" + Integer.toString(hour);
                    }

                    String str_min = Integer.toString(min);
                    if (min < 10) {
                        str_min = "0" + Integer.toString(min);
                    }

                    ret = str_hour + ":" + str_min + "(" + time + ")";
                }
            } catch (Exception e) {
                Logging.stackTrace(e);
            }
            return ret;
        }

        private LinearLayout createBlacklistLayout() {
            String black_contents = "Blacklist Contents";
            LinearLayout blacklist_layout;

            blacklist_layout = new LinearLayout(mContext);
            ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            blacklist_layout.setOrientation(LinearLayout.VERTICAL);

            ErrorDbHelper dbh = null;
            SQLiteDatabase db = null;

            LinearLayout blacklist_title = new LinearLayout(mContext);
            TextView title_text = new TextView(mContext);
            title_text.setTextColor(Color.YELLOW);
            title_text.setTypeface(Typeface.DEFAULT_BOLD);
            title_text.setText(black_contents);

            blacklist_title.addView(title_text);
            blacklist_layout.addView(blacklist_title, param);

            dbh = new ErrorDbHelper(mContext);
            db = dbh.getReadableDatabase();

            String select = "select " + ErrorDbHelper.TABLE_BLACK_LIST.ID + " from ";

            String sql = select + ErrorDbHelper.TABLE_BLACK_LIST.getName();
            try (Cursor cursor = db.rawQuery(sql, null)){

                if (cursor.getCount() == 0) {
                    LinearLayout data_none = new LinearLayout(mContext);
                    TextView none_text = new TextView(mContext);
                    none_text.setText(R.string.no_data);
                    data_none.addView(none_text);
                    blacklist_layout.addView(data_none, param);
                } else {
                    while(cursor.moveToNext()){
                        int id = cursor.getInt(0);
                        TextView id_view = new TextView(mContext);
                        id_view.setText(String.valueOf(id));
                        blacklist_layout.addView(id_view, param);
                    }
                }
            } catch (Exception e) {
                Logging.stackTrace(e);
            } finally {
                dbh.close();
            }
            Space sp = new Space(mContext);
            blacklist_layout.addView(sp, new LinearLayout.LayoutParams(1, 20));


            return blacklist_layout;
        }

    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try{
            setContentView(R.layout.schedule_info);
        } catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try{
            refreshView();
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try{
            finish();
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private void refreshView(){
        try{
            ScrollView scrollView = findViewById(R.id.sched_scroll);
            scrollView.removeAllViews();

            ViewingTask viewingTask = new ViewingTask(this, scrollView);
            viewingTask.execute();
        }catch (Exception e ){
            Logging.stackTrace(e);
        }
    }
}

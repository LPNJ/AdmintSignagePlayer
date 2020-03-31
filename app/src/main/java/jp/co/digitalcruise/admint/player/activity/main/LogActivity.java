package jp.co.digitalcruise.admint.player.activity.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.activity.AbstractAdmintActivity;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.db.LogDbHelper;
import jp.co.digitalcruise.admint.player.service.LoggingService;

public class LogActivity extends AbstractAdmintActivity {

    private static final int LATEST_LOG_LOWS = 100;
    private static final int MAX_LOG_ROWS = 5000;

    private ViewingTask mViewingTask = null;

    class DoInBackgroundParams {
        int row = 0;
        LinearLayout layout = null;
    }

    @SuppressLint("StaticFieldLeak")
    public class ViewingTask extends AsyncTask<DoInBackgroundParams, Void, LinearLayout>{
        private Context mContext = null;
        private ScrollView mScrollView = null;

        ViewingTask(Context context, ScrollView scroll_view){
            mContext = context;
            mScrollView = scroll_view;
        }

        @Override
        protected LinearLayout doInBackground(DoInBackgroundParams... params) {
            LinearLayout tb_layout = null;
            try{
                int rows = 0;
                if(params[0] != null){
                    rows = params[0].row;
                }

                tb_layout = params[0] != null ? params[0].layout : null;

                ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);

                setDbLog(rows, tb_layout, param);
            } catch (Exception e){
                Logging.stackTrace(e);
            }
            return  tb_layout;
        }

        @Override
        protected void onPostExecute(LinearLayout tb_layout) {
            try {
                if (mScrollView != null) {

                    mScrollView.removeAllViews();

                    ViewGroup.LayoutParams param = new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                    mScrollView.addView(tb_layout, param);
                }
            } catch (Exception e){
                Logging.stackTrace(e);
            }
        }

        @SuppressLint("Recycle")
        private void setDbLog(int max_row, LinearLayout tb_layout, ViewGroup.LayoutParams param){
            LogDbHelper log_dbh = null;

            String sql = "select " +
                    LogDbHelper.TABLE_LOG.DATETIME + ", " +
                    LogDbHelper.TABLE_LOG.TYPE + ", " +
                    LogDbHelper.TABLE_LOG.TAG + ", " +
                    LogDbHelper.TABLE_LOG.MSG +
                    " from " + LogDbHelper.TABLE_LOG.getName() +
                    " order by " + LogDbHelper.TABLE_LOG._ID + " desc ";
            log_dbh = new LogDbHelper(mContext);
            if(max_row > 0){
                sql += " limit " + Integer.toString(max_row);
            }

            try (SQLiteDatabase db = log_dbh.getReadableDatabase(); Cursor cursor = db.rawQuery(sql, null)) {

                while (cursor.moveToNext() && !this.isCancelled()) {
                    TextView value = new TextView(mContext);
                    value.setSingleLine(false);

                    StringBuffer set_text = new StringBuffer();

                    set_text.append("[").append(cursor.getString(0)).append("]  ");
                    int type = Integer.parseInt(cursor.getString(1));

                    switch (type) {
                        case LoggingService.LOG_TYPE_INFO:
                            set_text.append("INFO");
                            break;
                        case LoggingService.LOG_TYPE_ERROR:
                            set_text.append("ERROR");
                            value.setTextColor(Color.YELLOW);
                            break;
                        case LoggingService.LOG_TYPE_NOTICE:
                            set_text.append("NOTICE");
                            break;
                        default:
                            set_text.append(" ");
                            break;
                    }

                    set_text.append("[").append(cursor.getString(2)).append("]  ");
                    set_text.append(cursor.getString(3));

                    value.setText(set_text);
                    tb_layout.addView(value, param);
                }

            } catch (Exception e) {
                Logging.stackTrace(e);
            } finally {
                log_dbh.close();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log);

        Button refreshBtn = findViewById(R.id.log_refresh_btn);
        refreshBtn.setOnClickListener(v -> refreshView());
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
    }

    private void cancelAsyncTask(){
        if(mViewingTask != null){
            mViewingTask.cancel(true);
            mViewingTask = null;
        }
    }

    private void refreshView(){
        try{
            ScrollView sv = findViewById(R.id.log_scroll);
            sv.removeAllViews();

            int max_row = MAX_LOG_ROWS;

            CheckBox check_latest = findViewById(R.id.log_checkbox);
            if(check_latest.isChecked()){
                max_row = LATEST_LOG_LOWS;
            }

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            DoInBackgroundParams params = new DoInBackgroundParams();
            params.row = max_row;
            params.layout = layout;

            cancelAsyncTask();
            ViewingTask viewingTask = new ViewingTask(this, sv);
            viewingTask.execute(params);
            mViewingTask = viewingTask;

        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }
}

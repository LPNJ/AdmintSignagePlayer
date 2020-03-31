package jp.co.digitalcruise.admint.player.activity.viewer;

import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Xml;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.ricoh.view360.lib.PhotoSphereView;
import com.ricoh.view360.lib.VideoSphereView;

import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import jp.co.digitalcruise.admint.player.AdmintApplication;
import jp.co.digitalcruise.admint.player.BuildConfig;
import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.file.AdmintPath;
import jp.co.digitalcruise.admint.player.component.file.ContentFile;
import jp.co.digitalcruise.admint.player.component.log.Logging;
import jp.co.digitalcruise.admint.player.db.PlayLogDbHelper;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;
import jp.co.digitalcruise.admint.player.service.LoggingService;

public class TouchContent {
    private static final String TAG = "TouchContent";
    public static final String APPLICATION_ID = BuildConfig.APPLICATION_ID;
    public static final String ACTION_PREFIX = APPLICATION_ID + ".TouchContent.";
    public static final String ACTION_TIMEOUT_DURATION_END = ACTION_PREFIX + "TIMEOUT_DURATION_END";

    private static final String XML_TAG_CONTENT = "content";
    private static final String XML_TAG_GENERAL = "general";
    private static final String XML_TAG_PATTERN = "pattern";
    private static final String XML_TAG_PAGE = "page";
    private static final String XML_TAG_MOVIE = "movie";
    private static final String XML_TAG_OBJECT = "various";
    private static final String XML_TAG_TOUCH = "touch";
    private static final String XML_TAG_SPINNER = "spinner";

    private static final String X = "X";
    private static final String Y = "Y";
    private static final String WIDTH = "WIDTH";
    private static final String HEIGHT = "HEIGHT";
    private static final String DISPLAY_RATIO_X = "DISPLAY_RATIO_X";
    private static final String DISPLAY_RATIO_Y = "DISPLAY_RATIO_Y";
    private static final String PATTERN_LIST = "PATTERN_LIST";

    private static WeakReference<TouchContent> mTouchContentReference;

    // general
    private class XmlGeneral{
        static final String ATTR_TIMEOUT = "timeout";
        static final String ATTR_TOP_PAGE = "top_page";
        static final String ATTR_DEFAULT_PATTERN = "default_pattern";
        static final String ATTR_WIDTH= "width";
        static final String ATTR_HEIGHT = "height";
    }

    class TouchGeneral{
        int timeout = 0;
        String top_page = "";
        String default_pattern = "";
        int width = 0;
        int height = 0;
    }

    // pattern
    private class XmlPattern{
        static final String ATTR_ID = "id";
        static final String ATTR_NAME = "name";
        static final String ATTR_ICON = "icon";
    }

    public class TouchPattern {
        String id = null;
        String name = null;
        public File icon = null;
    }

    // page
    private class XmlPage{
        static final String ATTR_ID = "id";
        static final String ATTR_NAME = "name";
        static final String ATTR_BACKGROUND = "background";
    }
    private class TouchPage{
        String pattern_id = null;
        String id = null;
        String name = null;
        String background = null;
    }

    // movie
    private class XmlMovie{
        static final String ATTR_FILE = "file";
        static final String ATTR_X = "x";
        static final String ATTR_Y = "y";
        static final String ATTR_WIDTH = "width";
        static final String ATTR_HEIGHT = "height";
        static final String ATTR_LOOP = "loop";
        static final String ATTR_COMMON = "common";
    }

    private class TouchMovie{
        String pattern_id = null;
        String page_id = null;
        String file = null;
        int x = 0;
        int y = 0;
        int width = 0;
        int height = 0;
        boolean loop = false;
        boolean common = false;
    }

    private class XmlTHETAObject{
        static final String ATTR_TYPE = "type";
        static final String ATTR_X = "x";
        static final String ATTR_Y = "y";
        static final String ATTR_WIDTH = "width";
        static final String ATTR_HEIGHT = "height";
        static final String ATTR_FILE = "file";
        static final String ATTR_ROTATE = "rotate";
        static final String ATTR_LOOP = "loop";
        static final String ATTR_DIRECTION = "direction";
        static final String ATTR_DIRECTION_LEFT = "left";
        static final String ATTR_DIRECTION_RIGHT = "right";
        static final String ATTR_PITCH = "pitch";
        static final String ATTR_ROLL = "roll";
        static final String ATTR_YAW = "yaw";
        static final String ATTR_SPEED = "speed";
        static final String ATTR_ANGLE = "angle";
        static final String ATTR_COMMON = "common";
    }

    private class THETAObjectInitialize {
        float angle = 200.0f;
        float pitch = 0.0f;
        float yaw = 0.0f;
    }

    private class THETAObject{
        private static final int TYPE_MOVIE = 1;
        private static final int TYPE_PICTURE = 2;
        private static final int RIGHT = 1;
        private static final int LEFT = 2;
        String pattern_id = null;
        String page_id = null;
        int type = 0;
        int x = 0;
        int y = 0;
        int width = 0;
        int height = 0;
        String file = "";
        boolean rotate = false;
        boolean loop = false;
        int direction = RIGHT;
        float speed = 0.1f;
        float angle = 160.0f;
        THETAObjectInitialize initial = null;
        boolean common = false;
    }

    /**
     * XML解析用
     */
    private class XmlTouch {
        static final String ATTR_NAME = "name";
        static final String ATTR_X = "x";
        static final String ATTR_Y = "y";
        static final String ATTR_WIDTH = "width";
        static final String ATTR_HEIGHT = "height";
        static final String ATTR_ACTION = "action";
        static final String ATTR_TYPE = "type";
        static final String ATTR_TARGET = "target";
        static final String ATTR_FILE = "file";
        static final String VALUE_ACTION_TOP = "top";
        static final String VALUE_ACTION_PREVIOUS = "previous";
        static final String VALUE_ACTION_NEXT = "next";
        static final String VALUE_ACTION_PATTERN = "pattern";
        static final String VALUE_TYPE_TOP = "top";
        static final String VALUE_TYPE_PREVIOUS = "previous";
        static final String VALUE_TYPE_NEXT = "next";
        static final String VALUE_TYPE_PATTERN = "pattern";
    }

    private class TouchTouch {
        static final int ACTION_NEXT = 0;
        static final int ACTION_PREVIOUS = 1;
        static final int ACTION_TOP = 2;
        static final int ACTION_PATTERN = 3;
        String name = null;
        String pattern_id = null;
        String page_id = null;
        String file = "";
        int x = 0;
        int y = 0;
        int width = 0;
        int height = 0;
        int action = 0;
        String target = null;
    }

    private class XmlMenu{
        static final String ATTR_NAME = "name";
        static final String ATTR_X = "x";
        static final String ATTR_Y = "y";
        static final String ATTR_WIDTH = "width";
        static final String ATTR_HEIGHT = "height";
    }

    private class TouchMenu {
        String name = null;
        String pattern_id = null;
        String page_id = null;
        int x = 0;
        int y = 0;
        int width = 0;
        int height = 0;
    }

    private String mBasePath = null;
    private String mBaseCommonPath = "";
    private Context mContext;
    private boolean mDebugMode = false;
//    private final boolean mOneContent;

    private TouchGeneral mGeneral = null;
    private HashMap<String, TouchPattern> mPatternMap = null;
    private HashMap<String, TouchPage> mPageMap = null;
    private HashMap<String, ArrayList<TouchTouch>> mTouchMap = null;
    private HashMap<String, TouchMovie> mMovieMap = null;
    private HashMap<String, ArrayList<THETAObject>> mObjectMap = null;
    private HashMap<String, ArrayList<TouchMenu>> mMenuMap = null;

    private ArrayList<TouchPattern> mPatternList = null;

    private ArrayList<String> mViewStack = null;
    private RelativeLayout mParentLayout = null;
    private VideoView mVideoView = null;
    private PhotoSphereView mTHETAImageView = null;
    private VideoSphereView mTHETAVideoView = null;
    private String mCurrentPatternId = null;
    private String mCurrentPageId = null;

    private TouchMovie mCurrentTouchMovie = null;

    private int mContentId = 0;
    private long mDuration = 0;

    private long mDurationAlarmTime = 0;

    private float mDisplayRatioX = 1.0f;
    private float mDisplayRatioY = 1.0f;

    private Point mPointDisplay = null;

    private FragmentManager mFragment;

    private CustomDialogFragment mDialog;

    private TouchMenu mTouchMenu = null;

    private PlayLogDbHelper mPlayLogDbHelper = null;

    private ReturnContentWaitThread mReturnContentWaitThread = null;

    private class ReturnContentWaitThread extends Thread{
        Messenger mViewerMessenger = null;
        long mSleepTime = 0;
        long mStartViewTimestamp = 0;
        int mWhatType = -1;

        private ReturnContentWaitThread(Messenger handler,long sleep_time, long start_view_time, int what){
            mViewerMessenger = handler;
            mSleepTime = sleep_time;
            mStartViewTimestamp = start_view_time;
            mWhatType = what;
        }

        @Override
        public void run(){
            try{
                Thread.sleep(mSleepTime);
                if(!isInterrupted()){
                    Message msg = Message.obtain(null, mWhatType, mStartViewTimestamp);
                    mViewerMessenger.send(msg);
                }
            } catch (InterruptedException ignore) {

            } catch (RemoteException e) {
                Logging.stackTrace(e);
            }
        }
    }


    /**
     * THETA静止画，動画をタッチ後自動で回転させるためのタイマー
     */
    private class WaitAsyncTask extends AsyncTask<Long, Void, Void> {

        private int mType = 0;
        private boolean mRotate = false;
        private int mDirection = 0;
        private float mSpeed = 0f;
        private THETAObjectInitialize mInitial = null;
        private float mAngle = 0f;

        void setData(int type, boolean rotate, int direction, float speed, THETAObjectInitialize initial, float angle) {
            mType = type;
            mRotate = rotate;
            mDirection = direction;
            mSpeed = speed;
            mInitial = initial;
            mAngle = angle;
        }

        @Override
        protected Void doInBackground(Long... params) {
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e){
                //Nothing
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void a){
            if(mType == 0) {
                if(mInitial != null) {
                    mTHETAImageView.setCameraAngle(mInitial.angle);
                    mTHETAImageView.setCameraYaw(mInitial.yaw);
                    mTHETAImageView.setCameraPitch(mInitial.pitch);
                } else {
                    mTHETAImageView.setCameraAngle(mAngle);
                    mTHETAImageView.setCameraYaw(0f);
                    mTHETAImageView.setCameraPitch(0f);
                }
                if(mRotate) {
                    if(mDirection == THETAObject.LEFT) {
                        mTHETAImageView.startAutoRotationByRadian(mSpeed);
                    } else {
                        mTHETAImageView.startAutoRotationByRadian(- mSpeed);
                    }
                }
            } else {
                if(mInitial != null) {
                    mTHETAVideoView.setCameraAngle(mInitial.angle);
                    mTHETAVideoView.setCameraYaw(mInitial.yaw);
                    mTHETAVideoView.setCameraPitch(mInitial.pitch);
                } else {
                    mTHETAVideoView.setCameraAngle(mAngle);
                    mTHETAVideoView.setCameraYaw(0f);
                    mTHETAVideoView.setCameraPitch(0f);
                }
                if(mRotate) {
                    if(mDirection == THETAObject.LEFT) {
                        mTHETAVideoView.startAutoRotationByRadian(mSpeed);
                    } else {
                        mTHETAVideoView.startAutoRotationByRadian(- mSpeed);
                    }
                }
            }
        }
    }

    private WaitAsyncTask mWaitAsyncTaskForTHETAPhoto = null;
    private WaitAsyncTask mWaitAsyncTaskForTHETAVideo = null;

//    TouchContent(Context context, boolean one_content){
    TouchContent(Context context){
        mContext = context;
//        mOneContent = one_content;
    }

    private void setTimer(int type, boolean rotate, int direction, float speed, THETAObjectInitialize initial, float angle) {
        cancelWaitAsyncTask(type);
        WaitAsyncTask wt = new WaitAsyncTask();
        wt.setData(type, rotate, direction, speed, initial, angle);
        wt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        if(type == 0) {
            mWaitAsyncTaskForTHETAPhoto = wt;
        } else {
            mWaitAsyncTaskForTHETAVideo = wt;
        }
    }

    private void cancelWaitAsyncTask(int type) {
        if(type == 0) {
            if(mWaitAsyncTaskForTHETAPhoto != null) {
                mWaitAsyncTaskForTHETAPhoto.cancel(true);
                mWaitAsyncTaskForTHETAPhoto = null;
            }
        } else {
            if(mWaitAsyncTaskForTHETAVideo != null) {
                mWaitAsyncTaskForTHETAVideo.cancel(true);
                mWaitAsyncTaskForTHETAVideo = null;
            }
        }
    }

    CustomDialogFragment getDialog() {
        return mDialog;
    }

    TouchGeneral getContentSize() {
        return mGeneral;
    }

    private Messenger mViewerMessenger = null;
    private Point mDisplayPoint = null;
    private Point mScalePoint = null;
    private long mStartViewTimestamp = 0;
    private int mWhatType = 0;

    void initialize(Messenger handler, Point display_size, Point scale_size, RelativeLayout touch_view, FragmentManager fm){
        mViewerMessenger = handler;
        mDisplayPoint = display_size;
        mScalePoint = scale_size;
        mParentLayout = touch_view;
        mFragment = fm;
        mDebugMode = DefaultPref.getDebugMode();
    }

    public void resetDurationAlarm(long start_view_time){
        mStartViewTimestamp = start_view_time;
        setReturnContentTimer();
    }

//    void createTouchContent(Context context, RelativeLayout layout, int content_id, String path, String common_path, long content_duration, Point pw, Point ph, FragmentManager fm) {
    void loadTouchContent(int id, long duration, long start_view_time, int what_type) {
        try{
//            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//            mDebugMode = prefs.getBoolean(context.getString(R.string.setting_key_debug_mode), false);
            mStartViewTimestamp = start_view_time;
            mWhatType = what_type;
//            mFragment = fm;
//            mBasePath = new File(path).getAbsolutePath() + "/";
            mBasePath = ContentFile.getContentDir(id).getAbsolutePath() + File.separator;
//            mBaseCommonPath = new File(common_path).getAbsolutePath() + "/";
            mBaseCommonPath = AdmintPath.getMaterialsDir().getAbsolutePath() + File.separator;
            mContentId = id;
            mDuration = duration;
            mTouchContentReference = new WeakReference<>(this);

            mPlayLogDbHelper = new PlayLogDbHelper(AdmintApplication.getInstance());

            parseXml(ContentFile.getTouchDataFile(id).getAbsolutePath());
            TouchContent.TouchGeneral contentSize = getContentSize();

            if(contentSize.height > contentSize.width) {
                if(mDisplayPoint.x >= mDisplayPoint.y){
                    setRatio(mScalePoint);
                } else {
                    setRatio(mDisplayPoint);
                }
            } else {
                if(mDisplayPoint.x >= mDisplayPoint.y){
                    setRatio(mDisplayPoint);
                } else {
                    setRatio(mScalePoint);
                }
            }

            FrameLayout.LayoutParams param;
            if(mDisplayPoint.x >= mDisplayPoint.y) {
                if(contentSize.width > contentSize.height){
                    param = new FrameLayout.LayoutParams(mDisplayPoint.x, mDisplayPoint.y);
                }else{
                    param = new FrameLayout.LayoutParams(mScalePoint.x, mScalePoint.y);
                }
            }else{
                if(contentSize.width > contentSize.height){
                    param = new FrameLayout.LayoutParams(mScalePoint.x, mScalePoint.y);
                }else{
                    param = new FrameLayout.LayoutParams(mDisplayPoint.x, mDisplayPoint.y);
                }
            }

            param.gravity = Gravity.CENTER;
            mParentLayout.setLayoutParams(param);

            createVideoView();
            mViewStack = new ArrayList<>();

            cancelReturnTopAlarm();
            cancelReturnContentTimer();
            cancelWaitAsyncTask(0);
            cancelWaitAsyncTask(1);

            moveOnCreate();

        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void startTouchContent(){
        cancelReturnTopAlarm();
//        cancelDurationAlarm();
        cancelReturnContentTimer();
        cancelWaitAsyncTask(0);
        cancelWaitAsyncTask(1);

        moveOnCreate();
    }

    private class ButtonListener implements View.OnClickListener{
        private TouchTouch mTouchButton = null;
        ButtonListener(TouchTouch touch){
            super();
            mTouchButton = touch;
        }

        private final long CLICK_DELAY = 1000;
        private long mOldClickTime = 0;

        /**
         * クリックイベントが実行可能か判断する。
         * @return クリックイベントの実行可否 (true:可, false:否)
         */
        private boolean isClickEvent() {
            long time = System.currentTimeMillis();

            if (time - mOldClickTime < CLICK_DELAY) {
                return false;
            }

            mOldClickTime = time;
            return true;
        }

        public void onClick(View v) {
            try{
                if (isClickEvent()) {
                    if (mTouchButton.action == TouchTouch.ACTION_NEXT) {
                        moveNext(mTouchButton);
                    } else if (mTouchButton.action == TouchTouch.ACTION_PREVIOUS) {
                        movePrevious(mTouchButton);
                    } else if (mTouchButton.action == TouchTouch.ACTION_TOP) {
                        moveTop(mTouchButton);
                    } else if (mTouchButton.action == TouchTouch.ACTION_PATTERN) {
                        changePattern(mTouchButton);
                    }
                }
            }catch(Exception e){
                Logging.stackTrace(e);
            }
        }
    }

    TouchMenu getTouchMenu() {
        return mTouchMenu;
    }

    private void setTouchMenu(TouchMenu menu) {
        mTouchMenu = menu;
    }

    private class MenuButtonListener implements View.OnClickListener{
        private TouchMenu mTouchMenuButton = null;
        MenuButtonListener(TouchMenu menu){
            super();
            mTouchMenuButton = menu;
            setTouchMenu(mTouchMenuButton);
        }

        private final long CLICK_DELAY = 1000;
        private long mOldClickTime = 0;

        /**
         * クリックイベントが実行可能か判断する。
         * @return クリックイベントの実行可否 (true:可, false:否)
         */
        private boolean isClickEvent() {
            // 現在時間を取得する
            long time = System.currentTimeMillis();

            // 一定時間経過していなければクリックイベント実行不可
            if (time - mOldClickTime < CLICK_DELAY) {
                return false;
            }

            // 一定時間経過したらクリックイベント実行可能
            mOldClickTime = time;
            return true;
        }

        public void onClick(View v) {
            try{
                // 前回クリックから一定時間経過していなければクリックイベントを実行しない
                if (isClickEvent()) {
                    alertDialog(mTouchMenuButton.x, mTouchMenuButton.y, mTouchMenuButton.width, mTouchMenuButton.height);
                }
            }catch(Exception e){
                Logging.stackTrace(e);
            }
        }
    }

    private void alertDialog(int x, int y, int width, int height) {
        mDialog = new CustomDialogFragment();
        Bundle args = new Bundle();
        args.putDouble(DISPLAY_RATIO_X, mDisplayRatioX);
        args.putDouble(DISPLAY_RATIO_Y, mDisplayRatioY);
        args.putInt(X, x);
        args.putInt(Y, y);
        args.putInt(WIDTH, width);
        args.putInt(HEIGHT, height);
        args.putSerializable(PATTERN_LIST, mPatternList);
        mDialog.setArguments(args);
        mDialog.show(mFragment, "alert_dialog");
    }

    void changePattern(TouchMenu touch_menu, String pattern_id){
        if(pattern_id != null && !pattern_id.equals(mCurrentPatternId)){
            String object_name = touch_menu.name + "[" + pattern_id + "]";
            viewPage(pattern_id, mCurrentPageId, object_name, true);
        }
    }

    private void changePattern(TouchTouch touch_button){
        if(touch_button.target != null && !touch_button.target.equals(mCurrentPatternId)) {
            viewPage(touch_button.target, mCurrentPageId, touch_button.name, true);
        }
    }

    private void moveNext(TouchTouch touch_button){
        if(viewPage(touch_button.pattern_id, touch_button.target, touch_button.name, true)){
            mViewStack.add(touch_button.page_id);
        }
    }

    private void movePrevious(TouchTouch touch_button){
        if(mViewStack.size() > 0){
            int index = mViewStack.size() - 1;
            String prev = mViewStack.get(index);
            viewPage(mCurrentPatternId, prev, touch_button.name, true);
            mViewStack.remove(index);
        }else{
            // トップページに前に戻るボタンが存在する？
            if(mViewStack.size() > 0) {
                mViewStack.clear();
            }
            viewPage(mCurrentPatternId, mGeneral.top_page, touch_button.name, true);
        }
    }

    private void moveTop(TouchTouch touch_button){
        if(mViewStack.size() > 0){
            mViewStack.clear();
        }
        viewPage(mCurrentPatternId, mGeneral.top_page, touch_button.name, true);
    }

    private void moveTimeout(){
        if(mViewStack.size() > 0){
            mViewStack.clear();
        }
        String OBJECT_NAME_TIMEOUT = "ON_TIMEOUT";
        viewPage(mGeneral.default_pattern, mGeneral.top_page, OBJECT_NAME_TIMEOUT, false);
    }

    private void moveOnCreate(){
        String OBJECT_NAME_CONTENT_START = "ON_CONTENT_START";
        viewPage(mCurrentPatternId, mGeneral.top_page, OBJECT_NAME_CONTENT_START, false);
    }

    private void setRatio(Point p){
        if(getContentSize().width > 0 && getContentSize().height > 0 && p.x > 0 && p.y > 0){
            mDisplayRatioX = (float)p.x / (float)mGeneral.width;
            mDisplayRatioY = (float)p.y / (float)mGeneral.height;
        }
        mPointDisplay = p;
    }

    private int getPatternPosition(String pattern_id){
        int position = 0;
        for(int i = 0 ; i < mPatternList.size() ; i++){
            TouchPattern pattern = mPatternList.get(i);
            if(pattern_id != null && pattern_id.equals(pattern.id)){
                position = i;
                break;
            }
        }
        return position;
    }

    private String getPageMapId(String pattern_id, String page_id){
        return pattern_id + "-" + page_id;
    }

    ArrayList<TouchPattern> getPatternList() {
        return mPatternList;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private boolean viewPage(String pattern_id, String page_id, String object_name, boolean isLogging){
        TouchPage page = mPageMap.get(getPageMapId(pattern_id, page_id));

        if(page != null){
            clearPage();

            Drawable bground = Drawable.createFromPath(mBasePath + pattern_id + "/" + page.background);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
                mParentLayout.setBackground(bground);
            }else{
                mParentLayout.setBackgroundDrawable(bground);
            }

            ArrayList<TouchTouch> touch_list;
            ArrayList<TouchMenu> menu_list;

            String page_map_id = getPageMapId(pattern_id, page_id);

            TouchMovie movie = mMovieMap.get(page_map_id);
            if(movie != null){
                setVideo(movie.x, movie.y, movie.x + movie.width, movie.y + movie.height);
                if(movie.common) {
                    mVideoView.setVideoPath(mBaseCommonPath + movie.file);
                } else {
                    mVideoView.setVideoPath(mBasePath + movie.file);
                }
                mCurrentTouchMovie = movie;
            }else{
                mCurrentTouchMovie = null;
            }

            createTHETAView(page_id, page_map_id);

            touch_list = mTouchMap.get(page_map_id);
            if(touch_list != null){
                for(int i = 0;i < touch_list.size();i++){
                    TouchTouch touch = touch_list.get(i);
                    ImageButton android_button;
                    if(!touch.file.equals("")) {
                        android_button = createButton(touch.x, touch.y, touch.x + touch.width, touch.y + touch.height, mBasePath + pattern_id + "/" + touch.file);
                    } else {
                        android_button = createButton(touch.x, touch.y, touch.x + touch.width, touch.y + touch.height, "");
                    }
                    android_button.setOnClickListener(new ButtonListener(touch));

                    mParentLayout.addView(android_button);
                }
            }

            menu_list = mMenuMap.get(page_map_id);
            if(menu_list != null){
                for(int i = 0; i < menu_list.size(); i++){
                    TouchMenu menu = menu_list.get(i);
                    int x1 = (int)((float)menu.x * mDisplayRatioX);
                    int x2 = (int)(((float)menu.x + menu.width) * mDisplayRatioX);
                    int y1 = (int)((float)menu.y * mDisplayRatioY);
                    int y2 = (int)(((float)menu.y + menu.height) * mDisplayRatioY);

                    TouchPattern pattern = mPatternList.get(getPatternPosition(pattern_id));

                    View convertView;
                    LayoutInflater inflater = LayoutInflater.from(mContext);
                    convertView = inflater.inflate(R.layout.custom_list_row, mParentLayout, false);
                    ImageView image = (ImageView)convertView.findViewById(R.id.menu_image_id);
                    LinearLayout.LayoutParams image_param = new LinearLayout.LayoutParams(x2 - x1, y2 - y1);
                    Bitmap bm = BitmapFactory.decodeFile(pattern.icon.getAbsolutePath());
                    image.setImageBitmap(bm);
                    image.setLayoutParams(image_param);
                    LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(x2 - x1, y2 - y1);
                    param.setMargins(x1, y1, 0, 0);
                    image.setLayoutParams(param);
                    image.setPadding(0, 0, 0, 0);
                    image.setBackgroundColor(Color.argb(0, 0, 0, 0));
                    image.setOnClickListener(new MenuButtonListener(menu));
                    mParentLayout.addView(convertView);
                }
            }

            cancelReturnTopAlarm();
//            cancelDurationAlarm();
            cancelReturnContentTimer();
            if(page_id.equals(mGeneral.top_page)){
//                if(!mOneContent) {
//                    setDurationAlarm();
//                }
                setReturnContentTimer();
            }else{
                setReturnTopAlarm();
            }

            if(isLogging){
                TouchPattern log_pattern = mPatternMap.get(mCurrentPatternId);
                String pattern_name = "";
                if(log_pattern != null){
                    pattern_name = log_pattern.name;
                }

                TouchPage log_page = mPageMap.get(getPageMapId(mCurrentPatternId, mCurrentPageId));
                String page_name = "";
                if(log_page != null){
                    page_name = log_page.name;
                }
                loggingTouch(pattern_name, page_name, object_name);
            }

            mCurrentPatternId = page.pattern_id;
            mCurrentPageId = page.id;

            return true;
        } else {
            return false;
        }
    }

    private void loggingTouch(String pattern_name, String page_name, String object_name){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        try {
            long timestamp = System.currentTimeMillis();
            String datetime = sdf.format(timestamp);

            Intent intent = new Intent(AdmintApplication.getInstance().getApplicationContext(), LoggingService.class);
            intent.putExtra(LoggingService.INTENT_EXTRA_LOG_CONTENT_ID, mContentId);
            intent.putExtra(LoggingService.INTENT_EXTRA_LOG_PATTERN_NAME, pattern_name);
            intent.putExtra(LoggingService.INTENT_EXTRA_LOG_PAGE_NAME, page_name);
            intent.putExtra(LoggingService.INTENT_EXTRA_LOG_OBJECT_NAME, object_name);
            intent.putExtra(LoggingService.INTENT_EXTRA_TIMESTAMP, timestamp);
            intent.putExtra(LoggingService.INTENT_EXTRA_DATETIME, datetime);
            intent.setAction(LoggingService.ACTION_LOGGING_TOUCH_CONTENT);
            mContext.startService(intent);
        }catch (Exception e){
            Logging.stackTrace(e);
        }
    }

    private String loadFile(String path){
        StringBuilder result = new StringBuilder();
        BufferedReader reader = null;
        try{
            FileInputStream is = new FileInputStream(new File(path));
            reader = new BufferedReader(new InputStreamReader(is));
            String buffer;
            while ((buffer = reader.readLine()) != null) {
                result.append(buffer);
            }
        }catch(Exception e){
            Logging.stackTrace(e);
        }finally{
            if(reader != null){
                try{
                    reader.close();
                }catch(Exception e2){
                    Logging.stackTrace(e2);
                }
            }
        }
        return result.toString();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void clearPage(){
        try {
            if (mParentLayout != null) {
                // layoutクリア
                mParentLayout.removeAllViews();

                // out of memory 対策
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    // 4.2
                    mParentLayout.setBackground(null);
                } else {
                    mParentLayout.setBackgroundDrawable(null);
                }
            }

            destroyVideoView();
            destroyTHETAView();

            System.gc();
        } catch (OutOfMemoryError e) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
                Logging.error(sw.toString());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch(Exception e) {
            Logging.stackTrace(e);
        }
    }

    private void destroyVideoView() {
        if(mVideoView != null && mVideoView.isPlaying()){
            mVideoView.setOnPreparedListener(null);
            mVideoView.setOnCompletionListener(null);
            mVideoView.pause();
            mVideoView.seekTo(0);
            mVideoView.stopPlayback();
            mVideoView.destroyDrawingCache();
            mVideoView = null;
        }
    }

    private void destroyTHETAView() {
        mTHETAImageView = null;
        if(mTHETAVideoView != null) {
            mTHETAVideoView.finalize();
            mTHETAVideoView = null;
        }
    }

    @SuppressLint("NewApi")
    private ImageButton createButton(int x1, int y1, int x2, int y2, String file){
        ImageButton btn = new ImageButton(mContext);

        x1 = (int)((float)x1 * mDisplayRatioX);
        x2 = (int)((float)x2 * mDisplayRatioX);
        y1 = (int)((float)y1 * mDisplayRatioY);
        y2 = (int)((float)y2 * mDisplayRatioY);

        RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(x2-x1, y2-y1);

        param.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1);
        param.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1);
        param.setMargins(x1, y1, 0, 0);

        if(!file.equals("")) {
            Drawable image = Drawable.createFromPath(file);
            btn.setImageDrawable(image);
            btn.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        }

        btn.setLayoutParams(param);
        btn.setPadding(0, 0, 0, 0);

        if(mDebugMode){
            btn.setBackgroundColor(Color.argb(64, 0, 0, 0));
        }else{
            btn.setBackgroundColor(Color.argb(0, 0, 0, 0));
        }
        return btn;
    }

    private void setVideo(int x1, int y1, int x2, int y2){

        x1 = (int)((float)x1 * mDisplayRatioX);
        x2 = (int)((float)x2 * mDisplayRatioX);
        y1 = (int)((float)y1 * mDisplayRatioY);
        y2 = (int)((float)y2 * mDisplayRatioY);

        RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(x2-x1, y2-y1);
        param.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1);
        param.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1);
        param.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
        param.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);

        param.setMargins(x1, y1, mPointDisplay.x - x2, mPointDisplay.y - y2);

        mVideoView.setLayoutParams(param);

        mParentLayout.addView(mVideoView);
    }

    private RelativeLayout.LayoutParams setObject(int x1, int y1, int x2, int y2){

        x1 = (int)((float)x1 * mDisplayRatioX);
        x2 = (int)((float)x2 * mDisplayRatioX);
        y1 = (int)((float)y1 * mDisplayRatioY);
        y2 = (int)((float)y2 * mDisplayRatioY);

        RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(x2-x1, y2-y1);
        param.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 1);
        param.addRule(RelativeLayout.ALIGN_PARENT_TOP, 1);
        param.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
        param.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);

        param.setMargins(x1, y1, mPointDisplay.x - x2, mPointDisplay.y - y2);
        return param;
    }

    private void createVideoView(){
        if(mVideoView == null){
            mVideoView = new VideoView(mContext);
            // 再生準備完了後のイベントリスナー
            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    try{
                        mp.start();
                    }catch(Exception e){
                        Logging.stackTrace(e);
                    }
                }
            });

            mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    return true;
                }
            });

            mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    try {
                        if (mCurrentTouchMovie != null && mCurrentTouchMovie.loop) {
                            if(mCurrentTouchMovie.common) {
                                mVideoView.setVideoPath(mBaseCommonPath + mCurrentTouchMovie.file);
                            } else {
                                mVideoView.setVideoPath(mBasePath + mCurrentTouchMovie.file);
                            }
                        }
                    } catch (Exception e) {
                        Logging.stackTrace(e);
                    }
                }
            });

            mVideoView.setPadding(0, 0, 0, 0);
        }
    }

    private void createTHETAView(String page_id, String page_map_id){
        cancelWaitAsyncTask(0);
        cancelWaitAsyncTask(1);
        final String mPageId = page_id;
        ArrayList<THETAObject> theta_objects = mObjectMap.get(page_map_id);
        if(theta_objects != null) {
            for(int i = 0; i < theta_objects.size(); i++) {
                final THETAObject object = theta_objects.get(i);
                if(object.type == THETAObject.TYPE_MOVIE) {
                    mTHETAVideoView = null;
                    try {
                        mTHETAVideoView = createTHETAVideoView(mPageId, object.rotate, object.direction, object.speed, object.initial, object.angle);

                        if(object.common) {
                            mTHETAVideoView.initialize(mBaseCommonPath + object.file, object.width/2, object.height/2);
                        } else {
                            mTHETAVideoView.initialize(mBasePath + object.file, object.width/2, object.height/2);
                        }
                        mTHETAVideoView.setEnableTouch(true);
                        if(object.initial != null) {
                            mTHETAVideoView.setCameraAngle(object.initial.angle);
                            mTHETAVideoView.setCameraYaw(object.initial.yaw);
                            mTHETAVideoView.setCameraPitch(object.initial.pitch);
                        } else {
                            mTHETAVideoView.setCameraAngle(object.angle);
                        }
                        if(object.rotate) {
                            if(object.direction == THETAObject.LEFT) {
                                mTHETAVideoView.startAutoRotationByRadian(object.speed);
                            } else {
                                mTHETAVideoView.startAutoRotationByRadian(- object.speed);
                            }
                        }
                        mTHETAVideoView.setLooping(object.loop);
                        mTHETAVideoView.setOnCompleteionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                if(object.initial != null) {
                                    mTHETAVideoView.setCameraAngle(object.initial.angle);
                                    mTHETAVideoView.setCameraYaw(object.initial.yaw);
                                    mTHETAVideoView.setCameraPitch(object.initial.pitch);
                                } else {
                                    mTHETAVideoView.setCameraAngle(object.angle);
                                }
                                if(object.rotate) {
                                    if(object.direction == THETAObject.LEFT) {
                                        mTHETAVideoView.startAutoRotationByRadian(object.speed);
                                    } else {
                                        mTHETAVideoView.startAutoRotationByRadian(- object.speed);
                                    }
                                }
                            }
                        });
                        RelativeLayout.LayoutParams param = setObject(object.x, object.y, object.x + object.width, object.y + object.height);
                        mTHETAVideoView.setLayoutParams(param);
                        mParentLayout.addView(mTHETAVideoView);
                    } catch(final Exception e) {
                        Logging.stackTrace(e);
//                        dev_error("createTHETAView() video" + e.toString());
                    }
                } else{
                    mTHETAImageView = null;
                    try {
                        mTHETAImageView = createTHETAPhotoView(mPageId, object.rotate, object.direction, object.speed, object.initial, object.angle);

                        if(object.common) {
                            mTHETAImageView.initialize(mBaseCommonPath + object.file, object.width/2, object.height/2);
                        } else {
                            mTHETAImageView.initialize(mBasePath + object.file, object.width/2, object.height/2);
                        }
                        mTHETAImageView.setEnableTouch(true);
                        if(object.initial != null) {
                            mTHETAImageView.setCameraAngle(object.initial.angle);
                            mTHETAImageView.setCameraYaw(object.initial.yaw);
                            mTHETAImageView.setCameraPitch(object.initial.pitch);
                        } else {
                            mTHETAImageView.setCameraAngle(object.angle);
                        }
                        if(object.rotate) {
                            if(object.direction == THETAObject.LEFT) {
                                mTHETAImageView.startAutoRotationByRadian(object.speed);
                            } else {
                                mTHETAImageView.startAutoRotationByRadian(- object.speed);
                            }
                        }
                        RelativeLayout.LayoutParams param = setObject(object.x, object.y, object.x + object.width, object.y + object.height);
                        mTHETAImageView.setLayoutParams(param);
                        mParentLayout.addView(mTHETAImageView);
                    } catch(Exception e) {
                        Logging.stackTrace(e);
//                        dev_error("createTHETAView() image" + e.toString());
                    }
                }
            }
        }
    }

    /**
     *
     * @param page_id
     * @return
     */
    private PhotoSphereView createTHETAPhotoView(final String page_id, final boolean rotate, final int direction, final float spped, final THETAObjectInitialize initial, final float angle) {
        return new PhotoSphereView(mContext) {
            public boolean onTouchEvent(MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        this.stopAutoRotation();
                        cancelWaitAsyncTask(0);
                        cancelReturnTopAlarm();
//                        cancelDurationAlarm();
                        cancelReturnContentTimer();
                        break;
                    case MotionEvent.ACTION_UP:
                        if (page_id.equals(mGeneral.top_page)) {
//                            if (!mOneContent) {
//                                setDurationAlarm();
//                            }
                            setReturnContentTimer();
                        } else {
                            setReturnTopAlarm();
                        }
                        setTimer(0, rotate, direction, spped, initial, angle);
                        break;
                    default:
                        break;
                }
                return super.onTouchEvent(event);
            }
        };
    }

    /**
     *
     * @param page_id
     * @return
     */
    private VideoSphereView createTHETAVideoView(final String page_id, final boolean rotate, final int direction, final float speed, final THETAObjectInitialize initial, final float angle) {
        return  new VideoSphereView(mContext) {
            public boolean onTouchEvent(MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        this.stopAutoRotation();
                        cancelWaitAsyncTask(1);
                        cancelReturnTopAlarm();
//                        cancelDurationAlarm();
                        cancelReturnContentTimer();
                        break;
                    case MotionEvent.ACTION_UP:
                        if (page_id.equals(mGeneral.top_page)) {
//                            if (!mOneContent) {
//                                setDurationAlarm();
//                            }
                            setReturnContentTimer();
                        } else {
                            setReturnTopAlarm();
                        }
                        setTimer(1, rotate, direction, speed, initial, angle);
                        break;
                    default:
                        break;
                }
                return super.onTouchEvent(event);
            }

            public boolean onError(MediaPlayer mp, int what, int extra) {
//                dev_error("THETA VIDEO ERROR");
                return true;
            }
        };
    }

    private static class ReturnTopAlarmHandler extends Handler {
        private final WeakReference<TouchContent> refTouchContent;

        ReturnTopAlarmHandler(WeakReference<TouchContent> refTouchContent) {
            this.refTouchContent = refTouchContent;
        }

        @Override
        public void handleMessage(Message msg) {
            TouchContent tc = refTouchContent.get();
            if(tc == null) {
                return;
            }
            tc.returnTop();
        }
    }

    private void returnTop() {
        mReturnTopAlarmThread = null;
        if(mDialog != null) {
            try {
                mDialog.dismiss();
            } catch(Exception e) {
                //
            }
        }
        moveTimeout();
    }

    private class ReturnTopAlarmThread extends Thread {
        private long mInterval;

        @Override
        public void run() {
            try {
                Thread.sleep(mInterval);
                if(!isInterrupted()){
                    mReturnTopAlarmHandler.sendEmptyMessage(0);
                }
            } catch (InterruptedException ignore) {}
        }
    }

    private ReturnTopAlarmHandler mReturnTopAlarmHandler;
    private ReturnTopAlarmThread mReturnTopAlarmThread;

    private void setReturnTopAlarm() {
        if(mGeneral.timeout > 0){
            if(mReturnTopAlarmHandler == null) {
                mReturnTopAlarmHandler = new ReturnTopAlarmHandler(mTouchContentReference);
            }
            if(mReturnTopAlarmThread == null) {
                mReturnTopAlarmThread = new ReturnTopAlarmThread();
                mReturnTopAlarmThread.mInterval = (long)mGeneral.timeout * 1000;
                mReturnTopAlarmThread.start();
            }
        }
    }

    private void cancelReturnTopAlarm() {
        if(mReturnTopAlarmThread != null) {
            if(!mReturnTopAlarmThread.isInterrupted()){
                mReturnTopAlarmThread.interrupt();
            }
            mReturnTopAlarmThread = null;
        }
    }

    private void cancelReturnContentTimer(){
        if(mReturnContentWaitThread != null){
            if(!mReturnContentWaitThread.isInterrupted()){
                mReturnContentWaitThread.interrupt();
            }
            mReturnContentWaitThread = null;
        }
    }

    private void setReturnContentTimer(){
        cancelReturnContentTimer();
        mReturnContentWaitThread = new ReturnContentWaitThread(mViewerMessenger, mDuration, mStartViewTimestamp, mWhatType);
        mReturnContentWaitThread.start();
    }


//    private void setDurationAlarm(){
//        if(mDuration > 0){
//            Intent intent = new Intent();
//            intent.setAction(TouchContent.ACTION_TIMEOUT_DURATION_END);
//
//            PendingIntent pintent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
//
//            mDurationAlarmTime =  System.currentTimeMillis() + mDuration;
//            AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                am.setExact(AlarmManager.RTC, mDurationAlarmTime, pintent);
//            } else {
//                am.set(AlarmManager.RTC, mDurationAlarmTime, pintent);
//            }
//        }
//    }

//    private void cancelDurationAlarm(){
//        if(mDuration > 0){
//            mDurationAlarmTime = 0;
//
//            Intent intent = new Intent();
//            intent.setAction(TouchContent.ACTION_TIMEOUT_DURATION_END);
//
//            PendingIntent pintent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
//
//            AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
//            am.cancel(pintent);
//        }
//    }

    public void cleanTouchObject(){
        try{
            if(mPlayLogDbHelper != null){
                mPlayLogDbHelper.close();
                mPlayLogDbHelper = null;
            }

            if(mDialog != null) {
                mDialog.dismiss();
                mDialog = null;
            }

            mStartViewTimestamp = 0;
            mWhatType = -1;

            if(mTouchContentReference != null) {
                mTouchContentReference = null;
            }
            cancelReturnTopAlarm();
//            cancelDurationAlarm();
            cancelReturnContentTimer();
            cancelWaitAsyncTask(0);
            cancelWaitAsyncTask(1);
            clearPage();
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private void parseXml(String path){
        try{
            mGeneral = new TouchGeneral();
            mPatternMap = new HashMap<>();
            mPageMap = new HashMap<>();
            mMovieMap = new HashMap<>();
            mObjectMap = new HashMap<>();
            mTouchMap = new HashMap<>();
            mMenuMap = new HashMap<>();
            mPatternList = new ArrayList<>();

            String doc = loadFile(path);

            XmlPullParser parser;
            parser = Xml.newPullParser();
            parser.setInput(new StringReader(doc));
            int event_type = parser.getEventType();

            boolean is_content = false;
            boolean is_pattern = false;
            boolean is_page = false;
            boolean is_exit = false;
            String curPatternId = null;
            String curPageId = null;

            ArrayList<TouchTouch> touch_list = null;
            ArrayList<TouchMenu> menu_list = null;
            ArrayList<THETAObject> theta_list = null;

            while(event_type != XmlPullParser.END_DOCUMENT){
                String tag;
                switch(event_type){
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        tag = parser.getName();
                        if(XML_TAG_CONTENT.equals(tag)){
                            is_content = true;
                        }else{
                            if(is_content){
                                if(XML_TAG_GENERAL.equals(tag)){
                                    mGeneral.timeout = Integer.parseInt(parser.getAttributeValue(null, XmlGeneral.ATTR_TIMEOUT));
                                    mGeneral.top_page = parser.getAttributeValue(null, XmlGeneral.ATTR_TOP_PAGE).equals("undefined") ? "1" : parser.getAttributeValue(null, XmlGeneral.ATTR_TOP_PAGE);
                                    mGeneral.default_pattern = parser.getAttributeValue(null, XmlGeneral.ATTR_DEFAULT_PATTERN);
                                    mGeneral.width = Integer.parseInt(parser.getAttributeValue(null, XmlGeneral.ATTR_WIDTH));
                                    mGeneral.height = Integer.parseInt(parser.getAttributeValue(null, XmlGeneral.ATTR_HEIGHT));
                                }else if(XML_TAG_PATTERN.equals(tag)){
                                    is_pattern = true;
                                    TouchPattern pattern = new TouchPattern();
                                    pattern.id = parser.getAttributeValue(null, XmlPattern.ATTR_ID);
                                    pattern.name = parser.getAttributeValue(null, XmlPattern.ATTR_NAME);
                                    pattern.icon = new File(mBasePath + pattern.id + "/" + parser.getAttributeValue(null, XmlPattern.ATTR_ICON));
                                    curPatternId = pattern.id;
                                    mPatternMap.put(curPatternId, pattern);
                                    mPatternList.add(pattern);
                                }else if(XML_TAG_PAGE.equals(tag)){
                                    if(is_pattern){
                                        is_page = true;
                                        TouchPage page = new TouchPage();
                                        page.id = parser.getAttributeValue(null, XmlPage.ATTR_ID);
                                        page.name = parser.getAttributeValue(null, XmlPage.ATTR_NAME);
                                        page.background = parser.getAttributeValue(null, XmlPage.ATTR_BACKGROUND);
                                        page.pattern_id = curPatternId;
                                        curPageId = page.id;
                                        mPageMap.put(getPageMapId(curPatternId, page.id), page);

                                        touch_list = new ArrayList<>();
                                        menu_list = new ArrayList<>();
                                    }
                                }else if(XML_TAG_MOVIE.equals(tag)) {
                                    if (is_page) {
                                        TouchMovie movie = new TouchMovie();
                                        movie.x = Integer.parseInt(parser.getAttributeValue(null, XmlMovie.ATTR_X));
                                        movie.y = Integer.parseInt(parser.getAttributeValue(null, XmlMovie.ATTR_Y));
                                        movie.width = Integer.parseInt(parser.getAttributeValue(null, XmlMovie.ATTR_WIDTH));
                                        movie.height = Integer.parseInt(parser.getAttributeValue(null, XmlMovie.ATTR_HEIGHT));

                                        int xml_loop = Integer.parseInt(parser.getAttributeValue(null, XmlMovie.ATTR_LOOP));
                                        movie.loop = xml_loop > 0;
                                        movie.file = parser.getAttributeValue(null, XmlMovie.ATTR_FILE);
                                        movie.pattern_id = curPatternId;
                                        movie.page_id = curPageId;
                                        if(isExistListXml() && parser.getAttributeValue(null, XmlMovie.ATTR_COMMON) != null) {
                                            int common = Integer.parseInt(parser.getAttributeValue(null, XmlMovie.ATTR_COMMON));
                                            if(common == 1) {
                                                movie.common = true;
                                            } else {
                                                movie.common = false;
                                            }
                                        } else {
                                            movie.common = false;
                                        }

                                        mMovieMap.put(getPageMapId(curPatternId, curPageId), movie);
                                    }
                                } else if(XML_TAG_OBJECT.equals(tag)) {
                                    if(is_page) {
                                        if(theta_list == null) {
                                            theta_list = new ArrayList<>();
                                        }
                                        THETAObject object = new THETAObject();
                                        object.type = Integer.parseInt(parser.getAttributeValue(null, XmlTHETAObject.ATTR_TYPE));
                                        object.x = Integer.parseInt(parser.getAttributeValue(null, XmlTHETAObject.ATTR_X));
                                        object.y = Integer.parseInt(parser.getAttributeValue(null, XmlTHETAObject.ATTR_Y));
                                        object.width = Integer.parseInt(parser.getAttributeValue(null, XmlTHETAObject.ATTR_WIDTH));
                                        object.height = Integer.parseInt(parser.getAttributeValue(null, XmlTHETAObject.ATTR_HEIGHT));
                                        object.file = parser.getAttributeValue(null, XmlTHETAObject.ATTR_FILE);
                                        int xml_rotate = Integer.parseInt(parser.getAttributeValue(null, XmlTHETAObject.ATTR_ROTATE));
                                        object.rotate = xml_rotate == 1;
                                        int xml_loop = Integer.parseInt(parser.getAttributeValue(null, XmlTHETAObject.ATTR_LOOP));
                                        object.loop = xml_loop == 1;
                                        String xml_direction = parser.getAttributeValue(null, XmlTHETAObject.ATTR_DIRECTION);
                                        if(object.rotate && xml_direction.equals(XmlTHETAObject.ATTR_DIRECTION_LEFT)) {
                                            object.direction = THETAObject.LEFT;
                                        } else if(object.rotate && xml_direction.equals(XmlTHETAObject.ATTR_DIRECTION_RIGHT)) {
                                            object.direction = THETAObject.RIGHT;
                                        } else {
                                            object.direction = THETAObject.LEFT;
                                        }
                                        object.speed = Float.parseFloat(parser.getAttributeValue(null, XmlTHETAObject.ATTR_SPEED));
                                        if(parser.getAttributeValue(null, XmlTHETAObject.ATTR_PITCH) != null && parser.getAttributeValue(null, XmlTHETAObject.ATTR_PITCH) != null && parser.getAttributeValue(null, XmlTHETAObject.ATTR_ANGLE) != null) {
                                            object.initial = new THETAObjectInitialize();
                                            object.initial.pitch = Float.parseFloat(parser.getAttributeValue(null, XmlTHETAObject.ATTR_PITCH));
                                            object.initial.yaw = Float.parseFloat(parser.getAttributeValue(null, XmlTHETAObject.ATTR_YAW));
                                            object.initial.angle = Float.parseFloat(parser.getAttributeValue(null, XmlTHETAObject.ATTR_ANGLE));
                                        }
                                        object.page_id = curPageId;
                                        object.pattern_id = curPatternId;
                                        if(isExistListXml() && parser.getAttributeValue(null, XmlTHETAObject.ATTR_COMMON) != null) {
                                            int common = Integer.parseInt(parser.getAttributeValue(null, XmlTHETAObject.ATTR_COMMON));
                                            if(common == 1) {
                                                object.common = true;
                                            } else {
                                                object.common = false;
                                            }
                                        } else {
                                            object.common = false;
                                        }

                                        if(theta_list.size() == 0) {
                                            theta_list.add(object);
                                        } else if(theta_list.get(0).pattern_id.equals(object.pattern_id) && theta_list.get(0).page_id.equals(object.page_id)) {
                                            theta_list.add(object);
                                        } else {
                                            mObjectMap.put(getPageMapId(curPatternId, curPageId), new ArrayList<>(theta_list));
                                            theta_list.clear();
                                        }
                                    }
                                }else if(XML_TAG_TOUCH.equals(tag)){
                                    if(is_page){
                                        TouchTouch touch = new TouchTouch();
                                        touch.name = parser.getAttributeValue(null, XmlTouch.ATTR_NAME);
                                        touch.x = Integer.parseInt(parser.getAttributeValue(null, XmlTouch.ATTR_X));
                                        touch.y = Integer.parseInt(parser.getAttributeValue(null, XmlTouch.ATTR_Y));
                                        touch.width = Integer.parseInt(parser.getAttributeValue(null, XmlTouch.ATTR_WIDTH));
                                        touch.height = Integer.parseInt(parser.getAttributeValue(null, XmlTouch.ATTR_HEIGHT));
                                        if(parser.getAttributeValue(null, XmlTouch.ATTR_FILE) != null) {
                                            touch.file = parser.getAttributeValue(null, XmlTouch.ATTR_FILE);
                                        } else {
                                            touch.file = "";
                                        }

                                        if(parser.getAttributeValue(null, XmlTouch.ATTR_TYPE) != null) {
                                            String xml_type = parser.getAttributeValue(null, XmlTouch.ATTR_TYPE);
                                            if(XmlTouch.VALUE_TYPE_NEXT.equals(xml_type)){
                                                touch.action = TouchTouch.ACTION_NEXT;
                                            }else if(XmlTouch.VALUE_TYPE_TOP.equals(xml_type)){
                                                touch.action = TouchTouch.ACTION_TOP;
                                            }else if(XmlTouch.VALUE_TYPE_PREVIOUS.equals(xml_type)){
                                                touch.action = TouchTouch.ACTION_PREVIOUS;
                                            }else if(XmlTouch.VALUE_TYPE_PATTERN.equals(xml_type)){
                                                touch.action = TouchTouch.ACTION_PATTERN;
                                            } else {
                                                touch.action = TouchTouch.ACTION_TOP;
                                            }
                                        } else {
                                            String xml_action = parser.getAttributeValue(null, XmlTouch.ATTR_ACTION);
                                            if(XmlTouch.VALUE_ACTION_NEXT.equals(xml_action)) {
                                                touch.action = TouchTouch.ACTION_NEXT;
                                            } else if(XmlTouch.VALUE_ACTION_TOP.equals(xml_action)) {
                                                touch.action = TouchTouch.ACTION_TOP;
                                            } else if(XmlTouch.VALUE_ACTION_PREVIOUS.equals(xml_action)) {
                                                touch.action = TouchTouch.ACTION_PREVIOUS;
                                            } else if(XmlTouch.VALUE_ACTION_PATTERN.equals(xml_action)) {
                                                touch.action = TouchTouch.ACTION_PATTERN;
                                            } else {
                                                touch.action = TouchTouch.ACTION_TOP;
                                            }
                                        }

                                        touch.target = parser.getAttributeValue(null, XmlTouch.ATTR_TARGET);

                                        touch.pattern_id = curPatternId;
                                        touch.page_id = curPageId;

                                        touch_list.add(touch);
                                    }
                                }else if(XML_TAG_SPINNER.equals(tag) && menu_list != null){
                                    TouchMenu menu = new TouchMenu();
                                    menu.name = parser.getAttributeValue(null, XmlMenu.ATTR_NAME);
                                    menu.x = Integer.parseInt(parser.getAttributeValue(null, XmlMenu.ATTR_X));
                                    menu.y = Integer.parseInt(parser.getAttributeValue(null, XmlMenu.ATTR_Y));
                                    menu.width = Integer.parseInt(parser.getAttributeValue(null, XmlMenu.ATTR_WIDTH));
                                    menu.height = Integer.parseInt(parser.getAttributeValue(null, XmlMenu.ATTR_HEIGHT));
                                    menu.pattern_id = curPatternId;
                                    menu.page_id = curPageId;

                                    menu_list.add(menu);
                                }
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        tag = parser.getName();
                        if(XML_TAG_CONTENT.equals(tag)){
                            is_exit = true;
                        }else if(XML_TAG_PATTERN.equals(tag)){
                            is_pattern = false;
                            curPatternId = null;
                        }else if(XML_TAG_PAGE.equals(tag)){
                            is_page = false;

                            if(touch_list != null){
                                mTouchMap.put(getPageMapId(curPatternId, curPageId), touch_list);
                                touch_list = null;
                            }
                            if(menu_list != null){
                                mMenuMap.put(getPageMapId(curPatternId, curPageId), menu_list);
                                menu_list = null;
                            }
                            if(theta_list != null && theta_list.size() != 0) {
                                mObjectMap.put(getPageMapId(curPatternId, curPageId), new ArrayList<>(theta_list));
                                theta_list.clear();
                                theta_list = null;
                            }
                            curPageId = null;
                        }
                        break;
                }

                if(is_exit){
                    break;
                }
                event_type = parser.next();
            }

            for(TouchPattern tp : mPatternList){
                if(mGeneral.default_pattern.equals(tp.id)){
                    mCurrentPatternId = tp.id;
                    break;
                }
            }

            if(mCurrentPatternId == null || mCurrentPatternId.length() == 0){
                mCurrentPatternId = mPatternList.get(0).id;
                mGeneral.default_pattern = mCurrentPatternId;
            }

            mCurrentPageId = mGeneral.top_page;
        }catch(Exception e){
            Logging.stackTrace(e);
        }
    }

    private boolean isExistListXml() {
        return ContentFile.getTouchListFile(mContentId).isFile();
//        return (new File(mBasePath + Def.TOUCH_LIST_XML)).exists();
    }

    void pressKeyBoard(int keyCode){
        if(mTouchMap == null){
            return;
        }

        String targetName = "["+keyCode+"]";
        TouchTouch targetTouch = null;

        ArrayList<TouchTouch> touchList = mTouchMap.get(getPageMapId(mCurrentPatternId, mCurrentPageId));
        if (touchList == null) {
            return;
        }

        for (TouchTouch touchButton : touchList) {
            if(touchButton.name != null && touchButton.name.contains(targetName)){
                targetTouch = touchButton;
            }
        }

        if (targetTouch != null) {
            if (targetTouch.action == TouchTouch.ACTION_NEXT) {
                moveNext(targetTouch);
            } else if (targetTouch.action == TouchTouch.ACTION_PREVIOUS) {
                movePrevious(targetTouch);
            } else if (targetTouch.action == TouchTouch.ACTION_TOP) {
                moveTop(targetTouch);
            } else if (targetTouch.action == TouchTouch.ACTION_PATTERN) {
                changePattern(targetTouch);
            }
        }
    }
}
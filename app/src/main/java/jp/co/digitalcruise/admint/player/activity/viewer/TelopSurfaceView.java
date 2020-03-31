package jp.co.digitalcruise.admint.player.activity.viewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Build;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jp.co.digitalcruise.admint.player.component.define.DeviceDef;

public class TelopSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private final static int MAX_TEXT_UNIT = 200;

    private SurfaceHolder mHolder;
    private DrawingThread mDrawThread;

    private float mCurPos = -300;
    private int mAngle = 0;
    private int mSpeed = 100;
    private int mFps = 30;
    private int mSize = 0;
    private List<Bitmap> mBmpBuffers;

    public TelopSurfaceView(Context context) {
        super(context);

        // 初期化
        mBmpBuffers = Collections.synchronizedList(new ArrayList<>());

        //ビューの背景を透過させる
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        //コールバック登録
        getHolder().addCallback(this);
    }

    /**
     * テロップの描画位置を初期化
     */
    public void initPos() {
        mCurPos = -300.0f;
    }

    /**
     * テロップの描画位置を描画にかかった時間分だけ進める
     * @param duration 1描画にかかった時間
     */
    private void subPos(long duration) {
        mCurPos += ((float)duration / 1000.0f) * mSpeed;
    }

    /**
     * 現在のテロップ描画位置
     * @return 描画位置
     */
    private float getCurPos() {
        return mCurPos;
    }

    public void setAngle(int angle) {
        mAngle = angle;
    }

    private Matrix createDrawMatrix() {
        Matrix matrix = new Matrix();
        matrix.reset();

        float width = mHolder.getSurfaceFrame().width();
        float height = mHolder.getSurfaceFrame().height();

        switch(mAngle){
            case 270:
                matrix.preRotate(-90.0f);
                break;
            case 90:
                matrix.preRotate(90.0f);
                //matrix.preTranslate(0.0f, -width);
                //matrix.preTranslate(height, 0.0f);
                matrix.preTranslate(height, -width);
                break;
            case 0:
            default:
                matrix.preTranslate(width, 0.0f);
                break;
        }

        return matrix;
    }

    private int getRangeMax() {
        int width = mHolder.getSurfaceFrame().width();
        int height = mHolder.getSurfaceFrame().height();
        switch(mAngle){
            case 270:
            case 90:
                return height;
            case 0:
            default:
                return width;
        }
    }

    /**
     * テロップが移動するスピードを設定する
     * @param speed 1秒の間の進むピクセル数
     */
    public void setSpeed(int speed) {
        mSpeed = speed;
    }

    private int getSpeed() {
        return mSpeed;
    }

    /**
     * テロップのFPSを設定する
     * @param fps 1秒の間に描画される回数
     */
    public void setFps(int fps) {
        mFps = fps;
    }

    private int getFps() {
        return mFps;
    }

    public void setTextSize(int size) {
        mSize = size;
    }

    /**
     * Bitmapバッファリストに貯められたBitmapオブジェクト一覧を取得する
     * @return
     */
    private List<Bitmap> getLastBmps() {
        synchronized (mBmpBuffers) {
            if(mBmpBuffers.size() > 0){
                List<Bitmap> bmpList = new ArrayList<>();
                for(Bitmap bmp : mBmpBuffers) {
                    bmpList.add(bmp);
                }
                return bmpList;
            } else {
                return null;
            }
        }
    }

    /**
     * 指定されたテキストで文字描画を行い、生成されたBMPをBMPキューに追加する<br/>
     * キューに追加された文字BMPは次の周回から画面に文字描画される
     * @param text
     */
    public void updateText(String text) {

        // テキストをMAX_TEXT_UNIT単位で分割
        StringBuilder buffText = new StringBuilder();
        List<String> textList = new ArrayList<>();
        for(int i = 0; i < text.length(); i++) {
            buffText.append(text.charAt(i));
            if(buffText.length() >= MAX_TEXT_UNIT) {
                textList.add(buffText.toString());
                buffText.setLength(0);
            }
        }
        if(buffText.length() > 0) {
            textList.add(buffText.toString());
        }

        // 分割したテキストごとにBitmapを生成
        List<Bitmap> buffBmpList = new ArrayList<>();
        for(String textUnit : textList) {
            Bitmap textBmp = createTextBitmap(textUnit);
            buffBmpList.add(textBmp);
        }

        // 文字描画されたバッファBMPをバッファリスト登録する
        synchronized (mBmpBuffers) {
            if(mBmpBuffers.size() > 0){
                for(Bitmap bmp : mBmpBuffers) {
                    bmp.recycle();
                }
                mBmpBuffers.clear();
            }
            for(Bitmap buffBmp : buffBmpList) {
                mBmpBuffers.add(buffBmp);
            }
        }
    }

    /**
     * Bitmapオブジェクトを生成したいテキスト
     * @param text Bitmap生成対象テキスト
     * @return 生成したBitmapオブジェクト
     */
    private Bitmap createTextBitmap(String text) {
        // テキストサイズ取得
        Rect tSize = measureTextSize(text, mSize);

        // オフスクリーンキャンバス準備
        Bitmap buffBmp = Bitmap.createBitmap(tSize.width(), tSize.height(), Bitmap.Config.ARGB_8888);
        Canvas offCanvas = new Canvas(buffBmp);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setTextSize(mSize);
        offCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // オフスクリーンキャンバスに文字描画
        float y_pos = 0;
        if(Build.MODEL.equals(DeviceDef.HWLD_XM6502) || DeviceDef.isGroova() || Build.MODEL.equals(DeviceDef.MITACHI)){
            y_pos = mSize * 1.1f;
        } else {
            y_pos = mSize;
        }
        offCanvas.drawText(text, 0, y_pos, paint);

        return buffBmp;
    }

    /**
     * 指定の文字を描画した際のサイズを求める
     * @param text 描画サイズを求めたいテキスト
     * @param size 描画サイズを計算する際に使用する文字サイズ
     * @return 描画サイズ矩形
     */
    private Rect measureTextSize(String text, int size) {
        Paint paint = new Paint();
        paint.setTextSize(size);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);

        Rect result = new Rect();
        //paint.getTextBounds(text, 0, text.length(), result);
        float width = paint.measureText(text, 0, text.length());
        Paint.FontMetrics metrics = paint.getFontMetrics();
        result.right = (int)width;
        result.bottom = (int)(metrics.descent - metrics.ascent);
        return result;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        android.util.Log.e("test", "surfaceCreated:");
        // SurfaceHolder保持
        mHolder = holder;

        // 描画開始
        mDrawThread = new DrawingThread(holder, this);
        mDrawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        android.util.Log.e("test", "surfaceChanged:");
        if(mDrawThread == null){
            // SurfaceHolder保持
            mHolder = holder;

            // 描画位置初期化
            initPos();

            // 描画開始
            mDrawThread = new DrawingThread(holder, this);
            mDrawThread.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        android.util.Log.e("test", "surfaceDestroyed:");
        if(mDrawThread != null) {
            // 描画停止
            mDrawThread.requestStop();
            mDrawThread = null;
        }
    }

    private static class DrawingThread extends Thread {

        private SurfaceHolder mHolder;
        private boolean mIsRunning;
        private List<Bitmap> mTgtBmps;
        private TelopSurfaceView mView;

        public DrawingThread(SurfaceHolder holder, TelopSurfaceView view) {
            mHolder = holder;
            mView = view;
            mIsRunning = false;
            mTgtBmps = null;
        }

        @Override
        public void run() {
            try {
                long spf = 1000 / mView.getFps();
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

                float totalWidth = 0.0f;
                mIsRunning = true;
                while(mIsRunning){
                    long start = System.currentTimeMillis();

                    if(mTgtBmps != null) {
                        // 描画キャンバス取得（取得できなければ、描画終わり）
                        Canvas canvas = mHolder.lockCanvas();
                        if(canvas == null){
                            mIsRunning = false;
                            continue;
                        }

                        // 背景カラー描画
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                        // テキストBMP描画
                        float curPos = mView.getCurPos();
                        Matrix matrix = mView.createDrawMatrix();
                        matrix.preTranslate(-curPos,0.0f);
                        float addPos = 0.0f;
                        for(Bitmap tgtBmp : mTgtBmps) {
                            float bmpLeft = -curPos + addPos;
                            float bmpRight = -curPos + addPos + tgtBmp.getWidth();
                            float viewWidth = mView.getRangeMax();
                            if( ( bmpLeft < -viewWidth && -viewWidth < bmpRight ) ||
                                  ( -viewWidth <= bmpLeft && bmpLeft <= 0.0f ) ) {

                                canvas.drawBitmap(tgtBmp, matrix, paint);
                            }
                            matrix.preTranslate(tgtBmp.getWidth(),0.0f);
                            addPos += tgtBmp.getWidth();
                        }
                        mHolder.unlockCanvasAndPost(canvas);

                        // テキストの描画が一巡し、BMPキューにたまっている場合、テキストBMPを変更する
                        if(totalWidth + mView.getRangeMax() < curPos ) {
                            // 描画位置初期化
                            mView.initPos();
                            // テキストBMP入れ替え
                            List<Bitmap> lastBmps = mView.getLastBmps();
                            if(lastBmps != null) {
                                // テキストBMPリストクリア
                                for(Bitmap tgtBmp : mTgtBmps) {
                                    tgtBmp.recycle();
                                }
                                mTgtBmps.clear();      /////
                                // テキストBMP取得
                                for(Bitmap lastBmp : lastBmps){
                                    Bitmap tgtBmp = lastBmp.copy(lastBmp.getConfig(), true);
                                    mTgtBmps.add(tgtBmp);
                                }
                                // テキストの合計幅
                                totalWidth = 0.0f;
                                for(Bitmap tgtBmp : mTgtBmps) {
                                    totalWidth += tgtBmp.getWidth();
                                }
                            }
                        }
                    } else {
                        // 最新テキストBMP取得
                        List<Bitmap> lastBmps = mView.getLastBmps();
                        if(lastBmps != null) {
                            // テキストBMPリスト初期化
                            mTgtBmps = new ArrayList<>();
                            // テキストBMP取得
                            for(Bitmap lastBmp : lastBmps){
                                Bitmap tgtBmp = lastBmp.copy(lastBmp.getConfig(), true);
                                mTgtBmps.add(tgtBmp);
                            }
                            // テキストの合計幅
                            totalWidth = 0.0f;
                            for(Bitmap tgtBmp : mTgtBmps) {
                                totalWidth += tgtBmp.getWidth();
                            }
                        }
                    }

                    // 設定FPSに対して処理時間が短すぎた場合はスリープで調整
                    long interval = System.currentTimeMillis() - start;
                    if(spf > interval) {
                        Thread.sleep(spf - interval);
                    }

                    // 描画にかかった秒数分描画位置を移動
                    mView.subPos(spf > interval ? spf : interval);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                android.util.Log.e("test", e.getMessage());
            }
        }

        public void requestStop(){
            mIsRunning = false;
        }
    }
}

package jp.co.digitalcruise.admint.player.activity.viewer;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class ViewerSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private MediaPlayer mMediaPlayer = null;

    private boolean mMediaPlayerReady = false;

    public ViewerSurfaceView(Context context) {
        super(context);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
    }

    public void setMediaPlayer(MediaPlayer mp) {
        mMediaPlayer = mp;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mMediaPlayerReady){
            startMediaPlayer();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    public void startMediaPlayer(boolean is_start_after_change){
        if(is_start_after_change){
            mMediaPlayerReady = true;
        }else{
            startMediaPlayer();
        }
    }

    private void startMediaPlayer() {
        mMediaPlayerReady = false;
        mMediaPlayer.setDisplay(getHolder());
        mMediaPlayer.start();
    }

    public void startMediaPlayerChangeSize(int width, int height, boolean is_start_after_change) {
        getHolder().setFixedSize(width, height);
        startMediaPlayer(is_start_after_change);
//        if(is_start_after_change) {
//            mMediaPlayerReady = true;
//        }else{
//            startMediaPlayer();
//        }
    }
}
package jp.co.digitalcruise.admint.player.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;

import java.lang.reflect.Method;

import jp.co.digitalcruise.admint.player.BuildConfig;
import jp.co.digitalcruise.admint.player.component.define.DeviceDef;
import jp.co.digitalcruise.admint.player.component.log.Logging;

@SuppressLint("Registered")
public class AbstractAdmintActivity extends Activity {
    public static final String APPLICATION_ID = BuildConfig.APPLICATION_ID;

    @Override
    protected void onResume(){
        super.onResume();
        if(Build.MODEL.equals(DeviceDef.SHARP_BOX)) {
            getWindow().getDecorView().setSystemUiVisibility(0x00000008);
        }
    }

    protected Point getRealSize() {
        final int DEFAULT_DISPLAY_WIDTH = 1280;
        int DEFAULT_DISPLAY_HEIGHT = 800;

        Display display = getWindowManager().getDefaultDisplay();
        Point real = new Point(DEFAULT_DISPLAY_WIDTH, DEFAULT_DISPLAY_HEIGHT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealSize(real);
        } else {
            try {
                Method getRawWidth = Display.class.getMethod("getRawWidth");
                Method getRawHeight = Display.class.getMethod("getRawHeight");
                int width = (Integer) getRawWidth.invoke(display);
                int height = (Integer) getRawHeight.invoke(display);
                real.set(width, height);
            } catch (Exception e) {
                Logging.stackTrace(e);
            }
        }
        return real;
    }

}

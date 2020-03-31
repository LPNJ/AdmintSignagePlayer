package jp.co.digitalcruise.admint.player.activity.viewer;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

/*
 * Created by seki on 2016/04/21.
 * タッチコンテンツのダイアログ（言語変更等）でナビゲーションバーが出現するのを防ぐための処理
 * Sharpから伝授
 */
class FullScreenDialog extends Dialog {

    FullScreenDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getWindow() != null) {
            getWindow().getDecorView().setVisibility(View.GONE); // ちら見え対策
            getWindow().getDecorView().setSystemUiVisibility(0x00000008);
        }
    }

    @Override
    public void show() {
        super.show();
        if(getWindow() != null) {
            getWindow().getDecorView().setVisibility(View.VISIBLE); // ちら見え対策
        }
    }
}

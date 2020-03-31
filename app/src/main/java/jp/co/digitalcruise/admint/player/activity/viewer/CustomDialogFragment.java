package jp.co.digitalcruise.admint.player.activity.viewer;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.log.Logging;

/*
 * Created by seki on 2016/04/20.
 */
public class CustomDialogFragment extends DialogFragment {

    private static final String X = "X";
    private static final String Y = "Y";
    private static final String WIDTH = "WIDTH";
    private static final String HEIGHT = "HEIGHT";
    private static final String DISPLAY_RATIO_X = "DISPLAY_RATIO_X";
    private static final String DISPLAY_RATIO_Y = "DISPLAY_RATIO_Y";
    private static final String PATTERN_LIST = "PATTERN_LIST";

    public interface OnCustomDialogListener {
        void onCustomDialogClick(int which);
    }

    private OnCustomDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof OnCustomDialogListener)) {
            throw new UnsupportedOperationException("Listener is not Implementation.");
        } else {
            mListener = (OnCustomDialogListener) activity;
        }
    }

    int x1 = 0;
    int y1 = 0;
    int x2 = 0;
    int y2 = 0;
    double displayRatioX = 0;
    double displayRatioY = 0;

    private ArrayList<TouchContent.TouchPattern> castTouchPattern(Object a) {
        return (ArrayList<TouchContent.TouchPattern>)a;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = null;
        try {
            Bundle args = this.getArguments();

            x1 = args.getInt(X);
            y1 = args.getInt(Y);
            x2 = x1 + args.getInt(WIDTH);
            y2 = y1 + args.getInt(HEIGHT);
            displayRatioX = args.getDouble(DISPLAY_RATIO_X);
            displayRatioY = args.getDouble(DISPLAY_RATIO_Y);
            x1 = (int)((float)x1 * displayRatioX);
            x2 = (int)((float)x2 * displayRatioX);
            y1 = (int)((float)y1 * displayRatioY);
            y2 = (int)((float)y2 * displayRatioY);
            final ArrayList<TouchContent.TouchPattern> patternList = castTouchPattern(args.getSerializable(PATTERN_LIST));

            dialog = new FullScreenDialog(getActivity());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            ListView listView = new ListView(getActivity());
            LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(x2 -x1, ViewGroup.LayoutParams.WRAP_CONTENT);

            dialog.addContentView(listView, param);

            LinearLayout.LayoutParams image_param = new LinearLayout.LayoutParams(x2 - x1, y2 - y1);
            CustomAdapter adapter = new CustomAdapter(patternList, image_param);

            listView.setAdapter(adapter);
        } catch (Exception e) {
            Logging.stackTrace(e);
        }
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Dialog dialog = getDialog();

        WindowManager.LayoutParams param = dialog.getWindow().getAttributes();

        param.width = WindowManager.LayoutParams.WRAP_CONTENT;
        param.height = WindowManager.LayoutParams.WRAP_CONTENT;

        dialog.getWindow().setAttributes(param);
    }

    public class CustomAdapter extends BaseAdapter {

        private ArrayList<TouchContent.TouchPattern> mAdapterPatternList = null;
        private LinearLayout.LayoutParams mImageParam = null;

        public CustomAdapter(ArrayList<TouchContent.TouchPattern> pattern_list, LinearLayout.LayoutParams param) {
            super();
            mAdapterPatternList = pattern_list;
            mImageParam = param;
        }

        @Override
        public int getCount() {
            return mAdapterPatternList.size();
        }

        @Override
        public Object getItem(int position) {
            return mAdapterPatternList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            try{
                if(convertView == null) {
                    LayoutInflater inflater = LayoutInflater.from(getActivity());
                    convertView = inflater.inflate(R.layout.custom_list_row, parent, false);
                }
                TouchContent.TouchPattern pattern = mAdapterPatternList.get(position);
                ImageView image = (ImageView)convertView.findViewById(R.id.menu_image_id);

                Bitmap bm = BitmapFactory.decodeFile(pattern.icon.getAbsolutePath());
                image.setImageBitmap(bm);
                image.setLayoutParams(mImageParam);
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try{
                            if(mAdapterPatternList.size() > position){
                                dismiss();
                                mListener.onCustomDialogClick(position);
                            }
                        }catch(Exception e){
                            Logging.stackTrace(e);
                        }
                    }
                });
            }catch(Exception e){
                Logging.stackTrace(e);
            }
            return convertView;
        }
    }
}
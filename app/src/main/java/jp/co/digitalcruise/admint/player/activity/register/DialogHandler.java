package jp.co.digitalcruise.admint.player.activity.register;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.pref.DefaultPref;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class DialogHandler {
    private Runnable ans_true = null;
    private Runnable ans_false = null;
    private  CheckBox checkBox = null;

    // Dialog. --------------------------------------------------------------

    public void Confirm(Activity act, String Title, String CancelBtn, String OkBtn, Runnable aProcedure, Runnable bProcedure) {
        ans_true = aProcedure;
        ans_false= bProcedure;


        AlertDialog dialog = new AlertDialog.Builder(act).create();
        dialog.setTitle(Title);
        dialog.setCancelable(false);

        LayoutInflater layoutInflater = (LayoutInflater) act.getSystemService(LAYOUT_INFLATER_SERVICE);
        assert layoutInflater != null;
        final View dialogLayout = layoutInflater.inflate(R.layout.select_dialog_layout, act.findViewById(R.id.selectDialog_view));
        checkBox = dialogLayout.findViewById(R.id.next_view_flag);

        if(DefaultPref.getNotShowDialog()){
            checkBox.setChecked(true);
        }

        dialog.setView(dialogLayout);

        //チェックボックス動作
        dialogLayout.findViewById(R.id.next_view_flag).setOnClickListener(v ->{
            if(checkBox.isChecked()){
                DefaultPref.setNotShowDialog(true);
            } else {
                DefaultPref.setNotShowDialog(false);
            }
        });

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, OkBtn,
                (dialog1, buttonId) -> ans_true.run());
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, CancelBtn,
                (dialog12, buttonId) -> ans_false.run());
        dialog.setIcon(android.R.drawable.ic_dialog_info);
        dialog.show();
    }

    public void licenseViewer(Activity act, String title, String ok_btn){
        AlertDialog.Builder ad = new AlertDialog.Builder(act);
        ad.setTitle(title);

        LayoutInflater layoutInflater = (LayoutInflater)act.getSystemService(LAYOUT_INFLATER_SERVICE);
        assert layoutInflater != null;
        final View dialogLayout = layoutInflater.inflate(R.layout.license_view, act.findViewById(R.id.license_view));
        ad.setView(dialogLayout);

        ad.setPositiveButton(ok_btn, null);

        ad.show();


    }
}

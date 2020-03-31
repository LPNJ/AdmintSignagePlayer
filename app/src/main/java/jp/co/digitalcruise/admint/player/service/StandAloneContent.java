package jp.co.digitalcruise.admint.player.service;

import android.content.Intent;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import jp.co.digitalcruise.admint.player.AdmintApplication;
import jp.co.digitalcruise.admint.player.R;
import jp.co.digitalcruise.admint.player.component.file.AdmintPath;
import jp.co.digitalcruise.admint.player.component.file.FileUtil;
import jp.co.digitalcruise.admint.player.service.network.HealthCheckService;

import static jp.co.digitalcruise.admint.player.BuildConfig.APPLICATION_ID;

public class StandAloneContent extends AbstractService {

    private static final String ACTION_PREFIX = APPLICATION_ID + ".StandAloneContent.";

    public static final String ACTION_CP_CONTENT_DIR = ACTION_PREFIX + "CP_CONTENT_DIR";

    public StandAloneContent(String name) {
        super(name);
    }

    public StandAloneContent(){
        super(StandAloneContent.class.getName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        if (intent != null) {
            if (Objects.requireNonNull(intent.getAction()).equals(ACTION_CP_CONTENT_DIR)) {
                copyContentDirectory();
            }
        }
    }

    private void copyContentDirectory(){
        File out_dir = AdmintPath.getAplicationDir();
        File in_dir = new File(AdmintPath.getSdDir() + File.separator + "/AdmintPlayer" );

        try {
            if(in_dir.exists()){
                sendToast(getString(R.string.toast_msg_sdcard_start));
                copyDirectory(in_dir, out_dir);
                Intent intent = new Intent(AdmintApplication.getInstance().getApplicationContext(), HealthCheckService.class);
                intent.setAction(HealthCheckService.ACTION_LOAD_STAND_ALONE_SCHDULE);
                AdmintApplication.getInstance().getApplicationContext().startService(intent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void copyDirectory(File src, File dest) throws IOException {
        if(src.isDirectory()){
            if(!dest.exists()){
                FileUtil.makeDir(dest);
            }

            String[] origin_files = src.list();

            for(String cpfile : origin_files){
                File inFile = new File(src, cpfile);
                File outFile = new File(dest, cpfile);

                copyDirectory(inFile, outFile);
            }
        } else {
            InputStream in = new FileInputStream(src);
            OutputStream out = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];

            int length;

            while((length = in.read(buffer)) > 0){
                out.write(buffer, 0,length);
            }

            in.close();
            out.close();
        }
    }

}

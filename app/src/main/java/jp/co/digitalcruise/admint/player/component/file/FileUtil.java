package jp.co.digitalcruise.admint.player.component.file;

import android.os.Build;
import android.os.StatFs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;

public class FileUtil {

    public static void deleteRecursive(File parent){
        if(!parent.exists()){
            return ;
        }
        if(parent.isFile()){
            deleteFile(parent);
        }

        if(parent.isDirectory()){
            File[] files = parent.listFiles();
            if(files != null){
                for(File child : files){
                    deleteRecursive(child);
                }
                deleteFile(parent);
            }
        }
    }

    public static void copyTransfer(File src, File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(src.getAbsolutePath());
             FileOutputStream fos = new FileOutputStream(dest.getAbsolutePath());
             FileChannel srcChannel = fis.getChannel();
             FileChannel destChannel = fos.getChannel()) {
            srcChannel.transferTo(0, srcChannel.size(), destChannel);
        }
    }

    public static void createUtfFile(File file) throws IOException {
        if(!file.createNewFile()){
            throw new RuntimeException("File.createNewFile() return false ,file=" + file.getAbsolutePath());
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(0xef);
            fos.write(0xbb);
            fos.write(0xbf);
        }
    }

    public static void createFile(File file) throws IOException {
        if(!file.createNewFile()){
            throw new RuntimeException("File.createNewFile() return false ,file=" + file.getAbsolutePath());
        }
    }

    public static void renameFile(File src_file, File dest_file){
        if(!src_file.renameTo(dest_file)){
            throw new RuntimeException("File.renameTo() return false ,src file=" + src_file.getAbsolutePath() + ", dest file=" + dest_file.getAbsolutePath());
        }
    }

    public static void deleteFile(File file){
        if(!file.delete()){
            throw new RuntimeException("File.delete() return false, file=" + file.getAbsolutePath());
        }
    }

    public static void makeDir(File dir){
        if(!dir.mkdir()){
            throw new RuntimeException("File.mkdir() return false, dir=" + dir.getAbsolutePath());
        }
    }

    static void makeDirs(File dirs){
        if(!dirs.mkdirs()){
            throw new RuntimeException("File.mkdirs() return false, dir=" + dirs.getAbsolutePath());
        }
    }


    @SuppressWarnings("deprecation")
    public static long getAvailableMemorySize(File dir){
        StatFs fs = new StatFs(dir.getAbsolutePath());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return fs.getBlockSizeLong() * fs.getAvailableBlocksLong();
        } else {
            return (long)fs.getBlockSize() * (long)fs.getAvailableBlocks();
        }
    }

    @SuppressWarnings("deprecation")
    public static long getTotalMemorySize(File dir){
        StatFs fs = new StatFs(dir.getAbsolutePath());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return fs.getBlockSizeLong() * fs.getBlockCountLong();
        } else {
            return (long)fs.getBlockSize() * (long)fs.getBlockCount();
        }
    }

    public static String loadFile(String path) throws IOException {
        StringBuilder result = new StringBuilder();
        FileInputStream is = new FileInputStream(new File(path));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String buffer;
            while ((buffer = reader.readLine()) != null) {
                result.append(buffer);
            }
        }
        return result.toString();
    }
}

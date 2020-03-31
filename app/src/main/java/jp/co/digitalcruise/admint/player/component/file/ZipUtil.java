package jp.co.digitalcruise.admint.player.component.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtil {
    public static void unZip(File in_zip, File out_dir) throws IOException {
        FileOutputStream fos = null;

        long extended_size = 0;
        long zip_size = 0;
        try (FileInputStream fis = new FileInputStream(in_zip); ZipInputStream zis = new ZipInputStream(fis)) {

            ZipEntry ze;
            int len;
            byte[] readbuf = new byte[4096];
            while ((ze = zis.getNextEntry()) != null) {
                try {
                    // zipされたコンテンツのサイズ
                    zip_size += ze.getSize();

                    File ext_file = new File(out_dir.getAbsolutePath() + File.separator + ze.getName());

                    if (ze.isDirectory()) {
                        FileUtil.makeDirs(ext_file);
                    } else {
                        // ディレクトリ構成を復元
                        File parent_dir = ext_file.getParentFile();
                        if (!parent_dir.exists() || !parent_dir.isDirectory()) {
                            FileUtil.makeDirs(parent_dir);
                        }

                        fos = new FileOutputStream(ext_file.getAbsolutePath());
                        while ((len = zis.read(readbuf, 0, readbuf.length)) > 0) {
                            fos.write(readbuf, 0, len);
                        }

                        if (ext_file.isFile()) {
                            // 展開後のファイルサイズ
                            extended_size += ext_file.length();
                        }
                    }
                } finally {
                    if (fos != null) {
                        fos.close();
                        fos = null;
                    }
                    zis.closeEntry();
                }
            }
            // サイズが合わない場合は例外をスロー
            if (zip_size != extended_size) {
                throw new RuntimeException("miss match size zip_size = " + zip_size + ", extended_size = " + extended_size);
            }
        }
    }

    public static void makeZip(File target_path, File zip_path) throws IOException {
        // 出力先ファイル
        try (FileOutputStream fos = new FileOutputStream(zip_path);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos)) {
            archiveZip(target_path, zos, target_path);
            zos.finish();
        }
    }

    private static void archiveZip(File target_path, ZipOutputStream zos, File arc_file) throws IOException {
        if (arc_file.isDirectory()) {
            File[] files = arc_file.listFiles();
            for (File f : files) {
                archiveZip(target_path, zos, f);
            }
        }else{
            try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(arc_file))) {
                // 入力ストリーム生成

                // Entry 名称を取得する。
                String entry_path = arc_file.getAbsolutePath().replace(target_path.getAbsolutePath(), "");
                String entry_name;
                if (entry_path.length() > 0) {
                    entry_name = entry_path.substring(1);
                } else {
                    entry_name = target_path.getName();
                }

                // 出力先 Entry を設定する。
                zos.putNextEntry(new ZipEntry(entry_name));

                // 入力ファイルを読み込み出力ストリームに書き込んでいく
                int ava;
                while ((ava = fis.available()) > 0) {
                    byte[] bs = new byte[ava];
                    if (fis.read(bs) != -1) {
                        zos.write(bs);
                    }
                }

                // 書き込んだら Entry を close する。
                zos.closeEntry();
            }
        }
    }


}

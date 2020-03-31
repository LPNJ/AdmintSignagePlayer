package jp.co.digitalcruise.admint.player.component.file;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.regex.Pattern;

/**
 * コンテンツファイル・ディレクトリ情報取得クラス
 *
 * @author yterashima
 */
public class ContentFile {

    // メディアファイルの拡張子
    private static final Pattern PATTERN_MEDIA_EXT = Pattern.compile("(?i).*\\.(mp4|jpg|jpeg|png)$");

    /**
     * デイリーコンテンツサブディレクトリ取得
     * @param id コンテンツID
     * @param f_id サブID
     * @return コンテンツディレクトリ
     */
    public static@NonNull File getExternalDir(int id, int f_id) {
        return new File(AdmintPath.getContentsDir() + File.separator + id + File.separator + f_id);
    }

    /**
     * コンテンツディレクトリ取得
     * @param id コンテンツID
     * @return コンテンツディレクトリ
     */
    public static@NonNull File getContentDir(int id) {
        return new File(AdmintPath.getContentsDir() + File.separator + id);
    }

    /**
     * ファイル名から拡張子除去
     * @param str ファイル名
     * @return 拡張子を除去したファイル名
     */
    public static String splitFileExt(@NonNull String str){
        int pos = str.lastIndexOf('.');
        if(pos >= 0){
            return str.substring(0, pos);
        }else{
            return str;
        }
    }

    /**
     * メディアファイル取得
     * ファイルが存在しない時はNullが返る
     * @param dir メディアファイルのディレクトリ
     * @return メディアファイル
     */
    public static @Nullable File getMediaFile(File dir){

        if(!dir.isDirectory()){
            return null;
        }

        File[] files = dir.listFiles();
        if(files != null){
            for(File f : files){
                // ファイルかつ、拡張子が対応メディアファイルと一致
                if(f.isFile() && PATTERN_MEDIA_EXT.matcher(f.getName()).matches()){
                    return new File(f.getAbsolutePath());
                }
            }
        }
        return null;
    }

    /**
     * コンテンツファイル取得
     * ファイルが存在しない時はNullが返る
     * @param id コンテンツID
     * @return コンテンツファイル
     */
    public static @Nullable File getContentMediaFile(int id){
        File dir = getContentDir(id);
        if(dir.isDirectory()) {
            return getMediaFile(dir);
        }
        return null;
    }

    /**
     * デイリーコンテンツファイル取得
     * ファイルが存在しない時はNullが返る
     * @param id コンテンツID
     * @param f_id サブID
     * @return コンテンツファイル
     */
    public static @Nullable File getExternalMediaFile(int id, int f_id){
        File dir = getExternalDir(id, f_id);
        if(dir.isDirectory()){
//            File external_dir = new File(dir.getAbsolutePath() + File.separator + f_id);
            return getMediaFile(dir);
        }
        return null;
    }

    /**
     * タッチコンテンツdata.xmlファイル取得
     * ファイルの有無に関わらずFileオブジェクトを返す
     * @param id コンテンツID
     * @return コンテンツファイル
     */
    public static @NonNull File getTouchDataFile(int id){
        final String DATA_XML = "data.xml";
        File dir = getContentDir(id);
        return new File(dir.getAbsolutePath() + File.separator + DATA_XML);
    }

    /**
     * タッチコンテンツlist.xmlファイル取得
     * ファイルの有無に関わらずFileオブジェクトを返す
     * @param id コンテンツID
     * @return コンテンツファイル
     */
    public static @NonNull File getTouchListFile(int id){
        final String LIST_XML = "list.xml";
        File dir = getContentDir(id);
        return new File(dir.getAbsolutePath() + File.separator + LIST_XML);
    }

    /**
     * タッチコンテンツReadyファイル取得
     * ファイルの有無に関わらずFileオブジェクトを返す
     * @param id コンテンツID
     * @return Readyファイル
     */
    public static @NonNull File getTouchContentReady(int id){
        final String TOUCH_CONTENT_READY = "touch_content_ready.dci";
        return new File(getContentDir(id).getAbsolutePath() + File.separator + TOUCH_CONTENT_READY);
    }
}

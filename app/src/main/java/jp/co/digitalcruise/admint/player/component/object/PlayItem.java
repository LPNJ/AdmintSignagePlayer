package jp.co.digitalcruise.admint.player.component.object;

import android.support.annotation.NonNull;

public class PlayItem implements Comparable<PlayItem>{

    public int id = 0;
    public int o = 0;
    public int f_id = 0;
    public int f_o = 0;
    public int t = -1;
    public String w_url = "";
//    public int w_up = 0;
    public int d = 0;
    public int tcid = 0;
    public int tcd = 0;
    public int view_type = -1;
    public String media_path = "";
    public String touch_media_path = "";

    @Override
    public int compareTo(@NonNull PlayItem play_item) {
        if(o < play_item.o){
            return -1;
        }else if(o > play_item.o){
            return 1;
        }else{
            if(f_o < play_item.f_o){
                return -1;
            }else if(f_o > play_item.f_o){
                return 1;
            }
        }
        return 0;
    }
}


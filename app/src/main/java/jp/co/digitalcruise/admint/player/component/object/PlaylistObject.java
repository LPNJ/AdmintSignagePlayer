package jp.co.digitalcruise.admint.player.component.object;

import java.util.ArrayList;

public class PlaylistObject {
    public static final int SCHEDULE_NOTHING = -1;
    public int sched_type = SCHEDULE_NOTHING;
    public long st = 0;
    public long ut = 0;
    public long pt = 0;
    public int po = 0;
    public int tcd = 0;
    public int tcid = 0;
    public long update_at = 0;

    public ArrayList<PlayItem> play_items = new ArrayList<>();

}

package jp.co.digitalcruise.admint.player.component.parsexml.object;

import java.util.ArrayList;

public class ScheduleObject{
    public class Info{
        public int sched_type = 0;
        public long update_at = 0;
        public long start_time = 0;
        public long end_time = 0;
    }

    public class Program{
        public long st = 0;
        public long ut = 0;
        public long pt = 0;
        public int po = 0;
        public int tcid = 0;
        public int tcd = 0;
    }

    public class Content{
        public long st = 0;
        public int o = 0;
        public int id = 0;
        public int d = 0;
        public int t = 0;
        public int u = 0;
        public int tcid = 0;
        public int tcd = 0;
    }

    public class External{
        public int id = 0;
        public int f_id = 0;
        public int f_o = 0;
        public String f_name = "";
    }

    public class Webview{
        public int id = 0;
        public String w_url = "";
//        public int w_up = 0;
    }

    public class RescueContent{
        public int id = 0;
        public String type = "";
        public String duration = "";
        public String use = "";
        public String order = "";
        public int expires = 0;
        public String file_name = "";
    }
    public class RescueContentFile{
        public int order = 0;
        public String content_name = "";
    }
    public class RescueTelop{
        public String order = "";
        public String text = "";
        public int expires = 0;
    }
    public class RescueTelopInfo{
        public int direction = 0;
        public int rotate = 0;
    }

    public Info info = new Info();
    public ArrayList<Program> programs = new ArrayList<Program>();
    public ArrayList<Content> contents = new ArrayList<Content>();
    public ArrayList<External> externals = new ArrayList<External>();
    public ArrayList<Webview> webviews = new ArrayList<Webview>();
    public ArrayList<RescueContent> rescue_content = new ArrayList<RescueContent>();
    public ArrayList<RescueContentFile> rescue_content_file = new ArrayList<>();
    public ArrayList<RescueTelopInfo> rescue_telop_info = new ArrayList<RescueTelopInfo>();
    public ArrayList<RescueTelop> rescue_telop = new ArrayList<RescueTelop>();
}

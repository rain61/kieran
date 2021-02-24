package kieran.uitls;

import kieran.constant.KieranConstant;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DateUtils {

    /*
     * 毫秒转化时分秒毫秒
     */
    public static Map<String,Object> formatTime(Long ms) {
        Integer ss = 1000;
        Integer mi = ss * 60;
        Integer hh = mi * 60;
        Integer dd = hh * 24;

        Long day = ms / dd;
        Long hour = (ms - day * dd) / hh;
        Long minute = (ms - day * dd - hour * hh) / mi;
        Long second = (ms - day * dd - hour * hh - minute * mi) / ss;
        //Long milliSecond = ms - day * dd - hour * hh - minute * mi - second * ss;

        Map<String,Object> map = new ConcurrentHashMap<String, Object>();
        if(day > 0) {
            map.put("dim", KieranConstant.DAY_DIMENSION);
            map.put("index",day);
            return map;
        }
        if(hour > 0) {
            map.put("dim", KieranConstant.HOURS_DIMENSION);
            map.put("index",hour);
            return map;
        }
        if(minute > 0) {
            map.put("dim", KieranConstant.MINUTE_DIMENSION);
            map.put("index",minute);
            return map;
        }
        if(second > 0) {
            map.put("dim", KieranConstant.SECOND_DIMENSION);
            map.put("index",second);
            return map;
        }
        return map;
    }
}

package com.netty.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonUtil {

    /**
     *
     * @param date  传入的时间
     * @param format 需要格式化的 标准字符串 如 yyyy-MM-dd
     * @return 格式化后的时间字符串
     */
    public static String DateToString(Date date,String format){
        return new SimpleDateFormat(format).format(date);
    }
}

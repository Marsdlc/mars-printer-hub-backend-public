package cn.mars.marsprinterhub.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public static String format(LocalDateTime time) {
        return time == null ? "" : time.format(DATETIME_FORMATTER);
    }

    public static String formatTimestamp(LocalDateTime time) {
        return time == null ? "" : time.format(TIMESTAMP);
    }
}
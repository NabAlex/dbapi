package app.util;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeWork {
    public static String getIsoTime(Timestamp timestamp) {
        return timestamp.toInstant()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static Timestamp toZonedDateTime(String date) {
        return getTimeStampByUTC(ZonedDateTime.parse(date).format(DateTimeFormatter.ISO_INSTANT));
    }

    public static Timestamp getTimeStampByUTC(String time) {
        return new Timestamp( toLong(time) );
    }
    
    public static long toLong(String utcTime) {
        return ZonedDateTime.parse(utcTime).toInstant().toEpochMilli();
    }
}

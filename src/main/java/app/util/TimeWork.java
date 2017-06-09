package app.util;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimeWork {
    public static String getIsoTime(Timestamp timestamp) {
        return timestamp.toInstant()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static Timestamp toZonedDateTime(String date) {
        return setUTC(ZonedDateTime.parse(date).format(DateTimeFormatter.ISO_INSTANT));
    }

    public static Timestamp setUTC(String time) {
        return new Timestamp(ZonedDateTime.parse(time).toLocalDateTime().toInstant(ZoneOffset.UTC).toEpochMilli());
    }
}

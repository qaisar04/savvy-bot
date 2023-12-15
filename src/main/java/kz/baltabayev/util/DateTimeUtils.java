package kz.baltabayev.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class DateTimeUtils {

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public String parseDate(LocalDateTime dateTime) {
        return dateTime.format(dateFormatter);
    }

    public String parseTime(LocalDateTime dateTime) {
        return dateTime.format(timeFormatter);
    }

    public String parseDateTime(LocalDateTime dateTime) {
        return dateTime.format(dateTimeFormatter);
    }

}

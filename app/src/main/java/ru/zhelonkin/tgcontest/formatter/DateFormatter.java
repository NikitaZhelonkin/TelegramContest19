package ru.zhelonkin.tgcontest.formatter;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class DateFormatter implements Formatter {

    private static final String DATE_FORMAT = "MMM dd";

    private static String capitalize(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    @Override
    public String format(long value) {
        return capitalize(new SimpleDateFormat(DATE_FORMAT, Locale.US).format(value));
    }
}

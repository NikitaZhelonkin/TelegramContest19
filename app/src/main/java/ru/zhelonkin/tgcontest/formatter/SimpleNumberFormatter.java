package ru.zhelonkin.tgcontest.formatter;

import java.text.Format;
import java.text.NumberFormat;
import java.util.Locale;

public class SimpleNumberFormatter implements Formatter {

    private Format mFormat;

    public SimpleNumberFormatter() {
        mFormat = NumberFormat.getNumberInstance(Locale.US);
    }

    @Override
    public String format(long value) {
        return mFormat.format(value);
    }
}

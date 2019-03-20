package ru.zhelonkin.tgcontest.formatter;

import java.text.NumberFormat;
import java.util.Locale;

public class NumberFormatter implements Formatter {

    @Override
    public String format(long number) {
        if (number > 0 && number < 0.1) {
            return String.format(Locale.US, "%.4f", number);
        }
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(0);
        if (number < 10000) return numberFormat.format(number);
        numberFormat.setMinimumFractionDigits(1);
        numberFormat.setMaximumFractionDigits(1);
        int exp = Math.min(2, (int) (Math.log(number) / Math.log(1000)));
        String numberStr = numberFormat.format(number / Math.pow(1000, exp));
        char character = "kM".charAt(exp - 1);
        return String.format(Locale.US, "%s%c", numberStr, character);
    }
}

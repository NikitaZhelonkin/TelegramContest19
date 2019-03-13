package ru.zhelonkin.tgcontest;

import android.content.Context;
import android.preference.PreferenceManager;

public class Prefs {

    private static final String PREFS_DARK_MODE = "prefs_dark_mode";

    public static boolean isDarkMode(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFS_DARK_MODE, false);
    }

    public static void setDarkMode(Context context, boolean darkMode) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(PREFS_DARK_MODE, darkMode)
                .apply();
    }
}

package ru.zhelonkin.tgcontest.utils;

import android.content.Context;
import android.content.res.TypedArray;

public class ThemeUtils {

    public static int getColor(Context context, int attr, int defaultColor){
        TypedArray a = context.obtainStyledAttributes(new int[]{attr});
        int color = a.getColor(0, defaultColor);
        a.recycle();
        return color;
    }
}

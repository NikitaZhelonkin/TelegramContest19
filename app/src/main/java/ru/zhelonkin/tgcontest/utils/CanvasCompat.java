package ru.zhelonkin.tgcontest.utils;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Region;
import android.os.Build;

public class CanvasCompat {

    public static void clipOutPath(Canvas canvas, Path path){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canvas.clipOutPath(path);
        }else {
            canvas.clipPath(path, Region.Op.DIFFERENCE);
        }
    }
}

package ru.zhelonkin.tgcontest.widget.chart.touch;

import android.view.MotionEvent;

public interface TouchHandler {

    boolean onTouchEvent(MotionEvent event);

    int getTarget();

    void setTarget(int target);
}

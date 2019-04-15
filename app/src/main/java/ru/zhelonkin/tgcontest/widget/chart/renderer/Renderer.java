package ru.zhelonkin.tgcontest.widget.chart.renderer;

import android.graphics.Canvas;
import android.view.MotionEvent;

public interface Renderer {

    void render(Canvas canvas);

    boolean onTouchEvent(MotionEvent event);

    int getTarget();

    void setTarget(int target);
}

package ru.zhelonkin.tgcontest.widget.chart.renderer;


import ru.zhelonkin.tgcontest.widget.chart.OnTargetChangeListener;

public abstract class BaseRenderer implements Renderer, OnTargetChangeListener {

    private Viewport mViewport;

    private int mTarget;

    BaseRenderer(Viewport viewport) {
        mViewport = viewport;
    }

    Viewport getViewport() {
        return mViewport;
    }

    float pointX(long x) {
        return mViewport.pointX(x);
    }

    float pointY(float y) {
        return mViewport.pointY(y);
    }

    @Override
    public void onTargetChanged(int target) {
        mTarget = target;
    }

    public int getTarget() {
        return mTarget;
    }
}

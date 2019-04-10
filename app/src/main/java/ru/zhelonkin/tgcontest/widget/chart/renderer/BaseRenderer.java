package ru.zhelonkin.tgcontest.widget.chart.renderer;



public abstract class BaseRenderer implements Renderer {

    private Viewport mViewport;

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

}

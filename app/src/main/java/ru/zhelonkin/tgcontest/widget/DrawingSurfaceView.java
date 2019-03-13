package ru.zhelonkin.tgcontest.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class DrawingSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    public interface Composer {
        void draw(Canvas canvas);
    }

    private DrawingThread drawingThread;

    private Composer composer;

    public DrawingSurfaceView(Context context) {
        this(context, null);
    }

    public DrawingSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawingSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
    }



    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        drawingThread = new DrawingThread(new SurfaceViewHolder(holder));
        drawingThread.setRunning(true);
        drawingThread.start();
        drawingThread.setComposer(composer);
    }

    public void setComposer(Composer composer) {
        this.composer = composer;
        if (drawingThread != null) {
            drawingThread.setComposer(composer);
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;

        drawingThread.setRunning(false);

        while (retry) {
            try {
                drawingThread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public interface ISurfaceHolder {

        void unlockCanvasAndPost(Canvas canvas);

        Canvas lockCanvas();

    }

    public static class SurfaceViewHolder implements ISurfaceHolder {
        private final SurfaceHolder surfaceHolder;

        public SurfaceViewHolder(SurfaceHolder surfaceHolder){
            this.surfaceHolder = surfaceHolder;
        }

        @Override
        public void unlockCanvasAndPost(Canvas canvas) {
            surfaceHolder.unlockCanvasAndPost(canvas);
        }

        @Override
        public Canvas lockCanvas() {
            return surfaceHolder.lockCanvas();
        }
    }

    public static class DrawingThread extends Thread {
        private Composer mComposer;
        private ISurfaceHolder surfaceHolder;
        private boolean isRunning = false;

        public DrawingThread(ISurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;
        }

        public void setComposer(Composer composer) {
            mComposer = composer;
        }

        public void setRunning(boolean run) {
            isRunning = run;
        }

        @Override
        public void run() {
            Canvas canvas;
            while (isRunning) {
                canvas = surfaceHolder.lockCanvas();

                if (canvas != null) {
                    if (mComposer != null) {
                        mComposer.draw(canvas);
                    }
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }


    }

}

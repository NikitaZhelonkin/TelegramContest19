package ru.zhelonkin.tgcontest.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

import java.lang.ref.WeakReference;

public class DrawingTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    public interface Composer {
        void draw(Canvas canvas);
    }

    private DrawingThread drawingThread;

    private Composer composer;

    public DrawingTextureView(Context context) {
        this(context, null);
    }

    public DrawingTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawingTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        drawingThread = new DrawingThread(new TextureViewHolder(this));
        drawingThread.setComposer(composer);
        drawingThread.setRunning(true);
        drawingThread.start();
    }

    public void setComposer(Composer composer) {
        this.composer = composer;
        if (drawingThread != null) {
            drawingThread.setComposer(composer);
        }

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
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
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public interface ISurfaceHolder {

        void unlockCanvasAndPost(Canvas canvas);

        Canvas lockCanvas();

    }

    public static class TextureViewHolder implements ISurfaceHolder {

        private final TextureView textureView;

        TextureViewHolder(TextureView textureView) {
            this.textureView = textureView;
        }

        @Override
        public void unlockCanvasAndPost(Canvas canvas) {
            textureView.unlockCanvasAndPost(canvas);
        }

        @Override
        public Canvas lockCanvas() {
            return textureView.lockCanvas();
        }
    }

    public static class DrawingThread extends Thread {
        private Composer mComposer;
        private final WeakReference<ISurfaceHolder> surfaceHolder;
        private boolean isRunning = false;

        DrawingThread(ISurfaceHolder surfaceHolder) {
            this.surfaceHolder = new WeakReference<>(surfaceHolder);
        }

        void setComposer(Composer composer) {
            mComposer = composer;
        }

        void setRunning(boolean run) {
            isRunning = run;
        }

        @Override
        public void run() {
            Canvas canvas;
            while (isRunning) {
                ISurfaceHolder holder = surfaceHolder.get();
                if (holder != null) {
                    canvas = holder.lockCanvas();

                    synchronized (surfaceHolder) {
                        if (canvas != null) {
                            if (mComposer != null) {
                                mComposer.draw(canvas);
                            }
                            holder.unlockCanvasAndPost(canvas);
                        }
                    }
                }


            }
        }


    }


}

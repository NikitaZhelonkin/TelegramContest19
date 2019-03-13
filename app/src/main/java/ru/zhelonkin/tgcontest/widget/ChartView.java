package ru.zhelonkin.tgcontest.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Line;
import ru.zhelonkin.tgcontest.model.PointL;

public class ChartView extends View {

    private Graph mGraph;

    private float mChartScaleX = 1;
    private float mChartScaleY = 1;

    private float mChartLeft = 0;

    private float mTargetChartScaleY = mChartScaleY;

    private ObjectAnimator mScaleYAnimator;

    private AnimatorSet mLineAlphaAnimator;

    private Paint mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);


    public ChartView(Context context) {
        super(context);
        init(context, null);
    }

    public ChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ChartView);
        int strokeWidth = a.getDimensionPixelSize(R.styleable.ChartView_strokeWidth, 1);
        a.recycle();

        mLinePaint.setStrokeWidth(strokeWidth);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setGraph(@NonNull Graph graph) {
        mGraph = graph;
        setChartScaleY(calculateScaleY());
        invalidate();
    }

    public void updateGraphLines() {
        setChartScaleYSmooth(calculateScaleY());

        if (mLineAlphaAnimator != null) mLineAlphaAnimator.cancel();
        List<Animator> animatorList = new ArrayList<>();
        for (Line line : mGraph.getLines()) {
            if (line.isVisible() && line.getAlpha() != 1) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(line, "alpha", 1);
                animator.addUpdateListener(animation -> invalidate());
                animatorList.add(animator);
            } else if (!line.isVisible() && line.getAlpha() == 1) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(line, "alpha", 0);
                animator.addUpdateListener(animation -> invalidate());
                animatorList.add(animator);
            }
        }
        mLineAlphaAnimator = new AnimatorSet();
        mLineAlphaAnimator.playTogether(animatorList);
        mLineAlphaAnimator.setInterpolator(new FastOutSlowInInterpolator());
        mLineAlphaAnimator.setDuration(200);
        mLineAlphaAnimator.start();
        invalidate();
    }

    public void setChartScaleYSmooth(float chartScaleY) {
        if (mTargetChartScaleY != chartScaleY) {
            mTargetChartScaleY = chartScaleY;
            if (mScaleYAnimator != null) mScaleYAnimator.cancel();
            mScaleYAnimator = ObjectAnimator.ofFloat(this, "chartScaleY", mTargetChartScaleY);
            mScaleYAnimator.setInterpolator(new FastOutSlowInInterpolator());
            mScaleYAnimator.setDuration(200);
            mScaleYAnimator.start();
        }
    }

    public void setChartScaleY(float chartScaleY) {
        mChartScaleY = chartScaleY;
        invalidate();
    }

    public float getChartScaleY() {
        return mChartScaleY;
    }

    public void setLeftAndRight(float left, float right) {
        mChartLeft = left;
        mChartScaleX = (right - left);
        setChartScaleYSmooth(calculateScaleY());
        invalidate();

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mGraph == null) return;
        for (Line line : mGraph.getLines()) {
            drawLine(canvas, line);
        }
    }


    private void drawLine(Canvas canvas, Line line) {
        mLinePaint.setColor(line.getColor());
        mLinePaint.setAlpha((int) (255 * line.getAlpha()));

        PointL[] points = line.getPoints();
        float lastX = pointX(points[0].x);
        float lastY = pointY(points[0].y);
        for (int i = 1; i < points.length; i++) {
            float x = pointX(points[i].x);
            float y = pointY(points[i].y);
            canvas.drawLine(lastX, lastY, x, y, mLinePaint);
            lastX = x;
            lastY = y;
        }
    }

    private float calculateScaleY() {
        if (mGraph == null) return 1;
        float maxY = Float.MIN_VALUE;
        for (Line line : mGraph.getLines()) {
            if (!line.isVisible()) continue;
            PointL[] points = line.getPoints();
            for (int i = 1; i < points.length; i++) {
                float x = pointX(points[i].x);
                if (x > -getWidth() / 4 && x < getWidth() * 5 / 4) {
                    if (points[i].y > maxY) {
                        maxY = points[i].y;
                    }
                }
            }
        }
        return maxY / mGraph.rangeY();
    }

    private float translationX() {
        return -mGraph.rangeX() * scaleX() * mChartLeft;
    }

    private float pointX(long x) {
        return (x - mGraph.minX()) * scaleX() + translationX();
    }

    private float pointY(long y) {
        return getHeight() - (y - mGraph.minY()) * scaleY();
    }

    private float scaleX() {
        return getWidth() / mGraph.rangeX() * 1 / mChartScaleX;
    }

    private float scaleY() {
        return getHeight() / mGraph.rangeY() * 1 / mChartScaleY;
    }


}

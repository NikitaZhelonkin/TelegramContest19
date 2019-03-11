package ru.zhelonkin.tgcontest.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;

import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Line;
import ru.zhelonkin.tgcontest.model.PointL;

public class ChartView extends View {

    private Graph mGraph;

    private float mChartScaleX = 1;
    private float mChartScaleY = 1;

    private float mChartLeft = 0;
    private float mChartBottom = 0;

    private float mTargetChartScaleY = mChartScaleY;

    private ObjectAnimator mScaleYAnimator;

    private Paint mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);


    public ChartView(Context context) {
        super(context);
        init(context);
    }

    public ChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mLinePaint.setStrokeWidth(6);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setGraph(Graph graph) {
        mGraph = graph;
        invalidate();
    }


    public void setChartScaleYSmooth(float chartScaleY) {
        if (mTargetChartScaleY != chartScaleY) {
            mTargetChartScaleY = chartScaleY;
            if (mScaleYAnimator != null) mScaleYAnimator.cancel();
            mScaleYAnimator = ObjectAnimator.ofFloat(this, "chartScaleY", mTargetChartScaleY);
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
        int saveCount = canvas.save();
        canvas.translate(translationX(), translationY());

        for (Line line : mGraph.getLines()) {
            drawLine(canvas, line);
        }

        canvas.restoreToCount(saveCount);
    }


    private void drawLine(Canvas canvas, Line line) {
        mLinePaint.setColor(line.getColor());

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
        float maxY = Float.MIN_VALUE;
        for (Line line : mGraph.getLines()) {
            PointL[] points = line.getPoints();
            for (int i = 1; i < points.length; i++) {
                float x = pointX(points[i].x) + translationX();
                if (x > -getWidth()/3 && x < getWidth()*4/3) {
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

    private float translationY() {
        return mGraph.rangeY() * scaleY() * mChartBottom;
    }

    private float pointX(long x) {
        return (x - mGraph.minX()) * scaleX();
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

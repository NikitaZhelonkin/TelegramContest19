package ru.zhelonkin.tgcontest.widget.chart.renderer;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.annotation.Keep;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import java.util.List;

import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Point;
import ru.zhelonkin.tgcontest.utils.Alpha;
import ru.zhelonkin.tgcontest.widget.chart.ChartView;

public class PieChartRenderer implements Renderer {

    private static final int MIN_TEXT_SIZE = 10;
    private static final int MAX_TEXT_SIZE = 24;
    private static final int CURRENT_OFFSET = 8;

    private ChartView mView;
    private Chart mChart;
    private Viewport mViewport;

    private Paint mPaint;

    private TextPaint mTextPaint;

    private RectF mRectF = new RectF();
    private RectF mRectCurrent = new RectF();

    private final float mMinTextSize;
    private final float mMaxTextSize;

    private final float mCurrentOffset;

    private final AnimatedFloat[] mOffsets;


    public PieChartRenderer(ChartView view, Chart chart, Viewport viewport) {
        mView = view;
        mChart = chart;
        mViewport = viewport;

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);

        DisplayMetrics dm = view.getContext().getResources().getDisplayMetrics();
        mMinTextSize = dm.density * MIN_TEXT_SIZE;
        mMaxTextSize = dm.density * MAX_TEXT_SIZE;

        mCurrentOffset = dm.density * CURRENT_OFFSET;

        mOffsets = new AnimatedFloat[chart.getGraphs().size()];
        for (int i = 0; i < mOffsets.length; i++) {
            mOffsets[i] = new AnimatedFloat(0f);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                setCurrentItem(hitItem(event.getX(), event.getY()));
                break;
        }
        return true;
    }

    private void setCurrentItem(int currentItem) {
        for (int i = 0; i < mOffsets.length; i++) {
            mOffsets[i].animate(currentItem == i ? mCurrentOffset : 0);
        }
    }

    @Override
    public void render(Canvas canvas, int targetPosition) {
        float lastAngle = 0;
        float startAngle = 0;

        invalidateBounds();


        int leftIndex = (int) (mViewport.getChartLeft() * (mChart.getXValues().size()));
        int rightIndex = (int) (mViewport.getChartRight() * (mChart.getXValues().size()));
        rightIndex = Math.min(mChart.getXValues().size() - 1, rightIndex);

        long total = sum(leftIndex, rightIndex);
        for (int i = 0; i < mChart.getGraphs().size(); i++) {
            Graph graph = mChart.getGraphs().get(i);
            long value = sum(graph.getPoints(), leftIndex, rightIndex);
            float percent = value * 100f / total * graph.getAlpha();
            float sweepAngle = 360 * percent / 100f;
            mPaint.setColor(graph.getColor());
            float bisectorAngle = startAngle + lastAngle + sweepAngle / 2;

            mRectCurrent.set(mRectF);
            float offsetX = (float) (mOffsets[i].getValue() * Math.cos(Math.toRadians(bisectorAngle)));
            float offsetY = (float) (mOffsets[i].getValue() * Math.sin(Math.toRadians(bisectorAngle)));
            mRectCurrent.offset(offsetX, offsetY);
            canvas.drawArc(mRectCurrent, startAngle + lastAngle, sweepAngle, true, mPaint);
            float textX = (float) (mRectCurrent.centerX() + mRectCurrent.width() / 3 * Math.cos(Math.toRadians(bisectorAngle)));
            float textY = (float) (mRectCurrent.centerY() + mRectCurrent.height() / 3 * Math.sin(Math.toRadians(bisectorAngle)));
            mPaint.setColor(Color.BLACK);
            String text = (int) percent + "%";
            mTextPaint.setTextSize(Math.max(mMinTextSize, Math.min(mMaxTextSize, percent / 100f * 2 * (mMaxTextSize))));
            float textWidth = mTextPaint.measureText(text);
            float textHeight = mTextPaint.descent() - mTextPaint.ascent();
            mTextPaint.setAlpha(Alpha.toInt(graph.getAlpha()));
            canvas.drawText(text, textX - textWidth / 2, textY - mTextPaint.descent() + textHeight / 2, mTextPaint);
            lastAngle += sweepAngle;
        }
    }

    private void invalidateBounds() {
        int viewWidth = mView.getWidth() - mView.getPaddingLeft() - mView.getPaddingRight();
        int viewHeight = mView.getHeight() - mView.getPaddingTop() - mView.getPaddingBottom();
        int size = Math.min(viewWidth, viewHeight);
        int left = mView.getPaddingLeft() + (viewWidth - size) / 2;
        int top = mView.getPaddingTop() + (viewHeight - size) / 2;
        mRectF.set(left, top, left + size, top + size);
    }

    private int hitItem(float x, float y) {
        float lastAngle = 0;
        float startAngle = 0;

        float cx = mRectF.centerX();
        float cy = mRectF.centerY();
        double dst = Math.sqrt(Math.pow((cx - x), 2) + Math.pow((cy - y), 2));

        int leftIndex = (int) (mViewport.getChartLeft() * (mChart.getXValues().size() - 1));
        int rightIndex = (int) (mViewport.getChartRight() * (mChart.getXValues().size() - 1));
        long total = sum(leftIndex, rightIndex);

        for (int i = 0; i < mChart.getGraphs().size(); i++) {
            Graph graph = mChart.getGraphs().get(i);
            long value = sum(graph.getPoints(), leftIndex, rightIndex);
            float percent = value * 100f / total * graph.getAlpha();
            float sweepAngle = 360 * percent / 100f;

            double angle = anglePoint(x, y, cx, cy) % 360;

            float start = startAngle + lastAngle;
            float end = start + sweepAngle;

            boolean hitRadius = dst < mRectF.width() / 2;
            boolean hitAngle = angle > start && angle < end;
            if (hitRadius && hitAngle) return i;
            lastAngle += sweepAngle;
        }
        return -1;
    }

    private double anglePoint(float x1, float y1, float cx, float cy) {
        double angle = Math.toDegrees(Math.atan2(y1 - cy, x1 - cx));
        return angle < 0 ? angle + 360 : angle;
    }


    private long sum(int from, int to) {
        long sum = 0;
        for (int i = from; i <= to; i++) {
            sum += mChart.getSums()[i];
        }
        return sum;
    }

    private long sum(List<Point> points, int from, int to) {
        long sum = 0;
        for (int i = from; i <= to; i++) {
            sum += points.get(i).y;
        }
        return sum;
    }

    private class AnimatedFloat {

        private float mValue;

        public AnimatedFloat(float value) {
            mValue = value;
        }

        @Keep
        public void setValue(float value) {
            mValue = value;
        }

        @Keep
        public float getValue() {
            return mValue;
        }

        public void animate(float value) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(this, "value", value);
            animator.setDuration(200);
            animator.setAutoCancel(true);
            animator.addUpdateListener(animation -> mView.invalidate());
            animator.start();
        }
    }
}

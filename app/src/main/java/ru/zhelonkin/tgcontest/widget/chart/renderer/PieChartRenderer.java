package ru.zhelonkin.tgcontest.widget.chart.renderer;

import android.animation.ObjectAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.annotation.Keep;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import java.util.Collections;
import java.util.List;

import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Point;
import ru.zhelonkin.tgcontest.utils.Alpha;
import ru.zhelonkin.tgcontest.widget.chart.ChartPopupView;
import ru.zhelonkin.tgcontest.widget.chart.ChartView;
import ru.zhelonkin.tgcontest.widget.chart.OnTargetChangeListener;

public class PieChartRenderer implements Renderer, OnTargetChangeListener {

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

    private final Slice[] mSlices;


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

        mSlices = new Slice[chart.getGraphs().size()];
        for (int i = 0; i < mSlices.length; i++) {
            mSlices[i] = new Slice();
        }
    }


    @Override
    public void onTargetChanged(int target) {
        for (int i = 0; i < mSlices.length; i++) {
            mSlices[i].animateOffset(target == i ? mCurrentOffset : 0);
        }
    }


    @Override
    public void render(Canvas canvas) {
        float lastAngle = 0;
        float startAngle = 0;

        invalidateBounds();
        fillSlices();

        for (int i = 0; i < mSlices.length; i++) {
            Graph graph = mChart.getGraphs().get(i);
            float percent = mSlices[i].percent;
            float offset = mSlices[i].offset;
            float sweepAngle = 360 * percent / 100f;
            mPaint.setColor(graph.getColor());
            float bisectorAngle = startAngle + lastAngle + sweepAngle / 2;

            mRectCurrent.set(mRectF);
            float offsetX = (float) (offset * Math.cos(Math.toRadians(bisectorAngle)));
            float offsetY = (float) (offset * Math.sin(Math.toRadians(bisectorAngle)));
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

    private void fillSlices() {
        int leftIndex = leftIndex();
        int rightIndex = rightIndex();


        long total = sum(leftIndex, rightIndex);
        for (int i = 0; i < mChart.getGraphs().size(); i++) {
            Graph graph = mChart.getGraphs().get(i);
            long value = sum(graph.getPoints(), leftIndex, rightIndex);
            float percent = value * 100f / total * graph.getAlpha();
            mSlices[i].value = value;
            mSlices[i].percent = percent;
        }
    }

    private int leftIndex() {
        return (int) (mViewport.getChartLeft() * (mChart.getXValues().size()));
    }

    private int rightIndex() {
        int rightIndex = (int) (mViewport.getChartRight() * (mChart.getXValues().size()));
        return Math.min(mChart.getXValues().size() - 1, rightIndex);
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

    private class Slice {
        long value;
        float percent;
        float offset;

        @Keep
        public void setOffset(float offset) {
            this.offset = offset;
        }

        @Keep
        public float getOffset() {
            return offset;
        }

        public void animateOffset(float offset) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(this, "offset", offset);
            animator.setDuration(200);
            animator.setAutoCancel(true);
            animator.addUpdateListener(animation -> mView.invalidate());
            animator.start();
        }
    }

    public int hitItem(float x, float y) {
        float lastAngle = 0;
        float startAngle = 0;

        float cx = mRectF.centerX();
        float cy = mRectF.centerY();
        double dst = Math.sqrt(Math.pow((cx - x), 2) + Math.pow((cy - y), 2));
        double angle = anglePoint(x, y, cx, cy) % 360;

        for (int i = 0; i < mSlices.length; i++) {
            float percent = mSlices[i].percent;
            float sweepAngle = 360 * percent / 100f;

            float start = startAngle + lastAngle;
            float end = start + sweepAngle;

            boolean hitRadius = dst < mRectF.width() / 2;
            boolean hitAngle = angle > start && angle < end;
            if (hitRadius && hitAngle) return i;
            lastAngle += sweepAngle;
        }
        return ChartView.INVALID_TARGET;
    }

    public long getValue(int target){
        return mSlices[target].value;
    }

}

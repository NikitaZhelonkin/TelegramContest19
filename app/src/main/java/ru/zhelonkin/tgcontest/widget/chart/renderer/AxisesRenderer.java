package ru.zhelonkin.tgcontest.widget.chart.renderer;

import android.animation.ObjectAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Keep;
import android.text.TextPaint;
import android.util.LongSparseArray;
import android.view.View;

import java.util.concurrent.TimeUnit;

import ru.zhelonkin.tgcontest.formatter.CachingFormatter;
import ru.zhelonkin.tgcontest.formatter.DateFormatter;
import ru.zhelonkin.tgcontest.formatter.Formatter;
import ru.zhelonkin.tgcontest.formatter.NumberFormatter;
import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.utils.Alpha;
import ru.zhelonkin.tgcontest.widget.chart.ChartView;

public class AxisesRenderer extends BaseRenderer {

    private final Formatter NUMBER_FORMATTER = new CachingFormatter(new NumberFormatter());

    private static final long HOUR = TimeUnit.HOURS.toMillis(1);

    private static final int X_GRID_COUNT = 6;
    private static final int Y_GRID_COUNT = 4;

    private LongSparseArray<Value> mYValues = new LongSparseArray<>();
    private LongSparseArray<Value> mXValues = new LongSparseArray<>();

    private long mXGridSize = -1;

    private TextPaint mTextPaint;

    private Paint mGridPaint;

    private int mTextPadding;

    private View mView;

    private Viewport mViewportSecondary;

    private Chart mChart;

    private int mDefaultTextColor;

    private Formatter mDateFormatter =  new CachingFormatter(new DateFormatter("MMM dd"));

    public AxisesRenderer(ChartView view, Chart chart, Viewport viewport, Viewport viewportSecondary,
                          Paint gridPaint,
                          TextPaint textPaint,
                          int textPadding) {
        super(view, chart, viewport);
        mView = view;
        mChart = chart;
        mViewportSecondary = viewportSecondary;
        mTextPadding = textPadding;
        mGridPaint = gridPaint;
        mTextPaint = textPaint;
        mDefaultTextColor = mTextPaint.getColor();
    }

    public void setDateFormatter(Formatter dateFormatter) {
        mDateFormatter = new CachingFormatter(dateFormatter);
    }

    @Override
    public void render(Canvas canvas) {
        drawXAxis(canvas);
        drawYAxis(canvas);
    }

    public void updateGrid(boolean animate) {
        updateXGridSize(animate);
        updateYGridSize(animate);
    }

    private void drawYAxis(Canvas canvas) {
        int count = canvas.save();
        boolean yScaled = mChart.isYScaled();
        canvas.clipRect(mView.getPaddingLeft(), 0, mView.getWidth() - mView.getPaddingRight(), mView.getHeight() - mView.getPaddingBottom());
        for (int i = 0; i < mYValues.size(); i++) {
            long index = mYValues.keyAt(i);
            Value v = mYValues.get(index);
            if (v != null && v.getAlpha() != 0) {
                //draw primary axis
                float y = getViewport().pointY(v.value);
                mGridPaint.setAlpha(Alpha.toInt((v.getAlpha() * 0.1f)));
                canvas.drawLine(mView.getPaddingLeft(), y, mView.getWidth() - mView.getPaddingRight(), y, mGridPaint);
                mTextPaint.setAlpha(Alpha.toInt(v.getAlpha() * 0.5f));
                if (yScaled) {
                    mTextPaint.setColor(mChart.getGraphs().get(0).getColor());
                    mTextPaint.setAlpha(Alpha.toInt(mChart.getGraphs().get(0).getAlpha() * v.getAlpha()));
                }
                canvas.drawText(NUMBER_FORMATTER.format(v.value), mView.getPaddingLeft(), y - mTextPadding, mTextPaint);

                //draw secondary axis
                if (yScaled) {
                    mTextPaint.setColor(mChart.getGraphs().get(1).getColor());
                    mTextPaint.setAlpha(Alpha.toInt(mChart.getGraphs().get(1).getAlpha() * v.getAlpha()));
                    long valueSecondary = mViewportSecondary.valueY(y);
                    String text = NUMBER_FORMATTER.format(valueSecondary);
                    float textWidth = mTextPaint.measureText(text);
                    canvas.drawText(text, mView.getWidth() - mView.getPaddingRight() - textWidth, y - mTextPadding, mTextPaint);
                }
            }
            mGridPaint.setAlpha(Alpha.toInt(0.1f));
        }
        mTextPaint.setColor(mDefaultTextColor);
        canvas.restoreToCount(count);
    }


    private void drawXAxis(Canvas canvas) {
        for (int i = 0; i < mXValues.size(); i++) {
            long index = mXValues.keyAt(i);
            Value v = mXValues.get(index);
            if (v != null && v.getAlpha() != 0) {
                float x = getViewport().pointX(v.value);
                mTextPaint.setAlpha(Alpha.toInt(v.getAlpha() * 0.5f));
                canvas.drawText(mDateFormatter.format(v.value), x, mView.getHeight() - mTextPaint.descent(), mTextPaint);
            }
        }
    }

    private void updateXGridSize(boolean animate) {
        long rangeY = (long) (getViewport().chartScaleX() * (long) getViewport().rangeX());
        long gridSize = Math.max(HOUR, calcXGridSize(rangeY/HOUR, X_GRID_COUNT) * HOUR);
        if (mXGridSize == gridSize) return;
        mXGridSize = gridSize;
        for (int i = 0; i < mXValues.size(); i++) {
            long index = mXValues.keyAt(i);
            Value v = mXValues.get(index);
            if (v != null) {
                v.setVisible((index - getViewport().minX()) % gridSize == 0, animate);
            }
        }
        for (long i = getViewport().minX(); i < getViewport().maxX(); i += gridSize) {
            if (mXValues.indexOfKey(i) < 0) {
                Value value = new Value(i);
                mXValues.put(value.value, value);
                value.setVisible(true, animate);
            }
        }
    }

    private void updateYGridSize(boolean animate) {
        long rangeY = (long) ((getViewport().targetChartScaleY()) * getViewport().rangeY());
        long gridSize = calcYGridSize(rangeY, Y_GRID_COUNT);

        for (int i = 0; i < mYValues.size(); i++) {
            long index = mYValues.keyAt(i);
            Value v = mYValues.get(index);
            if (v != null) {
                float targetPoint = getViewport().targetPointY(v.value);
                boolean targetVisible = targetPoint >= mView.getPaddingTop() && targetPoint <= mView.getHeight() - mView.getPaddingBottom();
                v.setVisible(targetVisible && (index - getViewport().minY()) % gridSize == 0, animate);
            }
        }
        for (long i = getViewport().minY(); i <= getViewport().maxY(); i += gridSize) {
            float targetPoint = getViewport().targetPointY(i);
            boolean targetVisible = targetPoint >= mView.getPaddingTop() && targetPoint <= mView.getHeight() - mView.getPaddingBottom();
            if (mYValues.indexOfKey(i) < 0 && targetVisible) {
                Value value = new Value(i);
                mYValues.put(i, value);
                value.setVisible(true, animate);
            }
        }
    }

    private long calcYGridSize(long range, int count) {
        int[] steps = new int[]{1, 2, 5, 10, 20, 40, 50, 100};
        long avg = range / count;
        int degree = 1;
        long temp = avg;
        while (temp > 100) {
            temp /= 10;
            degree *= 10;
        }
        int step = steps[0];
        long minDist = Math.abs(step - temp);
        for (int s : steps) {
            long dist = Math.abs(s - temp);
            if (dist < minDist) {
                minDist = dist;
                step = s;
            }
        }
        return step * degree;
    }

    private long calcXGridSize(long range, int count) {
        return (long) Math.pow(2, Math.ceil(Math.log(range / (float) count) / Math.log(2)));
    }


    public class Value {

        long value;

        private float alpha = 1;
        private float targetAlpha = 1;

        Value(long value) {
            this.value = value;
        }

        @Keep
        public void setAlpha(float alpha) {
            this.alpha = alpha;
        }

        @Keep
        public float getAlpha() {
            return alpha;
        }

        void setVisible(boolean visible, boolean animate) {
            if (!animate) {
                targetAlpha = visible ? 1 : 0;
                alpha = targetAlpha;
                mView.invalidate();
                return;
            }
            if (visible && targetAlpha != 1) {
                animateAlpha(1);
            } else if (!visible && targetAlpha != 0) {
                animateAlpha(0);
            }
        }

        private void animateAlpha(float alpha) {
            targetAlpha = alpha;
            ObjectAnimator animator = ObjectAnimator.ofFloat(this, "alpha", alpha);
            animator.setAutoCancel(true);
            animator.addUpdateListener(it -> {
                mView.invalidate();
            });
            animator.setDuration(250);
            animator.start();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Value value1 = (Value) o;

            return this.value == value1.value;
        }

        @Override
        public int hashCode() {
            return (int) (value ^ (value >>> 32));
        }
    }
}


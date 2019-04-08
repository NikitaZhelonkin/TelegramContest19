package ru.zhelonkin.tgcontest.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import ru.zhelonkin.tgcontest.Alpha;
import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.formatter.CachingFormatter;
import ru.zhelonkin.tgcontest.formatter.DateFormatter;
import ru.zhelonkin.tgcontest.formatter.Formatter;
import ru.zhelonkin.tgcontest.formatter.NumberFormatter;
import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Point;

public class ChartView extends FrameLayout {

    private final Formatter DATE_FORMATTER = new CachingFormatter(new DateFormatter("MMM dd"));
    private final Formatter NUMBER_FORMATTER = new CachingFormatter(new NumberFormatter());

    private static final int INVALID_TARGET = -1;

    private Chart mChart;

    private float[] mStackBuffer;

    private Axises mAxises;

    private float mChartLeft = 0;
    private float mChartRight = 1;
    private float mChartTop = 1;
    private float mChartBottom = 0;

    private float mTargetChartTop = mChartTop;
    private float mTargetChartBottom = mChartBottom;

    private ObjectAnimator mChartTopBotAnimator;

    private AnimatorSet mLineAlphaAnimator;

    private Paint mGraphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private Path mGraphPath = new Path();

    private int mSurfaceColor;

    private boolean mIsPreviewMode;

    private int mTargetPosition = INVALID_TARGET;

    private boolean mIsDragging;

    private float mTouchDownX;

    private int mScaledTouchSlop;

    private float mTextPadding;

    private ChartPopupView mChartPopupView;

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

    @SuppressLint("ResourceType")
    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ChartView);
        int lineWidth = a.getDimensionPixelSize(R.styleable.ChartView_lineWidth, 1);
        int gridColor = a.getColor(R.styleable.ChartView_gridColor, Color.BLACK);
        mSurfaceColor = a.getColor(R.styleable.ChartView_surfaceColor, Color.WHITE);
        mIsPreviewMode = a.getBoolean(R.styleable.ChartView_previewMode, false);
        mTextPadding = a.getDimensionPixelSize(R.styleable.ChartView_textPadding, 0);

        int textAppearance = a.getResourceId(R.styleable.ChartView_textAppearance, -1);
        a.recycle();

        mGraphPaint.setStrokeWidth(lineWidth);
        mGraphPaint.setStyle(Paint.Style.STROKE);
        mGraphPaint.setStrokeCap(Paint.Cap.SQUARE);
        mGraphPaint.setStrokeJoin(Paint.Join.ROUND);

        mGridPaint.setStrokeWidth(lineWidth / 2f);
        mGridPaint.setColor(gridColor);

        if (textAppearance != -1) {
            a = context.obtainStyledAttributes(textAppearance, new int[]{android.R.attr.textSize, android.R.attr.textColor});
            mTextPaint.setTextSize(a.getDimensionPixelSize(0, 12));
            mTextPaint.setColor(a.getColor(1, Color.BLACK));
            a.recycle();
        }

        setWillNotDraw(false);

        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mChartPopupView = new ChartPopupView(context);
        mChartPopupView.hide(false);
        LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 0);
        addView(mChartPopupView, layoutParams);
    }

    public void setChart(@NonNull Chart chart) {
        mChart = chart;
        mAxises = new Axises();
        mStackBuffer = new float[chart.getXValues().size()];
        float[] range = calculateRangeY();
        setChartTopAndBottom(range[1], range[0], false);
        invalidate();
    }


    public void updateGraphLines() {
        if (mChart == null) return;
        float[] range = calculateRangeY();
        setChartTopAndBottom(range[1], range[0], true);

        if (mLineAlphaAnimator != null) mLineAlphaAnimator.cancel();
        List<Animator> animatorList = new ArrayList<>();
        for (Graph graph : mChart.getGraphs()) {
            if (graph.isVisible() && graph.getAlpha() != 1) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(graph, "alpha", 1);
                animator.addUpdateListener(animation -> invalidate());
                animatorList.add(animator);
            } else if (!graph.isVisible() && graph.getAlpha() != 0) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(graph, "alpha", 0);
                animator.addUpdateListener(animation -> invalidate());
                animatorList.add(animator);
            }
        }
        mLineAlphaAnimator = new AnimatorSet();
        mLineAlphaAnimator.playTogether(animatorList);
        mLineAlphaAnimator.setInterpolator(new FastOutSlowInInterpolator());
        mLineAlphaAnimator.setDuration(250);
        mLineAlphaAnimator.start();
        invalidate();
    }


    public void setChartLeftAndRight(float left, float right, boolean animate) {
        if (mChart == null) return;
        mChartLeft = left;
        mChartRight = right;
        float[] range = calculateRangeY();
        setChartTopAndBottom(range[1], range[0], animate);
        mAxises.updateXGridSize(animate);
        invalidate();
    }

    public void setChartTopAndBottom(float top, float bot, boolean animate) {
        if (animate) {
            if (mTargetChartTop != top || mTargetChartBottom != bot) {
                mTargetChartTop = top;
                mTargetChartBottom = bot;
                mAxises.updateYGridSize(true);
                if (mChartTopBotAnimator != null) mChartTopBotAnimator.cancel();
                PropertyValuesHolder pvhTop = PropertyValuesHolder.ofFloat("chartTop", mTargetChartTop);
                PropertyValuesHolder pvhBop = PropertyValuesHolder.ofFloat("chartBottom", mTargetChartBottom);
                mChartTopBotAnimator = ObjectAnimator.ofPropertyValuesHolder(this, pvhTop, pvhBop);
                mChartTopBotAnimator.addUpdateListener(animation -> {
                    invalidate();
                });
                mChartTopBotAnimator.setInterpolator(new FastOutSlowInInterpolator());
                mChartTopBotAnimator.setDuration(250);
                mChartTopBotAnimator.start();

            }
        } else {
            mTargetChartTop = top;
            mTargetChartBottom = bot;
            mAxises.updateYGridSize(false);
            setChartTop(top);
            setChartBottom(bot);
            invalidate();
        }
    }

    @Keep
    public void setChartTop(float chartTop) {
        mChartTop = chartTop;
    }

    @Keep
    public void setChartBottom(float chartBottom) {
        mChartBottom = chartBottom;
    }

    @Keep
    public float getChartTop() {
        return mChartTop;
    }

    @Keep
    public float getChartBottom() {
        return mChartBottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mChart == null) return;
        for (int i = 0; i < mStackBuffer.length; i++) {
            mStackBuffer[i] = 0;
        }
        if (!mIsPreviewMode && mTargetPosition != INVALID_TARGET) drawX(canvas, pointX(mChart.getXValues().get(mTargetPosition)));
        for (Graph graph : mChart.getGraphs()) {
            drawGraph(canvas, graph);

        }
        if (!mIsPreviewMode) drawYAxis(canvas);
        if (!mIsPreviewMode) drawXAxis(canvas);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || mIsPreviewMode || mChart == null)
            return false;

        final int action = event.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownX = event.getX();
            case MotionEvent.ACTION_MOVE:
                final float x = event.getX();
                if (!mIsDragging) {
                    if (Math.abs(x - mTouchDownX) > mScaledTouchSlop) {
                        mIsDragging = true;
                        attemptClaimDrag();
                    }
                }
                setTarget(mChart.findTargetPosition(valueX(x)));
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsDragging = false;
                setTarget(INVALID_TARGET);
                break;
        }

        return true;

    }

    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    private void setTarget(int target) {
        if (mTargetPosition != target) {
            if (!mChart.hasVisibleGraphs()) {
                target = INVALID_TARGET;
            }
            if (target == INVALID_TARGET) {
                hidePopup();
            } else if (mChartPopupView.isShowing()) {
                updatePopup(target);
            } else {
                showPopup(target);
            }
            mTargetPosition = target;
            invalidate();
        }
    }

    private void showPopup(int targetPosition) {
        if (!mChartPopupView.isShowing()) {
            mChartPopupView.show(true);
            mChartPopupView.bindData(mChart.pointsAt(targetPosition));
            updatePopupPosition(targetPosition);
        }
    }

    private void updatePopup(int targetPosition) {
        if (mChartPopupView.isShowing()) {
            mChartPopupView.bindData(mChart.pointsAt(targetPosition));
            updatePopupPosition(targetPosition);
        }
    }

    private void updatePopupPosition(int targetPosition) {
        float x = pointX(mChart.getXValues().get(targetPosition));
        mChartPopupView.measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int popupWidth = mChartPopupView.getMeasuredWidth();
        float position = Math.max(0, Math.min(chartWidth() - popupWidth, x - popupWidth / 2f));
        mChartPopupView.setTranslationX(position);
    }

    private void hidePopup() {
        if (mChartPopupView.isShowing()) {
            mChartPopupView.hide(true);
        }
    }


    private void drawGraph(Canvas canvas, Graph graph) {
        mGraphPaint.setColor(graph.getColor());
        mGraphPaint.setAlpha(Alpha.toInt(graph.getAlpha()));

        if (Graph.TYPE_LINE.equals(graph.getType())) {
            drawLineGraph(canvas, graph);
        } else if (Graph.TYPE_BAR.equals(graph.getType())) {
            drawBarGraph(canvas, graph);
        } else if (Graph.TYPE_AREA.equals(graph.getType())) {
            drawAreaGraph(canvas, graph);
        }
    }

    private void drawLineGraph(Canvas canvas, Graph graph) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            drawLineWithPath(canvas, graph);
        } else {
            drawLineDefault(canvas, graph);
        }
        if (mTargetPosition != INVALID_TARGET && graph.isVisible())
            drawDot(canvas, graph);
    }

    private void drawLineDefault(Canvas canvas, Graph graph) {
        List<Point> points = graph.getPoints();
        float[] lineBuffer = new float[points.size() * 4];
        float lastX = pointX(points.get(0).x);
        float lastY = pointY(points.get(0).y);
        int j = 0;
        for (int i = 1; i < points.size(); i++) {
            float x = pointX(points.get(i).x);
            float y = pointY(points.get(i).y);
            lineBuffer[j++] = lastX;
            lineBuffer[j++] = lastY;
            lineBuffer[j++] = x;
            lineBuffer[j++] = y;
            lastX = x;
            lastY = y;
        }
        canvas.drawLines(lineBuffer, mGraphPaint);
    }

    private void drawLineWithPath(Canvas canvas, Graph graph) {
        mGraphPath.reset();
        List<Point> points = graph.getPoints();
        mGraphPath.moveTo(pointX(points.get(0).x), pointY(points.get(0).y));
        for (int i = 1; i < points.size(); i++) {
            mGraphPath.lineTo(pointX(points.get(i).x), pointY(points.get(i).y));
        }
        canvas.drawPath(mGraphPath, mGraphPaint);
    }


    private void drawBarGraph(Canvas canvas, Graph graph) {
        mBarPaint.setStyle(Paint.Style.STROKE);
        mBarPaint.setColor(graph.getColor());
        List<Point> points = graph.getPoints();
        float first = pointX(points.get(0).x);
        float last = pointX(points.get(points.size() - 1).x);
        float width = (last - first) / points.size()+0.5f;
        mBarPaint.setStrokeWidth(width);
        float[] lineBuffer = new float[points.size() * 4];
        int j = 0;
        for (int i = 0; i < points.size(); i++) {
            float startX = pointX(points.get(i).x);
            float height = (getHeight() - getPaddingBottom() - pointY(points.get(i).y)) * graph.getAlpha();
            float endY = mStackBuffer[i] == 0 ? getHeight() - getPaddingBottom() : mStackBuffer[i];
            float startY = endY - height;
            lineBuffer[j++] = startX;
            lineBuffer[j++] = startY;
            lineBuffer[j++] = startX;
            lineBuffer[j++] = endY;
            mStackBuffer[i] = mChart.isStacked() ? startY : 0;
        }

        mBarPaint.setAlpha(Alpha.toInt(mTargetPosition == INVALID_TARGET ? 1 : 0.8f));
        canvas.drawLines(lineBuffer, mBarPaint);

        if (mTargetPosition != INVALID_TARGET) {
            mBarPaint.setAlpha(Alpha.toInt(1));
            float startX = lineBuffer[mTargetPosition * 4];
            float startY = lineBuffer[mTargetPosition * 4 + 1];
            float endX = lineBuffer[mTargetPosition * 4 + 2];
            float endY = lineBuffer[mTargetPosition * 4 + 3];
            canvas.drawLine(startX, startY, endX, endY, mBarPaint);
        }
    }

    private void drawAreaGraph(Canvas canvas, Graph graph) {
        //TODO draw area
        drawLineGraph(canvas, graph);
    }

    private void drawDot(Canvas canvas, Graph graph) {
        mGraphPaint.setColor(graph.getColor());
        Point point = graph.getPoints().get(mTargetPosition);
        float x = pointX(point.x);
        float y = pointY(point.y);
        int color = mGraphPaint.getColor();
        mGraphPaint.setColor(mSurfaceColor);
        mGraphPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(x, y, mGraphPaint.getStrokeWidth() * 2, mGraphPaint);
        mGraphPaint.setColor(color);
        mGraphPaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(x, y, mGraphPaint.getStrokeWidth() * 2, mGraphPaint);
    }

    private void drawX(Canvas canvas, float x) {
        canvas.drawLine(x, getPaddingTop(), x, getHeight() - getPaddingBottom(), mGridPaint);
    }

    private void drawYAxis(Canvas canvas) {
        int count = canvas.save();
        canvas.clipRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        for (Long index : mAxises.mYValues.keySet()) {
            Axises.Value v = mAxises.mYValues.get(index);
            float y = pointY(v.value);
            if (v.getAlpha() != 0) {
                mGridPaint.setAlpha(Alpha.toInt(v.getAlpha() * 0.1f));
                canvas.drawLine(getPaddingLeft(), y, getWidth() - getPaddingRight(), y, mGridPaint);
                mTextPaint.setAlpha(Alpha.toInt(v.getAlpha() * 0.5f));
                canvas.drawText(NUMBER_FORMATTER.format(v.value), getPaddingLeft(), y - mTextPadding, mTextPaint);
            }
            mGridPaint.setAlpha(Alpha.toInt(0.1f));
        }
        canvas.restoreToCount(count);
    }


    private void drawXAxis(Canvas canvas) {
        for (Long index : mAxises.mXValues.keySet()) {
            Axises.Value v = mAxises.mXValues.get(index);
            float x = pointX(v.value);
            if (v.getAlpha() != 0) {
                mTextPaint.setAlpha(Alpha.toInt(v.getAlpha() * 0.5f));
                canvas.drawText(DATE_FORMATTER.format(v.value), x, getHeight() - mTextPaint.descent(), mTextPaint);
            }
        }
    }

    private float[] calculateRangeY() {
        if (mChart == null) return new float[]{0, 1};
        long minY = Long.MAX_VALUE;
        long maxY = Long.MIN_VALUE;
        long[] sumArr = new long[mChart.getXValues().size()];
        for (Graph graph : mChart.getGraphs()) {
            if (!graph.isVisible()) continue;
            List<Point> points = graph.getPoints();
            for (int i = 0; i < points.size(); i++) {
                Point point = points.get(i);
                float x = pointX(point.x);
                if (x >= 0 && x <= getWidth()) {
                    if (point.y < minY) {
                        minY = point.y;
                    }
                    if (point.y > maxY) {
                        maxY = point.y;
                    }
                    sumArr[i] += point.y;
                }

            }
        }
        if (mChart.isStacked()) {
            //find max sum
            for (long sum : sumArr) {
                if (sum > maxY) maxY = sum;
            }
        }
        float[] result = new float[2];
        result[0] = 0;//minY == Long.MAX_VALUE ? 0 : (minY - mChart.minY()) / mChart.rangeY();
        result[1] = maxY == Long.MIN_VALUE ? 1 : (maxY - mChart.minY()) / mChart.rangeY();
        return result;
    }

    private long valueX(float pointX) {
        return (long) ((pointX + mChart.minX() * scaleX() - translationX() - getPaddingLeft()) / scaleX());
    }

    private float pointX(long x) {
        return getPaddingLeft() + (x - mChart.minX()) * scaleX() + translationX();
    }

    private float pointY(long y) {
        return getHeight() - getPaddingBottom() - (y - mChart.minY()) * scaleY() + translationY();
    }


    private float translationX() {
        return -mChart.rangeX() * scaleX() * mChartLeft;
    }

    private float translationY() {
        return mChart.rangeY() * scaleY() * mChartBottom;
    }

    private float scaleX() {
        return chartWidth() / mChart.rangeX() * 1 / chartScaleX();
    }

    private float scaleY() {
        return chartHeight() / mChart.rangeY() * 1 / chartScaleY();
    }

    private float chartScaleX() {
        return mChartRight - mChartLeft;
    }

    private float chartScaleY() {
        return mChartTop - mChartBottom;
    }

    private int chartWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int chartHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    //target values for animation
    private float targetPointY(long y) {
        return getHeight() - getPaddingBottom() - (y - mChart.minY()) * targetScaleY() + targetTranslationY();
    }

    private float targetTranslationY() {
        return mChart.rangeY() * targetScaleY() * mTargetChartBottom;
    }

    private float targetScaleY() {
        return chartHeight() / mChart.rangeY() * 1 / targetChartScaleY();
    }

    private float targetChartScaleY() {
        return mTargetChartTop - mTargetChartBottom;
    }


    private class Axises {

        Map<Long, Value> mYValues = new HashMap<>();
        Map<Long, Value> mXValues = new HashMap<>();

        private long mXGridSize = -1;

        private static final int X_GRID_COUNT = 6;
        private static final int Y_GRID_COUNT = 4;


        private void updateXGridSize(boolean animate) {
            if (mChart == null || mIsPreviewMode) return;
            long rangeY = (long) (chartScaleX() * (long) mChart.rangeX());
            long gridSize = calcXGridSize(rangeY, X_GRID_COUNT);
            if (mXGridSize == gridSize) return;
            mXGridSize = gridSize;
            for (Long index : mXValues.keySet()) {
                Value v = mXValues.get(index);
                if (v != null) {
                    v.setVisible((index - mChart.minX()) % gridSize == 0, animate);
                }
            }
            for (long i = mChart.minX(); i < mChart.maxX(); i += gridSize) {
                if (!mXValues.containsKey(i)) {
                    Value value = new Value(i);
                    mXValues.put(value.value, value);
                    value.setVisible(true, animate);
                }
            }
        }


        private void updateYGridSize(boolean animate) {
            if (mChart == null || mIsPreviewMode) return;
            long rangeY = (long) ((mTargetChartTop - mTargetChartBottom) * mChart.rangeY());
            long gridSize = calcYGridSize(rangeY, Y_GRID_COUNT);

            for (Long index : mYValues.keySet()) {
                Value v = mYValues.get(index);
                if (v != null) {
                    boolean targetVisible = targetPointY(v.value) > 0;
                    v.setVisible(targetVisible && (index - mChart.minY()) % gridSize == 0, animate);
                }
            }
            for (long i = mChart.minY(); i < mChart.maxY(); i += gridSize) {
                boolean targetVisible = targetPointY(i) > 0;
                if (!mYValues.containsKey(i) && targetVisible) {
                    Value value = new Value(i);
                    mYValues.put(i, value);
                    value.setVisible(true, animate);
                }
            }
        }

        long calcYGridSize(long range, int count) {
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

        long calcXGridSize(long range, int count) {
            return (long) Math.pow(2, Math.ceil(Math.log(range / (float) count) / Math.log(2)));
        }


        private class Value {

            private long value;

            private float alpha = 1;
            private float targetAlpha = 1;

            private ObjectAnimator animator;

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
                    invalidate();
                    return;
                }
                if (visible && targetAlpha != 1) {
                    targetAlpha = 1;
                    if (animator != null) animator.cancel();
                    animator = ObjectAnimator.ofFloat(this, "alpha", 1);
                    animator.addUpdateListener(it -> {
                        invalidate();
                    });
                    animator.setDuration(250);
                    animator.start();
                } else if (!visible && targetAlpha != 0) {
                    targetAlpha = 0;
                    if (animator != null) animator.cancel();
                    animator = ObjectAnimator.ofFloat(this, "alpha", 0);
                    animator.addUpdateListener(it -> {
                        invalidate();
                    });
                    animator.setDuration(250);
                    animator.start();
                }
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

}
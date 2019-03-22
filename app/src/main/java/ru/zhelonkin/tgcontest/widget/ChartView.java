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
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.zhelonkin.tgcontest.Alpha;
import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.formatter.CachingFormatter;
import ru.zhelonkin.tgcontest.formatter.DateFormatter;
import ru.zhelonkin.tgcontest.formatter.Formatter;
import ru.zhelonkin.tgcontest.formatter.NumberFormatter;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Line;
import ru.zhelonkin.tgcontest.model.PointL;

public class ChartView extends View {

    private final Formatter DATE_FORMATTER = new CachingFormatter(new DateFormatter("MMM dd"));
    private final Formatter NUMBER_FORMATTER = new CachingFormatter(new NumberFormatter());

    private static final long INVALID_TARGET = -1L;

    private Graph mGraph;

    private Axises mAxises;

    private float mChartLeft = 0;
    private float mChartRight = 1;
    private float mChartTop = 1;
    private float mChartBottom = 0;

    private float mTargetChartTop = mChartTop;
    private float mTargetChartBottom = mChartBottom;

    private ObjectAnimator mChartTopBotAnimator;

    private AnimatorSet mLineAlphaAnimator;

    private Paint mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private Path mLinePath = new Path();

    private int mSurfaceColor;

    private ChartPopup mChartPopup;

    private boolean mIsPreviewMode;

    private long mTargetX = INVALID_TARGET;

    private boolean mIsDragging;

    private float mTouchDownX;

    private int mScaledTouchSlop;

    private float mTextPadding;

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

        setLayerType(LAYER_TYPE_HARDWARE, null);

        mLinePaint.setStrokeWidth(lineWidth);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);

        mGridPaint.setStrokeWidth(lineWidth / 2f);
        mGridPaint.setColor(gridColor);

        if (textAppearance != -1) {
            a = context.obtainStyledAttributes(textAppearance, new int[]{android.R.attr.textSize, android.R.attr.textColor});
            mTextPaint.setTextSize(a.getDimensionPixelSize(0, 12));
            mTextPaint.setColor(a.getColor(1, Color.BLACK));
            a.recycle();
        }

        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setGraph(@NonNull Graph graph) {
        mGraph = graph;
        mAxises = new Axises();
        float[] range = calculateRangeY();
        setChartTopAndBottom(range[1], range[0], false);
        invalidate();
    }


    public void updateGraphLines() {
        float[] range = calculateRangeY();
        setChartTopAndBottom(range[1], range[0], true);

        if (mLineAlphaAnimator != null) mLineAlphaAnimator.cancel();
        List<Animator> animatorList = new ArrayList<>();
        for (Line line : mGraph.getLines()) {
            if (line.isVisible() && line.getAlpha() != 1) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(line, "alpha", 1);
                animator.addUpdateListener(animation -> invalidate());
                animatorList.add(animator);
            } else if (!line.isVisible() && line.getAlpha() != 0) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(line, "alpha", 0);
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
        if (mGraph == null) return;
        if (!mIsPreviewMode) drawYAxis(canvas);
        if (!mIsPreviewMode && mTargetX != INVALID_TARGET) drawX(canvas, pointX(mTargetX));
        for (Line line : mGraph.getLines()) {
            drawLine(canvas, line);
            if (mTargetX != INVALID_TARGET && line.isVisible()) drawDots(canvas, line);
        }
        if (!mIsPreviewMode) drawYAxisText(canvas);
        if (!mIsPreviewMode) drawXAxis(canvas);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || mIsPreviewMode)
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
                setTarget(findTargetX(Math.round(x)));
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

    private void setTarget(long target) {
        if (mTargetX != target) {
            if (target == INVALID_TARGET) {
                hidePopup();
            } else if (isShowingPopup()) {
                updatePopup(target);
            } else {
                showPopup(target);
            }

            mTargetX = target;
            invalidate();
        }

    }

    private void showPopup(long target) {
        if (!isShowingPopup()) {
            mChartPopup = new ChartPopup(getContext());
            mChartPopup.bindData(mGraph.pointsAt(target));
            mChartPopup.showAtLocation(this, (int) pointX(target), 0);
        }
    }

    private void updatePopup(long target) {
        if (isShowingPopup()) {
            int[] location = new int[2];
            getLocationInWindow(location);
            mChartPopup.bindData(mGraph.pointsAt(target));
            mChartPopup.update(this, (int) pointX(target), 0);
        }
    }

    private void hidePopup() {
        if (isShowingPopup()) {
            mChartPopup.dismiss();
        }
    }

    private boolean isShowingPopup() {
        return mChartPopup != null && mChartPopup.isShowing();
    }

    private void drawLine(Canvas canvas, Line line) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            drawLineWithPath(canvas, line);
        } else {
            drawLineDefault(canvas, line);
        }
    }

    private void drawLineDefault(Canvas canvas, Line line) {
        mLinePaint.setColor(line.getColor());
        mLinePaint.setAlpha(Alpha.toInt(line.getAlpha()));

        PointL[] points = line.getPoints();
        float[] lineBuffer = new float[points.length * 4];
        float lastX = pointX(points[0].x);
        float lastY = pointY(points[0].y);
        int j = 0;
        for (int i = 1; i < points.length; i++) {
            float x = pointX(points[i].x);
            float y = pointY(points[i].y);
            lineBuffer[j++] = lastX;
            lineBuffer[j++] = lastY;
            lineBuffer[j++] = x;
            lineBuffer[j++] = y;
            lastX = x;
            lastY = y;
        }
        canvas.drawLines(lineBuffer, mLinePaint);
    }

    private void drawLineWithPath(Canvas canvas, Line line) {
        mLinePaint.setColor(line.getColor());
        mLinePaint.setAlpha(Alpha.toInt(line.getAlpha()));
        mLinePath.reset();
        PointL[] points = line.getPoints();
        mLinePath.moveTo(pointX(points[0].x), pointY(points[0].y));
        for (int i = 1; i < points.length; i++) {
            mLinePath.lineTo(pointX(points[i].x), pointY(points[i].y));
        }
        canvas.drawPath(mLinePath, mLinePaint);
    }

    private void drawDots(Canvas canvas, Line line) {
        mLinePaint.setColor(line.getColor());

        PointL[] points = line.getPoints();
        for (int i = 1; i < points.length; i++) {
            float x = pointX(points[i].x);
            float y = pointY(points[i].y);
            if (points[i].x == mTargetX) {
                drawDot(canvas, x, y);
            }
        }
    }

    private void drawX(Canvas canvas, float x) {
        canvas.drawLine(x, getPaddingTop(), x, getHeight() - getPaddingBottom(), mGridPaint);
    }

    private void drawDot(Canvas canvas, float x, float y) {
        int color = mLinePaint.getColor();
        mLinePaint.setColor(mSurfaceColor);
        mLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(x, y, mLinePaint.getStrokeWidth() * 2, mLinePaint);
        mLinePaint.setColor(color);
        mLinePaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(x, y, mLinePaint.getStrokeWidth() * 2, mLinePaint);
    }

    private void drawYAxis(Canvas canvas) {
        int count = canvas.save();
        canvas.clipRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        for (Long index : mAxises.mYValues.keySet()) {
            Axises.Value v = mAxises.mYValues.get(index);
            float y = pointY(v.value);
            if (v.getAlpha() != 0) {
                mGridPaint.setAlpha(Alpha.toInt(v.getAlpha()));
                canvas.drawLine(getPaddingLeft(), y, getWidth() - getPaddingRight(), y, mGridPaint);
            }
            mGridPaint.setAlpha(Alpha.toInt(1f));
        }
        canvas.restoreToCount(count);
    }

    private void drawYAxisText(Canvas canvas) {
        int count = canvas.save();
        canvas.clipRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        for (Long index : mAxises.mYValues.keySet()) {
            Axises.Value v = mAxises.mYValues.get(index);
            float y = pointY(v.value) - mTextPadding;
            if (v.getAlpha() != 0) {
                mTextPaint.setAlpha(Alpha.toInt(v.getAlpha()));
                canvas.drawText(NUMBER_FORMATTER.format(v.value), 0, y, mTextPaint);
            }
        }
        mTextPaint.setAlpha(Alpha.toInt(1f));
        canvas.restoreToCount(count);
    }

    private void drawXAxis(Canvas canvas) {
        for (Long index : mAxises.mXValues.keySet()) {
            Axises.Value v = mAxises.mXValues.get(index);
            float x = pointX(v.value);
            if (v.getAlpha() != 0) {
                mTextPaint.setAlpha(Alpha.toInt(v.getAlpha()));
                canvas.drawText(DATE_FORMATTER.format(v.value), x, getHeight() - mTextPaint.descent(), mTextPaint);
            }
        }
        mTextPaint.setAlpha(Alpha.toInt(1f));
    }


    private Long findTargetX(float touchX) {
        Line line = null;
        for (Line l : mGraph.getLines()) {
            if (l.isVisible()) {
                line = l;
                break;
            }
        }
        if (line != null) {
            float minDistance = Float.MAX_VALUE;
            PointL targetP = null;
            for (PointL p : line.getPoints()) {
                float distance = Math.abs(pointX(p.x) - touchX);
                if (distance < minDistance) {
                    minDistance = distance;
                    targetP = p;
                }
            }
            return targetP != null ? targetP.x : INVALID_TARGET;
        }
        return INVALID_TARGET;
    }

    private float[] calculateRangeY() {
        if (mGraph == null) return new float[]{0, 1};
        long minY = Long.MAX_VALUE;
        long maxY = Long.MIN_VALUE;
        for (Line line : mGraph.getLines()) {
            if (!line.isVisible()) continue;
            PointL[] points = line.getPoints();
            for (PointL point : points) {
                float x = pointX(point.x);
                if (x >= 0 && x <= getWidth()) {
                    if (point.y < minY) {
                        minY = point.y;
                    }
                    if (point.y > maxY) {
                        maxY = point.y;
                    }
                }
            }
        }
        float[] result = new float[2];
        result[0] = 0;//minY == Long.MAX_VALUE ? 0 : (minY - mGraph.minY()) / mGraph.rangeY();
        result[1] = maxY == Long.MIN_VALUE ? 1 : (maxY - mGraph.minY()) / mGraph.rangeY();
        return result;
    }

    private float pointX(long x) {
        return getPaddingLeft() + (x - mGraph.minX()) * scaleX() + translationX();
    }

    private float pointY(long y) {
        return getHeight() - getPaddingBottom() - (y - mGraph.minY()) * scaleY() + translationY();
    }

    private float translationX() {
        return -mGraph.rangeX() * scaleX() * mChartLeft;
    }

    private float translationY() {
        return mGraph.rangeY() * scaleY() * mChartBottom;
    }

    private float scaleX() {
        return chartWidth() / mGraph.rangeX() * 1 / chartScaleX();
    }

    private float scaleY() {
        return chartHeight() / mGraph.rangeY() * 1 / chartScaleY();
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


    private class Axises {

        Map<Long, Value> mYValues = new HashMap<>();
        Map<Long, Value> mXValues = new HashMap<>();

        private long mXGridSize = -1;
        private long mYGridSize = -1;

        private void updateXGridSize(boolean animate) {
            if (mGraph == null || mIsPreviewMode) return;
            long rangeY = (long) (chartScaleX() * (long) mGraph.rangeX());
            long gridSize = gridSizeForXRange(rangeY);
            if (mXGridSize == gridSize) return;
            mXGridSize = gridSize;
            for (Long index : mXValues.keySet()) {
                Value v = mXValues.get(index);
                if (v != null) {
                    v.setVisible((index - mGraph.minX()) % gridSize == 0, animate);
                }
            }
            for (long i = mGraph.minX(); i < mGraph.maxX(); i += gridSize) {
                if (!mXValues.containsKey(i)) {
                    Value value = new Value(i);
                    mXValues.put(value.value, value);
                    value.setVisible(true, animate);
                }
            }
        }


        private void updateYGridSize(boolean animate) {
            if (mGraph == null || mIsPreviewMode) return;
            long rangeY = (long) ((mTargetChartTop - mTargetChartBottom) * mGraph.rangeY());
            long gridSize = gridSizeForYRange(rangeY);
            if (mYGridSize == gridSize) return;
            mYGridSize = gridSize;

            for (Long index : mYValues.keySet()) {
                Value v = mYValues.get(index);
                if (v != null) {
                    v.setVisible((index - mGraph.minY()) % gridSize == 0, animate);
                }
            }
            for (long i = mGraph.minY(); i < mGraph.maxY(); i += gridSize) {
                if (!mYValues.containsKey(i)) {
                    Value value = new Value(i);
                    mYValues.put(i, value);
                    value.setVisible(true, animate);
                }
            }
        }

        public long gridSizeForYRange(long range) {
            int[] steps = new int[]{5, 10, 25, 50, 100, 200, 250, 500};
            int degree = 0;
            long temp = range;
            while (temp > 500) {
                temp /= 10;
                degree++;
            }
            for (int step : steps) {
                if (temp <= step*1.2) {
                    return (long) (step / 5 * Math.pow(10, degree));
                }
            }
            return (long) (steps[steps.length - 1] / 5 * Math.pow(10, degree));
        }

        public long gridSizeForXRange(long range) {
            return (long) Math.pow(2, Math.ceil(Math.log(range / 6f) / Math.log(2)));
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
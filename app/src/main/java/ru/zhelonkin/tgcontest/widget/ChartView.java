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
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Line;
import ru.zhelonkin.tgcontest.model.PointL;

public class ChartView extends FrameLayout {

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

    private boolean mIsPreviewMode;

    private float mTargetX = INVALID_TARGET;

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

        setLayerType(LAYER_TYPE_HARDWARE, null);

        mLinePaint.setStrokeWidth(lineWidth);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeCap(Paint.Cap.SQUARE);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);

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
        if (!mIsPreviewMode && mTargetX != INVALID_TARGET) drawX(canvas, mTargetX);
        for (Line line : mGraph.getLines()) {
            drawLine(canvas, line);

            if (mTargetX != INVALID_TARGET && line.isVisible()) drawDot(canvas, line);
        }
        if (!mIsPreviewMode) drawYAxis(canvas);
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
                setTarget(x);
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

    private void setTarget(float target) {
        if (mTargetX != target) {
            if (!mGraph.hasVisibleLines()) {
                target = INVALID_TARGET;
            }
            if (target != INVALID_TARGET) {
                target = Math.max(pointX(mGraph.minX()), Math.min(pointX(mGraph.maxX()), target));
            }

            if (target == INVALID_TARGET) {
                hidePopup();
            } else if (mChartPopupView.isShowing()) {
                updatePopup(target);
            } else {
                showPopup(target);
            }
            mTargetX = target;
            invalidate();
        }
    }

    private void showPopup(float targetX) {
        if (!mChartPopupView.isShowing()) {
            mChartPopupView.show(true);
            mChartPopupView.bindData(mGraph.pointsAt(valueX(targetX)));
            updatePopupPosition(targetX);
        }
    }

    private void updatePopup(float targetX) {
        if (mChartPopupView.isShowing()) {
            mChartPopupView.bindData(mGraph.pointsAt(valueX(targetX)));
            updatePopupPosition(targetX);
        }
    }

    private void updatePopupPosition(float x) {
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

        List<PointL> points = line.getPoints();
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
        canvas.drawLines(lineBuffer, mLinePaint);
    }

    private void drawLineWithPath(Canvas canvas, Line line) {
        mLinePaint.setColor(line.getColor());
        mLinePaint.setAlpha(Alpha.toInt(line.getAlpha()));
        mLinePath.reset();
        List<PointL> points = line.getPoints();
        mLinePath.moveTo(pointX(points.get(0).x), pointY(points.get(0).y));
        for (int i = 1; i < points.size(); i++) {
            mLinePath.lineTo(pointX(points.get(i).x), pointY(points.get(i).y));
        }
        canvas.drawPath(mLinePath, mLinePaint);
    }

    private void drawDot(Canvas canvas, Line line) {
        mLinePaint.setColor(line.getColor());
        float x = mTargetX;
        float y = pointY(line.getY(valueX(x), false));
        int color = mLinePaint.getColor();
        mLinePaint.setColor(mSurfaceColor);
        mLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(x, y, mLinePaint.getStrokeWidth() * 2, mLinePaint);
        mLinePaint.setColor(color);
        mLinePaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(x, y, mLinePaint.getStrokeWidth() * 2, mLinePaint);
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
                mGridPaint.setAlpha(Alpha.toInt(v.getAlpha() * 0.2f));
                canvas.drawLine(getPaddingLeft(), y, getWidth() - getPaddingRight(), y, mGridPaint);
                mTextPaint.setAlpha(Alpha.toInt(v.getAlpha()));
                canvas.drawText(NUMBER_FORMATTER.format(v.value), getPaddingLeft(), y - mTextPadding, mTextPaint);
            }
            mGridPaint.setAlpha(Alpha.toInt(0.2f));
        }
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

    private float[] calculateRangeY() {
        if (mGraph == null) return new float[]{0, 1};
        long minY = Long.MAX_VALUE;
        long maxY = Long.MIN_VALUE;
        for (Line line : mGraph.getLines()) {
            if (!line.isVisible()) continue;
            List<PointL> points = line.getPoints();
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

    private long valueX(float pointX) {
        return (long) ((pointX + mGraph.minX() * scaleX() - translationX() - getPaddingLeft()) / scaleX());
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

        private static final int X_GRID_COUNT = 6;
        private static final int Y_GRID_COUNT = 4;


        private void updateXGridSize(boolean animate) {
            if (mGraph == null || mIsPreviewMode) return;
            long rangeY = (long) (chartScaleX() * (long) mGraph.rangeX());
            long gridSize = calcXGridSize(rangeY, X_GRID_COUNT);
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
            long gridSize = calcYGridSize(rangeY, Y_GRID_COUNT);
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
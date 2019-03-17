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
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Line;
import ru.zhelonkin.tgcontest.model.PointL;

public class ChartView extends View {

    private static final long DAY = TimeUnit.DAYS.toMillis(1);

    private static final DateFormatter DATE_FORMATTER = new DateFormatter();

    private static final long INVALID_TARGET = -1L;

    private Graph mGraph;

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

    private int mSurfaceColor;

    private ChartPopup mChartPopup;

    private boolean mIsPreviewMode;

    private long mTargetX = INVALID_TARGET;

    private boolean mIsDragging;

    private float mTouchDownX;

    private int mScaledTouchSlop;

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

        int textAppearance = a.getResourceId(R.styleable.ChartView_textAppearance, -1);
        a.recycle();

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


    public void setLeftAndRight(float left, float right, boolean animate) {
        mChartLeft = left;
        mChartRight = right;
        float[] range = calculateRangeY();
        setChartTopAndBottom(range[1], range[0], animate);
        invalidate();
    }

    public void setChartTopAndBottom(float top, float bot, boolean animate) {
        if (animate) {
            if (mTargetChartTop != top || mTargetChartBottom != bot) {
                mTargetChartTop = top;
                mTargetChartBottom = bot;
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
            setChartTop(top);
            setChartBottom(bot);
            invalidate();
        }
    }

    public void setChartTop(float chartTop) {
        mChartTop = chartTop;
    }

    public void setChartBottom(float chartBottom) {
        mChartBottom = chartBottom;
    }

    public float getChartTop() {
        return mChartTop;
    }

    public float getChartBottom() {
        return mChartBottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mGraph == null) return;
        if (!mIsPreviewMode) drawYAxis(canvas);
        if (!mIsPreviewMode) drawXAxis(canvas);
        if (!mIsPreviewMode && mTargetX != INVALID_TARGET) drawX(canvas, pointX(mTargetX));
        for (Line line : mGraph.getLines()) {
            drawLine(canvas, line);
            if (mTargetX != INVALID_TARGET && line.isVisible()) drawDots(canvas, line);
        }
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
            mChartPopup.bindData(pointsAt(target));
            mChartPopup.showAtLocation(this, (int) pointX(target), 0);
        }
    }

    private void updatePopup(long target) {
        if (isShowingPopup()) {
            int[] location = new int[2];
            getLocationInWindow(location);
            mChartPopup.bindData(pointsAt(target));
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
        mLinePaint.setColor(line.getColor());
        mLinePaint.setAlpha((int) (255 * line.getAlpha()));

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
        long rangeY = (long) (chartScaleY() * mGraph.rangeY());
        long d = findD(rangeY);
        int count = (int) (mGraph.rangeY() / d);
        for (long i = 0; i < count + 1; i++) {
            long value = mGraph.minY() + i * d;
            float y = pointY(value);
            if (y <= getHeight() - getPaddingBottom()) {
                canvas.drawLine(getPaddingLeft(), y, getWidth() - getPaddingRight(), y, mGridPaint);
                canvas.drawText(String.valueOf(value), 0, y - 16, mTextPaint);
            }
        }
    }

    private void drawXAxis(Canvas canvas) {
        long rangeY = (long) (chartScaleX() * (long)mGraph.rangeX());
        long rangeInDays = rangeY / DAY;
        long d = findD(rangeInDays);
        long allRangeInDays = (long) (mGraph.rangeX() / DAY);
        int count = (int) (allRangeInDays / d);
        for (long i = 0; i < count + 1; i++) {
            long value = mGraph.minX() + i * d * DAY;
            float x = pointX(value);
            canvas.drawText(DATE_FORMATTER.format(value), x, getHeight()-mTextPaint.descent(), mTextPaint);
        }
    }

    private long findD(long range) {
        int[] steps = new int[]{5, 10, 25, 50, 100, 125, 200, 250, 400};
        int degree = 0;
        long temp = range;
        while (temp > 500) {
            temp /= 10;
            degree++;
        }
        for (int step : steps) {
            if (temp < step) {
                return (int) (step / 5 * Math.pow(10, degree));
            }
        }
        return (long) (steps[steps.length - 1] / 5 * Math.pow(10, degree));
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

    private List<PointAndLine> pointsAt(long target) {
        List<PointAndLine> pointLList = new ArrayList<>();
        for (Line line : mGraph.getLines()) {
            if (!line.isVisible()) continue;
            for (PointL p : line.getPoints()) {
                if (p.x == target) pointLList.add(new PointAndLine(p, line));
            }
        }
        return pointLList;
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

    class PointAndLine {
        PointL point;
        Line line;

        PointAndLine(PointL point, Line line) {
            this.point = point;
            this.line = line;
        }
    }

    private int chartWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int chartHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    private static class DateFormatter {

        private static final String DATE_FORMAT = "MMM dd";

        String format(long date) {
            return capitalize(new SimpleDateFormat(DATE_FORMAT, Locale.US).format(date));
        }

        private static String capitalize(String string) {
            return string.substring(0, 1).toUpperCase() + string.substring(1);
        }
    }


}

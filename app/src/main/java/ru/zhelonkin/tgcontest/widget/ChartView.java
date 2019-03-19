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
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Line;
import ru.zhelonkin.tgcontest.model.PointL;

public class ChartView extends View {

    private static final long DAY = TimeUnit.DAYS.toMillis(1);

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
        updateXAxisVisibility(animate);
        invalidate();
    }

    public void setChartTopAndBottom(float top, float bot, boolean animate) {
        if (animate) {
            if (mTargetChartTop != top || mTargetChartBottom != bot) {
                mTargetChartTop = top;
                mTargetChartBottom = bot;
                 updateYAxisVisibilityY(true);
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
            updateYAxisVisibilityY(false);
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
        for (Graph.Value v : mGraph.getYXis()) {
            float y = pointY(v.value);
            if (v.getAlpha() != 0) {
                mGridPaint.setAlpha(((int) (v.getAlpha() * 255)));
                canvas.drawLine(getPaddingLeft(), y, getWidth() - getPaddingRight(), y, mGridPaint);
            }
            mGridPaint.setAlpha(255);
        }
    }

    private void drawYAxisText(Canvas canvas) {
        for (Graph.Value v : mGraph.getYXis()) {
            float y = pointY(v.value)-mTextPadding;
            if (v.getAlpha() != 0) {
                mTextPaint.setAlpha(((int) (v.getAlpha() * 255)));
                canvas.drawText(v.displayValue, 0, y, mTextPaint);
            }
        }
        mTextPaint.setAlpha(255);
    }

    private Paint mPaint = new Paint();


    private void drawXAxis(Canvas canvas) {
        mPaint.setColor(mSurfaceColor);
        canvas.drawRect(0, getHeight()-getPaddingBottom(), getWidth(), getHeight(), mPaint);
        for (Graph.Value v : mGraph.getXAxis()) {
            float x = pointX(v.value);
            if (v.getAlpha() != 0) {
                mTextPaint.setAlpha(((int) (v.getAlpha() * 255)));
                canvas.drawText(v.displayValue, x, getHeight() - mTextPaint.descent(), mTextPaint);
            }
        }

        mTextPaint.setAlpha(255);
    }

    private void updateXAxisVisibility(boolean animate) {
        if (mGraph == null || mIsPreviewMode) return;
        long rangeY = (long) (chartScaleX() * (long) mGraph.rangeX());
        long rangeInDays = rangeY / DAY;
        long d = mGraph.gridSizeForRange(rangeInDays);
        for (int i = 0; i < mGraph.getXAxis().length; i += 1) {
            Graph.Value v = mGraph.getXAxis()[i];
            v.setVisible(v.index % d == 0, animate, this::invalidate);
        }
    }

    private void updateYAxisVisibilityY(boolean animate) {
        if (mGraph == null || mIsPreviewMode) return;
        long rangeY = (long) ((mTargetChartTop-mTargetChartBottom) * mGraph.rangeY());
        long d = mGraph.gridSizeForRange(rangeY);
        for (int i = 0; i < mGraph.getYXis().length; i += 1) {
            Graph.Value v = mGraph.getYXis()[i];
            v.setVisible(v.index % d == 0, animate, this::invalidate);
        }
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

    private int chartWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int chartHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    class PointAndLine {
        PointL point;
        Line line;

        PointAndLine(PointL point, Line line) {
            this.point = point;
            this.line = line;
        }
    }

}

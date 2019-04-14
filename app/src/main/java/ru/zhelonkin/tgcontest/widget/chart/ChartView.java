package ru.zhelonkin.tgcontest.widget.chart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import java.util.Collections;
import java.util.List;

import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.formatter.Formatter;
import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.widget.chart.renderer.AreaRenderer;
import ru.zhelonkin.tgcontest.widget.chart.renderer.AxisesRenderer;
import ru.zhelonkin.tgcontest.widget.chart.renderer.BarRenderer;
import ru.zhelonkin.tgcontest.widget.chart.renderer.BaseRenderer;
import ru.zhelonkin.tgcontest.widget.chart.renderer.LineRenderer;
import ru.zhelonkin.tgcontest.widget.chart.renderer.Renderer;
import ru.zhelonkin.tgcontest.widget.chart.renderer.Viewport;

public class ChartView extends FrameLayout {

    public interface OnPopupClickedListener {
        void onPopupClicked(long date);
    }

    public static final int INVALID_TARGET = -1;

    private Chart mChart;

    private GraphAnimator mGraphAnimator;

    private Paint mGridPaint;

    private TextPaint mTextPaint;

    private int mTextPadding;

    private int mLineWidth;

    private int mSurfaceColor;

    private boolean mIsPreviewMode;

    private int mCornerRadius;

    private int mTargetPosition = INVALID_TARGET;

    private boolean mIsDragging;

    private float mTouchDownX;

    private int mScaledTouchSlop;

    private Viewport mViewport;

    private Viewport mViewportSecondary;

    private Renderer mChartRenderer;

    private Renderer mChartRendererSecondary;

    private AxisesRenderer mAxisesRenderer;

    private ChartPopupView mChartPopupView;

    private Path mCornerPath = new Path();

    private OnPopupClickedListener mOnPopupClickedListener;

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
        mLineWidth = a.getDimensionPixelSize(R.styleable.ChartView_lineWidth, 1);
        int gridColor = a.getColor(R.styleable.ChartView_gridColor, Color.BLACK);
        mSurfaceColor = a.getColor(R.styleable.ChartView_surfaceColor, Color.WHITE);
        mIsPreviewMode = a.getBoolean(R.styleable.ChartView_previewMode, false);
        mCornerRadius = a.getDimensionPixelSize(R.styleable.ChartView_cornerRadius, 0);

        mTextPadding = a.getDimensionPixelSize(R.styleable.ChartView_textPadding, 0);
        int textAppearance = a.getResourceId(R.styleable.ChartView_textAppearance, -1);
        a.recycle();


        mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGridPaint.setStrokeWidth(mLineWidth / 2f);
        mGridPaint.setColor(gridColor);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
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
        mGraphAnimator = new GraphAnimator(this);
    }

    private OnClickListener mOnPopupClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mOnPopupClickedListener != null && mTargetPosition != INVALID_TARGET) {
                mOnPopupClickedListener.onPopupClicked(mChart.getXValues().get(mTargetPosition));
            }
        }
    };

    public void setOnPopupClickedListener(OnPopupClickedListener onPopupClickedListener) {
        mOnPopupClickedListener = onPopupClickedListener;
        mChartPopupView.setOnClickListener(onPopupClickedListener != null ? mOnPopupClickListener : null);
    }

    public void setChart(@NonNull Chart chart) {
        mChart = chart;
        if (chart.isYScaled()) {
            List<Graph> graphs = Collections.singletonList(chart.getGraphs().get(0));
            List<Graph> graphsSecondary = Collections.singletonList(chart.getGraphs().get(1));
            mViewport = new Viewport(this, chart, graphs);
            mChartRenderer = createRendererForChart(chart, graphs, mViewport);
            mViewportSecondary = new Viewport(this, chart, graphsSecondary);
            mChartRendererSecondary = createRendererForChart(chart, graphsSecondary, mViewportSecondary);
        } else {
            mViewport = new Viewport(this, chart, chart.getGraphs());
            mChartRenderer = createRendererForChart(chart, chart.getGraphs(), mViewport);
            mChartRendererSecondary = null;
            mViewportSecondary = null;
        }
        mAxisesRenderer = new AxisesRenderer(this, mChart, mViewport, mViewportSecondary, mGridPaint, mTextPaint, mTextPadding);
        setTarget(INVALID_TARGET);
        invalidate();
    }


    public void setAxisDateFormatter(Formatter dateFormatter) {
        if(mAxisesRenderer!=null){
            mAxisesRenderer.setDateFormatter(dateFormatter);
        }
    }

    public void setPopupDateFormatter(Formatter dateFormatter){
        mChartPopupView.setDateFormatter(dateFormatter);
    }

    public void onFiltersChanged() {
        updatePopup(mTargetPosition);
        mViewport.setChartLeftAndRight(mViewport.getChartLeft(), mViewport.getChartRight(), true);
        if (mViewportSecondary != null) {
            mViewportSecondary.setChartLeftAndRight(mViewportSecondary.getChartLeft(), mViewportSecondary.getChartRight(), true);
        }
        mAxisesRenderer.updateGrid(true);
        mGraphAnimator.animateVisibility(mChart.getGraphs());
    }

    public void setChartLeftAndRight(float left, float right, boolean animate) {
        if (mChart == null) return;
        hidePopup();
        mViewport.setChartLeftAndRight(left, right, animate);
        if (mViewportSecondary != null) {
            mViewportSecondary.setChartLeftAndRight(left, right, animate);
        }
        mAxisesRenderer.updateGrid(animate);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mChart == null || mChartRenderer == null) return;

        int saveCount = canvas.save();
        if (mCornerRadius != 0) {
            canvas.clipPath(mCornerPath);
        }

        mChart.updateSums();//<<--костыль?

        mChartRenderer.render(canvas, mTargetPosition);

        if (mChartRendererSecondary != null) {
            mChartRendererSecondary.render(canvas, mTargetPosition);
        }

        if (!mIsPreviewMode) mAxisesRenderer.render(canvas, mTargetPosition);
        canvas.restoreToCount(saveCount);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mCornerRadius != 0) {
            RectF rectF = new RectF(getPaddingLeft(), getPaddingTop(), w - getPaddingRight(), h - getPaddingBottom());
            mCornerPath.reset();
            mCornerPath.addRoundRect(rectF, mCornerRadius, mCornerRadius, Path.Direction.CW);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || mIsPreviewMode || mChart == null)
            return false;

        final int action = event.getAction();
        removeCallbacks(mDismissPopupRunnable);

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
                setTarget(mChart.findTargetPosition(mViewport.valueX(x)));
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsDragging = false;
                postDelayed(mDismissPopupRunnable, 2000);
                break;
        }

        return true;

    }

    private Runnable mDismissPopupRunnable = () -> setTarget(INVALID_TARGET);

    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    private void setTarget(int target) {
        if (mTargetPosition != target) {
            if (mChart.getVisibleGraphs().size() == 0) {
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
            mChartPopupView.bindData(mChart, targetPosition);
            updatePopupPosition(targetPosition, false);
        }
    }

    private void updatePopup(int targetPosition) {
        if (mChartPopupView.isShowing()) {
            mChartPopupView.bindData(mChart, targetPosition);
            updatePopupPosition(targetPosition, true);
        }
    }

    private void updatePopupPosition(int targetPosition, boolean animate) {
        float x = mViewport.pointX(mChart.getXValues().get(targetPosition)) - getPaddingLeft();
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        mChartPopupView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int popupWidth = mChartPopupView.getMeasuredWidth();
        int gravity = x > width / 2f ? Gravity.START : Gravity.END;
        int lastGravity = mChartPopupView.getTranslationX() - mChartPopupView.getPopupOffset() > width / 2f ? Gravity.START : Gravity.END;
        mChartPopupView.setTranslationX(x + mChartPopupView.getPopupOffset());
        if (!animate) {
            mChartPopupView.setPopupOffset(gravity == Gravity.START ? -popupWidth : 0);
        } else if (lastGravity != gravity) {
            mChartPopupView.animateOffset(gravity == Gravity.START ? -popupWidth : 0);
        }
    }


    private void hidePopup() {
        if (mChartPopupView.isShowing()) {
            mChartPopupView.hide(true);
        }
    }

    private BaseRenderer createRendererForChart(Chart chart, List<Graph> graphList, Viewport viewport) {
        String type = chart.getType();
        if (Graph.TYPE_BAR.equals(type)) {
            return new BarRenderer(this, chart, graphList, viewport);
        } else if (Graph.TYPE_AREA.equals(type)) {
            return new AreaRenderer(this, chart, graphList, viewport, mGridPaint);
        } else {
            return new LineRenderer(this, chart, graphList, viewport, mGridPaint, mLineWidth, mSurfaceColor);
        }
    }
}
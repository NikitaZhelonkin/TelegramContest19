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
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import ru.zhelonkin.tgcontest.R;
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

    private Renderer mChartRenderer;

    private AxisesRenderer mAxisesRenderer;

    private ChartPopupView mChartPopupView;

    private Path mCornerPath = new Path();

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

    public void setChart(@NonNull Chart chart) {
        mChart = chart;
        mViewport = new Viewport(this, chart);
        mChartRenderer = createRendererForChart(chart);
        mAxisesRenderer = new AxisesRenderer(this, chart, mViewport, mGridPaint, mTextPaint, mTextPadding);
        invalidate();
    }

    public void onFiltersChanged() {
        mViewport.setChartLeftAndRight(mViewport.getChartLeft(), mViewport.getChartRight(), true);
        mAxisesRenderer.updateGrid(true);
        mGraphAnimator.animateVisibility(mChart.getGraphs());
    }

    public void setChartLeftAndRight(float left, float right, boolean animate) {
        if (mChart == null) return;
        mViewport.setChartLeftAndRight(left, right, animate);
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
            updatePopupPosition(targetPosition);
        }
    }

    private void updatePopup(int targetPosition) {
        if (mChartPopupView.isShowing()) {
            mChartPopupView.bindData(mChart, targetPosition);
            updatePopupPosition(targetPosition);
        }
    }

    private void updatePopupPosition(int targetPosition) {
        float x = mViewport.pointX(mChart.getXValues().get(targetPosition));
        mChartPopupView.measure(MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int popupWidth = mChartPopupView.getMeasuredWidth();
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        float position = Math.max(0, Math.min(width - popupWidth, x - popupWidth / 2f));
        mChartPopupView.setTranslationX(position);
    }

    private void hidePopup() {
        if (mChartPopupView.isShowing()) {
            mChartPopupView.hide(true);
        }
    }

    private BaseRenderer createRendererForChart(Chart chart) {
        String type = chart.getGraphs().get(0).getType();
        if (Graph.TYPE_BAR.equals(type)) {
            return new BarRenderer(this, chart, mViewport);
        } else if (Graph.TYPE_AREA.equals(type)) {
            return new AreaRenderer(this, chart, mViewport, mGridPaint);
        } else {
            return new LineRenderer(this, chart, mViewport, mGridPaint, mLineWidth, mSurfaceColor);
        }
    }
}
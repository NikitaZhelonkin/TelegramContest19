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
import android.view.MotionEvent;
import android.view.View;
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
import ru.zhelonkin.tgcontest.widget.chart.renderer.LineRenderer;
import ru.zhelonkin.tgcontest.widget.chart.renderer.PieChartRenderer;
import ru.zhelonkin.tgcontest.widget.chart.renderer.Renderer;
import ru.zhelonkin.tgcontest.widget.chart.renderer.Viewport;
import ru.zhelonkin.tgcontest.widget.chart.touch.PieTouchHandler;
import ru.zhelonkin.tgcontest.widget.chart.touch.SimpleTouchHandler;
import ru.zhelonkin.tgcontest.widget.chart.touch.TouchHandler;

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

    private Viewport mViewport;

    private Viewport mViewportSecondary;

    private Renderer mChartRenderer;

    private Renderer mChartRendererSecondary;

    private AxisesRenderer mAxisesRenderer;

    private ChartPopupView mChartPopupView;

    private TouchHandler mTouchHandler;

    private Path mCornerPath = new Path();

    private OnPopupClickedListener mOnPopupClickedListener;

    private boolean mDrawAxis;

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

        mChartPopupView = new ChartPopupView(context);
        mChartPopupView.hide(false);
        LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 0);
        addView(mChartPopupView, layoutParams);
        mGraphAnimator = new GraphAnimator(this);

        setDrawAxis(true);
    }

    public ChartPopupView getChartPopupView() {
        return mChartPopupView;
    }

    private OnClickListener mOnPopupClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int target = mTouchHandler.getTarget();
            if (mOnPopupClickedListener != null && target != INVALID_TARGET) {
                mOnPopupClickedListener.onPopupClicked(mChart.getXValues().get(target));
            }
        }
    };

    public void setOnPopupClickedListener(OnPopupClickedListener onPopupClickedListener) {
        mOnPopupClickedListener = onPopupClickedListener;
        mChartPopupView.setOnClickListener(mOnPopupClickListener);
        mChartPopupView.setClickable(onPopupClickedListener!=null);
    }

    public void setDrawAxis(boolean drawAxis) {
        mDrawAxis = drawAxis;
        invalidate();
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
        mTouchHandler = null;
        if(mChartRenderer instanceof PieChartRenderer){
            mTouchHandler = new PieTouchHandler(this, mChart, (PieChartRenderer) mChartRenderer);
        }else {
            mTouchHandler = new SimpleTouchHandler(this, mChart, mViewport);
            if(mChartRenderer instanceof OnTargetChangeListener) ((SimpleTouchHandler) mTouchHandler).addListener((OnTargetChangeListener) mChartRenderer);
            if(mChartRendererSecondary instanceof OnTargetChangeListener) ((SimpleTouchHandler) mTouchHandler).addListener((OnTargetChangeListener) mChartRendererSecondary);

        }
        mAxisesRenderer = new AxisesRenderer(this, mChart, mViewport, mViewportSecondary, mGridPaint, mTextPaint, mTextPadding);
        mTouchHandler.setTarget(INVALID_TARGET);
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
        mTouchHandler.setTarget(INVALID_TARGET);
        mViewport.setChartLeftAndRight(mViewport.getChartLeft(), mViewport.getChartRight(), true, false);
        if (mViewportSecondary != null) {
            mViewportSecondary.setChartLeftAndRight(mViewportSecondary.getChartLeft(), mViewportSecondary.getChartRight(), true, false);
        }
        mAxisesRenderer.updateGrid(true);
        mGraphAnimator.animateVisibility(mChart.getGraphs());
    }

    public void setChartLeftAndRight(float left, float right, boolean animate) {
        if (mChart == null) return;
        mTouchHandler.setTarget(INVALID_TARGET);
        mViewport.setChartLeftAndRight(left, right, animate, true);
            if (mViewportSecondary != null) {
            mViewportSecondary.setChartLeftAndRight(left, right, animate, true);
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

        mChartRenderer.render(canvas);

        if (mChartRendererSecondary != null) {
            mChartRendererSecondary.render(canvas);
        }

        if (!mIsPreviewMode && mDrawAxis) mAxisesRenderer.render(canvas);
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

        return mTouchHandler.onTouchEvent(event);
    }


    private Renderer createRendererForChart(Chart chart, List<Graph> graphList, Viewport viewport) {
        if(chart.isPieChart()){
            if(mIsPreviewMode){
                return new BarRenderer(this, chart, graphList, viewport);
            }else {
                return new PieChartRenderer(this, chart, viewport);
            }
        }
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
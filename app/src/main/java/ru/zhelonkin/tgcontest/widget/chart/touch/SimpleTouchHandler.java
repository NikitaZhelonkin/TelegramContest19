package ru.zhelonkin.tgcontest.widget.chart.touch;

import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;
import java.util.List;

import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.widget.chart.ChartPopupView;
import ru.zhelonkin.tgcontest.widget.chart.ChartView;
import ru.zhelonkin.tgcontest.widget.chart.OnTargetChangeListener;
import ru.zhelonkin.tgcontest.widget.chart.renderer.Viewport;

public class SimpleTouchHandler implements TouchHandler {

    private Chart mChart;

    private Viewport mViewport;

    private ChartView mChartView;

    private ChartPopupView mChartPopupView;

    private final GestureDetector gestureDetector;

    private boolean mIsDragging;

    private float mTouchDownX;

    private int mScaledTouchSlop;

    private int mTarget = ChartView.INVALID_TARGET;

    private List<OnTargetChangeListener> mOnTargetChangeListeners;

    public SimpleTouchHandler(ChartView chartView, Chart chart, Viewport viewport) {
        mChartView = chartView;
        mChart = chart;
        mViewport = viewport;
        mScaledTouchSlop = ViewConfiguration.get(chartView.getContext()).getScaledTouchSlop();
        gestureDetector = new GestureDetector(chartView.getContext(), new GestureDetector.SimpleOnGestureListener() {
            public void onLongPress(MotionEvent e) {
                setTarget(e);
            }
        });
        mChartPopupView = chartView.getChartPopupView();
        mChartPopupView.hide(false);
        mOnTargetChangeListeners = new ArrayList<>();
    }

    public void addListener(OnTargetChangeListener listener){
        mOnTargetChangeListeners.add(listener);
    }

    public void clearListeners(){
        mOnTargetChangeListeners.clear();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);

        final int action = event.getAction();
        mChartView.removeCallbacks(mDismissPopupRunnable);

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
                if (mIsDragging || getTarget() != ChartView.INVALID_TARGET) {
                    setTarget(event);
                }
                break;
            case MotionEvent.ACTION_UP:
                setTarget(event);
            case MotionEvent.ACTION_CANCEL:
                mIsDragging = false;
                mChartView.postDelayed(mDismissPopupRunnable, 2000);
                break;
        }
        return true;
    }

    private Runnable mDismissPopupRunnable = () -> setTarget(ChartView.INVALID_TARGET);

    private void attemptClaimDrag() {
        if (mChartView.getParent() != null) {
            mChartView.requestDisallowInterceptTouchEvent(true);
        }
    }

    private void setTarget(MotionEvent event) {
        setTarget(mChart.findTargetPosition(mViewport.valueX(event.getX())));
    }

    public int getTarget() {
        return mTarget;
    }

    public void setTarget(int target) {
        if (mTarget != target) {
            if (mChart.getVisibleGraphs().size() == 0) {
                target = ChartView.INVALID_TARGET;
            }
            if (target == ChartView.INVALID_TARGET) {
                hidePopup();
            } else if (mChartPopupView.isShowing()) {
                updatePopup(target);
            } else {
                showPopup(target);
            }
            mTarget = target;
            notifyTargetChanged(mTarget);
        }
    }

    private void notifyTargetChanged(int target){
        for(OnTargetChangeListener l:mOnTargetChangeListeners){
            l.onTargetChanged(target);
        }
        mChartView.invalidate();
    }

    private void showPopup(int targetPosition) {
        if (!mChartPopupView.isShowing()) {
            mChartPopupView.show(true);
            mChartPopupView.bindData(mChart, targetPosition);
            updatePopupPosition(targetPosition, true);
        }
    }

    private void updatePopup(int targetPosition) {
        if (mChartPopupView.isShowing()) {
            mChartPopupView.bindData(mChart, targetPosition);
            updatePopupPosition(targetPosition, false);
        }
    }

    private void updatePopupPosition(int targetPosition, boolean justShown) {
        float x = mViewport.pointX(mChart.getXValues().get(targetPosition)) - mChartView.getPaddingLeft();
        int width = mChartView.getWidth() - mChartView.getPaddingLeft() - mChartView.getPaddingRight();
        mChartPopupView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int popupWidth = mChartPopupView.getMeasuredWidth();
        int gravity = x > width / 2f ? Gravity.START : Gravity.END;
        int lastGravity = mChartPopupView.getTranslationX() - mChartPopupView.getPopupOffset() > width / 2f ? Gravity.START : Gravity.END;
        float lastTranslation = mChartPopupView.getTranslationX();
        float lastOffset = mChartPopupView.getPopupOffset();
        mChartPopupView.setTranslationX(x + lastOffset);

        if (justShown) {
            mChartPopupView.setPopupOffset(gravity == Gravity.START ? -popupWidth : 0);
        } else if (!mIsDragging) {
            float diff = lastTranslation - x;
            mChartPopupView.animateOffset(diff, gravity == Gravity.START ? -popupWidth : 0);
        } else if (lastGravity != gravity) {
            float diff = lastTranslation - x;
            mChartPopupView.animateOffset(diff, gravity == Gravity.START ? -popupWidth : 0);
        }
    }


    private void hidePopup() {
        if (mChartPopupView.isShowing()) {
            mChartPopupView.hide(true);
        }
    }
}

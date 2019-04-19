package ru.zhelonkin.tgcontest.widget.chart.renderer;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.support.annotation.Keep;
import android.util.Log;

import java.util.List;

import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.widget.FastOutSlowInInterpolator;
import ru.zhelonkin.tgcontest.widget.chart.ChartView;

public class Viewport {

    private ChartView mView;
    private Chart mChart;

    private long mMinX;
    private long mMaxX;
    private long mMinY;
    private long mMaxY;

    private float mChartLeft = 0;
    private float mChartRight = 1;
    private float mChartTop = 1;
    private float mChartBottom = 0;

    private float mTargetChartTop = mChartTop;
    private float mTargetChartBottom = mChartBottom;

    private ObjectAnimator mChartTopBotAnimator;

    private List<Graph> mGraphs;

    private float yMaxFilterBuff = 1f;
    private float yMinFilterBuff = 1f;

    public Viewport(ChartView view, Chart chart, List<Graph> graphs) {
        mView = view;
        mChart = chart;
        mGraphs = graphs;

        mMinX = calcMinX();
        mMaxX = calcMaxX();
        mMaxY = calcMaxY();
        mMinY = 0;
    }

    long minX() {
        return mMinX;
    }

    long maxX() {
        return mMaxX;
    }

    long minY() {
        return mMinY;
    }

    long maxY() {
        return mMaxY;
    }

    float rangeX() {
        return Math.abs(mMaxX - mMinX);
    }

    float rangeY() {
        return Math.abs(mMaxY - mMinY);
    }

    private long calcMaxX() {
        List<Long> xValues = mChart.getXValues();
        return xValues.get(xValues.size() - 1);
    }

    private long calcMinX() {
        return mChart.getXValues().get(0);
    }

    private long calcMaxY() {
        if (mChart.isPercentage()) return 100;

        long maxY = 0;
        if (mChart.isStacked()) {
            float[] sumArr = mChart.calcSums(mGraphs);
            for (float sum : sumArr) {
                if (sum > maxY) maxY = (long) sum;
            }
            return maxY;
        } else {
            for (Graph graph : mGraphs) {
                long graphMax = graph.maxY();
                if (graphMax > maxY) maxY = graphMax;
            }
        }
        return maxY;
    }

    private float[] calculateRangeY() {
        int startIndex = mChart.findTargetPosition(valueX(0));
        int endIndex = mChart.findTargetPosition(valueX(mView.getWidth()));
        return calculateRangeY(startIndex, endIndex);
    }


    private float[] calculateRangeY(int startIndex, int endIndex) {
        if (mChart.isPercentage()) return new float[]{0, 1};
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        float[] sumArr = new float[mChart.getXValues().size()];
        for (Graph graph : mGraphs) {
            if (!mChart.isYScaled() && !graph.isVisible()) continue;
            for (int i = startIndex; i < endIndex && i < mChart.getXValues().size(); i++) {
                if (mChart.getY(graph, i) < minY) {
                    minY = mChart.getY(graph, i);
                }
                if (mChart.getY(graph, i) > maxY) {
                    maxY = mChart.getY(graph, i);
                }
                sumArr[i] += mChart.getY(graph, i);
            }
        }
        if (mChart.isStacked() && maxY != Float.MIN_VALUE) {
            //find max sum
            for (float sum : sumArr) {
                if (sum > maxY) maxY = sum;
            }
        }
        float[] result = new float[2];

        if (Graph.TYPE_LINE.equals(mChart.getType())) {
            result[0] = minY == Float.MAX_VALUE ? 0 : (minY - minY()) / rangeY();
        } else {
            result[0] = 0;
        }
        result[1] = maxY == Float.MIN_VALUE ? 1 : (maxY - minY()) / rangeY();
        return result;
    }


    public long valueX(float pointX) {
        return (long) ((pointX + minX() * scaleX() - translationX() - mView.getPaddingLeft()) / scaleX());
    }

    public long valueY(float pointY) {
        return (long) (-(pointY - mView.getHeight() + mView.getPaddingBottom() - translationY() - minY() * scaleY()) / scaleY());
    }

    public float pointX(long x) {
        return mView.getPaddingLeft() + (x - minX()) * scaleX() + translationX();
    }


    public float pointY(float y) {
        return mView.getHeight() - mView.getPaddingBottom() - (y - minY()) * scaleY() + translationY();
    }

    private float translationX() {
        return -rangeX() * scaleX() * mChartLeft;
    }

    private float translationY() {
        return rangeY() * scaleY() * mChartBottom;
    }

    private float scaleX() {
        return chartWidth() / rangeX() * 1 / chartScaleX();
    }

    private float scaleY() {
        return chartHeight() / rangeY() * 1 / chartScaleY();
    }

    public float chartScaleX() {
        return mChartRight - mChartLeft;
    }

    public float chartScaleY() {
        return mChartTop - mChartBottom;
    }

    public int chartWidth() {
        return mView.getWidth() - mView.getPaddingLeft() - mView.getPaddingRight();
    }

    public int chartHeight() {
        return mView.getHeight() - mView.getPaddingTop() - mView.getPaddingBottom();
    }

    //target values for animation
    public float targetPointY(long y) {
        return mView.getHeight() - mView.getPaddingBottom() - (y - minY()) * targetScaleY() + targetTranslationY();
    }

    public float targetTranslationY() {
        return rangeY() * targetScaleY() * mTargetChartBottom;
    }

    public float targetScaleY() {
        return chartHeight() / rangeY() * 1 / targetChartScaleY();
    }

    public float targetChartScaleY() {
        return mTargetChartTop - mTargetChartBottom;
    }

    public void setChartLeftAndRight(float left, float right, boolean animate, boolean useFilter) {
        if (mChart == null) return;
        float dx = Math.max(Math.abs(left - mChartLeft), Math.abs(right - mChartRight));
        mChartLeft = left;
        mChartRight = right;
        float[] range = calculateRangeY();
        setChartTopAndBottom(range[1], range[0], dx, animate, useFilter);
        mView.invalidate();
    }

    private static final float KALMAN_FILTER_FACTOR = 0.15f;
    private static final float KALMAN_FILTER_NOISE_FACTOR = 25f;

    // Kalman filter for smooth min-max morphling
    public float[] filterSmooth(float top, float bot, float targetTop, float targetBot, float distanceX) {
        float filterFactor;
        if (distanceX != 0f) {
            filterFactor = KALMAN_FILTER_FACTOR * Math.abs(distanceX);
        } else {
            filterFactor = KALMAN_FILTER_FACTOR;
        }

        float targetYMax = targetTop;
        float targetYMin = targetBot;
        float currentYMax = top;
        float currentYMin = bot;

        float yMaxFilterPrediction = yMaxFilterBuff + filterFactor;
        float filterNoise = KALMAN_FILTER_NOISE_FACTOR;
        float factor = yMaxFilterPrediction / (yMaxFilterPrediction + filterNoise);
        targetYMax = currentYMax + factor * (targetYMax - currentYMax);
        yMaxFilterBuff = (1f - factor) * yMaxFilterPrediction;

        float yMinFilterPrediction = yMinFilterBuff + filterFactor;
        factor = yMinFilterPrediction / (yMinFilterPrediction + filterNoise);
        targetYMin = currentYMin + factor * (targetYMin - currentYMin);
        yMinFilterBuff = (1f - factor) * yMinFilterPrediction;

        return new float[]{targetYMax, targetYMin};
    }


    public void setChartTopAndBottom(float top, float bot, float distanceX, boolean animate, boolean useFilter) {
        if (animate) {
            if(useFilter){
                setChartTopAndBottomSmooth(top, bot, distanceX);
                animateTopAndBottom(top, bot, 8);
            }else {
                if (mTargetChartTop != top || mTargetChartBottom != bot) {
                    mTargetChartTop = top;
                    mTargetChartBottom = bot;
                    animateTopAndBottom(mTargetChartTop, mTargetChartBottom, 0);
                }
            }
        } else {
            mTargetChartTop = top;
            mTargetChartBottom = bot;
            setChartTop(top);
            setChartBottom(bot);
            mView.invalidate();
        }
    }

    private void setChartTopAndBottomSmooth(float top, float bot, float distanceX) {
        float[] result = filterSmooth(mChartTop, mChartBottom, top, bot, distanceX * chartWidth());
        float newTop = result[0];
        float newBot = result[1];
        mTargetChartTop = newTop;
        mTargetChartBottom = newBot;
        setChartTop(newTop);
        setChartBottom(newBot);
        mView.invalidate();
    }

    private void animateTopAndBottom(float top, float bot, int delay){
        if (mChartTopBotAnimator != null) mChartTopBotAnimator.cancel();
        PropertyValuesHolder pvhTop = PropertyValuesHolder.ofFloat("chartTop", top);
        PropertyValuesHolder pvhBop = PropertyValuesHolder.ofFloat("chartBottom", bot);
        mChartTopBotAnimator = ObjectAnimator.ofPropertyValuesHolder(this, pvhTop, pvhBop);
        mChartTopBotAnimator.addUpdateListener(animation -> {
            mView.invalidate();
        });
        mChartTopBotAnimator.setStartDelay(delay);
        mChartTopBotAnimator.setInterpolator(new FastOutSlowInInterpolator());
        mChartTopBotAnimator.setDuration(250);
        mChartTopBotAnimator.start();
    }


    @Keep
    public void setChartLeft(float chartLeft) {
        mChartLeft = chartLeft;
    }

    @Keep
    public void setChartRight(float chartRight) {
        mChartRight = chartRight;
    }

    @Keep
    public float getChartLeft() {
        return mChartLeft;
    }

    @Keep
    public float getChartRight() {
        return mChartRight;
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
}

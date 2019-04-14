package ru.zhelonkin.tgcontest.model;

public class ChartWithZoom {

    Chart mChart;
    Chart mZoomedChart;

    boolean mIsZoomed;

    public ChartWithZoom(Chart chart) {
        mChart = chart;
    }

    public void setZoomedChart(Chart zoomedChart) {
        mZoomedChart = zoomedChart;
    }

    public void setZoomed(boolean zoomed) {
        mIsZoomed = zoomed;
    }

    public boolean isZoomed() {
        return mIsZoomed;
    }

    public Chart getCurrent() {
        return mIsZoomed ? mZoomedChart : mChart;
    }

    public boolean getZoomedChart() {
        return mIsZoomed;
    }
}

package ru.zhelonkin.tgcontest.model;

public class ChartWithZoom {

    private Chart mChart;
    private Chart mZoomedChart;

    ChartWithZoom(Chart chart) {
        mChart = chart;
    }

    public void setZoomedChart(Chart zoomedChart) {
        mZoomedChart = zoomedChart;
    }

    public boolean isZoomed() {
        return mZoomedChart!=null;
    }

    public Chart getCurrent() {
        return mZoomedChart!=null ? mZoomedChart : mChart;
    }
}

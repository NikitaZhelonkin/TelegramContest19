package ru.zhelonkin.tgcontest.model;

import java.util.ArrayList;
import java.util.List;

public class ChartData {

    private List<ChartWithZoom> mCharts;

    public ChartData(List<Chart> charts) {
        List<ChartWithZoom> chartWithZooms = new ArrayList<>(charts.size());
        for(Chart chart:charts){
            chartWithZooms.add(new ChartWithZoom(chart));
        }
        mCharts = chartWithZooms;
    }

    public List<ChartWithZoom> getCharts() {
        return mCharts;
    }
}

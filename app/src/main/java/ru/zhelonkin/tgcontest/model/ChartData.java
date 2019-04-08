package ru.zhelonkin.tgcontest.model;

import java.util.List;

public class ChartData {

    private List<Chart> mCharts;

    public ChartData(List<Chart> charts) {
        mCharts = charts;
    }

    public List<Chart> getCharts() {
        return mCharts;
    }
}

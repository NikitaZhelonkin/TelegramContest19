package ru.zhelonkin.tgcontest.model;

import java.util.List;

public class ChartData {

    List<Graph> mGraphs;

    public ChartData(List<Graph> graphs) {
        mGraphs = graphs;
    }

    public List<Graph> getGraphs() {
        return mGraphs;
    }
}

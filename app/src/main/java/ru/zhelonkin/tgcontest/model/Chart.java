package ru.zhelonkin.tgcontest.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.support.annotation.NonNull;

public class Chart {

    private List<Graph> mGraphs;
    private List<Long> mXValues;
    private float[] mSums;

    private boolean mYScaled;
    private boolean mStacked;
    private boolean mPercentage;

    public float left = 70f;
    public float right = 100f;

    public Chart(@NonNull List<Graph> graphs, List<Long> xValues, boolean yScaled, boolean stacked, boolean percentage) {
        mGraphs = graphs;
        mXValues = xValues;
        mYScaled = yScaled;
        mStacked = stacked;
        mPercentage = percentage;

        mSums = calcSums(graphs);
    }

    public String getType(){
       return mGraphs.get(0).getType();
    }

    @NonNull
    public List<Graph> getGraphs() {
        return mGraphs;
    }

    public List<Long> getXValues() {
        return mXValues;
    }

    public boolean isYScaled() {
        return mYScaled;
    }

    public boolean isStacked() {
        return mStacked;
    }

    public boolean isPercentage() {
        return mPercentage;
    }

    public int findTargetPosition(long xValue) {
        int index = Collections.binarySearch(mXValues, xValue);
        if (index >= 0) {
            return index;
        } else {
            index = (-index) - 1;
        }
        return Math.max(0, Math.min(index, mXValues.size() - 1));
    }

    public List<Graph> getVisibleGraphs() {
        List<Graph> graphs = new ArrayList<>();
        for (Graph graph : getGraphs()) {
            if (graph.isVisible()) graphs.add(graph);
        }
        return graphs;
    }

    public long getX(Graph graph, int pointPosition) {
        Point point = graph.getPoints().get(pointPosition);
        return point.x;
    }

    public float getY(Graph graph, int pointPosition) {
        Point point = graph.getPoints().get(pointPosition);
        if (mPercentage) {
            return (100 * point.y) / mSums[pointPosition];
        }
        return point.y;
    }

    public float[] calcSums(List<Graph> graphs){
        float[] sums = new float[mXValues.size()];
        for (int i = 0; i < getXValues().size(); i++) {
            sums[i] = 0;
            for (Graph graph : graphs) {
                sums[i] += graph.getPoints().get(i).y * graph.getAlpha();
            }
        }
        return sums;
    }

    public void updateSums() {
        if (mPercentage) mSums = calcSums(mGraphs);
    }

}

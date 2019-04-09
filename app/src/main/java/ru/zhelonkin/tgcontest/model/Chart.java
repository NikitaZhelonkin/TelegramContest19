package ru.zhelonkin.tgcontest.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

public class Chart {

    private List<Graph> mGraphs;
    private List<Long> mXValues;
    private float[] sums;

    private boolean mYScaled;
    private boolean mStacked;
    private boolean mPercentage;

    private long mMinX;
    private long mMaxX;
    private long mMinY;
    private long mMaxY;

    public float left = 70f;
    public float right = 100f;

    public Chart(@NonNull List<Graph> graphs, List<Long> xValues, boolean yScaled, boolean stacked, boolean percentage) {
        mGraphs = graphs;
        mXValues = xValues;
        mYScaled = yScaled;
        mStacked = stacked;
        mPercentage = percentage;

        mMinX = calcMinX();
        mMaxX = calcMaxX();
        mMaxY = calcMaxY();
        mMinY = 0;

        sums = new float[xValues.size()];
        updateSums();
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

    public long minX() {
        return mMinX;
    }

    public long maxX() {
        return mMaxX;
    }

    public long minY() {
        return mMinY;
    }

    public long maxY() {
        return mMaxY;
    }

    public float rangeX() {
        return Math.abs(mMaxX - mMinX);
    }

    public float rangeY() {
        return Math.abs(mMaxY - mMinY);
    }

    private long calcMaxX() {
        List<Point> points = mGraphs.get(0).getPoints();
        return points.get(points.size() - 1).x;
    }

    private long calcMinX() {
        List<Point> points = mGraphs.get(0).getPoints();
        return points.get(0).x;
    }

    private long calcMaxY() {
        if (mPercentage) return 100;
        long maxY = mGraphs.get(0).getPoints().get(0).y;

        long[] sumArr = new long[mXValues.size()];
        for (Graph graph : mGraphs) {
            for (int i = 0; i < graph.getPoints().size(); i++) {
                Point p = graph.getPoints().get(i);
                sumArr[i] += p.y;
                if (p.y > maxY) maxY = p.y;

            }
        }
        if (isStacked()) {
            //find max sum
            for (long sum : sumArr) {
                if (sum > maxY) maxY = sum;
            }
        }
        return maxY;
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
            return (100 * point.y) / sums[pointPosition];
        }
        return point.y;
    }


    public void updateSums() {
        if (!mPercentage) {
            return;
        }
        for (int i = 0; i < getXValues().size(); i++) {
            sums[i] = 0;
            for (Graph graph : mGraphs) {
                sums[i] += graph.getPoints().get(i).y * graph.getAlpha();
            }
        }
    }

}

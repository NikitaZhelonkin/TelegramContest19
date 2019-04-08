package ru.zhelonkin.tgcontest.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

public class Chart {

    private List<Graph> mGraphs;
    private List<Long> mXValues;

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
        mMinY = 0;//calcMinY();
        mMaxY = calcMaxY();
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

    private long calcMinY() {
        long minY = mGraphs.get(0).getPoints().get(0).y;
        for (Graph graph : mGraphs) {
            for (Point p : graph.getPoints()) {
                if (p.y < minY) minY = p.y;

            }
        }
        return minY;
    }

    public boolean hasVisibleGraphs() {
        for (Graph graph : mGraphs) {
            if (graph.isVisible()) return true;
        }
        return false;
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


    public List<PointAndGraph> pointsAt(int position) {
        List<PointAndGraph> pointLList = new ArrayList<>();
        for (Graph graph : getGraphs()) {
            if (!graph.isVisible()) continue;
            pointLList.add(new PointAndGraph(graph.getPoints().get(position), graph));
        }
        return pointLList;
    }

    public class PointAndGraph {
        public Point point;
        public Graph mGraph;

        PointAndGraph(Point point, Graph graph) {
            this.point = point;
            this.mGraph = graph;
        }
    }

}

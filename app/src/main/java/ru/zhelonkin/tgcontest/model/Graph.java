package ru.zhelonkin.tgcontest.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class Graph {

    private List<Line> mLines;

    private long mMinX;
    private long mMaxX;
    private long mMinY;
    private long mMaxY;

    public float left = 70f;
    public float right = 100f;

    public Graph(@NonNull List<Line> lines) {
        mLines = lines;
        mMinX = calcMinX();
        mMaxX = calcMaxX();
        mMinY = 0;//calcMinY();
        mMaxY = calcMaxY();
    }


    @NonNull
    public List<Line> getLines() {
        return mLines;
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
        List<PointL> points = mLines.get(0).getPoints();
        return points.get(points.size() - 1).x;
    }

    private long calcMinX() {
        List<PointL> points = mLines.get(0).getPoints();
        return points.get(0).x;
    }

    private long calcMaxY() {
        long maxY = mLines.get(0).getPoints().get(0).y;
        for (Line line : mLines) {
            for (PointL p : line.getPoints()) {
                if (p.y > maxY) maxY = p.y;

            }
        }
        return maxY;
    }

    private long calcMinY() {
        long minY = mLines.get(0).getPoints().get(0).y;
        for (Line line : mLines) {
            for (PointL p : line.getPoints()) {
                if (p.y < minY) minY = p.y;

            }
        }
        return minY;
    }

    public boolean hasVisibleLines(){
        for (Line line : mLines) {
            if(line.isVisible()) return true;
        }
        return false;
    }


    public List<PointAndLine> pointsAt(long target) {
        List<PointAndLine> pointLList = new ArrayList<>();
        for (Line line : getLines()) {
            if (!line.isVisible()) continue;
            long y = line.getY(target, false);
            pointLList.add(new PointAndLine(new PointL(target, y), line));
        }
        return pointLList;
    }

    public class PointAndLine {
        public PointL point;
        public Line line;

        PointAndLine(PointL point, Line line) {
            this.point = point;
            this.line = line;
        }
    }

}

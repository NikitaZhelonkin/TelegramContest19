package ru.zhelonkin.tgcontest.model;

import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
        PointL[] points = mLines.get(0).getPoints();
        return points[points.length - 1].x;
    }

    private long calcMinX() {
        PointL[] points = mLines.get(0).getPoints();
        return points[0].x;
    }

    private long calcMaxY() {
        long maxY = mLines.get(0).getPoints()[0].y;
        for (Line line : mLines) {
            for (PointL p : line.getPoints()) {
                if (p.y > maxY) maxY = p.y;

            }
        }
        return maxY;
    }

    private long calcMinY() {
        long minY = mLines.get(0).getPoints()[0].y;
        for (Line line : mLines) {
            for (PointL p : line.getPoints()) {
                if (p.y < minY) minY = p.y;

            }
        }
        return minY;
    }


    public List<PointAndLine> pointsAt(long target) {
        List<PointAndLine> pointLList = new ArrayList<>();
        for (Line line : getLines()) {
            if (!line.isVisible()) continue;
            for (PointL p : line.getPoints()) {
                if (p.x == target) pointLList.add(new PointAndLine(p, line));
            }
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

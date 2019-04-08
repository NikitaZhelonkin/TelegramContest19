package ru.zhelonkin.tgcontest.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Keep;

public class Line {

    private final PointL POINT = new PointL();

    private String mName;
    private int mColor;
    private List<PointL> mPoints;
    private boolean mVisible;
    private float mAlpha;

    public Line(PointL[] points, String name, int color) {
        mPoints = new ArrayList<>(Arrays.asList(points));
        mName = name;
        mColor = color;
        mVisible = true;
        mAlpha = 1f;
    }

    @Keep
    public void setAlpha(float alpha) {
        mAlpha = alpha;
    }

    @Keep
    public float getAlpha() {
        return mAlpha;
    }

    public void setVisible(boolean visible) {
        mVisible = visible;
    }

    public boolean isVisible() {
        return mVisible;
    }

    public List<PointL> getPoints() {
        return mPoints;
    }

    public String getName() {
        return mName;
    }

    public int getColor() {
        return mColor;
    }


    public final long getY(long x, boolean discretely) {
        int targetIndex = findTarget(x);
        if (discretely || targetIndex == 0) {
            return getPoints().get(targetIndex).y;
        } else {
            PointL p1 = getPoints().get(targetIndex - 1);
            PointL p2 = getPoints().get(targetIndex);
            return (long) (((((float) (p2.y - p1.y)) / ((float) (p2.x - p1.x))) * ((float) (x - p1.x))) + ((float) p1.y));
        }
    }

    private int findTarget(long x) {
        int index = binarySearchX(x);
        List<PointL> points = getPoints();
        if (index >= 0) {
            return index;
        } else {
            index = (-index) - 1;
        }
        return Math.min(index, points.size() - 1);
    }

    private int binarySearchX(long targetX) {
        POINT.x = targetX;
        return Collections.binarySearch(mPoints, POINT);
    }

}

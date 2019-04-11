package ru.zhelonkin.tgcontest.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.support.annotation.Keep;

public class Graph {

    public static final String TYPE_LINE = "line";
    public static final String TYPE_BAR = "bar";
    public static final String TYPE_AREA = "area";

    private final List<Point> mPoints;
    private final String mType;
    private final String mName;
    private final int mColor;

    private boolean mVisible;
    private float mAlpha;

    public Graph(Point[] points, String type, String name, int color) {
        mPoints = new ArrayList<>(Arrays.asList(points));
        mType = type;
        mName = name;
        mColor = color;
        mVisible = true;
        mAlpha = 1f;
    }

    public String getType() {
        return mType;
    }

    public List<Point> getPoints() {
        return mPoints;
    }

    public String getName() {
        return mName;
    }

    public int getColor() {
        return mColor;
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

    public long rangeY() {
        return maxY() - minY();
    }

    public long minY() {
        return 0;
    }

    public long maxY() {
        return calcMaxY();
    }

    private long calcMaxY() {
        long maxY = getPoints().get(0).y;
        for (int i = 0; i < getPoints().size(); i++) {
            Point p = getPoints().get(i);
            if (p.y > maxY) maxY = p.y;
        }
        return maxY;
    }

}

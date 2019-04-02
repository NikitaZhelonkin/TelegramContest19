package ru.zhelonkin.tgcontest.model;

import androidx.annotation.Keep;

import java.util.Arrays;

public class Line {

    private String mName;
    private int mColor;
    private PointL[] mPoints;

    private boolean mVisible;
    private float mAlpha;

    public Line(PointL[] points, String name, int color) {
        mPoints = points;
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

    public PointL[] getPoints() {
        return mPoints;
    }

    public String getName() {
        return mName;
    }

    public int getColor() {
        return mColor;
    }

    @Override
    public String toString() {
        return "Line{" +
                "mName='" + mName + '\'' +
                ", mColor='" + mColor + '\'' +
                ", mPoints=" + Arrays.toString(mPoints) +
                '}';
    }

}

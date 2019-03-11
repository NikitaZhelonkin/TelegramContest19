package ru.zhelonkin.tgcontest.model;

import java.util.Arrays;

public class Line {

    private String mName;
    private int mColor;
    private PointL[] mPoints;


    public Line(PointL[] points, String name, int color) {
        mPoints = points;
        mName = name;
        mColor = color;
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

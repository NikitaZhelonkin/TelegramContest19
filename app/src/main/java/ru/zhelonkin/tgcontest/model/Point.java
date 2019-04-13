package ru.zhelonkin.tgcontest.model;

public class Point {

    public long x;
    public long y;

    Point() {
    }

    public Point(long x, long y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "{" + x + "," + y + "}";
    }

}

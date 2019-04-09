package ru.zhelonkin.tgcontest.model;

public class Point {

    long x;
    long y;

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

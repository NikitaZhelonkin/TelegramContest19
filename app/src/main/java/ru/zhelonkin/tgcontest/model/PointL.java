package ru.zhelonkin.tgcontest.model;

public class PointL {

    public long x;
    public long y;

    public PointL() {
    }

    public PointL(long x, long y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "{" + x + "," + y + "}";
    }

}

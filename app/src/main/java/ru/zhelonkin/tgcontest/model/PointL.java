package ru.zhelonkin.tgcontest.model;

public class PointL implements Comparable<PointL> {

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

    @Override
    public int compareTo(PointL o) {
        return Long.compare(x, o.x);
    }

}

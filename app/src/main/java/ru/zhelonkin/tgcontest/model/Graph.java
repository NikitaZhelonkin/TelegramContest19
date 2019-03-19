package ru.zhelonkin.tgcontest.model;

import android.animation.ObjectAnimator;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Graph {

    private List<Line> mLines;

    private long mMinX;
    private long mMaxX;
    private long mMinY;
    private long mMaxY;

    public float left = 0.7f;
    public float right = 1f;

    private Value[] mXAxis;
    private Value[] mYAxis;

    public Value[] getXAxis() {
        return mXAxis;
    }

    public Value[] getYXis() {
        return mYAxis;
    }

    public Graph(@NonNull List<Line> lines) {
        mLines = lines;
        mMinX = calcMinX();
        mMaxX = calcMaxX();
        mMinY = 0;//calcMinY();
        mMaxY = calcMaxY();
        fillXAxis();
        fillYAxis();
    }

    private static final DateFormatter DATE_FORMATTER = new DateFormatter();

    private static final long DAY = TimeUnit.DAYS.toMillis(1);

    private void fillXAxis() {
        int rangeInDays = (int) (rangeX() / DAY);
        long d = gridSizeForRange((long) (rangeInDays * (0.1f)));
        int count = (int) (rangeInDays / d);
        mXAxis = new Value[count];
        for (int i = 0; i < count; i++) {
            Value v = new Value();
            v.index = i * d;
            v.value = minX() + v.index * DAY;
            v.displayValue = DATE_FORMATTER.format(v.value);
            mXAxis[i] = v;
        }
    }

    private void fillYAxis() {
        long rangeY = (long) (rangeY());
        long d = gridSizeForRange((long) (rangeY * (0.1f)));
        int count = (int) (rangeY() / d);

        mYAxis = new Value[count];
        for (int i = 0; i < count; i++) {
            Value v = new Value();
            v.index = i * d;
            v.value = minY() + v.index;
            v.displayValue = String.valueOf(v.value);
            mYAxis[i] = v;
        }
    }

    public long gridSizeForRange(long range) {
        int[] steps = new int[]{5, 10, 25, 50, 100, 200, 250, 500};
        int degree = 0;
        long temp = range;
        while (temp > 500) {
            temp /= 10;
            degree++;
        }
        for (int step : steps) {
            if (temp < step) {
                return (long) (step / 5 * Math.pow(10, degree));
            }
        }
        return (long) (steps[steps.length - 1] / 5 * Math.pow(10, degree));
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

    public static class Value {

        public long index;
        public long value;
        public String displayValue;

        private float alpha = 1;
        private float targetAlpha = 1;

        private ObjectAnimator animator;

        @Keep
        public void setAlpha(float alpha) {
            this.alpha = alpha;
        }

        @Keep
        public float getAlpha() {
            return alpha;
        }

        public void setVisible(boolean visible, boolean animate, Runnable runnable) {
            if (!animate) {
                targetAlpha = visible ? 1 : 0;
                alpha = targetAlpha;
                runnable.run();
                return;
            }
            WeakReference<Runnable> reference = new WeakReference<>(runnable);
            if (visible && targetAlpha != 1) {
                targetAlpha = 1;
                if (animator != null) animator.cancel();
                animator = ObjectAnimator.ofFloat(this, "alpha", 1);
                animator.addUpdateListener(it -> {
                    if (reference.get() != null) reference.get().run();
                });
                animator.setDuration(200);
                animator.start();
            } else if (!visible && targetAlpha != 0) {
                targetAlpha = 0;
                if (animator != null) animator.cancel();
                animator = ObjectAnimator.ofFloat(this, "alpha", 0);
                animator.addUpdateListener(it -> {
                    if (reference.get() != null) reference.get().run();
                });
                animator.setDuration(200);
                animator.start();
            }
        }

    }

    private static class DateFormatter {

        private static final String DATE_FORMAT = "MMM dd";

        String format(long date) {
            return capitalize(new SimpleDateFormat(DATE_FORMAT, Locale.US).format(date));
        }

        private static String capitalize(String string) {
            return string.substring(0, 1).toUpperCase() + string.substring(1);
        }
    }

}

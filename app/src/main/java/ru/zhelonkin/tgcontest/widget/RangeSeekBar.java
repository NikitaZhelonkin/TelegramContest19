package ru.zhelonkin.tgcontest.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import ru.zhelonkin.tgcontest.R;

public class RangeSeekBar extends View {

    public interface OnRangeSeekBarChangeListener {
        void onRangeChanged(float minValue, float maxValue, boolean fromUser);
    }

    private static final int INVALID_POINTER_ID = 255;

    private OnRangeSeekBarChangeListener mOnRangeSeekBarChangeListener;

    private float mMinValue;
    private float mMaxValue;
    private float mGap;

    private float mLeftValue;
    private float mRightValue;

    private int mBackgroundColor;
    private int mThumbColor;
    private int mThumbStrokeWidth;
    private int mThumbWidth;

    private Paint mPaint;

    private boolean mIsDragging;

    private int mScaledEdgeSlop;

    private int mActivePointerId = INVALID_POINTER_ID;

    private DragMode mDragMode;

    protected enum DragMode {LEFT, RIGHT, BOTH}

    public RangeSeekBar(Context context) {
        this(context, null);
    }

    public RangeSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.rangeSeekBarStyle);
    }

    public RangeSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RangeSeekBar, defStyleAttr, 0);
        mMinValue = array.getFloat(R.styleable.RangeSeekBar_minValue, 0);
        mMaxValue = array.getFloat(R.styleable.RangeSeekBar_maxValue, 100);
        float leftStartValue = array.getFloat(R.styleable.RangeSeekBar_minStartValue, mMinValue);
        float rightStartValue = array.getFloat(R.styleable.RangeSeekBar_maxStartValue, mMaxValue);
        float gap = array.getFloat(R.styleable.RangeSeekBar_gap, (mMaxValue - mMinValue) / 10);
        mBackgroundColor = array.getColor(R.styleable.RangeSeekBar_backgroundColor, Color.GRAY);
        mThumbColor = array.getColor(R.styleable.RangeSeekBar_thumbColor, Color.BLACK);
        mThumbStrokeWidth = array.getDimensionPixelSize(R.styleable.RangeSeekBar_thumbStrokeWidth, (int) dm.density);
        mThumbWidth = array.getDimensionPixelSize(R.styleable.RangeSeekBar_thumbWidth, (int) (dm.density));

        array.recycle();


        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mDragMode = null;

        mScaledEdgeSlop = ViewConfiguration.get(context).getScaledEdgeSlop();

        setWillNotDraw(false);

        setGap(gap);

        setLeftValue(leftStartValue);

        setRightValue(rightStartValue);
    }

    public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener onRangeSeekbarChangeListener) {
        this.mOnRangeSeekBarChangeListener = onRangeSeekbarChangeListener;
    }

    public float getLeftValue() {
        return mLeftValue;
    }

    public float getRightValue() {
        return mRightValue;
    }

    public float getGap() {
        return mGap;
    }

    private void setLeftValue(float value) {
        mLeftValue = Math.max(mMinValue, Math.min(mMaxValue, Math.min(value, mRightValue)));
        addMinGap();
        invalidate();
    }

    private void setRightValue(float value) {
        mRightValue = Math.max(mMinValue, Math.min(mMaxValue, Math.max(value, mLeftValue)));
        addMaxGap();
        invalidate();
    }

    private void setGap(float gap) {
        mGap = Math.max(0, Math.min(gap, mMaxValue - mMinValue));
    }

    private void trackTouchEvent(MotionEvent event) {
        final int pointerIndex = event.findPointerIndex(mActivePointerId);
        try {
            final float x = event.getX(pointerIndex);

            if (DragMode.LEFT.equals(mDragMode)) {
                setLeftValue(coordToValue(x));
            } else if (DragMode.RIGHT.equals(mDragMode)) {
                setRightValue(coordToValue(x));
            }
            notifyRangeChanged(true);
        } catch (Exception ignored) {
        }
    }

    private void notifyRangeChanged(boolean fromUser) {
        if (mOnRangeSeekBarChangeListener != null) {
            mOnRangeSeekBarChangeListener.onRangeChanged(mLeftValue, mRightValue, fromUser);
        }
    }

    private DragMode evalDragMode(float touchX) {
        DragMode result = null;

        boolean minThumbPressed = isInThumbRange(touchX, mLeftValue);
        boolean maxThumbPressed = isInThumbRange(touchX, mRightValue);
        if (minThumbPressed && maxThumbPressed) {
            // if both thumbs are pressed (they lie on top of each other), choose the one with more room to drag. this avoids "stalling" the thumbs in a corner, not being able to drag them apart anymore.
            result = (touchX / getWidth() > 0.5f) ? DragMode.LEFT : DragMode.RIGHT;
        } else if (minThumbPressed) {
            result = DragMode.LEFT;
        } else if (maxThumbPressed) {
            result = DragMode.RIGHT;
        }
        if (result == null) {
            result = findClosestThumb(touchX);
        }
        return result;
    }

    private boolean isInThumbRange(float touchX, double value) {
        float thumbPos = valueToCoord(value);
        float left = thumbPos - (mScaledEdgeSlop);
        float right = thumbPos + (mScaledEdgeSlop);
        float x = touchX - (mScaledEdgeSlop);
        if (thumbPos > (getWidth() - mScaledEdgeSlop * 2)) x = touchX;
        return (x >= left && x <= right);
    }

    private DragMode findClosestThumb(float touchX) {
        float screenMinX = valueToCoord(mLeftValue);
        float screenMaxX = valueToCoord(mRightValue);
        if (touchX >= screenMaxX) {
            return DragMode.RIGHT;
        } else if (touchX <= screenMinX) {
            return DragMode.LEFT;
        }

        double minDiff = Math.abs(screenMinX - touchX);
        double maxDiff = Math.abs(screenMaxX - touchX);
        return minDiff < maxDiff ? DragMode.LEFT : DragMode.RIGHT;
    }

    private void onStartTrackingTouch() {
        mIsDragging = true;
    }

    private void onStopTrackingTouch() {
        mIsDragging = false;
    }

    private float valueToCoord(double coord) {
        float width = getWidth();
        return (float) (coord - mMinValue) / (mMaxValue - mMinValue) * width;
    }

    private float coordToValue(float coord) {
        float width = getWidth();
        float result = coord / width * (mMaxValue - mMinValue);
        result = Math.min(mMaxValue, Math.max(mMinValue, result));
        return result;
    }


    private void addMinGap() {
        if ((mLeftValue + mGap) > mRightValue) {
            float g = mLeftValue + mGap;
            mRightValue = g;
            mRightValue = Math.max(mMinValue, Math.min(mMaxValue, Math.max(g, mLeftValue)));

            if (mLeftValue >= (mRightValue - mGap)) {
                mLeftValue = mRightValue - mGap;
            }
        }
    }

    private void addMaxGap() {
        if ((mRightValue - mGap) < mLeftValue) {
            float g = mRightValue - mGap;
            mLeftValue = g;
            mLeftValue = Math.max(mMinValue, Math.min(mMaxValue, Math.min(g, mRightValue)));
            if (mRightValue <= (mLeftValue + mGap)) {
                mRightValue = mLeftValue + mGap;
            }
        }
    }

    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float min = valueToCoord(mLeftValue);
        float max = valueToCoord(mRightValue);

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
        mPaint.setColor(mBackgroundColor);
        canvas.drawRect(0, 0, min, getHeight(), mPaint);
        canvas.drawRect(max, 0, getWidth(), getHeight(), mPaint);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mThumbColor);
        mPaint.setStrokeWidth(mThumbStrokeWidth);

        canvas.drawRect(min, mThumbStrokeWidth / 2f, max, getHeight() - mThumbStrokeWidth / 2f, mPaint);

        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(min, 0, min + mThumbWidth, getHeight(), mPaint);
        canvas.drawRect(max - mThumbWidth, 0, max, getHeight(), mPaint);
    }

    private void startDrag(MotionEvent event) {
        setPressed(true);
        invalidate();
        onStartTrackingTouch();
        trackTouchEvent(event);
        attemptClaimDrag();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public synchronized boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled())
            return false;

        final int action = event.getAction();

        switch (action & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:
                mActivePointerId = event.getPointerId(event.getPointerCount() - 1);
                int pointerIndex = event.findPointerIndex(mActivePointerId);
                float downMotionX = event.getX(pointerIndex);

                mDragMode = evalDragMode(downMotionX);

                if (mDragMode == null) return super.onTouchEvent(event);

                startDrag(event);

                break;

            case MotionEvent.ACTION_MOVE:
                if (mDragMode != null) {

                    if (mIsDragging) {
                        trackTouchEvent(event);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    // Touch up when we never crossed the touch slop threshold
                    // should be interpreted as a tap-seek to that location.
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }

                mDragMode = null;
                invalidate();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
        }

        return true;

    }


    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);

        ss.leftValue = mLeftValue;
        ss.rightValue = mRightValue;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        setLeftValue(ss.leftValue);
        setRightValue(ss.rightValue);
        notifyRangeChanged(false);

    }

    static class SavedState extends BaseSavedState {
        float leftValue;
        float rightValue;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            leftValue = in.readFloat();
            rightValue = in.readFloat();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeFloat(leftValue);
            out.writeFloat(rightValue);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }


}

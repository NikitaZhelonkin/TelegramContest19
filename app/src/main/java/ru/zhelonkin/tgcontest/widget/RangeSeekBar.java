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

    private int mScaledEdgeSlop;
    private int mScaledTouchSlop;

    private TouchInfo mPrimaryTouch;
    private TouchInfo mSecondaryTouch;

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

        mBackgroundColor = array.getColor(R.styleable.RangeSeekBar_backgroundColor, Color.GRAY);
        mThumbColor = array.getColor(R.styleable.RangeSeekBar_thumbColor, Color.BLACK);
        mThumbStrokeWidth = array.getDimensionPixelSize(R.styleable.RangeSeekBar_thumbStrokeWidth, (int) dm.density);
        mThumbWidth = array.getDimensionPixelSize(R.styleable.RangeSeekBar_thumbWidth, (int) (dm.density));
        mMinValue = array.getFloat(R.styleable.RangeSeekBar_minValue, 0);
        mMaxValue = array.getFloat(R.styleable.RangeSeekBar_maxValue, 100);
        float leftValue = array.getFloat(R.styleable.RangeSeekBar_leftValue, mMinValue);
        float rightValue = array.getFloat(R.styleable.RangeSeekBar_rightValue, mMaxValue);
        float gap = array.getFloat(R.styleable.RangeSeekBar_gap, (mMaxValue - mMinValue) / 10);

        array.recycle();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mScaledEdgeSlop = (int) (ViewConfiguration.get(context).getScaledEdgeSlop() * 1.5f);

        setWillNotDraw(false);

        setGap(gap);

        setValues(leftValue, rightValue);
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

    public void setValues(float leftValue, float rightValue) {
        setLeftValue(leftValue);
        setRightValue(rightValue);
        notifyRangeChanged(false);
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

    private void trackTouchEvent(MotionEvent event, TouchInfo touchInfo) {
        int index = event.findPointerIndex(touchInfo.id);
        if (index == -1) return;
        float x = event.getX(index);
        float dx = x - touchInfo.lastX;
        if (DragMode.LEFT.equals(touchInfo.dragMode)) {
            setLeftValue(coordToValue(valueToCoord(mLeftValue) + dx));
        } else if (DragMode.RIGHT.equals(touchInfo.dragMode)) {
            setRightValue(coordToValue(valueToCoord(mRightValue) + dx));
        } else if (DragMode.BOTH.equals(touchInfo.dragMode)) {
            final float range = mRightValue - mLeftValue;
            if (dx > 0) {
                setRightValue(coordToValue(valueToCoord(mRightValue) + dx));
                setLeftValue(mRightValue - range);
            } else {
                setLeftValue(coordToValue(valueToCoord(mLeftValue) + dx));
                setRightValue(mLeftValue + range);
            }

        }
        touchInfo.lastX = x;
        notifyRangeChanged(true);
    }


    private void notifyRangeChanged(boolean fromUser) {
        if (mOnRangeSeekBarChangeListener != null) {
            mOnRangeSeekBarChangeListener.onRangeChanged(mLeftValue, mRightValue, fromUser);
        }
    }

    private DragMode evalDragMode(float touchX) {
        if (touchX < valueToCoord(mLeftValue) + mScaledEdgeSlop) {
            return DragMode.LEFT;
        } else if (touchX > valueToCoord(mRightValue) - mScaledEdgeSlop) {
            return DragMode.RIGHT;
        } else {
            return DragMode.BOTH;
        }
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

        canvas.drawRect(min + mThumbWidth + mPaint.getStrokeWidth() / 2f,
                mPaint.getStrokeWidth() / 2f,
                max - mThumbWidth - mPaint.getStrokeWidth() / 2f,
                getHeight() - mPaint.getStrokeWidth() / 2f,
                mPaint);

        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(min, 0, min + mThumbWidth, getHeight(), mPaint);
        canvas.drawRect(max - mThumbWidth, 0, max, getHeight(), mPaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public synchronized boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled())
            return false;

        final int action = event.getAction();

        if (event.getPointerCount() > 2) {
            return false;
        }

        switch (action & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:
                mPrimaryTouch = new TouchInfo(event);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mSecondaryTouch = new TouchInfo(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mPrimaryTouch != null && event.findPointerIndex(mPrimaryTouch.id) != -1) {
                    if (mPrimaryTouch.isDragging) {
                        trackTouchEvent(event, mPrimaryTouch);
                    } else {
                        final float x = event.getX(event.findPointerIndex(mPrimaryTouch.id));
                        if (Math.abs(x - mPrimaryTouch.touchDownX) > mScaledTouchSlop) {
                            mPrimaryTouch.lastX = x;

                            setPressed(true);
                            invalidate();
                            mPrimaryTouch.isDragging = true;
                            trackTouchEvent(event, mPrimaryTouch);
                            attemptClaimDrag();
                        }
                    }
                }
                if (mSecondaryTouch != null && event.findPointerIndex(mSecondaryTouch.id) != -1) {
                    if (mSecondaryTouch.isDragging) {
                        trackTouchEvent(event, mSecondaryTouch);
                    } else {
                        int index = event.findPointerIndex(mSecondaryTouch.id);
                        final float x = event.getX(index);
                        if (Math.abs(x - mSecondaryTouch.touchDownX) > mScaledTouchSlop) {
                            mSecondaryTouch.lastX = x;
                            mSecondaryTouch.isDragging = true;
                            trackTouchEvent(event, mSecondaryTouch);
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                float pointerIndex = event.getActionIndex();
                if (event.getPointerId(event.getActionIndex()) == mPrimaryTouch.id) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mPrimaryTouch.touchDownX = event.getX(newPointerIndex);
                    mPrimaryTouch.lastX = mPrimaryTouch.touchDownX;
                    mPrimaryTouch.dragMode = evalDragMode(mPrimaryTouch.touchDownX);
                    mPrimaryTouch.id = event.getPointerId(newPointerIndex);
                }
                mSecondaryTouch = null;
                break;
            case MotionEvent.ACTION_UP:
                if (mPrimaryTouch.isDragging) {
                    trackTouchEvent(event, mPrimaryTouch);
                    mPrimaryTouch.isDragging = false;
                    setPressed(false);
                }
                mPrimaryTouch = null;
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mPrimaryTouch.isDragging) {
                    mPrimaryTouch.isDragging = false;
                    setPressed(false);
                }
                mPrimaryTouch = null;
                invalidate();
                break;
        }

        return true;

    }

    private class TouchInfo {
        private int id;
        private boolean isDragging;
        private float touchDownX;
        private float lastX;
        private DragMode dragMode;

        TouchInfo(MotionEvent event) {
            int index = event.getActionIndex();
            id = event.getPointerId(index);
            touchDownX = event.getX(index);
            dragMode = evalDragMode(touchDownX);
            lastX = touchDownX;
        }
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
        setValues(ss.leftValue, ss.rightValue);
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

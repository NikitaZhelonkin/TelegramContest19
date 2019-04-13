package ru.zhelonkin.tgcontest.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Keep;
import android.util.AttributeSet;
import android.widget.CompoundButton;


public class Checkbox extends CompoundButton {

    private int mOffsetX = 0;

    public Checkbox(Context context) {
        super(context);
    }

    public Checkbox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Checkbox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setChecked(boolean checked) {
        int paddingLeft = getCompoundPaddingLeft();
        int paddingRight = getCompoundPaddingRight();
        int offset = (paddingLeft - paddingRight) / 2;
        if (checked != isChecked()) {
            animateOffset(checked ? 0 : -offset);
        } else {
            setOffsetX(checked ? 0 : -offset);
            invalidate();
        }
        super.setChecked(checked);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int save = canvas.save();
        canvas.translate(mOffsetX, 0);
        super.onDraw(canvas);
        canvas.restoreToCount(save);
    }

    @Keep
    public void setOffsetX(int offsetX) {
        mOffsetX = offsetX;
    }

    @Keep
    public int getOffsetX() {
        return mOffsetX;
    }

    private void animateOffset(int x) {
        ObjectAnimator animator = ObjectAnimator.ofInt(this, "offsetX", x);
        animator.setDuration(200);
        animator.setAutoCancel(true);
        animator.start();
    }

}

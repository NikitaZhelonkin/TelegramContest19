package ru.zhelonkin.tgcontest.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.CompoundButton;

import android.support.annotation.Nullable;


public class Checkbox extends CompoundButton {

    private Drawable mDrawable;
    private ColorStateList mTint;

    private int mPaddingLeft = -1;
    private int mPaddingRight = -1;

    private boolean inited = false;

    public Checkbox(Context context) {
        super(context);
        init(context, null);
    }

    public Checkbox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public Checkbox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        int[] tintAttr = new int[]{android.R.attr.backgroundTint};
        TypedArray a = context.obtainStyledAttributes(attrs, tintAttr);

        int tint = a.getColor(0, 0);
        if (tint != 0) {
            setSupportBackgroundTintList(ColorStateList.valueOf(tint));
        }
        a.recycle();
        mPaddingLeft = getPaddingLeft();
        mPaddingRight = getPaddingRight();
        inited = true;
    }

    @Override
    public void setButtonDrawable(@Nullable Drawable drawable) {
        super.setButtonDrawable(drawable);
        mDrawable = drawable;
    }

    @Override
    public void setBackground(Drawable background) {
        super.setBackground(background);
        if (mTint != null) {
            setSupportBackgroundTintList(mTint);
        }
    }

    public void setSupportBackgroundTintList(@Nullable ColorStateList tint) {
        mTint = tint;
        if (tint == null) {
            return;
        }
        setSupportTintList(getBackground(), tint, getDrawableState());
    }


    private static void setSupportTintList(Drawable drawable, ColorStateList tint, int[] state) {
        if (tint != null) {
            int color = tint.getColorForState(state, Color.TRANSPARENT);
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        } else {
            drawable.clearColorFilter();
        }

        if (Build.VERSION.SDK_INT <= 23) {
            // Pre-v23 there is no guarantee that a state change will invoke an invalidation,
            // so we force it ourselves
            drawable.invalidateSelf();
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        mPaddingRight = right;
        mPaddingLeft = left;
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        if (inited) {
            super.setButtonDrawable(checked ? mDrawable : null);
            int newPadding = (mPaddingLeft + mPaddingRight + mDrawable.getIntrinsicWidth()) / 2;
            super.setPadding(checked ? mPaddingLeft : newPadding, getPaddingTop(), checked ? mPaddingRight : newPadding, getPaddingBottom());
        }
    }

}

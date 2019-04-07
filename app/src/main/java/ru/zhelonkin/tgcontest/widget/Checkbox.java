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

import androidx.annotation.Nullable;
import androidx.core.view.TintableBackgroundView;
import ru.zhelonkin.tgcontest.R;


public class Checkbox extends CompoundButton implements TintableBackgroundView {


    private ColorStateList mTint;

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
        int[] tintAttr = new int[]{R.attr.backgroundTint};
        TypedArray a = context.obtainStyledAttributes(attrs, tintAttr);

        int tint = a.getColor(0, 0);
        if (tint != 0) {
            setSupportBackgroundTintList(ColorStateList.valueOf(tint));
        }
        a.recycle();
    }


    @Override
    public void setBackground(Drawable background) {
        super.setBackground(background);
        if (mTint != null) {
            setSupportBackgroundTintList(mTint);
        }
    }

    @Override
    public void setSupportBackgroundTintList(@Nullable ColorStateList tint) {
        mTint = tint;
        if (tint == null) {
            return;
        }
        setSupportTintList(getBackground(), tint, getDrawableState());
    }

    @Nullable
    @Override
    public ColorStateList getSupportBackgroundTintList() {
        return mTint;
    }

    @Override
    public void setSupportBackgroundTintMode(@Nullable PorterDuff.Mode tintMode) {
        //do nothing
    }

    @Nullable
    @Override
    public PorterDuff.Mode getSupportBackgroundTintMode() {
        return null;
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
}

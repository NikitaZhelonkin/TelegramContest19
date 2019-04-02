package ru.zhelonkin.tgcontest.widget;

import android.content.Context;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class DynamicLinearLayout extends LinearLayout {

    private DynamicViewDelegate mDynamicViewDelegate;

    public DynamicLinearLayout(Context context) {
        super(context);
        init(context);
    }

    public DynamicLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DynamicLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public DynamicLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mDynamicViewDelegate = new DynamicViewDelegate(this);
    }

    public void setAdapter(DynamicViewDelegate.Adapter adapter) {
        mDynamicViewDelegate.setAdapter(adapter);
    }

    public DynamicViewDelegate.Adapter getAdapter() {
        return mDynamicViewDelegate.getAdapter();
    }

}

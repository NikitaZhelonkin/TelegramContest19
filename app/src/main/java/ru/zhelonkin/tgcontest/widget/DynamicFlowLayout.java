package ru.zhelonkin.tgcontest.widget;

import android.content.Context;
import android.util.AttributeSet;

import android.support.annotation.Nullable;

public class DynamicFlowLayout extends FlowLayout {

    private DynamicViewDelegate mDynamicViewDelegate;

    public DynamicFlowLayout(Context context) {
        super(context);
        init(context);
    }

    public DynamicFlowLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
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

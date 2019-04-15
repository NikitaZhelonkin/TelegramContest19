package ru.zhelonkin.tgcontest.widget.chart;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.ListUpdateCallback;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.formatter.CachingFormatter;
import ru.zhelonkin.tgcontest.formatter.DateFormatter;
import ru.zhelonkin.tgcontest.formatter.Formatter;
import ru.zhelonkin.tgcontest.formatter.SimpleNumberFormatter;
import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.utils.ThemeUtils;
import ru.zhelonkin.tgcontest.widget.DynamicLinearLayout;
import ru.zhelonkin.tgcontest.widget.DynamicViewDelegate;
import ru.zhelonkin.tgcontest.widget.FastOutSlowInInterpolator;

public class ChartPopupView extends LinearLayout {

    private TextView mXValueView;

    private Adapter mAdapter;

    private boolean mIsShowing;

    private Formatter mDateFormatter = new CachingFormatter(new DateFormatter("E, dd MMM YYYY"));

    private float mPopupOffset;

    private Drawable mArrowDrawable;

    public ChartPopupView(@NonNull Context context) {
        this(context, null);
    }

    public ChartPopupView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.popupStyle);
    }

    public ChartPopupView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.popup_chart, this);
        mXValueView = v.findViewById(R.id.xValue);
        DynamicLinearLayout valuesLayout = v.findViewById(R.id.values_layout);
        valuesLayout.setAdapter(mAdapter = new ChartPopupView.Adapter(context));
        setOrientation(VERTICAL);

        mArrowDrawable = context.getDrawable(R.drawable.ic_arrow_right);
        mArrowDrawable.setTint(ThemeUtils.getColor(context, android.R.attr.textColorPrimary, 0));
    }

    public void show(boolean animate) {
        setVisibility(View.VISIBLE);
        if (animate) {
            if (mIsShowing) return;
            animate().cancel();
            animate().setListener(null);
            animate().alpha(1).setDuration(200).start();
        } else {
            setAlpha(1);
        }
        mIsShowing = true;
    }

    public void hide(boolean animate) {
        if (animate) {
            if (!mIsShowing) return;
            animate().cancel();
            animate().setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    setVisibility(View.INVISIBLE);
                }
            });
            animate().alpha(0).setDuration(200).start();
        } else {
            setVisibility(View.INVISIBLE);
            setAlpha(0);
        }
        mIsShowing = false;
    }

    public void animateOffset(int offset) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "popupOffset", offset);
        animator.setAutoCancel(true);
        animator.setDuration(300);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.start();
    }

    @Keep
    public float getPopupOffset() {
        return mPopupOffset;
    }

    @Keep
    public void setPopupOffset(float popupOffset) {
        setTranslationX(getTranslationX() - mPopupOffset + popupOffset);
        mPopupOffset = popupOffset;
    }

    public boolean isShowing() {
        return mIsShowing;
    }

    public void setDateFormatter(Formatter dateFormatter) {
        mDateFormatter = new CachingFormatter(dateFormatter);
    }

    public void bindData(Chart chart, int position) {
        mXValueView.setVisibility(View.VISIBLE);
        mXValueView.setText(mDateFormatter.format(chart.getXValues().get(position)));
        mXValueView.setCompoundDrawablesWithIntrinsicBounds(null, null, isClickable() ? mArrowDrawable : null, null);
        mAdapter.setData(chart, position);
    }

    public void bindData(List<Item> items){
        mXValueView.setVisibility(View.GONE);
        mAdapter.setData(items);
    }

    void clearData() {
        mAdapter.setData(null, 0);
    }

    private static class Adapter extends DynamicViewDelegate.Adapter<Adapter.ViewHolder> implements ListUpdateCallback {

        private Formatter mValueFormatter = new CachingFormatter(new SimpleNumberFormatter());

        private List<Item> mItems;
        private long mSumm;
        private boolean mPercentage;

        private int mDefaultColor;

        private int mMargin;

        private Adapter(Context context) {
            mMargin = context.getResources().getDimensionPixelSize(R.dimen.popup_item_margin);
            mDefaultColor = ThemeUtils.getColor(context, android.R.attr.textColorPrimary, 0);
        }

        void setData(Chart chart, int position) {
            List<Item> items = new ArrayList<>();
            mPercentage = chart.isPercentage();
            boolean showAll = chart.isStacked() && !chart.isPercentage();
            mSumm = 0;
            for (Graph graph : chart.getVisibleGraphs()) {
                long value = graph.getPoints().get(position).y;
                items.add(new Item(
                        value,
                        graph.getName(),
                        graph.getColor()));
                mSumm += value;

            }
            if (showAll) {
                items.add(new Item(mSumm, "All", mDefaultColor));
            }
            mItems = items;
            notifyDataChanged();
        }

        void setData(List<Item> items){
            mItems = items;
            notifyDataChanged();
        }

        @Override
        public int getCount() {
            return mItems == null ? 0 : mItems.size();
        }

        @Override
        protected ChartPopupView.Adapter.ViewHolder onCreateViewHolder(ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new ChartPopupView.Adapter.ViewHolder(inflater.inflate(R.layout.item_popup_value, parent, false));
        }

        @Override
        protected void onBindViewHolder(ChartPopupView.Adapter.ViewHolder viewHolder, int position, Object payload) {
            ((MarginLayoutParams) viewHolder.itemView.getLayoutParams()).topMargin = position == 0 ? 0 : mMargin;
            Context context = viewHolder.itemView.getContext();
            Item item = mItems.get(position);
            viewHolder.lineNameView.setText(item.title);
            viewHolder.valueView.setTextColor(item.color);
            viewHolder.valueView.setText(mValueFormatter.format(item.value));
            long percent = mSumm == 0 ? 0 : (100 * item.value) / mSumm;
            viewHolder.percentView.setText(context.getString(R.string.percent, percent));
            viewHolder.percentView.setVisibility(mPercentage ? View.VISIBLE : View.GONE);
        }

        class ViewHolder extends DynamicViewDelegate.ViewHolder {
            TextView percentView;
            TextView valueView;
            TextView lineNameView;

            ViewHolder(View itemView) {
                super(itemView);
                valueView = itemView.findViewById(R.id.value);
                lineNameView = itemView.findViewById(R.id.line_name);
                percentView = itemView.findViewById(R.id.percent);
            }
        }
    }

    public static class Item {
        long value;
        String title;
        int color;

        public Item(long value, String title, int color) {
            this.value = value;
            this.title = title;
            this.color = color;
        }
    }
}

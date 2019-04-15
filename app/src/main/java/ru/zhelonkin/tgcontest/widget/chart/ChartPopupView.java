package ru.zhelonkin.tgcontest.widget.chart;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
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

    void bindData(Chart chart, int position) {
        mXValueView.setText(mDateFormatter.format(chart.getXValues().get(position)));
        mXValueView.setCompoundDrawablesWithIntrinsicBounds(null, null, isClickable() ? mArrowDrawable : null, null);
        mAdapter.setData(chart, position);
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

        private Adapter(Context context) {
            mDefaultColor = ThemeUtils.getColor(context, android.R.attr.textColorPrimary, 0);
        }

        void setData(Chart chart, int position) {
            List<Item> mItems = new ArrayList<>();
            mPercentage = chart.isPercentage();
            boolean showAll = chart.isStacked() && !chart.isPercentage();
            mSumm = 0;
            for (Graph graph : chart.getVisibleGraphs()) {
                long value = graph.getPoints().get(position).y;
                mItems.add(new Item(
                        value,
                        graph.getName(),
                        graph.getColor()));
                mSumm += value;

            }
            if (showAll) {
                mItems.add(new Item(mSumm, "All", mDefaultColor));
            }
            this.mItems = mItems;
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

        private class Item {
            long value;
            String title;
            int color;

            Item(long value, String title, int color) {
                this.value = value;
                this.title = title;
                this.color = color;
            }
        }

        private class DiffUtilCallback extends DiffUtil.Callback {


            private List<Item> oldValue;
            private List<Item> newValue;

            DiffUtilCallback(List<Item> oldValue, List<Item> newValue) {
                this.oldValue = oldValue;
                this.newValue = newValue;
            }

            @Override
            public int getOldListSize() {
                return oldValue == null ? 0 : oldValue.size();
            }

            @Override
            public int getNewListSize() {
                return newValue == null ? 0 : newValue.size();
            }

            @Override
            public boolean areItemsTheSame(int i, int i1) {
                return oldValue.get(i).color == newValue.get(i1).color;
            }

            @Override
            public boolean areContentsTheSame(int i, int i1) {
                return false;
            }
        }
    }
}

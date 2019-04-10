package ru.zhelonkin.tgcontest.widget.chart;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.formatter.CachingFormatter;
import ru.zhelonkin.tgcontest.formatter.DateFormatter;
import ru.zhelonkin.tgcontest.formatter.Formatter;
import ru.zhelonkin.tgcontest.formatter.SimpleNumberFormatter;
import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.widget.DynamicLinearLayout;
import ru.zhelonkin.tgcontest.widget.DynamicViewDelegate;

public class ChartPopupView extends LinearLayout {

    private TextView mXValueView;

    private Adapter mAdapter;

    private boolean mIsShowing;

    private Formatter mDateFormatter = new CachingFormatter(new DateFormatter("E, dd MMM YYYY"));

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

    private void init(Context context){
        LayoutInflater inflater = LayoutInflater.from(context);
        View v= inflater.inflate(R.layout.popup_chart, this);
        mXValueView = v.findViewById(R.id.xValue);
        DynamicLinearLayout valuesLayout = v.findViewById(R.id.values_layout);
        valuesLayout.setAdapter(mAdapter = new ChartPopupView.Adapter());
        setOrientation(VERTICAL);
    }

    public void show(boolean animate){
        if(animate){
            if(mIsShowing) return;
            animate().cancel();
            animate().alpha(1).setDuration(200).start();
        }else {
            setAlpha(1);
        }
        mIsShowing = true;
    }

    public void hide(boolean animate){
        if(animate){
            if(!mIsShowing) return;
            animate().cancel();
            animate().alpha(0).setDuration(200).start();
        }else {
            setAlpha(0);
        }
        mIsShowing = false;
    }

    public boolean isShowing(){
        return mIsShowing;
    }

    void bindData(Chart chart, int position) {
        mXValueView.setText(mDateFormatter.format(chart.getXValues().get(position)));
        mAdapter.setData(chart, position);
    }

    private static class Adapter extends DynamicViewDelegate.Adapter<Adapter.ViewHolder> {

        private Formatter mValueFormatter = new SimpleNumberFormatter();

        private Chart mChart;
        private int mPointPosition;
        private List<Graph> mGraphs;

        void setData(Chart chart, int position) {
            mChart = chart;
            mPointPosition = position;
            mGraphs = mChart.getVisibleGraphs();
            notifyDataChanged();
        }

        @Override
        public int getCount() {
            return mGraphs == null ? 0 : mGraphs.size();
        }

        @Override
        protected ChartPopupView.Adapter.ViewHolder onCreateViewHolder(ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new ChartPopupView.Adapter.ViewHolder(inflater.inflate(R.layout.item_popup_value, parent, false));
        }

        @Override
        protected void onBindViewHolder(ChartPopupView.Adapter.ViewHolder viewHolder, int position, Object payload) {
            Graph graph = mGraphs.get(position);
            viewHolder.lineNameView.setText(graph.getName());
            viewHolder.valueView.setTextColor(graph.getColor());
            viewHolder.valueView.setText(mValueFormatter.format((long) mChart.getY(graph, mPointPosition)));
        }

        class ViewHolder extends DynamicViewDelegate.ViewHolder {
            TextView valueView;
            TextView lineNameView;

            ViewHolder(View itemView) {
                super(itemView);
                valueView = itemView.findViewById(R.id.value);
                lineNameView = itemView.findViewById(R.id.line_name);
            }
        }
    }
}

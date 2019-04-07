package ru.zhelonkin.tgcontest.widget;

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
import ru.zhelonkin.tgcontest.model.Graph;

public class ChartPopupView extends LinearLayout {

    private TextView mXValueView;

    private Adapter mAdapter;

    private boolean mIsShowing;

    private Formatter mDateFormatter = new CachingFormatter(new DateFormatter("E, MMM dd"));

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
            animate().alpha(1).start();
        }else {
            setAlpha(1);
        }
        mIsShowing = true;
    }

    public void  hide(boolean animate){

        if(animate){
            if(!mIsShowing) return;
            animate().cancel();
            animate().alpha(0).start();
        }else {
            setAlpha(0);
        }
        mIsShowing = false;
    }

    public boolean isShowing(){
        return mIsShowing;
    }

    void bindData(List<Graph.PointAndLine> points) {
        if (points.size() == 0) return;
        mXValueView.setText(mDateFormatter.format(points.get(0).point.x));
        mAdapter.setData(points);
    }

    private static class Adapter extends DynamicViewDelegate.Adapter<ChartPopupView.Adapter.ViewHolder> {

        private Formatter mValueFormatter = new SimpleNumberFormatter();

        private List<Graph.PointAndLine> mPointAndLines;

        void setData(List<Graph.PointAndLine> pointAndLines) {
            mPointAndLines = pointAndLines;
            notifyDataChanged();
        }

        @Override
        public int getCount() {
            return mPointAndLines == null ? 0 : mPointAndLines.size();
        }

        @Override
        protected ChartPopupView.Adapter.ViewHolder onCreateViewHolder(ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new ChartPopupView.Adapter.ViewHolder(inflater.inflate(R.layout.item_popup_value, parent, false));
        }

        @Override
        protected void onBindViewHolder(ChartPopupView.Adapter.ViewHolder viewHolder, int position, Object payload) {
            Graph.PointAndLine pointAndLine = mPointAndLines.get(position);
            viewHolder.lineNameView.setTextColor(pointAndLine.line.getColor());
            viewHolder.lineNameView.setText(pointAndLine.line.getName());
            viewHolder.valueView.setTextColor(pointAndLine.line.getColor());
            viewHolder.valueView.setText(mValueFormatter.format(pointAndLine.point.y));
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

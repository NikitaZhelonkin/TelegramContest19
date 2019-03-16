package ru.zhelonkin.tgcontest.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import ru.zhelonkin.tgcontest.R;

class ChartPopup {

    private TextView mXValueView;

    private Adapter mAdapter;

    private PopupWindow mPopupWindow;

    private int mXOffset;
    private int mYOffset;

    ChartPopup(Context context) {
        TypedArray a = context.obtainStyledAttributes(null, new int[]{
                android.R.attr.dropDownHorizontalOffset,
                android.R.attr.dropDownVerticalOffset,
        }, R.attr.popupStyle, 0);
        mXOffset = a.getDimensionPixelSize(0, 0);
        mYOffset = a.getDimensionPixelSize(1, 0);
        a.recycle();
        mPopupWindow = new PopupWindow(context, null, R.attr.popupStyle, 0);
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.popup_chart, null);
        mXValueView = v.findViewById(R.id.xValue);
        DynamicLinearLayout valuesLayout = v.findViewById(R.id.values_layout);
        valuesLayout.setAdapter(mAdapter = new Adapter());
        mPopupWindow.setContentView(v);
        mPopupWindow.setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    void bindData(List<ChartView.PointAndLine> points) {
        if (points.size() == 0) return;
        mXValueView.setText(new DateFormatter().format(points.get(0).point.x));
        mAdapter.setData(points);

    }

    void showAtLocation(View anchor, int x, int y) {
        int[] location = locationInWindow(anchor);
        mPopupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, location[0] + x + mXOffset, location[1] + y + mYOffset);
    }

    void update(View anchor, int x, int y) {
        int[] location = locationInWindow(anchor);
        mPopupWindow.update(location[0] + x + mXOffset, location[1] + y + mYOffset, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    void dismiss() {
        mPopupWindow.dismiss();
    }

    boolean isShowing() {
        return mPopupWindow.isShowing();
    }

    private int[] locationInWindow(View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        return location;
    }

    private static class DateFormatter {

        private static final String DATE_FORMAT = "E, MMM dd";

        String format(long date) {
            return capitalize(new SimpleDateFormat(DATE_FORMAT, Locale.US).format(date));
        }
        private static String capitalize(String string) {
            return string.substring(0, 1).toUpperCase() + string.substring(1);
        }
    }

    private static class Adapter extends DynamicViewDelegate.Adapter<Adapter.ViewHolder> {

        private List<ChartView.PointAndLine> mPointAndLines;

        void setData(List<ChartView.PointAndLine> pointAndLines) {
            mPointAndLines = pointAndLines;
            notifyDataChanged();
        }

        @Override
        public int getCount() {
            return mPointAndLines == null ? 0 : mPointAndLines.size();
        }

        @Override
        protected ViewHolder onCreateViewHolder(ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new ViewHolder(inflater.inflate(R.layout.item_popup_value, parent, false));
        }

        @Override
        protected void onBindViewHolder(ViewHolder viewHolder, int position, Object payload) {
            ChartView.PointAndLine pointAndLine = mPointAndLines.get(position);
            viewHolder.lineNameView.setTextColor(pointAndLine.line.getColor());
            viewHolder.lineNameView.setText(pointAndLine.line.getName());
            viewHolder.valueView.setTextColor(pointAndLine.line.getColor());
            viewHolder.valueView.setText(String.valueOf(pointAndLine.point.y));
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

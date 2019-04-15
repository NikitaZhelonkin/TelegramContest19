package ru.zhelonkin.tgcontest.main;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.formatter.CachingFormatter;
import ru.zhelonkin.tgcontest.formatter.DateFormatter;
import ru.zhelonkin.tgcontest.formatter.Formatter;
import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.ChartData;
import ru.zhelonkin.tgcontest.model.ChartWithZoom;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.task.GetDayChartTask;
import ru.zhelonkin.tgcontest.utils.ThemeUtils;
import ru.zhelonkin.tgcontest.utils.ViewUtils;
import ru.zhelonkin.tgcontest.widget.DynamicFlowLayout;
import ru.zhelonkin.tgcontest.widget.RangeSeekBar;
import ru.zhelonkin.tgcontest.widget.chart.ChartView;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ChartViewHolder> {

    private ChartData mChartData;

    public MainAdapter() {

    }

    public void setChartData(ChartData chartData) {
        mChartData = chartData;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public ChartViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
        return new ChartViewHolder(layoutInflater.inflate(R.layout.list_item_chart, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ChartViewHolder chartViewHolder, int i) {
        chartViewHolder.bindView(mChartData.getCharts().get(i), i);
    }

    @Override
    public int getItemCount() {
        return mChartData == null ? 0 : mChartData.getCharts().size();
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    class ChartViewHolder extends RecyclerView.ViewHolder implements RangeSeekBar.OnRangeSeekBarChangeListener,
            FiltersAdapter.Callback {

        private Formatter mDateFormatter = new CachingFormatter(new DateFormatter("dd MMM YYYY"));
        private Formatter mSingleDateFormatter = new CachingFormatter(new DateFormatter("EEE, dd MMM YYYY"));

        private TextView titleView;
        private TextView zoomOutView;
        private TextView rangeView;
        private ChartView chartView;
        private ChartView chartPreview;
        private RangeSeekBar rangeSeekBar;
        private FiltersAdapter filtersAdapter;

        ChartViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.title);
            zoomOutView = itemView.findViewById(R.id.zoom_out);
            rangeView = itemView.findViewById(R.id.range);
            chartView = itemView.findViewById(R.id.chart_view);
            chartPreview = itemView.findViewById(R.id.chart_preview);
            rangeSeekBar = itemView.findViewById(R.id.rangeBar);
            rangeSeekBar.setOnRangeSeekBarChangeListener(this);
            DynamicFlowLayout linesLayout = itemView.findViewById(R.id.line_list_layout);
            linesLayout.setAdapter(filtersAdapter = new FiltersAdapter());
            filtersAdapter.setCallback(this);
            int colorAccent = ThemeUtils.getColor(itemView.getContext(), android.R.attr.colorAccent, 0);
            Drawable drawable = itemView.getContext().getDrawable(R.drawable.ic_zoom_out);
            if (drawable != null) drawable.setTint(colorAccent);
            zoomOutView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        }

        void bindView(ChartWithZoom chartWithZoom, int position) {
            Chart chart = chartWithZoom.getCurrent();
            chartView.setChart(chart);
            chartPreview.setChart(chart);
            filtersAdapter.setGraphs(chart);
            ViewUtils.onPreDraw(chartView, () -> {
                rangeSeekBar.setValues(chart.left, chart.right);
            });
            titleView.setText(itemView.getContext().getString(R.string.chart_title, position + 1));

            titleView.setVisibility(chartWithZoom.isZoomed() ? View.INVISIBLE : View.VISIBLE);
            zoomOutView.setVisibility(chartWithZoom.isZoomed() ? View.VISIBLE : View.INVISIBLE);

            chartView.setDrawAxis(!(chartWithZoom.isZoomed() && chart.isPercentage()));

            if (chartWithZoom.isZoomed()) {
                chartView.setAxisDateFormatter(new DateFormatter("HH:mm"));
                chartView.setPopupDateFormatter(new DateFormatter("HH:mm"));
                chartView.setOnPopupClickedListener(null);
            } else {
                chartView.setAxisDateFormatter(new DateFormatter("MMM dd"));
                chartView.setPopupDateFormatter(new DateFormatter("E, dd MMM YYYY"));
                chartView.setOnPopupClickedListener(this::zoomIn);
            }
            zoomOutView.setOnClickListener(v -> zoomOut());
        }

        private void zoomIn(long date) {
            if (getAdapterPosition() == RecyclerView.NO_POSITION) return;
            int position = getAdapterPosition();
            ChartWithZoom chartWithZoom = mChartData.getCharts().get(position);
            Chart chart = chartWithZoom.getCurrent();
            if (chart.isPercentage()) {
                int size = chart.getXValues().size();
                int index = Collections.binarySearch(chart.getXValues(), date);
                int leftIndex = Math.max(0, Math.min(size - 1, index - 3));
                int rightIndex = Math.max(0, Math.min(size - 1, index + 4));

                Chart chartZoomed = chart.subChart(leftIndex, rightIndex);
                chartZoomed.setPieChart(true);
                int range = 100 / chartZoomed.getXValues().size();
                chartZoomed.left = (100 - range) / 2f;
                chartZoomed.right = chartZoomed.left + range;

                chartWithZoom.setZoomedChart(chartZoomed);
                notifyItemChanged(position);
                return;
            }
            String dateString = new SimpleDateFormat("yyyy-MM/dd", Locale.getDefault()).format(date);
            String path = (position + 1) + "/" + dateString + ".json";
            GetDayChartTask task = new GetDayChartTask(itemView.getContext().getAssets(), path, new GetDayChartTask.Callback() {
                @Override
                public void onSuccess(Chart chart) {
                    if (getAdapterPosition() == RecyclerView.NO_POSITION) return;
                    int position = getAdapterPosition();
                    ChartWithZoom chartWithZoom = mChartData.getCharts().get(position);
                    chartWithZoom.setZoomedChart(chart);

                    setLeftAndRightForDate(chart, date);
                    notifyItemChanged(position);
                }

                @Override
                public void onError(Throwable t) {
                    //Do nothing
                }
            });
            task.execute();
        }

        private void setLeftAndRightForDate(Chart chart, long date){
            List<Long> xValues = chart.getXValues();
            Calendar leftDate = midnight(date);
            int leftIndex = Collections.binarySearch(xValues, leftDate.getTimeInMillis());
            Calendar rightDate = midnight(date);
            rightDate.add(Calendar.DAY_OF_YEAR, 1);
            int rightIndex = Collections.binarySearch(xValues, rightDate.getTimeInMillis())-1;

            chart.left = Math.max(0, Math.min(100, 100 * leftIndex / (float)(xValues.size())));
            chart.right = Math.max(0, Math.min(100, 100 * rightIndex / (float) (xValues.size())));
        }


        private Calendar midnight(long date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(date);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            return calendar;
        }

        private void zoomOut() {
            if (getAdapterPosition() == RecyclerView.NO_POSITION) return;
            int position = getAdapterPosition();
            ChartWithZoom chartWithZoom = mChartData.getCharts().get(position);
            chartWithZoom.setZoomedChart(null);
            notifyItemChanged(position);
        }

        @Override
        public void onRangeChanged(float leftValue, float rightValue, boolean fromUser) {
            if (getAdapterPosition() == RecyclerView.NO_POSITION) return;
            Chart chart = mChartData.getCharts().get(getAdapterPosition()).getCurrent();
            chart.left = leftValue;
            chart.right = rightValue;
            chartView.setChartLeftAndRight(chart.left / 100f, chart.right / 100f, fromUser);

            updateDateView(chart);

        }
        private void updateDateView(Chart chart){
            List<Long> xValues = chart.getXValues();
            int leftIndex =(int) ((xValues.size()) * chart.left / 100f) ;
            int rightIndex = Math.min(xValues.size()-1, (int) ((xValues.size()) * chart.right / 100f));
            String leftDate = mDateFormatter.format(xValues.get(leftIndex));
            String rightDate = mDateFormatter.format(xValues.get(rightIndex));

            if (leftDate.equals(rightDate)) {
                rangeView.setText(mSingleDateFormatter.format(xValues.get(leftIndex)));
            } else {
                rangeView.setText(itemView.getContext().getString(R.string.range_format, leftDate, rightDate));
            }
        }

        @Override
        public void onCheckChanged(Graph graph, boolean checked) {
            graph.setVisible(checked);
            chartView.onFiltersChanged();
            chartPreview.onFiltersChanged();
        }

        @Override
        public void onLongClick(Graph graph) {
            if (getAdapterPosition() == RecyclerView.NO_POSITION) return;
            Chart chart = mChartData.getCharts().get(getAdapterPosition()).getCurrent();
            for (Graph g : chart.getGraphs()) {
                g.setVisible(g == graph);
            }
            filtersAdapter.notifyDataChanged(FiltersAdapter.PAYLOAD_CHECKBOX);
            chartView.onFiltersChanged();
            chartPreview.onFiltersChanged();
        }
    }
}

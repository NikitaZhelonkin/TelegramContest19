package ru.zhelonkin.tgcontest.main;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.ViewUtils;
import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.ChartData;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.widget.ChartView;
import ru.zhelonkin.tgcontest.widget.DynamicFlowLayout;
import ru.zhelonkin.tgcontest.widget.RangeSeekBar;

public class MainAdapter extends RecyclerView.Adapter<MainAdapter.ChartViewHolder> {

    private ChartData mChartData;

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

    class ChartViewHolder extends RecyclerView.ViewHolder implements RangeSeekBar.OnRangeSeekBarChangeListener,
            GraphAdapter.OnCheckChangedListener {

        private TextView titleView;
        private ChartView chartView;
        private ChartView chartPreview;
        private RangeSeekBar rangeSeekBar;
        private GraphAdapter mGraphAdapter;

        ChartViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.title);
            chartView = itemView.findViewById(R.id.chart_view);
            chartPreview = itemView.findViewById(R.id.chart_preview);
            rangeSeekBar = itemView.findViewById(R.id.rangeBar);
            rangeSeekBar.setOnRangeSeekBarChangeListener(this);
            DynamicFlowLayout linesLayout = itemView.findViewById(R.id.line_list_layout);
            linesLayout.setAdapter(mGraphAdapter = new GraphAdapter());
            mGraphAdapter.setOnCheckChangedListener(this);
        }

        void bindView(Chart chart, int position) {
            chartView.setChart(chart);
            chartPreview.setChart(chart);
            mGraphAdapter.setChart(chart);
            titleView.setText(itemView.getContext().getString(R.string.chart_title, position + 1));

            ViewUtils.onPreDraw(chartView, () -> {
                rangeSeekBar.setValues(chart.left, chart.right);
            });
        }

        @Override
        public void onRangeChanged(float leftValue, float rightValue, boolean fromUser) {
            Chart chart = mChartData.getCharts().get(getAdapterPosition());
            chart.left = leftValue;
            chart.right = rightValue;
            chartView.setChartLeftAndRight(chart.left / 100f, chart.right / 100f, fromUser);
        }

        @Override
        public void onCheckChanged(Graph graph, boolean checked) {
            chartView.updateGraphLines();
            chartPreview.updateGraphLines();
        }

    }
}

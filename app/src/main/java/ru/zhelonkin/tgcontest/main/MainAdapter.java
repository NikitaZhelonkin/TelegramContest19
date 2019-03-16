package ru.zhelonkin.tgcontest.main;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.ViewUtils;
import ru.zhelonkin.tgcontest.model.ChartData;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Line;
import ru.zhelonkin.tgcontest.widget.ChartView;
import ru.zhelonkin.tgcontest.widget.DynamicLinearLayout;
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
        chartViewHolder.bindView(mChartData.getGraphs().get(i));
    }

    @Override
    public int getItemCount() {
        return mChartData == null ? 0 : mChartData.getGraphs().size();
    }

    class ChartViewHolder extends RecyclerView.ViewHolder implements RangeSeekBar.OnRangeSeekBarChangeListener,
            LinesAdapter.OnCheckChangedListener {

        private ChartView chartView;
        private ChartView chartPreview;
        private RangeSeekBar rangeSeekBar;
        private LinesAdapter linesAdapter;


        ChartViewHolder(@NonNull View itemView) {
            super(itemView);
            chartView = itemView.findViewById(R.id.chart_view);
            chartPreview = itemView.findViewById(R.id.chart_preview);
            rangeSeekBar = itemView.findViewById(R.id.rangeBar);
            rangeSeekBar.setOnRangeSeekBarChangeListener(this);
            DynamicLinearLayout linesLayout = itemView.findViewById(R.id.line_list_layout);
            linesLayout.setAdapter(linesAdapter = new LinesAdapter());
            linesAdapter.setOnCheckChangedListener(this);
        }

        void bindView(Graph graph) {
            chartView.setGraph(graph);
            chartPreview.setGraph(graph);
            linesAdapter.setGraph(graph);

            ViewUtils.onPreDraw(chartView, () -> {
                rangeSeekBar.setValues(graph.left * 100, graph.right * 100);
            });
        }

        @Override
        public void onRangeChanged(float leftValue, float rightValue, boolean fromUser) {
            Graph graph = mChartData.getGraphs().get(getAdapterPosition());
            graph.left = leftValue / 100f;
            graph.right = rightValue / 100f;
            chartView.setLeftAndRight(graph.left, graph.right, fromUser);
        }

        @Override
        public void onCheckChanged(Line line, boolean checked) {
            chartView.updateGraphLines();
            chartPreview.updateGraphLines();
        }

    }
}

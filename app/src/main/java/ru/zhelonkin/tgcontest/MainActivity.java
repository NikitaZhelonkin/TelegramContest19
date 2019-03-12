package ru.zhelonkin.tgcontest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import ru.zhelonkin.tgcontest.model.ChartData;
import ru.zhelonkin.tgcontest.task.GetChartDataTask;
import ru.zhelonkin.tgcontest.widget.ChartView;
import ru.zhelonkin.tgcontest.widget.RangeSeekBar;

public class MainActivity extends AppCompatActivity implements GetChartDataTask.Callback, RangeSeekBar.OnRangeSeekBarChangeListener {

    private GetChartDataTask mGetChartDataTask;

    private ChartView mChartView;
    private ChartView mChartPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mChartView = findViewById(R.id.chart_view);
        mChartPreview = findViewById(R.id.chart_preview);
        RangeSeekBar rangeSeekBar = findViewById(R.id.rangeBar);
        rangeSeekBar.setOnRangeSeekBarChangeListener(this);
        loadData();

    }

    private void loadData() {
        mGetChartDataTask = new GetChartDataTask(getAssets(), this);
        mGetChartDataTask.execute();
    }

    @Override
    public void onSuccess(ChartData chartData) {
        mChartView.setGraph(chartData.getGraphs().get(0));
        mChartPreview.setGraph(chartData.getGraphs().get(0));
    }

    @Override
    public void onError(Throwable t) {
        Toast.makeText(this, "Error:" + t, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGetChartDataTask != null) mGetChartDataTask.unsubscribe();
    }

    @Override
    public void onRangeChanged(float minValue, float maxValue, boolean fromUser) {
        mChartView.setLeftAndRight(minValue / 100f, maxValue / 100f);
    }
}

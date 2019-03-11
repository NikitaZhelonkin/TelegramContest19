package ru.zhelonkin.tgcontest;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import ru.zhelonkin.tgcontest.model.ChartData;
import ru.zhelonkin.tgcontest.task.GetChartDataTask;
import ru.zhelonkin.tgcontest.widget.ChartView;
import ru.zhelonkin.tgcontest.widget.DoubleSeekBar;

public class MainActivity extends AppCompatActivity implements GetChartDataTask.Callback, DoubleSeekBar.OnSeekBarChangeListener {

    private GetChartDataTask mGetChartDataTask;

    private ChartView mChartView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mChartView = findViewById(R.id.chart_view);
        DoubleSeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);
        loadData();

    }

    private void loadData() {
        mGetChartDataTask = new GetChartDataTask(getAssets(), this);
        mGetChartDataTask.execute();
    }

    @Override
    public void onSuccess(ChartData chartData) {
        mChartView.setGraph(chartData.getGraphs().get(0));
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
    public void onProgressChanged(DoubleSeekBar doubleSeekBar, float leftProgress, float rightProgress) {
        mChartView.setLeftAndRight(leftProgress, rightProgress);
    }

}

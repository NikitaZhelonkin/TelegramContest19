package ru.zhelonkin.tgcontest;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import ru.zhelonkin.tgcontest.model.ChartData;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Line;
import ru.zhelonkin.tgcontest.task.GetChartDataTask;
import ru.zhelonkin.tgcontest.widget.ChartView;
import ru.zhelonkin.tgcontest.widget.DynamicLinearLayout;
import ru.zhelonkin.tgcontest.widget.RangeSeekBar;

public class MainActivity extends AppCompatActivity implements
        GetChartDataTask.Callback,
        RangeSeekBar.OnRangeSeekBarChangeListener,
        MainAdapter.OnCheckChangedListener {

    private GetChartDataTask mGetChartDataTask;

    private ChartView mChartView;
    private ChartView mChartPreview;
    private RangeSeekBar mRangeSeekBar;

    private MainAdapter mMainAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Prefs.isDarkMode(this) ? R.style.AppTheme_Night : R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mChartView = findViewById(R.id.chart_view);
        mChartPreview = findViewById(R.id.chart_preview);
        mRangeSeekBar = findViewById(R.id.rangeBar);
        mRangeSeekBar.setOnRangeSeekBarChangeListener(this);
        DynamicLinearLayout linesLayout = findViewById(R.id.line_list_layout);
        linesLayout.setAdapter(mMainAdapter = new MainAdapter());
        mMainAdapter.setOnCheckChangedListener(this);

        if (savedInstanceState == null) {
            loadData();
        } else {
           mChartView.post(() -> onSuccess(HolderFragment.holderFragmentFor(MainActivity.this).getChartData())) ;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.theme) {
            switchTheme();
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadData() {
        mGetChartDataTask = new GetChartDataTask(getAssets(), this);
        mGetChartDataTask.execute();
    }

    @Override
    public void onSuccess(ChartData chartData) {
        HolderFragment.holderFragmentFor(this).setChartData(chartData);
        Graph graph = chartData.getGraphs().get(0);
        mChartView.setGraph(graph);
        mChartPreview.setGraph(graph);
        mMainAdapter.setGraph(graph);
        invalidateChartRange();
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
    public void onRangeChanged(float leftValue, float rightValue, boolean fromUser) {
        invalidateChartRange();
    }

    @Override
    public void onCheckChanged(Line line, boolean checked) {
        mChartView.updateGraphLines();
        mChartPreview.updateGraphLines();
    }

    private void invalidateChartRange() {
        mChartView.setLeftAndRight(mRangeSeekBar.getLeftValue() / 100f, mRangeSeekBar.getRightValue() / 100f);
    }

    private void switchTheme() {
        Prefs.setDarkMode(this, !Prefs.isDarkMode(this));
        recreate();
    }
}

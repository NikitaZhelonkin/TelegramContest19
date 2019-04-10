package ru.zhelonkin.tgcontest.main;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import ru.zhelonkin.tgcontest.Prefs;
import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.model.ChartData;
import ru.zhelonkin.tgcontest.task.GetChartDataTask;

public class MainActivity extends AppCompatActivity implements
        GetChartDataTask.Callback {

    private GetChartDataTask mGetChartDataTask;

    private MainAdapter mMainAdapter;

    private ChartData mChartData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mMainAdapter = new MainAdapter();
        setTheme(Prefs.isDarkMode(this) ? R.style.AppTheme_Night : R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(mMainAdapter);


        ChartData chartData = (ChartData) getLastCustomNonConfigurationInstance();
        if (chartData != null) {
            onSuccess(chartData);
        } else {
            loadData(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGetChartDataTask != null) mGetChartDataTask.unsubscribe();
    }

    @Nullable
    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return mChartData;
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

    private void loadData(Context context) {
        mGetChartDataTask = new GetChartDataTask(context.getAssets(), this);
        mGetChartDataTask.execute();
    }

    @Override
    public void onSuccess(ChartData chartData) {
        mChartData = chartData;
        mMainAdapter.setChartData(chartData);
    }

    @Override
    public void onError(Throwable t) {
        Toast.makeText(this, "Error:" + t, Toast.LENGTH_SHORT).show();
    }

    private void switchTheme() {
        Prefs.setDarkMode(this, !Prefs.isDarkMode(this));
        recreate();
    }
}

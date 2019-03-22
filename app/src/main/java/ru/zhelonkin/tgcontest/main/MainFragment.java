package ru.zhelonkin.tgcontest.main;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.model.ChartData;
import ru.zhelonkin.tgcontest.task.GetChartDataTask;

public class MainFragment extends Fragment implements
        GetChartDataTask.Callback {

    private GetChartDataTask mGetChartDataTask;

    private MainAdapter mMainAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMainAdapter = new MainAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(view.getContext(), DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(mMainAdapter);

        ChartData chartData = savedInstanceState != null ? HolderFragment.holderFragmentFor(this).getChartData() : null;
        if (chartData != null) {
            onSuccess(chartData);
        } else {
            loadData(view.getContext());
        }
    }

    private void loadData(Context context) {
        mGetChartDataTask = new GetChartDataTask(context.getAssets(), this);
        mGetChartDataTask.execute();
    }

    @Override
    public void onSuccess(ChartData chartData) {
        HolderFragment.holderFragmentFor(this).setChartData(chartData);
        mMainAdapter.setChartData(chartData);
    }

    @Override
    public void onError(Throwable t) {
        Toast.makeText(getContext(), "Error:" + t, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mGetChartDataTask != null) mGetChartDataTask.unsubscribe();
    }
}

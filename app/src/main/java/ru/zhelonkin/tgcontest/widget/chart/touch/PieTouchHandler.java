package ru.zhelonkin.tgcontest.widget.chart.touch;

import android.view.MotionEvent;

import java.util.Collections;

import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.widget.chart.ChartPopupView;
import ru.zhelonkin.tgcontest.widget.chart.ChartView;
import ru.zhelonkin.tgcontest.widget.chart.OnTargetChangeListener;
import ru.zhelonkin.tgcontest.widget.chart.renderer.PieChartRenderer;

public class PieTouchHandler implements TouchHandler {

    private int mTarget = ChartView.INVALID_TARGET;

    private Chart mChart;
    private ChartPopupView mPopupView;
    private PieChartRenderer mPieChartRenderer;


    public PieTouchHandler(ChartView chartView, Chart chart,PieChartRenderer pieChartRenderer) {
        mChart = chart;
        mPopupView = chartView.getChartPopupView();
        mPopupView.hide(false);
        mPieChartRenderer = pieChartRenderer;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                setTarget(mPieChartRenderer.hitItem(event.getX(), event.getY()));
                break;
        }
        return true;
    }

    @Override
    public int getTarget() {
        return mTarget;
    }

    @Override
    public void setTarget(int target) {
        if (target == mTarget) {
            target = ChartView.INVALID_TARGET;
        }
        if (target != mTarget) {
            mTarget = target;
            if (target == ChartView.INVALID_TARGET) {
                //hide popup
                mPopupView.hide(true);
            } else {
                //display popup
                Graph graph = mChart.getGraphs().get(target);
                long value = mPieChartRenderer.getValue(target);
                ChartPopupView.Item item = new ChartPopupView.Item(value, graph.getName(), graph.getColor());
                mPopupView.bindData(Collections.singletonList(item));
                mPopupView.show(true);
                mPopupView.setTranslationX(0);
            }
            mPieChartRenderer.onTargetChanged(target);
        }
    }

}

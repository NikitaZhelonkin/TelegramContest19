package ru.zhelonkin.tgcontest.widget.chart.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;

import java.util.List;

import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Point;
import ru.zhelonkin.tgcontest.widget.chart.ChartView;

public class AreaRenderer extends BaseRenderer {

    private ChartView mView;
    private Chart mChart;
    private List<Graph> mGraphs;
    private Path mGraphPath = new Path();
    private float[] mStackBuffer;
    private Paint mXPaint;

    public AreaRenderer(ChartView view, Chart chart, List<Graph> graphs, Viewport viewport, Paint gridPaint) {
        super(viewport);
        mView = view;
        mChart = chart;
        mGraphs = graphs;
        mXPaint = gridPaint;
        mStackBuffer = new float[chart.getXValues().size()];
    }

    @Override
    public void render(Canvas canvas, int targetPosition) {
        for (int i = 0; i < mStackBuffer.length; i++) {
            mStackBuffer[i] = 0;
        }
        mGraphPath.reset();


        float startX = pointX(mChart.getXValues().get(0));
        float endX = pointX(mChart.getXValues().get(mChart.getXValues().size() - 1));
        int bottom = mView.getHeight() - mView.getPaddingBottom();

        int saveCount = canvas.save();
        canvas.clipRect(Math.max(0, startX), mView.getPaddingTop(), Math.min(mView.getWidth(), endX), bottom);
        for (Graph graph : mGraphs) {
            drawAreaGraph(canvas, graph);
        }
        canvas.restoreToCount(saveCount);

        if (targetPosition != ChartView.INVALID_TARGET)
            drawX(canvas, pointX(mChart.getXValues().get(targetPosition)));
    }

    private void drawAreaGraph(Canvas canvas, Graph graph) {
        int saveCount = canvas.save();
        if(graph.getAlpha()==0) return;
        if (!mGraphPath.isEmpty()) canvas.clipPath( mGraphPath, Region.Op.DIFFERENCE);
        mGraphPath.reset();

        List<Point> points = graph.getPoints();
        float x = 0;
        int bottom = mView.getHeight() - mView.getPaddingBottom();
        float startX = pointX(mChart.getX(graph, 0));
        mGraphPath.moveTo(startX, bottom);
        for (int i = 0; i < points.size(); i++) {
            x = pointX(mChart.getX(graph, i));
            float height = (bottom - pointY(mChart.getY(graph, i))) * graph.getAlpha();
            float endY = mStackBuffer[i] == 0 ? bottom : mStackBuffer[i];
            float startY = endY - height;
            mStackBuffer[i] = mChart.isStacked() ? startY : 0;
            mGraphPath.lineTo(x, startY);
        }
        mGraphPath.lineTo(x, bottom);
        canvas.drawColor(graph.getColor());
        canvas.restoreToCount(saveCount);
    }

    private void drawX(Canvas canvas, float x) {
        canvas.drawLine(x, mView.getPaddingTop(), x, mView.getHeight() - mView.getPaddingBottom(), mXPaint);
    }

}

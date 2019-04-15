package ru.zhelonkin.tgcontest.widget.chart.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;

import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Point;
import ru.zhelonkin.tgcontest.utils.Alpha;
import ru.zhelonkin.tgcontest.widget.chart.ChartView;

public class BarRenderer extends BaseRenderer {

    private ChartView mView;
    private Chart mChart;
    private List<Graph> mGraphs;
    private float[] mStackBuffer;

    private Paint mBarPaint = new Paint();

    public BarRenderer(ChartView view, Chart chart, List<Graph> graphs, Viewport viewport) {
        super(viewport);
        mView = view;
        mChart = chart;
        mGraphs = graphs;
        mStackBuffer = new float[chart.getXValues().size()];
    }

    @Override
    public void render(Canvas canvas, int targetPosition) {
        for (int i = 0; i < mStackBuffer.length; i++) {
            mStackBuffer[i] = 0;
        }
        for (Graph graph : mGraphs) {
            drawBarGraph(canvas, graph, targetPosition);
        }
    }

    private void drawBarGraph(Canvas canvas, Graph graph, int targetPosition) {
        if (graph.getAlpha() == 0) return;
        List<Point> points = graph.getPoints();
        float first = pointX(mChart.getX(graph, 0));
        float last = pointX(mChart.getX(graph, points.size() - 1));
        float barWidth = (last - first) / (points.size());
        float start = first + barWidth / 2;
        mBarPaint.setStyle(Paint.Style.STROKE);
        mBarPaint.setColor(graph.getColor());
        mBarPaint.setStrokeWidth(barWidth);
        float[] lineBuffer = new float[points.size() * 4];
        int j = 0;
        int bottom = mView.getHeight() - mView.getPaddingBottom();
        for (int i = 0; i < points.size(); i++) {
            float startX = start + i * barWidth;
            float height = (bottom - pointY(mChart.getY(graph, i))) * graph.getAlpha();
            float endY = mStackBuffer[i] == 0 ? bottom : mStackBuffer[i];
            float startY = endY - height;
            lineBuffer[j++] = startX;
            lineBuffer[j++] = startY;
            lineBuffer[j++] = startX;
            lineBuffer[j++] = endY;
            mStackBuffer[i] = mChart.isStacked() ? startY : 0;
        }
        mBarPaint.setAlpha(Alpha.toInt(targetPosition == ChartView.INVALID_TARGET ? 1 : 0.8f));
        canvas.drawLines(lineBuffer, mBarPaint);

        if (targetPosition != ChartView.INVALID_TARGET) {
            mBarPaint.setAlpha(Alpha.toInt(1));
            float startX = lineBuffer[targetPosition * 4];
            float startY = lineBuffer[targetPosition * 4 + 1];
            float endX = lineBuffer[targetPosition * 4 + 2];
            float endY = lineBuffer[targetPosition * 4 + 3];
            canvas.drawLine(startX, startY, endX, endY, mBarPaint);
        }
    }

}

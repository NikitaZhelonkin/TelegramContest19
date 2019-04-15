package ru.zhelonkin.tgcontest.widget.chart.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;

import java.util.List;

import ru.zhelonkin.tgcontest.utils.Alpha;
import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Point;
import ru.zhelonkin.tgcontest.widget.chart.ChartView;

public class LineRenderer extends BaseRenderer {

    private Paint mGraphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private ChartView mView;
    private Chart mChart;
    private List<Graph> mGraphs;
    private int mSurfaceColor;
    private Paint mXPaint;

    public LineRenderer(ChartView view, Chart chart, List<Graph> graphs, Viewport viewport, Paint gridPaint,
                        int lineWidth,
                        int surfaceColor) {
        super(view, chart, viewport);
        mView = view;
        mChart = chart;
        mGraphs = graphs;
        mSurfaceColor = surfaceColor;
        mXPaint = gridPaint;

        mGraphPaint.setStrokeWidth(lineWidth);
        mGraphPaint.setStyle(Paint.Style.STROKE);
        mGraphPaint.setStrokeCap(Paint.Cap.SQUARE);
    }

    @Override
    public void render(Canvas canvas) {
        int targetPosition = getTarget();
        if (targetPosition != ChartView.INVALID_TARGET)
            drawX(canvas, pointX(mChart.getXValues().get(targetPosition)));

        for (Graph graph : mGraphs) {
            drawLineGraph(canvas, graph, targetPosition);
        }
    }

    private void drawLineGraph(Canvas canvas, Graph graph, int targetPosition) {
        mGraphPaint.setColor(graph.getColor());
        mGraphPaint.setAlpha(Alpha.toInt(graph.getAlpha()));
        drawLineDefault(canvas, graph);
        if (targetPosition != ChartView.INVALID_TARGET && graph.isVisible())
            drawDot(canvas, graph, targetPosition);
    }

    private void drawLineDefault(Canvas canvas, Graph graph) {
        List<Point> points = graph.getPoints();
        float[] lineBuffer = new float[points.size() * 4];
        float lastX = pointX(mChart.getX(graph, 0));
        float lastY = pointY(mChart.getY(graph, 0));
        int j = 0;
        for (int i = 1; i < points.size(); i++) {
            float x = pointX(mChart.getX(graph, i));
            float y = pointY(mChart.getY(graph, i));
            lineBuffer[j++] = lastX;
            lineBuffer[j++] = lastY;
            lineBuffer[j++] = x;
            lineBuffer[j++] = y;
            lastX = x;
            lastY = y;
        }
        canvas.drawLines(lineBuffer, mGraphPaint);
    }


    private void drawDot(Canvas canvas, Graph graph, int targetPosition) {
        mGraphPaint.setColor(graph.getColor());
        float x = pointX(mChart.getX(graph, targetPosition));
        float y = pointY(mChart.getY(graph, targetPosition));
        int color = mGraphPaint.getColor();
        mGraphPaint.setColor(mSurfaceColor);
        mGraphPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(x, y, mGraphPaint.getStrokeWidth() * 2, mGraphPaint);
        mGraphPaint.setColor(color);
        mGraphPaint.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(x, y, mGraphPaint.getStrokeWidth() * 2, mGraphPaint);
    }

    private void drawX(Canvas canvas, float x) {
        canvas.drawLine(x, mView.getPaddingTop(), x, mView.getHeight() - mView.getPaddingBottom(), mXPaint);
    }

}

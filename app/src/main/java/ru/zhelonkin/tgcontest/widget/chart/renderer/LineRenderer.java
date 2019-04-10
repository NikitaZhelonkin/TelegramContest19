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
    private int mSurfaceColor;
    private Path mGraphPath = new Path();
    private Paint mXPaint;

    public LineRenderer(ChartView view, Chart chart,  Viewport viewport, Paint gridPaint,
                        int lineWidth,
                        int surfaceColor) {
        super(viewport);
        mView = view;
        mChart = chart;
        mSurfaceColor = surfaceColor;
        mXPaint = gridPaint;

        mGraphPaint.setStrokeWidth(lineWidth);
        mGraphPaint.setStyle(Paint.Style.STROKE);
        mGraphPaint.setStrokeCap(Paint.Cap.SQUARE);
        mGraphPaint.setStrokeJoin(Paint.Join.BEVEL);
    }

    @Override
    public void render(Canvas canvas, int targetPosition) {
        if (targetPosition != ChartView.INVALID_TARGET)
            drawX(canvas, pointX(mChart.getXValues().get(targetPosition)));

        for (Graph graph : mChart.getGraphs()) {
            drawLineGraph(canvas, graph, targetPosition);
        }
    }

    private void drawLineGraph(Canvas canvas, Graph graph, int targetPosition) {
        mGraphPaint.setColor(graph.getColor());
        mGraphPaint.setAlpha(Alpha.toInt(graph.getAlpha()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            drawLineWithPath(canvas, graph);
        } else {
            drawLineDefault(canvas, graph);
        }
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

    private void drawLineWithPath(Canvas canvas, Graph graph) {
        mGraphPath.reset();
        List<Point> points = graph.getPoints();
        mGraphPath.moveTo(pointX(mChart.getX(graph, 0)), pointY(mChart.getY(graph, 0)));
        for (int i = 1; i < points.size(); i++) {
            mGraphPath.lineTo(pointX(mChart.getX(graph, i)), pointY(mChart.getY(graph, i)));
        }
        canvas.drawPath(mGraphPath, mGraphPaint);
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

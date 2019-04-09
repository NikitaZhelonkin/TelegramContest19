package ru.zhelonkin.tgcontest.widget.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.List;

import ru.zhelonkin.tgcontest.Alpha;
import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Point;
import ru.zhelonkin.tgcontest.widget.ChartView;
import ru.zhelonkin.tgcontest.widget.PointTransformer;

public class BarRenderer implements Renderer {

    private ChartView mView;
    private Chart mChart;
    private PointTransformer mPointTransformer;
    private float[] mStackBuffer;

    private Paint mBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarRenderer(ChartView view, Chart chart, PointTransformer transformer) {
        mView = view;
        mChart = chart;
        mPointTransformer = transformer;
        mStackBuffer = new float[chart.getXValues().size()];
    }

    @Override
    public void render(Canvas canvas, int targetPosition) {
        for (int i = 0; i < mStackBuffer.length; i++) {
            mStackBuffer[i] = 0;
        }
        for (Graph graph : mChart.getGraphs()) {
            drawBarGraph(canvas, graph, targetPosition);
        }
    }

    private void drawBarGraph(Canvas canvas, Graph graph, int targetPosition) {
        if (graph.getAlpha() == 0) return;
        List<Point> points = graph.getPoints();
        float first = pointX(mChart.getX(graph, 0));
        float last = pointX(mChart.getX(graph, points.size() - 1));
        float width = (last - first) / points.size() + 1f;
        mBarPaint.setStyle(Paint.Style.STROKE);
        mBarPaint.setColor(graph.getColor());
        mBarPaint.setStrokeWidth(width);
        float[] lineBuffer = new float[points.size() * 4];
        int j = 0;
        int bottom = mView.getHeight() - mView.getPaddingBottom();
        for (int i = 0; i < points.size(); i++) {
            float startX = pointX(mChart.getX(graph, i));
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


    protected float pointX(long x) {
        return mPointTransformer.pointX(x);
    }

    protected float pointY(float y) {
        return mPointTransformer.pointY(y);
    }
}

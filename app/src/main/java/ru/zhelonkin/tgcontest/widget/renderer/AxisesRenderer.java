package ru.zhelonkin.tgcontest.widget.renderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;

import ru.zhelonkin.tgcontest.Alpha;
import ru.zhelonkin.tgcontest.formatter.CachingFormatter;
import ru.zhelonkin.tgcontest.formatter.DateFormatter;
import ru.zhelonkin.tgcontest.formatter.Formatter;
import ru.zhelonkin.tgcontest.formatter.NumberFormatter;
import ru.zhelonkin.tgcontest.widget.ChartView;
import ru.zhelonkin.tgcontest.widget.PointTransformer;

public class AxisesRenderer implements Renderer {

    private final Formatter DATE_FORMATTER = new CachingFormatter(new DateFormatter("MMM dd"));
    private final Formatter NUMBER_FORMATTER = new CachingFormatter(new NumberFormatter());

    private TextPaint mTextPaint;

    private Paint mGridPaint;

    private ChartView mView;

    private ChartView.Axises mAxises;

    private PointTransformer mPointTransformer;

    private int mTextPadding;

    public AxisesRenderer(ChartView view,
                          PointTransformer pointTransformer,
                          ChartView.Axises axises,
                          Paint gridPaint,
                          TextPaint textPaint,
                          int textPadding) {
        mView = view;
        mPointTransformer = pointTransformer;
        mAxises = axises;

        mTextPadding = textPadding;
        mGridPaint = gridPaint;
        mTextPaint = textPaint;
    }

    @Override
    public void render(Canvas canvas, int targetPosition) {
        drawXAxis(canvas);
        drawYAxis(canvas);
    }

    private void drawYAxis(Canvas canvas) {
        int count = canvas.save();
        canvas.clipRect(mView.getPaddingLeft(), 0, mView.getWidth() - mView.getPaddingRight(), mView.getHeight() - mView.getPaddingBottom());
        for (Long index : mAxises.mYValues.keySet()) {
            ChartView.Axises.Value v = mAxises.mYValues.get(index);
            if (v != null && v.getAlpha() != 0) {
                float y = pointY(v.value);
                mGridPaint.setAlpha(Alpha.toInt(v.getAlpha() * 0.1f));
                canvas.drawLine(mView.getPaddingLeft(), y, mView.getWidth() - mView.getPaddingRight(), y, mGridPaint);
                mTextPaint.setAlpha(Alpha.toInt(v.getAlpha() * 0.5f));
                canvas.drawText(NUMBER_FORMATTER.format(v.value), mView.getPaddingLeft(), y - mTextPadding, mTextPaint);
            }
            mGridPaint.setAlpha(Alpha.toInt(0.1f));
        }
        canvas.restoreToCount(count);
    }


    private void drawXAxis(Canvas canvas) {
        for (Long index : mAxises.mXValues.keySet()) {
            ChartView.Axises.Value v = mAxises.mXValues.get(index);
            if (v != null && v.getAlpha() != 0) {
                float x = pointX(v.value);
                mTextPaint.setAlpha(Alpha.toInt(v.getAlpha() * 0.5f));
                canvas.drawText(DATE_FORMATTER.format(v.value), x, mView.getHeight() - mTextPaint.descent(), mTextPaint);
            }
        }
    }

    protected float pointX(long x) {
        return mPointTransformer.pointX(x);
    }

    protected float pointY(float y) {
        return mPointTransformer.pointY(y);
    }
}

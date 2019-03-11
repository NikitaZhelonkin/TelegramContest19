package ru.zhelonkin.tgcontest.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import ru.zhelonkin.tgcontest.R;

public class DoubleSeekBar extends FrameLayout implements SeekBar.OnSeekBarChangeListener {

    public interface OnSeekBarChangeListener {
        void onProgressChanged(DoubleSeekBar doubleSeekBar, float leftProgress, float rightProgress);
    }

    private static final int MIN_OFFSET = 200;

    private SeekBar left;
    private SeekBar right;

    private OnSeekBarChangeListener mOnSeekBarChangeListener;


    public DoubleSeekBar(@NonNull Context context) {
        super(context);
        init(context);
    }

    public DoubleSeekBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DoubleSeekBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    public void setOnSeekBarChangeListener(OnSeekBarChangeListener onSeekBarChangeListener) {
        mOnSeekBarChangeListener = onSeekBarChangeListener;
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.double_seek_bar, this);
        left = findViewById(R.id.seekBarLeft);
        right = findViewById(R.id.seekBarRight);
        left.setMax(1000);
        right.setMax(1000);
        right.setProgress(right.getMax());
        left.setOnSeekBarChangeListener(this);
        right.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.seekBarLeft:
                if (right.getProgress() - left.getProgress() < MIN_OFFSET) {
                    right.setProgress(left.getProgress() + MIN_OFFSET);
                    if (right.getProgress() == right.getMax()) {
                        left.setProgress(right.getMax() - MIN_OFFSET);
                    }
                }
                notifyProgressChanged();
                break;
            case R.id.seekBarRight:
                if (right.getProgress() - left.getProgress() < MIN_OFFSET) {
                    left.setProgress(right.getProgress() - MIN_OFFSET);
                    if (left.getProgress() == 0) {
                        right.setProgress(MIN_OFFSET);
                    }
                }
                notifyProgressChanged();
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void notifyProgressChanged() {
        if (mOnSeekBarChangeListener != null) {
            mOnSeekBarChangeListener.onProgressChanged(
                    this,
                    left.getProgress() / (float) left.getMax(),
                    right.getProgress() / (float) right.getMax());
        }
    }
}

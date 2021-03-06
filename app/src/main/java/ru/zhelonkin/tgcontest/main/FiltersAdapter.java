package ru.zhelonkin.tgcontest.main;

import android.animation.ObjectAnimator;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.support.annotation.NonNull;
import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.widget.Checkbox;
import ru.zhelonkin.tgcontest.widget.DynamicViewDelegate;
import ru.zhelonkin.tgcontest.widget.ShakeAnimator;

public class FiltersAdapter extends DynamicViewDelegate.Adapter<FiltersAdapter.ViewHolder> {

    static final String PAYLOAD_CHECKBOX = "checkbox";

    public interface Callback {
        void onCheckChanged(Graph graph, boolean checked);
        void onLongClick(Graph graph);
    }


    private Chart mChart;

    private Callback mCallback;

    public void setGraphs(Chart chart) {
        mChart = chart;
        notifyDataChanged();
    }

    @Override
    public int getCount() {
        return mChart == null || mChart.getGraphs().size() == 1 ? 0 : mChart.getGraphs().size();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    protected ViewHolder onCreateViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(R.layout.list_item_filter, parent, false));
    }

    @Override
    protected void onBindViewHolder(ViewHolder viewHolder, int position, Object payload) {
        viewHolder.bind(mChart.getGraphs().get(position), payload);
    }

    class ViewHolder extends DynamicViewDelegate.ViewHolder {

        Checkbox mCheckBox;

        int mShakeOffset;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mCheckBox = itemView.findViewById(R.id.checkbox);
            mShakeOffset = itemView.getResources().getDimensionPixelSize(R.dimen.shake_offset);
        }

        void bind(Graph graph, Object payload) {
            if(PAYLOAD_CHECKBOX.equals(payload)){
                mCheckBox.setChecked(graph.isVisible());
                return;
            }
            mCheckBox.setText(graph.getName());
            ColorStateList colorStateList = new ColorStateList(new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{-android.R.attr.state_checked}
            }, new int[]{Color.WHITE, graph.getColor()});
            mCheckBox.getBackground().setColorFilter(graph.getColor(), PorterDuff.Mode.SRC_IN);
            mCheckBox.setTextColor(colorStateList);
            mCheckBox.setOnCheckedChangeListener(null);
            mCheckBox.setChecked(graph.isVisible());
            mCheckBox.jumpDrawablesToCurrentState();
            mCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (mCallback != null) {
                    mCallback.onCheckChanged(graph, isChecked);
                }

            });
            itemView.setOnClickListener(v -> {
                if (mChart.getVisibleGraphs().size() < 2 && mCheckBox.isChecked()) {
                    ShakeAnimator.ofView(mCheckBox, mShakeOffset).start();
                }else {
                    mCheckBox.toggle();
                }
            });
            itemView.setOnLongClickListener(v -> {
                if (mCallback != null) {
                    mCallback.onLongClick(graph);
                    return true;
                }
                return false;
            });
        }
    }


}

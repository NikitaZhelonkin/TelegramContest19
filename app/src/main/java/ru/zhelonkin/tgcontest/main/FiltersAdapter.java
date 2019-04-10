package ru.zhelonkin.tgcontest.main;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.model.Chart;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.widget.Checkbox;
import ru.zhelonkin.tgcontest.widget.DynamicViewDelegate;

public class FiltersAdapter extends DynamicViewDelegate.Adapter<FiltersAdapter.ViewHolder> {

    public interface OnCheckChangedListener {
        void onCheckChanged(Graph graph, boolean checked);
    }

    private List<Graph> mGraphs;

    private OnCheckChangedListener mOnCheckChangedListener;

    public void setGraphs(List<Graph> graphs) {
        mGraphs = graphs;
        notifyDataChanged();
    }

    @Override
    public int getCount() {
        return mGraphs == null ? 0 : mGraphs.size();
    }

    public void setOnCheckChangedListener(OnCheckChangedListener onCheckChangedListener) {
        mOnCheckChangedListener = onCheckChangedListener;
    }

    @Override
    protected ViewHolder onCreateViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(R.layout.list_item_filter, parent, false));
    }

    @Override
    protected void onBindViewHolder(ViewHolder viewHolder, int position, Object payload) {
        viewHolder.bind(mGraphs.get(position));
    }

    class ViewHolder extends DynamicViewDelegate.ViewHolder {

        Checkbox mCheckBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mCheckBox = itemView.findViewById(R.id.checkbox);
        }

        void bind(Graph graph) {
            mCheckBox.setText(graph.getName());
            ColorStateList colorStateList = new ColorStateList(new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{-android.R.attr.state_checked}
            }, new int[]{Color.WHITE, graph.getColor()});
            ViewCompat.setBackgroundTintList(mCheckBox, ColorStateList.valueOf(graph.getColor()));
            mCheckBox.setTextColor(colorStateList);
            mCheckBox.setOnCheckedChangeListener(null);
            mCheckBox.setChecked(graph.isVisible());
            mCheckBox.jumpDrawablesToCurrentState();
            mCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (mOnCheckChangedListener != null) {
                    mOnCheckChangedListener.onCheckChanged(graph, isChecked);
                }
            });
            itemView.setOnClickListener(v -> mCheckBox.setChecked(!mCheckBox.isChecked()));
        }
    }


}

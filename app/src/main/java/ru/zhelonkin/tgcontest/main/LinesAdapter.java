package ru.zhelonkin.tgcontest.main;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Line;
import ru.zhelonkin.tgcontest.widget.Checkbox;
import ru.zhelonkin.tgcontest.widget.DynamicViewDelegate;

public class LinesAdapter extends DynamicViewDelegate.Adapter<LinesAdapter.ViewHolder> {

    public interface OnCheckChangedListener {
        void onCheckChanged(Line line, boolean checked);
    }

    private Graph mGraph;

    private OnCheckChangedListener mOnCheckChangedListener;

    public void setGraph(Graph graph) {
        mGraph = graph;
        notifyDataChanged();
    }

    @Override
    public int getCount() {
        return mGraph == null ? 0 : mGraph.getLines().size();
    }

    public void setOnCheckChangedListener(OnCheckChangedListener onCheckChangedListener) {
        mOnCheckChangedListener = onCheckChangedListener;
    }

    @Override
    protected ViewHolder onCreateViewHolder(ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(R.layout.list_item_line, parent, false));
    }

    @Override
    protected void onBindViewHolder(ViewHolder viewHolder, int position, Object payload) {
        viewHolder.bind(mGraph.getLines().get(position));
    }

    class ViewHolder extends DynamicViewDelegate.ViewHolder {

        Checkbox mCheckBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mCheckBox = itemView.findViewById(R.id.checkbox);
        }

        void bind(Line line) {
            mCheckBox.setText("Label " + line.getName());
            ColorStateList colorStateList = new ColorStateList(new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{-android.R.attr.state_checked}
            }, new int[]{Color.WHITE, line.getColor()});
            ViewCompat.setBackgroundTintList(mCheckBox, ColorStateList.valueOf(line.getColor()));
            mCheckBox.setTextColor(colorStateList);
            mCheckBox.setOnCheckedChangeListener(null);
            mCheckBox.setChecked(line.isVisible());
            mCheckBox.jumpDrawablesToCurrentState();
            mCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                line.setVisible(isChecked);
                if (mOnCheckChangedListener != null) {
                    mOnCheckChangedListener.onCheckChanged(line, isChecked);
                }
            });
            itemView.setOnClickListener(v -> mCheckBox.setChecked(!mCheckBox.isChecked()));
        }
    }


}

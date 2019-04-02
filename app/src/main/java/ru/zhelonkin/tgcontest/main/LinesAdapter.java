package ru.zhelonkin.tgcontest.main;

import android.content.res.ColorStateList;
import androidx.annotation.NonNull;
import androidx.core.widget.CompoundButtonCompat;
import androidx.appcompat.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ru.zhelonkin.tgcontest.R;
import ru.zhelonkin.tgcontest.model.Graph;
import ru.zhelonkin.tgcontest.model.Line;
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

        AppCompatCheckBox mCheckBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mCheckBox = itemView.findViewById(R.id.checkbox);
        }

        void bind(Line line) {
            mCheckBox.setText(line.getName());
            CompoundButtonCompat.setButtonTintList(mCheckBox, ColorStateList.valueOf(line.getColor()));
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

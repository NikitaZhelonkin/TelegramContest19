package ru.zhelonkin.tgcontest.widget;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public abstract class HolderAdapter<VH extends HolderAdapter.ViewHolder> extends BaseAdapter {

    @Override
    public final View getView(int position, View convertView, ViewGroup parent) {
        VH holder;
        if(convertView==null){
            holder = onCreateViewHolder(parent, getItemViewType(position));
            convertView = holder.itemView;
            convertView.setTag(holder);
        }else {
            holder = (VH) convertView.getTag();
        }
        holder.mAdapterPosition = position;
        onBindViewHolder(holder, position);
        return convertView;
    }

   protected abstract VH onCreateViewHolder(ViewGroup parent, int type);

    protected abstract void onBindViewHolder(VH viewHolder, int position);

    public static class ViewHolder{

        protected View itemView;
        protected int mAdapterPosition;

        public ViewHolder(View v){
            itemView = v;
        }

        public int getAdapterPosition() {
            return mAdapterPosition;
        }
    }
}

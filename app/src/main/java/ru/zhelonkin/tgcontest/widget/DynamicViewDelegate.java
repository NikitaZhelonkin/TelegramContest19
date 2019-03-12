package ru.zhelonkin.tgcontest.widget;


import android.database.Observable;
import android.view.View;
import android.view.ViewGroup;

public class DynamicViewDelegate {

    private ViewGroup mViewGroup;

    private Adapter mAdapter;

    public DynamicViewDelegate(ViewGroup viewGroup) {
        mViewGroup = viewGroup;
    }

    private AdapterDataObserver mAdapterDataObserver = new AdapterDataObserver() {
        @Override
        public void onChanged(Object payload) {
            populateViews(payload);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onItemChanged(int position, int count, Object payload) {
            int diff = getAdapter().getCount() - mViewGroup.getChildCount();
            for (int i = position; i < position + count; i++) {
                View v = mViewGroup.getChildAt(i);
                if (v == null) continue;
                ViewHolder holder = (ViewHolder) v.getTag();
                holder.adapterPosition = i + diff;
                mAdapter.onBindViewHolder(holder, holder.adapterPosition, payload);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onItemInserted(int position, int count) {
            for (int i = 0; i < count; i++) {
                ViewHolder holder = mAdapter.onCreateViewHolder(mViewGroup);
                holder.itemView.setTag(holder);
                holder.adapterPosition = position + i;
                mViewGroup.addView(holder.itemView, holder.adapterPosition);
                mAdapter.onBindViewHolder(holder, holder.adapterPosition, null);
            }
            updateHolderPosition();
        }

        @Override
        public void onItemRemoved(int position, int count) {
            for (int i = 0; i < count; i++) {
                mViewGroup.removeViewAt(position);
            }
            updateHolderPosition();
        }

        @Override
        public void onItemMoved(int fromPosition, int toPosition) {
            View v = mViewGroup.getChildAt(fromPosition);
            mViewGroup.removeViewAt(fromPosition);
            mViewGroup.addView(v, toPosition);
            updateHolderPosition();

        }
    };

    public void setAdapter(Adapter adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mAdapterDataObserver);
        }
        mAdapter = adapter;
        mAdapter.registerDataSetObserver(mAdapterDataObserver);
        mAdapter = adapter;
        populateViews(null);
    }

    public Adapter getAdapter() {
        return mAdapter;
    }

    @SuppressWarnings("unchecked")
    private void populateViews(Object payload) {
        int itemsCount = mAdapter.getCount();
        int viewsCount = mViewGroup.getChildCount();
        int diff = viewsCount - itemsCount;
        for (int i = 0; i < Math.abs(diff); i++) {
            if (diff > 0) {
                mViewGroup.removeViewAt(0);
            } else {
                ViewHolder holder = mAdapter.onCreateViewHolder(mViewGroup);
                holder.itemView.setTag(holder);
                mViewGroup.addView(holder.itemView);
            }
        }
        for (int i = 0; i < itemsCount; i++) {
            View v = mViewGroup.getChildAt(i);
            if (v == null) continue;
            if (i >= getAdapter().getCount()) continue;
            ViewHolder holder = (ViewHolder) v.getTag();
            holder.adapterPosition = i;
            mAdapter.onBindViewHolder(holder, holder.adapterPosition, payload);
        }
    }

    private void updateHolderPosition() {
        for (int i = 0; i < mViewGroup.getChildCount(); i++) {
            ViewHolder holder = (ViewHolder) mViewGroup.getChildAt(i).getTag();
            holder.adapterPosition = i;
        }
    }

    public static abstract class Adapter<VH extends ViewHolder> {

        AdapterDataObservable mObservable = new AdapterDataObservable();

        public abstract int getCount();

        protected abstract VH onCreateViewHolder(ViewGroup parent);

        protected abstract void onBindViewHolder(VH viewHolder, int position, Object payload);

        private void registerDataSetObserver(AdapterDataObserver observer) {
            mObservable.registerObserver(observer);
        }

        private void unregisterDataSetObserver(AdapterDataObserver observer) {
            mObservable.unregisterObserver(observer);
        }

        public void notifyDataChanged() {
            notifyDataChanged(null);
        }

        public void notifyDataChanged(Object payload) {
            mObservable.notifyChanged(payload);
        }

        public void onInserted(int position, int count) {
            mObservable.notifyItemInserted(position, count);
        }

        public void onChanged(int position, int count, Object payload) {
            mObservable.notifyItemChanged(position, count, payload);
        }

        public void onRemoved(int position, int count) {
            mObservable.notifyItemRemoved(position, count);
        }

        public void onMoved(int fromPosition, int toPosition) {
            mObservable.notifyItemMoved(fromPosition, toPosition);
        }
    }

    static abstract class AdapterDataObserver {
        public abstract void onChanged(Object payload);

        public abstract void onItemChanged(int position, int count, Object payload);

        public abstract void onItemInserted(int position, int count);

        public abstract void onItemRemoved(int position, int count);

        public abstract void onItemMoved(int fromPosition, int toPosition);
    }

    static class AdapterDataObservable extends Observable<AdapterDataObserver> {

        public void notifyChanged(Object payload) {
            for (AdapterDataObserver a : mObservers) {
                a.onChanged(payload);
            }
        }

        public void notifyItemChanged(int position, int count, Object payload) {
            for (AdapterDataObserver a : mObservers) {
                a.onItemChanged(position, count, payload);
            }
        }

        public void notifyItemInserted(int position, int count) {
            for (AdapterDataObserver a : mObservers) {
                a.onItemInserted(position, count);
            }
        }

        public void notifyItemRemoved(int position, int count) {
            for (AdapterDataObserver a : mObservers) {
                a.onItemRemoved(position, count);
            }
        }

        public void notifyItemMoved(int fromPosition, int toPosition) {
            for (AdapterDataObserver a : mObservers) {
                a.onItemMoved(fromPosition, toPosition);
            }
        }
    }

    public static class ViewHolder {

        public final View itemView;

        int adapterPosition = -1;

        public ViewHolder(View itemView) {
            this.itemView = itemView;
        }

        public int getAdapterPosition() {
            return adapterPosition;
        }
    }
}



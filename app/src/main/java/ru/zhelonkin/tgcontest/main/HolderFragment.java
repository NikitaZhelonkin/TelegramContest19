package ru.zhelonkin.tgcontest.main;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import ru.zhelonkin.tgcontest.model.ChartData;

public class HolderFragment extends Fragment {

    private static final String HOLDER_TAG = HolderFragment.class.getSimpleName();

    public HolderFragment() {
        setRetainInstance(true);
    }

    private ChartData mChartData;

    public void setChartData(ChartData chartData) {
        mChartData = chartData;
    }

    public ChartData getChartData() {
        return mChartData;
    }

    public static HolderFragment holderFragmentFor(FragmentActivity activity) {
        return holderFragmentFor(activity.getSupportFragmentManager());
    }

    public static HolderFragment holderFragmentFor(Fragment parentFragment) {
        return holderFragmentFor(parentFragment.getChildFragmentManager());
    }

    private static HolderFragment holderFragmentFor(FragmentManager fm) {
        HolderFragment holder = findHolderFragment(fm);
        if (holder != null) {
            return holder;
        }
        holder = createHolderFragment(fm);
        return holder;
    }

    private static HolderFragment createHolderFragment(FragmentManager fragmentManager) {
        HolderFragment holder = new HolderFragment();
        fragmentManager.beginTransaction().add(holder, HOLDER_TAG).commitAllowingStateLoss();
        return holder;
    }

    private static HolderFragment findHolderFragment(FragmentManager manager) {
        if (manager.isDestroyed()) {
            throw new IllegalStateException("Can't access data from onDestroy");
        }

        Fragment fragmentByTag = manager.findFragmentByTag(HOLDER_TAG);
        if (fragmentByTag != null && !(fragmentByTag instanceof HolderFragment)) {
            throw new IllegalStateException("Unexpected "
                    + "fragment instance was returned by HOLDER_TAG");
        }
        return (HolderFragment) fragmentByTag;
    }
}

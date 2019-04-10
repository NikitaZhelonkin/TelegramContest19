package ru.zhelonkin.tgcontest.widget.chart;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import ru.zhelonkin.tgcontest.FastOutSlowInInterpolator;
import ru.zhelonkin.tgcontest.model.Graph;

public class GraphAnimator {

    private AnimatorSet mGraphAlphaAnimator;

    private View mView;

    public GraphAnimator(View v){
        mView = v;
    }


    public void animateVisibility(List<Graph> graphs){
        if (mGraphAlphaAnimator != null) mGraphAlphaAnimator.cancel();
        List<Animator> animatorList = new ArrayList<>();
        for (Graph graph : graphs) {
            if (graph.isVisible() && graph.getAlpha() != 1) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(graph, "alpha", 1);
                animator.addUpdateListener(animation -> mView.invalidate());
                animatorList.add(animator);
            } else if (!graph.isVisible() && graph.getAlpha() != 0) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(graph, "alpha", 0);
                animator.addUpdateListener(animation -> mView.invalidate());
                animatorList.add(animator);
            }
        }
        mGraphAlphaAnimator = new AnimatorSet();
        mGraphAlphaAnimator.playTogether(animatorList);
        mGraphAlphaAnimator.setInterpolator(new FastOutSlowInInterpolator());
        mGraphAlphaAnimator.setDuration(250);
        mGraphAlphaAnimator.start();
    }
}

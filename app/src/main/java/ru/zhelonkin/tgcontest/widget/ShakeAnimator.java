package ru.zhelonkin.tgcontest.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.view.View;

public class ShakeAnimator {

    public static Animator ofView(View v, int offset){
        ObjectAnimator animator =  ObjectAnimator.ofFloat(v,"translationX" ,0,offset,0,-offset,0, offset,0, -offset,0, offset, 0);
        animator.setDuration(800);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        return animator;
    }

}

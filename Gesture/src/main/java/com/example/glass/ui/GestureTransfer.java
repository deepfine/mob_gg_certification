package com.example.glass.ui;

import android.view.MotionEvent;

/**
 * @author yc.park (DEEP.FINE)
 * @version 1.0.0
 * @Description Class설명
 * @since 2021-02-05
 */
public interface GestureTransfer {
    void onGesture(GlassGestureDetector.Gesture gesture);
    void onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY);
}

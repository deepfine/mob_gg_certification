package go.glass.realwear.hyundai.test.util;

import android.view.KeyEvent;

import com.example.glass.ui.GlassGestureDetector;

public class Common {
    public static GlassGestureDetector.Gesture getKeyEvent(int piKeyCode) {
        switch (piKeyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
                return GlassGestureDetector.Gesture.TAP;
            case KeyEvent.KEYCODE_DPAD_UP:
                return GlassGestureDetector.Gesture.SWIPE_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return GlassGestureDetector.Gesture.SWIPE_DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return GlassGestureDetector.Gesture.SWIPE_BACKWARD;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return GlassGestureDetector.Gesture.SWIPE_FORWARD;
            case KeyEvent.KEYCODE_DEL:
                return GlassGestureDetector.Gesture.TWO_FINGER_SWIPE_BACKWARD;
            case KeyEvent.KEYCODE_FORWARD_DEL:
                return GlassGestureDetector.Gesture.TWO_FINGER_SWIPE_FORWARD;
            case KeyEvent.KEYCODE_VOLUME_UP:
                return GlassGestureDetector.Gesture.TWO_FINGER_SWIPE_UP;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return GlassGestureDetector.Gesture.TWO_FINGER_SWIPE_DOWN;
            default: return null;
        }
    }
}

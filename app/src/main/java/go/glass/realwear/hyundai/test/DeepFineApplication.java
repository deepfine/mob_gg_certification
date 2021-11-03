package go.glass.realwear.hyundai.test;

import android.app.Application;
import android.content.Context;

public class DeepFineApplication extends Application {
    private static DeepFineApplication INSTANCE;
    public static synchronized DeepFineApplication getInstance(){
        return INSTANCE;
    }

    public static String SOCKET_URL = "https://52.231.52.17:555";
    public static String TURN_SERVER_URL = "turn:52.231.71.160:3478";
    public static String TURN_USER_NAME = "deepfine";
    public static String TURN_USER_PASS = "ckddjq0323";
    public static String STUN_SERVER_1 = "stun:stun.services.mozilla.com";
    public static String STUN_SERVER_2 = "stun:stun.l.google.com:19302";
    public static int VIDEO_WIDTH = 1280;
    public static int VIDEO_HEIGHT = 720;
    public static int VIDEO_FPS = 15;
    public static String VIDEO_CODEC = "VP8";

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
    }

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }
}

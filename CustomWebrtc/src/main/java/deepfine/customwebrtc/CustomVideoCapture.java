package deepfine.customwebrtc;

import android.content.Context;

import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;

/**
 * @author hj.kim (DEEP.FINE)
 * @version 1.0.0
 * @Description Class설명
 * @since 2020-12-22
 */
public interface
CustomVideoCapture {
    void initialize(SurfaceTextureHelper var1, Context var2, CapturerObserver var3);
    void startCapture(int var1, int var2, int var3);
    void stopCapture() throws InterruptedException;
    void changeCaptureFormat(int var1, int var2, int var3);
    void dispose();
    boolean isScreencast();
    float setZoom(double d2, boolean z);
    boolean toggleFlash(boolean z);
}

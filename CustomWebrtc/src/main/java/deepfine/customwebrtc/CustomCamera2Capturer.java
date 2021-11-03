package deepfine.customwebrtc;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.camera2.CameraManager;

import androidx.annotation.Nullable;

import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

/**
 * @author hj.kim (DEEP.FINE)
 * @version 1.0.0
 * @Description Class설명
 * @since 2020-12-22
 */
@TargetApi(21)
public class CustomCamera2Capturer extends CustomCameraCapturer implements VideoSink {
    private CustomCamera2Session currentZoomableCamera2Session;
    private boolean isFlashEnabled = false;

    @Nullable
    private final CameraManager cameraManager;
    private CustomCamera2Session.CameraStatusChangeListener cameraStatusChangeListener;

    @SuppressLint("WrongConstant")
    public CustomCamera2Capturer(Context context, String cameraName, CameraEventsHandler eventsHandler, CustomCamera2Session.CameraStatusChangeListener cameraStatusChangeListener2) {
        super(cameraName, eventsHandler, new CustomCamera2Enumerator(context, cameraStatusChangeListener2));
        this.cameraStatusChangeListener = cameraStatusChangeListener2;
        this.cameraManager = (CameraManager)context.getSystemService("camera");
    }

    protected void createCameraSession(CustomCameraSession.CreateSessionCallback createSessionCallback, CustomCameraSession.Events events, Context applicationContext, SurfaceTextureHelper surfaceTextureHelper, String cameraName, int width, int height, int framerate) {
        this.currentZoomableCamera2Session = CustomCamera2Session.create(createSessionCallback, events, applicationContext, this.cameraManager, surfaceTextureHelper, cameraName, width, height, framerate, this.cameraStatusChangeListener);
    }

    public synchronized void startCapture(int i, int i2, int i3) {
        super.startCapture(i, i2, i3);
    }

    public synchronized void stopCapture() {
        super.stopCapture();
    }

    @SuppressLint("WrongConstant")
    public synchronized void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context, CapturerObserver capturerObserver2) {
        super.initialize(surfaceTextureHelper, context, capturerObserver2);
        if (capturerObserver2 != null) {
            if (surfaceTextureHelper != null) {
                //this.mediaProjectionManager = (MediaProjectionManager) context.getSystemService("media_projection");
            } else {
                throw new RuntimeException("surfaceTextureHelper not set.");
            }
        } else {
            throw new RuntimeException("capturerObserver not set.");
        }
    }

    public void switchCamera(CameraSwitchHandler cameraSwitchHandler) {
        super.switchCamera(cameraSwitchHandler);
    }

    public float setZoom(double d2, boolean z) {
        CustomCamera2Session zoomableCamera2Session = this.currentZoomableCamera2Session;
        if (zoomableCamera2Session != null) {
            return zoomableCamera2Session.setZoom(d2, z);
        }
        return 1.0f;
    }

    public void setZoomWithDoubleTap() {
        CustomCamera2Session zoomableCamera2Session = this.currentZoomableCamera2Session;
        if (zoomableCamera2Session != null) {
            zoomableCamera2Session.setZoomWithDoubleTap();
        }
    }

    public void setZoomWithPinching(float f2) {
        CustomCamera2Session zoomableCamera2Session = this.currentZoomableCamera2Session;
        if (zoomableCamera2Session != null) {
            zoomableCamera2Session.setZoomWithPinching(f2);
        }
    }

    public boolean toggleFlash(boolean z) {
        CustomCamera2Session zoomableCamera2Session = this.currentZoomableCamera2Session;
        boolean z2 = zoomableCamera2Session != null ? zoomableCamera2Session.toggleFlash(z) : false;
        this.isFlashEnabled = z2;
        return z2;
    }

    @Override
    public void onFrame(VideoFrame videoFrame) {
    }
}


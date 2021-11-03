package deepfine.customwebrtc;

import android.content.Context;
import android.hardware.Camera;

import org.webrtc.SurfaceTextureHelper;

import lombok.Getter;

/**
 * @author hj.kim (DEEP.FINE)
 * @version 1.0.0
 * @Description Class설명
 * @since 2020-12-22
 */
public class CustomCamera1Capturer extends CustomCameraCapturer {
    private final boolean captureToTexture;
    @Getter
    public Camera o_camera;
    private Camera.Parameters o_params;
    private int zoom_level = 1;
    private final int zoom_level_interval = 5;
    private int default_zoom = 0;

    public interface CallbackListner {
        void callback(boolean pbFlag);
    }

    public CustomCamera1Capturer(String cameraName, CustomCameraVideoCapturer.CameraEventsHandler eventsHandler, boolean captureToTexture) {
        super(cameraName, eventsHandler, new CustomCamera1Enumerator(captureToTexture));
        this.captureToTexture = captureToTexture;
    }

    protected void createCameraSession(CustomCameraSession.CreateSessionCallback createSessionCallback, CustomCameraSession.Events events, Context applicationContext, SurfaceTextureHelper surfaceTextureHelper, String cameraName, int width, int height, int framerate) {
        CustomCamera1Session.create(createSessionCallback, events, this.captureToTexture, applicationContext, surfaceTextureHelper, CustomCamera1Enumerator.getCameraIndex(cameraName), width, height, framerate, new CustomCamera1Session.CameraInstantsCallback() {
            @Override
            public void getCamera(Camera po_camera, Camera.Parameters po_params) {
                o_camera = po_camera;
                o_params = po_params;
            }
        });
    }

    public float setZoom(double d2, boolean z) {
        zoom_level = (int)d2;

        int li_zoom = default_zoom + ((zoom_level - 1) * zoom_level_interval);

        o_params.setZoom(li_zoom);
        o_camera.setParameters(o_params);

        return 1.0f;
    }

    public boolean toggleFlash(boolean z) {
        if ( null != o_camera ) {
            o_params.setFlashMode(z ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
            o_camera.setParameters(o_params);
        }

        return true;
    }
}


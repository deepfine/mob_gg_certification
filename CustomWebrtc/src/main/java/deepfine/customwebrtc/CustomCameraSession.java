package deepfine.customwebrtc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.view.Surface;
import android.view.WindowManager;

import org.webrtc.TextureBufferImpl;
import org.webrtc.VideoFrame;

/**
 * @author hj.kim (DEEP.FINE)
 * @version 1.0.0
 * @Description Class설명
 * @since 2020-12-22
 */
public interface CustomCameraSession {
    void stop();

    interface Events {
        void onCameraOpening();
        void onCameraError(CustomCameraSession paramCameraSession, String paramString);
        void onCameraDisconnected(CustomCameraSession paramCameraSession);
        void onCameraClosed(CustomCameraSession paramCameraSession);
        void onFrameCaptured(CustomCameraSession paramCameraSession, VideoFrame paramVideoFrame);
    }

    interface CreateSessionCallback {
        void onDone(CustomCameraSession paramCameraSession);
        void onFailure(FailureType paramFailureType, String paramString);
    }

    enum FailureType {
        ERROR,
        DISCONNECTED;
        FailureType() {}
    }

    static int getDeviceOrientation(Context context) {
        @SuppressLint("WrongConstant")
        WindowManager wm = (WindowManager)context.getSystemService("window");
        switch (wm.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return 0;
        }
    }

    static VideoFrame.TextureBuffer createTextureBufferWithModifiedTransformMatrix(TextureBufferImpl buffer, boolean mirror, int rotation) {
        Matrix transformMatrix = new Matrix();
        transformMatrix.preTranslate(0.5F, 0.5F);
        if (mirror) {
            transformMatrix.preScale(-1.0F, 1.0F);
        }
        transformMatrix.preRotate(rotation);
        transformMatrix.preTranslate(-0.5F, -0.5F);

        return buffer.applyTransformMatrix(transformMatrix, buffer.getWidth(), buffer.getHeight());
    }
}

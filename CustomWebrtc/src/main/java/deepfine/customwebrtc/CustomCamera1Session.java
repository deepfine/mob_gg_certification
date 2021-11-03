package deepfine.customwebrtc;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.os.SystemClock;

import org.webrtc.NV21Buffer;
import org.webrtc.Size;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.TextureBufferImpl;
import org.webrtc.VideoFrame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import deepfine.customwebrtc.CustomCameraEnumerationAndroid.CaptureFormat;
import deepfine.customwebrtc.CustomCameraEnumerationAndroid.CaptureFormat.FramerateRange;

/**
 * @author hj.kim (DEEP.FINE)
 * @version 1.0.0
 * @Description Class설명
 * @since 2020-12-22
 */
public class CustomCamera1Session implements CustomCameraSession {
    public interface CameraInstantsCallback {
        void getCamera(Camera po_camera, Camera.Parameters po_params);
    }

    private final Handler cameraThreadHandler;
    private final Events events;
    private final boolean captureToTexture;
    private final Context applicationContext;
    private final SurfaceTextureHelper surfaceTextureHelper;
    private final int cameraId;
    private final Camera camera;
    private final Camera.CameraInfo info;
    private final CaptureFormat captureFormat;
    private final long constructionTimeNs;
    private SessionState state;
    private boolean firstFrameReported;

    //public static void create(CreateSessionCallback callback, Events events, boolean captureToTexture, Context applicationContext, SurfaceTextureHelper surfaceTextureHelper, int cameraId, int width, int height, int framerate) {
    public static void create(CreateSessionCallback callback, Events events, boolean captureToTexture, Context applicationContext, SurfaceTextureHelper surfaceTextureHelper, int cameraId, int width, int height, int framerate, CameraInstantsCallback po_callback) {
        long constructionTimeNs = System.nanoTime();
        events.onCameraOpening();

        Camera camera;

        try {
            camera = Camera.open(cameraId);
        }
        catch (RuntimeException var19) {
            callback.onFailure(FailureType.ERROR, var19.getMessage());
            return;
        }

        if (camera == null) {
            callback.onFailure(FailureType.ERROR, "android.hardware.Camera.open returned null for camera id = " + cameraId);
            po_callback.getCamera(null, null);
        }
        else {
            try {
                camera.setPreviewTexture(surfaceTextureHelper.getSurfaceTexture());
            }
            catch (RuntimeException | IOException var18) {
                camera.release();
                callback.onFailure(FailureType.ERROR, var18.getMessage());
                return;
            }

            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);

            CaptureFormat captureFormat;
            Camera.Parameters parameters = camera.getParameters();

            try {
                captureFormat = findClosestCaptureFormat(parameters, width, height, framerate);
                Size pictureSize = findClosestPictureSize(parameters, width, height);
                updateCameraParameters(camera, parameters, captureFormat, pictureSize, captureToTexture);
            } catch (RuntimeException var17) {
                camera.release();
                callback.onFailure(FailureType.ERROR, var17.getMessage());
                return;
            }

            if (!captureToTexture) {
                int frameSize = captureFormat.frameSize();

                for(int i = 0; i < 3; ++i) {
                    ByteBuffer buffer = ByteBuffer.allocateDirect(frameSize);
                    camera.addCallbackBuffer(buffer.array());
                }
            }

            camera.setDisplayOrientation(0);
            callback.onDone(new CustomCamera1Session(events, captureToTexture, applicationContext, surfaceTextureHelper, cameraId, camera, info, captureFormat, constructionTimeNs));
            po_callback.getCamera(camera, parameters);
        }
    }

    private static void updateCameraParameters(Camera camera, Camera.Parameters parameters, CaptureFormat captureFormat, Size pictureSize, boolean captureToTexture) {
        List<String> focusModes = parameters.getSupportedFocusModes();
        parameters.setPreviewFpsRange(captureFormat.framerate.min, captureFormat.framerate.max);
        parameters.setPreviewSize(captureFormat.width, captureFormat.height);
        parameters.setPictureSize(pictureSize.width, pictureSize.height);
        parameters.setPreviewFormat(ImageFormat.NV21);
        parameters.setPictureFormat(ImageFormat.NV21);

        if (!captureToTexture) {
            Objects.requireNonNull(captureFormat);
            parameters.setPreviewFormat(17);
        }

        //parameters.setPictureFormat(0x100);
        //parameters.setPreviewFormat(0x100);

        if (parameters.isVideoStabilizationSupported()) {
            parameters.setVideoStabilization(true);
        }

        /*
        if (focusModes.contains("continuous-video")) {
            parameters.setFocusMode("continuous-video");
        }
        */

        if ( focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO) ) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        camera.setParameters(parameters);
    }

    private static CaptureFormat findClosestCaptureFormat(Camera.Parameters parameters, int width, int height, int framerate) {
        List<FramerateRange> supportedFramerates = CustomCamera1Enumerator.convertFramerates(parameters.getSupportedPreviewFpsRange());
        FramerateRange fpsRange = CustomCameraEnumerationAndroid.getClosestSupportedFramerateRange(supportedFramerates, framerate);
        Size previewSize = CustomCameraEnumerationAndroid.getClosestSupportedSize(CustomCamera1Enumerator.convertSizes(parameters.getSupportedPreviewSizes()), width, height);
        return new CaptureFormat(previewSize.width, previewSize.height, fpsRange);
    }

    private static Size findClosestPictureSize(Camera.Parameters parameters, int width, int height) {
        return CustomCameraEnumerationAndroid.getClosestSupportedSize(CustomCamera1Enumerator.convertSizes(parameters.getSupportedPictureSizes()), width, height);
    }

    private CustomCamera1Session(Events events, boolean captureToTexture, Context applicationContext, SurfaceTextureHelper surfaceTextureHelper, int cameraId, Camera camera, Camera.CameraInfo info, CaptureFormat captureFormat, long constructionTimeNs) {
        this.cameraThreadHandler = new Handler();
        this.events = events;
        this.captureToTexture = captureToTexture;
        this.applicationContext = applicationContext;
        this.surfaceTextureHelper = surfaceTextureHelper;
        this.cameraId = cameraId;
        this.camera = camera;
        this.info = info;
        this.captureFormat = captureFormat;
        this.constructionTimeNs = constructionTimeNs;

        surfaceTextureHelper.setTextureSize(captureFormat.width, captureFormat.height);
        this.startCapturing();
    }

    public void stop() {
        this.checkIsOnCameraThread();
        if (this.state != SessionState.STOPPED) {
            //long stopStartTime = System.nanoTime();
            this.stopInternal();
            //int stopTimeMs = (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - stopStartTime);
            //camera1StopTimeMsHistogram.addSample(stopTimeMs);
        }
    }

    /*
    public void setFlashlightActive(boolean pb_isActive) {
        Camera.Parameters params = camera.getParameters();

        if ( pb_isActive ) {
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        } else {
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }

        camera.setParameters(params);
    }
    */

    private void startCapturing() {
        this.checkIsOnCameraThread();
        this.state = SessionState.RUNNING;
        this.camera.setErrorCallback(new Camera.ErrorCallback() {
            public void onError(int error, Camera camera) {
                String errorMessage;
                if (error == 100) {
                    errorMessage = "Camera server died!";
                } else {
                    errorMessage = "Camera error: " + error;
                }

                CustomCamera1Session.this.stopInternal();
                if (error == 2) {
                    CustomCamera1Session.this.events.onCameraDisconnected(CustomCamera1Session.this);
                } else {
                    CustomCamera1Session.this.events.onCameraError(CustomCamera1Session.this, errorMessage);
                }

            }
        });
        if (this.captureToTexture) {
            this.listenForTextureFrames();
        }
        else {
            this.listenForBytebufferFrames();
        }

        try {
            this.camera.startPreview();
        } catch (RuntimeException var2) {
            this.stopInternal();
            this.events.onCameraError(this, var2.getMessage());
        }

    }

    private void stopInternal() {
        this.checkIsOnCameraThread();
        if (this.state == SessionState.STOPPED) {
        } else {
            this.state = SessionState.STOPPED;
            this.surfaceTextureHelper.stopListening();
            this.camera.stopPreview();
            this.camera.release();
            this.events.onCameraClosed(this);
        }
    }

    private void listenForTextureFrames() {
        this.surfaceTextureHelper.startListening((frame) -> {
            this.checkIsOnCameraThread();
            if (this.state != SessionState.RUNNING) {
            } else {
                if (!this.firstFrameReported) {
                    int startTimeMs = (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - this.constructionTimeNs);
                    //camera1StartTimeMsHistogram.addSample(startTimeMs);
                    this.firstFrameReported = true;
                }

                VideoFrame modifiedFrame = new VideoFrame(CustomCameraSession.createTextureBufferWithModifiedTransformMatrix((TextureBufferImpl) frame.getBuffer(), this.info.facing == 1, 0), this.getFrameOrientation(), frame.getTimestampNs());
                this.events.onFrameCaptured(this, modifiedFrame);
                modifiedFrame.release();
            }
        });
    }

    private void listenForBytebufferFrames() {
        this.camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera callbackCamera) {
                CustomCamera1Session.this.checkIsOnCameraThread();

                if (callbackCamera != CustomCamera1Session.this.camera) {

                }
                else if (CustomCamera1Session.this.state != SessionState.RUNNING) {

                }
                else {
                    long captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());

                    if (!CustomCamera1Session.this.firstFrameReported) {
                        //int startTimeMs = (int)TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - CustomCamera1Session.this.constructionTimeNs);
                        //CustomCamera1Session.camera1StartTimeMsHistogram.addSample(startTimeMs);
                        CustomCamera1Session.this.firstFrameReported = true;
                    }

                    VideoFrame.Buffer frameBuffer = new NV21Buffer(data, CustomCamera1Session.this.captureFormat.width, CustomCamera1Session.this.captureFormat.height, () -> {
                        CustomCamera1Session.this.cameraThreadHandler.post(() -> {
                            if (CustomCamera1Session.this.state == SessionState.RUNNING) {
                                CustomCamera1Session.this.camera.addCallbackBuffer(data);
                            }
                        });
                    });

                    VideoFrame frame = new VideoFrame(frameBuffer, CustomCamera1Session.this.getFrameOrientation(), captureTimeNs);
                    CustomCamera1Session.this.events.onFrameCaptured(CustomCamera1Session.this, frame);
                    frame.release();
                }
            }
        });
    }

    private int getFrameOrientation() {
        int rotation = CustomCameraSession.getDeviceOrientation(this.applicationContext);
        if (this.info.facing == 0) {
            rotation = 360 - rotation;
        }

        return (this.info.orientation + rotation) % 360;
    }

    private void checkIsOnCameraThread() {
        if (Thread.currentThread() != this.cameraThreadHandler.getLooper().getThread()) {
            throw new IllegalStateException("Wrong thread");
        }
    }

    static {
        //camera1ResolutionHistogram = CustomHistogram.createEnumeration("WebRTC.Android.Camera1.Resolution", CustomCameraEnumerationAndroid.COMMON_RESOLUTIONS.size());
    }

    private static enum SessionState {
        RUNNING,
        STOPPED;

        private SessionState() {
        }
    }
}


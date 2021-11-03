package deepfine.customwebrtc;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.util.Range;
import android.view.Surface;

import androidx.annotation.Nullable;

import org.webrtc.Size;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.TextureBufferImpl;
import org.webrtc.VideoFrame;

import java.util.Arrays;
import java.util.List;

/**
 * @author hj.kim (DEEP.FINE)
 * @version 1.0.0
 * @Description Class설명
 * @since 2020-12-22
 */
@TargetApi(21)
public class CustomCamera2Session implements CustomCameraSession {
    private static final String TAG = "Camera2Session";
    private final Handler cameraThreadHandler;
    private final CreateSessionCallback callback;
    private final Events events;
    private final Context applicationContext;
    private final CameraManager cameraManager;
    private final SurfaceTextureHelper surfaceTextureHelper;
    private final String cameraId;
    private final int width;
    private final int height;
    private final int framerate;
    private CameraCharacteristics cameraCharacteristics;
    private int cameraOrientation;
    private boolean isCameraFrontFacing;
    private int fpsUnitFactor;
    private CustomCameraEnumerationAndroid.CaptureFormat captureFormat;
    @Nullable
    private CameraDevice cameraDevice;
    @Nullable
    private Surface surface;
    @Nullable
    public CameraCaptureSession captureSession;
    public SessionState state;
    public boolean firstFrameReported;
    private final long constructionTimeNs;
    public float currentZoomLevel = 1.0f;
    public Rect currentZoom = null;
    private float fingerSpacing;
    public boolean flashEnabled;
    CameraStatusChangeListener cameraStatusChangeListener;

    public static CustomCamera2Session create(CreateSessionCallback callback, Events events, Context applicationContext, CameraManager cameraManager, SurfaceTextureHelper surfaceTextureHelper, String cameraId, int width, int height, int framerate, CameraStatusChangeListener cameraStatusChangeListener2) {
        return new CustomCamera2Session(callback, events, applicationContext, cameraManager, surfaceTextureHelper, cameraId, width, height, framerate, cameraStatusChangeListener2);
    }

    private CustomCamera2Session(CreateSessionCallback callback, Events events, Context applicationContext, CameraManager cameraManager, SurfaceTextureHelper surfaceTextureHelper, String cameraId, int width, int height, int framerate, CameraStatusChangeListener cameraStatusChangeListener2) {
        this.cameraStatusChangeListener = cameraStatusChangeListener2;
        this.state = SessionState.RUNNING;
        this.constructionTimeNs = System.nanoTime();
        this.cameraThreadHandler = new Handler();
        this.callback = callback;
        this.events = events;
        this.applicationContext = applicationContext;
        this.cameraManager = cameraManager;
        this.surfaceTextureHelper = surfaceTextureHelper;
        this.cameraId = cameraId;
        this.width = width;
        this.height = height;
        this.framerate = framerate;

        this.start();
    }

    private void start() {
        this.checkIsOnCameraThread();

        /*
        try {
            this.cameraCharacteristics = this.cameraManager.getCameraCharacteristics(this.cameraId);
        } catch (CameraAccessException var2) {
            this.reportError("getCameraCharacteristics(): " + var2.getMessage());
            return;
        }

        this.cameraOrientation = (Integer)this.cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        this.isCameraFrontFacing = (Integer)this.cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == 0;
        this.findCaptureFormat();
        this.openCamera();
        */

        try {
            CameraCharacteristics cameraCharacteristics2 = this.cameraManager.getCameraCharacteristics(this.cameraId);
            this.cameraCharacteristics = cameraCharacteristics2;
            if (cameraCharacteristics2.get(CameraCharacteristics.SENSOR_ORIENTATION) != null) {
                this.cameraOrientation = ((Integer) this.cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
            }
            if (this.cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) != null) {
                this.isCameraFrontFacing = ((Integer) this.cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)).intValue() == 0;
            }
            if (findCaptureFormat()) {
                openCamera();
            }
        } catch (CameraAccessException e2) {
            reportError("getCameraCharacteristics(): " + e2.getMessage());
        }
    }

    private boolean findCaptureFormat() {
        String str;
        checkIsOnCameraThread();
        Range[] rangeArr = (Range[]) this.cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        int fpsUnitFactor2 = CustomCamera2Enumerator.getFpsUnitFactor(rangeArr);
        this.fpsUnitFactor = fpsUnitFactor2;
        List<CustomCameraEnumerationAndroid.CaptureFormat.FramerateRange> convertFramerates = CustomCamera2Enumerator.convertFramerates(rangeArr, fpsUnitFactor2);
        try {
            List<Size> supportedSizes = CustomCamera2Enumerator.getSupportedSizes(this.cameraCharacteristics);
            if (convertFramerates.isEmpty() || supportedSizes == null || supportedSizes.isEmpty()) {
                str = "No supported capture formats.";
                reportError(str);
                return false;
            }
            CustomCameraEnumerationAndroid.CaptureFormat.FramerateRange closestSupportedFramerateRange = CustomCameraEnumerationAndroid.getClosestSupportedFramerateRange(convertFramerates, this.framerate);
            Size closestSupportedSize = CustomCameraEnumerationAndroid.getClosestSupportedSize(supportedSizes, this.width, this.height);
            this.captureFormat = new CustomCameraEnumerationAndroid.CaptureFormat(closestSupportedSize.width, closestSupportedSize.height, closestSupportedFramerateRange);
            return true;
        } catch (Exception e2) {
            str = "Failed to get supported sizes : e " + e2.getMessage();
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        this.checkIsOnCameraThread();
        this.events.onCameraOpening();

        try {
            this.cameraManager.openCamera(this.cameraId, new CameraStateCallback(), this.cameraThreadHandler);
        } catch (CameraAccessException var2) {
            this.reportError("Failed to open camera: " + var2);
        }
    }

    public void stop() {
        /*
        this.checkIsOnCameraThread();
        if (this.state != CustomCamera2Session.SessionState.STOPPED) {
            long stopStartTime = System.nanoTime();
            this.state = CustomCamera2Session.SessionState.STOPPED;
            this.stopInternal();
            //int stopTimeMs = (int)TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - stopStartTime);
            //camera2StopTimeMsHistogram.addSample(stopTimeMs);
        }
        */

        checkIsOnCameraThread();
        boolean z = this.captureSession == null && this.state != SessionState.STOPPED;
        this.state = SessionState.STOPPED;
        stopInternal();
        if (z) {
            this.callback.onFailure(FailureType.ERROR, "");
        } else {
            this.events.onCameraError(this, "");
        }
    }

    public float getMaxZoom() {
        Float f2 = (Float) this.cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        if (f2 == null) {
            return 0.0f;
        }
        if (f2.floatValue() > 4.0f) {
            f2 = Float.valueOf(4.0f);
        }
        return f2.floatValue();
    }

    public float setZoom(double d2, boolean z) {
        if (this.currentZoomLevel != ((float) d2)) {
            if (d2 < 1.0d) {
                d2 = 1.0d;
            }
            if (d2 > ((double) getMaxZoom())) {
                d2 = (double) getMaxZoom();
            }
            float f2 = (float) d2;
            this.currentZoomLevel = f2;
            handleZoom(1.0f / f2, z);
        }
        return this.currentZoomLevel;
    }

    @SuppressLint("WrongConstant")
    public boolean toggleFlash(boolean z) {
        CameraDevice cameraDevice2;
        if (!(!this.applicationContext.getPackageManager().hasSystemFeature("android.hardware.camera.flash") || (cameraDevice2 = this.cameraDevice) == null || this.captureSession == null || this.surface == null || this.cameraThreadHandler == null)) {
            try {
                CaptureRequest.Builder createCaptureRequest = cameraDevice2.createCaptureRequest(3);
                createCaptureRequest.set(CaptureRequest.FLASH_MODE, Integer.valueOf(z ? 2 : 0));
                if (!(this.currentZoomLevel == 1.0f || this.currentZoom == null)) {
                    createCaptureRequest.set(CaptureRequest.SCALER_CROP_REGION, this.currentZoom);
                }
                createCaptureRequest.addTarget(this.surface);
                this.captureSession.setRepeatingRequest(createCaptureRequest.build(), (CameraCaptureSession.CaptureCallback) null, (Handler) null);
                this.flashEnabled = z;
            } catch (CameraAccessException e2) {
                reportError("Failed to start capture request. " + e2);
                this.flashEnabled = false;
            }
        }
        CameraStatusChangeListener cameraStatusChangeListener2 = this.cameraStatusChangeListener;
        if (cameraStatusChangeListener2 != null) {
            cameraStatusChangeListener2.flashStatusChanged(this.flashEnabled);
        }
        return this.flashEnabled;
    }

    public void setZoomWithDoubleTap() {
        float maxZoom = getMaxZoom();
        float f2 = this.currentZoomLevel;
        if (f2 < maxZoom) {
            float f3 = f2 + 1.0f;
            this.currentZoomLevel = f3;
            if (f3 > maxZoom) {
                this.currentZoomLevel = maxZoom;
            }
        } else if (f2 >= maxZoom) {
            this.currentZoomLevel = 1.0f;
        }
        handleZoom(1.0f / this.currentZoomLevel, false);
    }

    public void setZoomWithPinching(float f2) {
        float f3 = 0;
        float maxZoom = getMaxZoom();
        float f4 = this.fingerSpacing;
        if (f4 != 0.0f) {
            float f5 = 0.05f;
            if (f2 > f4) {
                float f6 = this.currentZoomLevel;
                if (maxZoom - f6 <= 0.05f) {
                    f5 = maxZoom - f6;
                }
                f3 = this.currentZoomLevel + f5;
            } else {
                if (f2 < f4) {
                    float f7 = this.currentZoomLevel;
                    if (f7 - 0.05f < 1.0f) {
                        f5 = f7 - 1.0f;
                    }
                    f3 = this.currentZoomLevel - f5;
                }
                handleZoom(1.0f / this.currentZoomLevel, false);
            }
            this.currentZoomLevel = f3;
            handleZoom(1.0f / this.currentZoomLevel, false);
        }
        this.fingerSpacing = f2;
    }

    @SuppressLint("WrongConstant")
    private void handleZoom(float f2, boolean z) {
        Rect rect = (Rect) this.cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (rect != null) {
            int width2 = (rect.width() - Math.round(((float) rect.width()) * f2)) / 2;
            int height2 = (rect.height() - Math.round(((float) rect.height()) * f2)) / 2;
            this.currentZoom = new Rect(width2, height2, rect.width() - width2, rect.height() - height2);
            CameraDevice cameraDevice2 = this.cameraDevice;
            if (!(cameraDevice2 == null || this.captureSession == null || this.surface == null || this.cameraThreadHandler == null)) {
                try {
                    CaptureRequest.Builder createCaptureRequest = cameraDevice2.createCaptureRequest(3);
                    if (this.flashEnabled) {
                        createCaptureRequest.set(CaptureRequest.FLASH_MODE, 2);
                    }
                    createCaptureRequest.set(CaptureRequest.SCALER_CROP_REGION, this.currentZoom);
                    createCaptureRequest.addTarget(this.surface);
                    this.captureSession.setRepeatingRequest(createCaptureRequest.build(), new CameraCaptureCallback(), this.cameraThreadHandler);
                } catch (CameraAccessException e2) {
                    reportError("Failed to start capture request. " + e2);
                }
            }
        }
        CameraStatusChangeListener cameraStatusChangeListener2 = this.cameraStatusChangeListener;
        if (cameraStatusChangeListener2 != null) {
            cameraStatusChangeListener2.zoomLevelChanged(this.currentZoomLevel, z);
        }
    }

    private void stopInternal() {
        this.checkIsOnCameraThread();
        this.surfaceTextureHelper.stopListening();
        if (this.captureSession != null) {
            this.captureSession.close();
            this.captureSession = null;
        }

        if (this.surface != null) {
            this.surface.release();
            this.surface = null;
        }

        if (this.cameraDevice != null) {
            this.cameraDevice.close();
            this.cameraDevice = null;
        }
    }

    private void reportError(String error) {
        this.checkIsOnCameraThread();
        boolean startFailure = this.captureSession == null && this.state != SessionState.STOPPED;
        this.state = SessionState.STOPPED;
        this.stopInternal();
        if (startFailure) {
            this.callback.onFailure(FailureType.ERROR, error);
        } else {
            this.events.onCameraError(this, error);
        }

    }

    private int getFrameOrientation() {
        int rotation = CustomCameraSession.getDeviceOrientation(this.applicationContext);
        if (!this.isCameraFrontFacing) {
            rotation = 360 - rotation;
        }

        return (this.cameraOrientation + rotation) % 360;
    }

    private void checkIsOnCameraThread() {
        if (Thread.currentThread() != this.cameraThreadHandler.getLooper().getThread()) {
            throw new IllegalStateException("Wrong thread");
        }
    }

    static {
        //camera2ResolutionHistogram = Histogram.createEnumeration("WebRTC.Android.Camera2.Resolution", CameraEnumerationAndroid.COMMON_RESOLUTIONS.size());
    }

    private static class CameraCaptureCallback extends CameraCaptureSession.CaptureCallback {
        private CameraCaptureCallback() {
        }

        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
        }
    }

    public interface CameraStatusChangeListener {
        void flashStatusChanged(boolean z);
        void zoomLevelChanged(float f2, boolean z);
    }

    private class CaptureSessionCallback extends CameraCaptureSession.StateCallback {
        private CaptureSessionCallback() {
        }

        public void onConfigureFailed(CameraCaptureSession session) {
            CustomCamera2Session.this.checkIsOnCameraThread();
            session.close();
            CustomCamera2Session.this.reportError("Failed to configure capture session.");
        }

        @SuppressLint("WrongConstant")
        public void onConfigured(CameraCaptureSession session) {
            CustomCamera2Session.this.checkIsOnCameraThread();
            CameraCaptureSession unused = CustomCamera2Session.this.captureSession = session;

            try {
                CaptureRequest.Builder createCaptureRequest = CustomCamera2Session.this.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                createCaptureRequest.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range(Integer.valueOf(CustomCamera2Session.this.captureFormat.framerate.min / CustomCamera2Session.this.fpsUnitFactor), Integer.valueOf(CustomCamera2Session.this.captureFormat.framerate.max / CustomCamera2Session.this.fpsUnitFactor)));
                createCaptureRequest.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                createCaptureRequest.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                createCaptureRequest.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
                createCaptureRequest.set(CaptureRequest.CONTROL_AE_LOCK, Boolean.FALSE);
                createCaptureRequest.set(CaptureRequest.LENS_FOCUS_DISTANCE, 5f);

                //CaptureRequest.Builder createCaptureRequest = CustomCamera2Session.this.cameraDevice.createCaptureRequest(3);
                //createCaptureRequest.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range(Integer.valueOf(CustomCamera2Session.this.captureFormat.framerate.min / CustomCamera2Session.this.fpsUnitFactor), Integer.valueOf(CustomCamera2Session.this.captureFormat.framerate.max / CustomCamera2Session.this.fpsUnitFactor)));
                //createCaptureRequest.set(CaptureRequest.CONTROL_AE_MODE, 1);
                //createCaptureRequest.set(CaptureRequest.CONTROL_AE_LOCK, Boolean.FALSE);
                createCaptureRequest.set(CaptureRequest.FLASH_MODE, Integer.valueOf(CustomCamera2Session.this.flashEnabled ? 2 : 0));
                if (!(CustomCamera2Session.this.currentZoomLevel == 1.0f || CustomCamera2Session.this.currentZoom == null)) {
                    createCaptureRequest.set(CaptureRequest.SCALER_CROP_REGION, CustomCamera2Session.this.currentZoom);
                }
                chooseStabilizationMode(createCaptureRequest);
                chooseFocusMode(createCaptureRequest);
                createCaptureRequest.addTarget(CustomCamera2Session.this.surface);
                session.setRepeatingRequest(createCaptureRequest.build(), new CameraCaptureCallback(), CustomCamera2Session.this.cameraThreadHandler);

                CustomCamera2Session.this.surfaceTextureHelper.startListening((frame) -> {
                    CustomCamera2Session.this.checkIsOnCameraThread();
                    if (CustomCamera2Session.this.state == SessionState.RUNNING) {
                        if (!CustomCamera2Session.this.firstFrameReported) {
                            CustomCamera2Session.this.firstFrameReported = true;
                        }

                        VideoFrame modifiedFrame = new VideoFrame(CustomCameraSession.createTextureBufferWithModifiedTransformMatrix((TextureBufferImpl) frame.getBuffer(), CustomCamera2Session.this.isCameraFrontFacing, -CustomCamera2Session.this.cameraOrientation), CustomCamera2Session.this.getFrameOrientation(), frame.getTimestampNs());
                        CustomCamera2Session.this.events.onFrameCaptured(CustomCamera2Session.this, modifiedFrame);
                        modifiedFrame.release();
                    }
                });

                CustomCamera2Session.this.callback.onDone(CustomCamera2Session.this);
            } catch (CameraAccessException e2) {
                CustomCamera2Session zoomableCamera2Session = CustomCamera2Session.this;
                zoomableCamera2Session.reportError("Failed to start capture request. " + e2);
            }
        }

        private void chooseStabilizationMode(CaptureRequest.Builder captureRequestBuilder) {
            /*
            int[] availableOpticalStabilization = (int[])CustomCamera2Session.this.cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);
            int[] availableVideoStabilization;
            int var5;
            int mode;
            if (availableOpticalStabilization != null) {
                availableVideoStabilization = availableOpticalStabilization;
                int var4 = availableOpticalStabilization.length;

                for(var5 = 0; var5 < var4; ++var5) {
                    mode = availableVideoStabilization[var5];
                    if (mode == 1) {
                        captureRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, 1);
                        captureRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, 0);
                        return;
                    }
                }
            }

            availableVideoStabilization = (int[])CustomCamera2Session.this.cameraCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);
            int[] var8 = availableVideoStabilization;
            var5 = availableVideoStabilization.length;

            for(mode = 0; mode < var5; ++mode) {
                int modex = var8[mode];
                if (modex == 1) {
                    captureRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, 1);
                    captureRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, 0);
                    return;
                }
            }
            */

            CaptureRequest.Key key;
            int[] iArr = (int[]) CustomCamera2Session.this.cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);
            if (iArr != null) {
                int length = iArr.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    } else if (iArr[i] == 1) {
                        captureRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, 1);
                        key = CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE;
                        break;
                    } else {
                        i++;
                    }
                }
            }
            int[] iArr2 = (int[]) CustomCamera2Session.this.cameraCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);
            if (iArr2 != null) {
                for (int i2 : iArr2) {
                    if (i2 == 1) {
                        captureRequestBuilder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, 1);
                        key = CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE;
                        captureRequestBuilder.set(key, 0);
                        return;
                    }
                }
            }
        }

        private void chooseFocusMode(CaptureRequest.Builder captureRequestBuilder) {
            /*
            int[] availableFocusModes = (int[])CustomCamera2Session.this.cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            int[] var3 = availableFocusModes;
            int var4 = availableFocusModes.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                int mode = var3[var5];
                if (mode == 3) {
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, 3);
                    return;
                }
            }
            */

            int[] iArr = (int[]) CustomCamera2Session.this.cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            if (iArr != null) {
                for (int i : iArr) {
                    if (i == 3) {
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, 3);
                        return;
                    }
                }
            }
        }
    }

    private class CameraStateCallback extends CameraDevice.StateCallback {
        private CameraStateCallback() {
        }

        private String getErrorDescription(int errorCode) {
            switch(errorCode) {
                case 1:
                    return "Camera device is in use already.";
                case 2:
                    return "Camera device could not be opened because there are too many other open camera devices.";
                case 3:
                    return "Camera device could not be opened due to a device policy.";
                case 4:
                    return "Camera device has encountered a fatal error.";
                case 5:
                    return "Camera service has encountered a fatal error.";
                default:
                    return "Unknown camera error: " + errorCode;
            }
        }

        public void onDisconnected(CameraDevice camera) {
            CustomCamera2Session.this.checkIsOnCameraThread();
            boolean startFailure = CustomCamera2Session.this.captureSession == null && CustomCamera2Session.this.state != SessionState.STOPPED;
            CustomCamera2Session.this.state = SessionState.STOPPED;
            CustomCamera2Session.this.stopInternal();
            if (startFailure) {
                CustomCamera2Session.this.callback.onFailure(FailureType.DISCONNECTED, "Camera disconnected / evicted.");
            } else {
                CustomCamera2Session.this.events.onCameraDisconnected(CustomCamera2Session.this);
            }

        }

        public void onError(CameraDevice camera, int errorCode) {
            CustomCamera2Session.this.checkIsOnCameraThread();
            CustomCamera2Session.this.reportError(this.getErrorDescription(errorCode));
        }

        public void onOpened(CameraDevice camera) {
            CustomCamera2Session.this.checkIsOnCameraThread();
            CustomCamera2Session.this.cameraDevice = camera;
            CustomCamera2Session.this.surfaceTextureHelper.setTextureSize(CustomCamera2Session.this.captureFormat.width, CustomCamera2Session.this.captureFormat.height);
            CustomCamera2Session.this.surface = new Surface(CustomCamera2Session.this.surfaceTextureHelper.getSurfaceTexture());

            try {
                camera.createCaptureSession(Arrays.asList(CustomCamera2Session.this.surface), CustomCamera2Session.this.new CaptureSessionCallback(), CustomCamera2Session.this.cameraThreadHandler);
            } catch (CameraAccessException var3) {
                CustomCamera2Session.this.reportError("Failed to create capture session. " + var3);
            }
        }

        public void onClosed(CameraDevice camera) {
            CustomCamera2Session.this.checkIsOnCameraThread();
            CustomCamera2Session.this.events.onCameraClosed(CustomCamera2Session.this);
        }
    }

    private static enum SessionState {
        RUNNING,
        STOPPED;

        private SessionState() {
        }
    }
}


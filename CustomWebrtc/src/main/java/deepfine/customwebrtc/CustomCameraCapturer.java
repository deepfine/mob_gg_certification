package deepfine.customwebrtc;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoFrame;

import java.util.Arrays;

/**
 * @author hj.kim (DEEP.FINE)
 * @version 1.0.0
 * @Description Class설명
 * @since 2020-12-22
 */
abstract class CustomCameraCapturer implements CustomCameraVideoCapturer {
    private static final int MAX_OPEN_CAMERA_ATTEMPTS = 3;
    private static final int OPEN_CAMERA_DELAY_MS = 500;
    private static final int OPEN_CAMERA_TIMEOUT = 10000;
    private final CustomCameraEnumerator cameraEnumerator;
    @Nullable
    private final CameraEventsHandler eventsHandler;
    private final Handler uiThreadHandler;

    static enum SwitchState {
        IDLE,  PENDING,  IN_PROGRESS;
        private SwitchState() {}
    }

    @Nullable
    private final CustomCameraSession.CreateSessionCallback createSessionCallback = new CustomCameraSession.CreateSessionCallback() {
        public void onDone(CustomCameraSession session) {
            CustomCameraCapturer.this.checkIsOnCameraThread();
            CustomCameraCapturer.this.uiThreadHandler.removeCallbacks(CustomCameraCapturer.this.openCameraTimeoutRunnable);

            synchronized (CustomCameraCapturer.this.stateLock) {
                CustomCameraCapturer.this.capturerObserver.onCapturerStarted(true);
                CustomCameraCapturer.this.sessionOpening = false;
                CustomCameraCapturer.this.currentSession = session;
                CustomCameraCapturer.this.cameraStatistics = new CameraStatistics(CustomCameraCapturer.this.surfaceHelper, CustomCameraCapturer.this.eventsHandler);
                CustomCameraCapturer.this.firstFrameObserved = false;
                CustomCameraCapturer.this.stateLock.notifyAll();

                if (CustomCameraCapturer.this.switchState == SwitchState.IN_PROGRESS) {
                    CustomCameraCapturer.this.switchState = SwitchState.IDLE;
                    if (CustomCameraCapturer.this.switchEventsHandler != null) {
                        CustomCameraCapturer.this.switchEventsHandler.onCameraSwitchDone(CustomCameraCapturer.this.cameraEnumerator.isFrontFacing(CustomCameraCapturer.this.cameraName));
                        CustomCameraCapturer.this.switchEventsHandler = null;
                    }
                }
                else if (CustomCameraCapturer.this.switchState == SwitchState.PENDING) {
                    CustomCameraCapturer.this.switchState = SwitchState.IDLE;
                    CustomCameraCapturer.this.switchCameraInternal(CustomCameraCapturer.this.switchEventsHandler);
                }
            }
        }

        public void onFailure(CustomCameraSession.FailureType failureType, String error) {
            CustomCameraCapturer.this.checkIsOnCameraThread();
            CustomCameraCapturer.this.uiThreadHandler.removeCallbacks(CustomCameraCapturer.this.openCameraTimeoutRunnable);
            synchronized (CustomCameraCapturer.this.stateLock) {
                CustomCameraCapturer.this.capturerObserver.onCapturerStarted(false);
                CustomCameraCapturer.this.openAttemptsRemaining--;
                if (CustomCameraCapturer.this.openAttemptsRemaining <= 0) {
                    CustomCameraCapturer.this.sessionOpening = false;
                    CustomCameraCapturer.this.stateLock.notifyAll();
                    if (CustomCameraCapturer.this.switchState != SwitchState.IDLE) {
                        if (CustomCameraCapturer.this.switchEventsHandler != null) {
                            CustomCameraCapturer.this.switchEventsHandler.onCameraSwitchError(error);
                            CustomCameraCapturer.this.switchEventsHandler = null;
                        }
                        CustomCameraCapturer.this.switchState = SwitchState.IDLE;
                    }
                    if (failureType == CustomCameraSession.FailureType.DISCONNECTED) {
                        CustomCameraCapturer.this.eventsHandler.onCameraDisconnected();
                    }
                    else {
                        CustomCameraCapturer.this.eventsHandler.onCameraError(error);
                    }
                }
                else {
                    CustomCameraCapturer.this.createSessionInternal(500);
                }
            }
        }
    };

    @Nullable
    private final CustomCameraSession.Events cameraSessionEventsHandler = new CustomCameraSession.Events() {
        public void onCameraOpening() {
            CustomCameraCapturer.this.checkIsOnCameraThread();
            synchronized (CustomCameraCapturer.this.stateLock) {
                if (CustomCameraCapturer.this.currentSession != null) {
                    return;
                }
                CustomCameraCapturer.this.eventsHandler.onCameraOpening(CustomCameraCapturer.this.cameraName);
            }
        }

        public void onCameraError(CustomCameraSession session, String error) {
            CustomCameraCapturer.this.checkIsOnCameraThread();
            synchronized (CustomCameraCapturer.this.stateLock) {
                if (session != CustomCameraCapturer.this.currentSession) {
                    return;
                }
                CustomCameraCapturer.this.eventsHandler.onCameraError(error);
                CustomCameraCapturer.this.stopCapture();
            }
        }

        public void onCameraDisconnected(CustomCameraSession session) {
            CustomCameraCapturer.this.checkIsOnCameraThread();
            synchronized (CustomCameraCapturer.this.stateLock) {
                if (session != CustomCameraCapturer.this.currentSession) {
                    return;
                }
                CustomCameraCapturer.this.eventsHandler.onCameraDisconnected();
                CustomCameraCapturer.this.stopCapture();
            }
        }

        public void onCameraClosed(CustomCameraSession session) {
            CustomCameraCapturer.this.checkIsOnCameraThread();
            synchronized (CustomCameraCapturer.this.stateLock) {
                if ((session != CustomCameraCapturer.this.currentSession) && (CustomCameraCapturer.this.currentSession != null)) {
                    return;
                }
                CustomCameraCapturer.this.eventsHandler.onCameraClosed();
            }
        }

        public void onFrameCaptured(CustomCameraSession session, VideoFrame frame) {
            CustomCameraCapturer.this.checkIsOnCameraThread();
            synchronized (CustomCameraCapturer.this.stateLock) {
                if (session != CustomCameraCapturer.this.currentSession) {
                    return;
                }
                if (!CustomCameraCapturer.this.firstFrameObserved) {
                    CustomCameraCapturer.this.eventsHandler.onFirstFrameAvailable();
                    CustomCameraCapturer.this.firstFrameObserved = true;
                }
                CustomCameraCapturer.this.cameraStatistics.addFrame();
                CustomCameraCapturer.this.capturerObserver.onFrameCaptured(frame);
            }
        }
    };

    private final Runnable openCameraTimeoutRunnable = new Runnable() {
        public void run() {
            CustomCameraCapturer.this.eventsHandler.onCameraError("Camera failed to start within timeout.");
        }
    };

    @Nullable
    private Handler cameraThreadHandler;
    private Context applicationContext;
    private CapturerObserver capturerObserver;

    @Nullable
    private SurfaceTextureHelper surfaceHelper;
    private final Object stateLock = new Object();
    private boolean sessionOpening;

    @Nullable
    private CustomCameraSession currentSession;
    private String cameraName;
    private int width, height, framerate, openAttemptsRemaining;
    private SwitchState switchState = SwitchState.IDLE;

    @Nullable
    private CameraSwitchHandler switchEventsHandler;

    @Nullable
    private CameraStatistics cameraStatistics;
    private boolean firstFrameObserved;

    public CustomCameraCapturer(String cameraName, @Nullable CameraEventsHandler eventsHandler, CustomCameraEnumerator cameraEnumerator) {
        if (eventsHandler == null) {
            eventsHandler = new CameraEventsHandler()
            {
                public void onCameraError(String errorDescription) {}

                public void onCameraDisconnected() {}

                public void onCameraFreezed(String errorDescription) {}

                public void onCameraOpening(String cameraName) {}

                public void onFirstFrameAvailable() {}

                public void onCameraClosed() {}
            };
        }

        this.eventsHandler = eventsHandler;
        this.cameraEnumerator = cameraEnumerator;
        this.cameraName = cameraName;
        this.uiThreadHandler = new Handler(Looper.getMainLooper());

        String[] deviceNames = cameraEnumerator.getDeviceNames();
        if (deviceNames.length == 0) {
            throw new RuntimeException("No cameras attached.");
        }
        if (!Arrays.asList(deviceNames).contains(this.cameraName)) {
            throw new IllegalArgumentException("Camera name " + this.cameraName + " does not match any known camera device.");
        }
    }

    public void initialize(@Nullable SurfaceTextureHelper surfaceTextureHelper, Context applicationContext, CapturerObserver capturerObserver) {
        this.applicationContext = applicationContext;
        this.capturerObserver = capturerObserver;
        this.surfaceHelper = surfaceTextureHelper;

        this.cameraThreadHandler = (surfaceTextureHelper == null ? null : surfaceTextureHelper.getHandler());
    }

    public void startCapture(int width, int height, int framerate) {
        if (this.applicationContext == null) {
            throw new RuntimeException("CustomCameraCapturer must be initialized before calling startCapture.");
        }
        synchronized (this.stateLock) {
            if ((this.sessionOpening) || (this.currentSession != null)) {
                return;
            }
            this.width = width;
            this.height = height;
            this.framerate = framerate;

            this.sessionOpening = true;
            this.openAttemptsRemaining = 3;
            createSessionInternal(0);
        }
    }

    private void createSessionInternal(int delayMs) {
        this.uiThreadHandler.postDelayed(this.openCameraTimeoutRunnable, delayMs + 10000);
        this.cameraThreadHandler.postDelayed(new Runnable() {
            public void run() {
                CustomCameraCapturer.this.createCameraSession(CustomCameraCapturer.this.createSessionCallback, CustomCameraCapturer.this.cameraSessionEventsHandler, CustomCameraCapturer.this.applicationContext,
                        CustomCameraCapturer.this.surfaceHelper, CustomCameraCapturer.this.cameraName, CustomCameraCapturer.this.width, CustomCameraCapturer.this.height, CustomCameraCapturer.this.framerate);
            }
        }, delayMs);
    }

    public void stopCapture() {
        synchronized (this.stateLock) {
            while (this.sessionOpening) {
                try {
                    this.stateLock.wait();
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            if (this.currentSession != null) {
                this.cameraStatistics.release();
                this.cameraStatistics = null;
                final CustomCameraSession oldSession = this.currentSession;
                this.cameraThreadHandler.post(new Runnable() {
                    public void run() {
                        oldSession.stop();
                    }
                });
                this.currentSession = null;
                this.capturerObserver.onCapturerStopped();
            }
        }
    }

    public void changeCaptureFormat(int width, int height, int framerate) {
        synchronized (this.stateLock) {
            stopCapture();
            startCapture(width, height, framerate);
        }
    }

    public void dispose()
    {
        stopCapture();
    }

    public void switchCamera(final CameraSwitchHandler switchEventsHandler) {
        this.cameraThreadHandler.post(new Runnable() {
            public void run() {
                CustomCameraCapturer.this.switchCameraInternal(switchEventsHandler);
            }
        });
    }

    public boolean isScreencast()
    {
        return false;
    }

    public void printStackTrace() {
        Thread cameraThread = null;

        if (this.cameraThreadHandler != null) {
            cameraThread = this.cameraThreadHandler.getLooper().getThread();
        }

        if (cameraThread != null) {
            StackTraceElement[] cameraStackTrace = cameraThread.getStackTrace();
            if (cameraStackTrace.length > 0) {
                for (StackTraceElement traceElem : cameraStackTrace) {
                }
            }
        }
    }

    private void reportCameraSwitchError(String error, @Nullable CameraSwitchHandler switchEventsHandler) {
        if (switchEventsHandler != null) {
            switchEventsHandler.onCameraSwitchError(error);
        }
    }

    private void switchCameraInternal(@Nullable CameraSwitchHandler switchEventsHandler) {
        String[] deviceNames = this.cameraEnumerator.getDeviceNames();

        if (deviceNames.length < 2) {
            if (switchEventsHandler != null) {
                switchEventsHandler.onCameraSwitchError("No camera to switch to.");
            }
            return;
        }

        synchronized (this.stateLock) {
            if (this.switchState != SwitchState.IDLE) {
                reportCameraSwitchError("Camera switch already in progress.", switchEventsHandler);
                return;
            }

            if ((!this.sessionOpening) && (this.currentSession == null)) {
                reportCameraSwitchError("switchCamera: camera is not running.", switchEventsHandler);
                return;
            }

            this.switchEventsHandler = switchEventsHandler;

            if (this.sessionOpening) {
                this.switchState = SwitchState.PENDING;
                return;
            }

            this.switchState = SwitchState.IN_PROGRESS;
            this.cameraStatistics.release();
            this.cameraStatistics = null;
            final CustomCameraSession oldSession = this.currentSession;
            this.cameraThreadHandler.post(new Runnable() {
                public void run() {
                    oldSession.stop();
                }
            });

            this.currentSession = null;
            int cameraNameIndex = Arrays.asList(deviceNames).indexOf(this.cameraName);
            this.cameraName = deviceNames[((cameraNameIndex + 1) % deviceNames.length)];

            this.sessionOpening = true;
            this.openAttemptsRemaining = 1;
            createSessionInternal(0);
        }
    }

    private void checkIsOnCameraThread() {
        if (Thread.currentThread() != this.cameraThreadHandler.getLooper().getThread()) {
            throw new RuntimeException("Not on camera thread.");
        }
    }

    protected String getCameraName() {
        synchronized(this.stateLock) {
            return this.cameraName;
        }
    }

    protected abstract void createCameraSession(CustomCameraSession.CreateSessionCallback paramCreateSessionCallback, CustomCameraSession.Events paramEvents, Context paramContext, SurfaceTextureHelper paramSurfaceTextureHelper, String paramString, int paramInt1, int paramInt2, int paramInt3);
}


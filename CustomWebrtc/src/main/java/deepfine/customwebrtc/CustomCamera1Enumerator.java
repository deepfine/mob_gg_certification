package deepfine.customwebrtc;

import android.hardware.Camera;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import android.hardware.Camera;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import org.webrtc.CameraEnumerationAndroid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author hj.kim (DEEP.FINE)
 * @version 1.0.0
 * @Description Class설명
 * @since 2020-12-22
 */
public class CustomCamera1Enumerator implements CustomCameraEnumerator {
    private static List<List<CustomCameraEnumerationAndroid.CaptureFormat>> cachedSupportedFormats;
    private final boolean captureToTexture;

    public CustomCamera1Enumerator() {
        this(true);
    }

    public CustomCamera1Enumerator(boolean captureToTexture) {
        this.captureToTexture = captureToTexture;
    }

    public String[] getDeviceNames() {
        ArrayList<String> namesList = new ArrayList();

        for(int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            String name = getDeviceName(i);
            if (name != null) {
                namesList.add(name);
            }
            else {
            }
        }

        String[] namesArray = new String[namesList.size()];
        return (String[])namesList.toArray(namesArray);
    }

    public boolean isFrontFacing(String deviceName) {
        Camera.CameraInfo info = getCameraInfo(getCameraIndex(deviceName));
        return info != null && info.facing == 1;
    }

    public boolean isBackFacing(String deviceName) {
        Camera.CameraInfo info = getCameraInfo(getCameraIndex(deviceName));
        return info != null && info.facing == 0;
    }

    public List<CustomCameraEnumerationAndroid.CaptureFormat> getSupportedFormats(String deviceName) {
        return getSupportedFormats(getCameraIndex(deviceName));
    }

    public CustomCameraVideoCapturer createCapturer(String deviceName, CustomCameraVideoCapturer.CameraEventsHandler eventsHandler) {
        return new CustomCamera1Capturer(deviceName, eventsHandler, this.captureToTexture);
    }

    @Nullable
    private static Camera.CameraInfo getCameraInfo(int index) {
        Camera.CameraInfo info = new Camera.CameraInfo();

        try {
            Camera.getCameraInfo(index, info);
            return info;
        } catch (Exception var3) {
            return null;
        }
    }

    static synchronized List<CustomCameraEnumerationAndroid.CaptureFormat> getSupportedFormats(int cameraId) {
        if (cachedSupportedFormats == null) {
            cachedSupportedFormats = new ArrayList();

            for(int i = 0; i < Camera.getNumberOfCameras(); ++i) {
                cachedSupportedFormats.add(enumerateFormats(i));
            }
        }

        return (List)cachedSupportedFormats.get(cameraId);
    }

    private static List<CustomCameraEnumerationAndroid.CaptureFormat> enumerateFormats(int cameraId) {
        long startTimeMs = SystemClock.elapsedRealtime();
        Camera camera = null;

        Camera.Parameters parameters;
        label94: {
            ArrayList var6;
            try {
                camera = Camera.open(cameraId);
                parameters = camera.getParameters();
                break label94;
            } catch (RuntimeException var15) {
                var6 = new ArrayList();
            } finally {
                if (camera != null) {
                    camera.release();
                }
            }

            return var6;
        }

        ArrayList formatList = new ArrayList();

        try {
            int minFps = 0;
            int maxFps = 0;
            List<int[]> listFpsRange = parameters.getSupportedPreviewFpsRange();
            if (listFpsRange != null) {
                int[] range = (int[])listFpsRange.get(listFpsRange.size() - 1);
                minFps = range[0];
                maxFps = range[1];
            }

            Iterator var19 = parameters.getSupportedPreviewSizes().iterator();

            while(var19.hasNext()) {
                Camera.Size size = (Camera.Size)var19.next();
                formatList.add(new CameraEnumerationAndroid.CaptureFormat(size.width, size.height, minFps, maxFps));
            }
        } catch (Exception var14) {
        }

        long endTimeMs = SystemClock.elapsedRealtime();
        return formatList;
    }

    static List<org.webrtc.Size> convertSizes(List<Camera.Size> cameraSizes) {
        List<org.webrtc.Size> sizes = new ArrayList();
        Iterator var2 = cameraSizes.iterator();

        while(var2.hasNext()) {
            Camera.Size size = (Camera.Size)var2.next();
            sizes.add(new org.webrtc.Size(size.width, size.height));
        }

        return sizes;
    }

    static List<CustomCameraEnumerationAndroid.CaptureFormat.FramerateRange> convertFramerates(List<int[]> arrayRanges) {
        List<CustomCameraEnumerationAndroid.CaptureFormat.FramerateRange> ranges = new ArrayList();
        Iterator var2 = arrayRanges.iterator();

        while(var2.hasNext()) {
            int[] range = (int[])var2.next();
            ranges.add(new CustomCameraEnumerationAndroid.CaptureFormat.FramerateRange(range[0], range[1]));
        }

        return ranges;
    }

    static int getCameraIndex(String deviceName) {
        for(int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            if (deviceName.equals(getDeviceName(i))) {
                return i;
            }
        }

        throw new IllegalArgumentException("No such camera: " + deviceName);
    }

    @Nullable
    static String getDeviceName(int index) {
        Camera.CameraInfo info = getCameraInfo(index);
        if (info == null) {
            return null;
        } else {
            String facing = info.facing == 1 ? "front" : "back";
            return "Camera " + index + ", Facing " + facing + ", Orientation " + info.orientation;
        }
    }
}

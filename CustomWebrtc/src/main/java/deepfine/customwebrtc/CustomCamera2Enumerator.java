package deepfine.customwebrtc;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.SystemClock;
import android.util.AndroidException;
import android.util.Range;

import androidx.annotation.Nullable;

import org.webrtc.Size;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author hj.kim (DEEP.FINE)
 * @version 1.0.0
 * @Description Class설명
 * @since 2020-12-22
 */
@TargetApi(21)
public class CustomCamera2Enumerator implements CustomCameraEnumerator {
    private static final String TAG = "Camera2Enumerator";
    private static final double NANO_SECONDS_PER_SECOND = 1.0E9D;
    private static final Map<String, List<CustomCameraEnumerationAndroid.CaptureFormat>> cachedSupportedFormats = new HashMap<>();
    final Context context;
    @Nullable
    final CameraManager cameraManager;
    private CustomCamera2Session.CameraStatusChangeListener cameraStatusChangeListener;

    @SuppressLint("WrongConstant")
    public CustomCamera2Enumerator(Context context, CustomCamera2Session.CameraStatusChangeListener cameraStatusChangeListener2) {
        this.context = context;
        this.cameraStatusChangeListener = cameraStatusChangeListener2;
        this.cameraManager = (CameraManager)context.getSystemService("camera");
    }

    public String[] getDeviceNames() {
        try {
            return this.cameraManager.getCameraIdList();
        } catch (AndroidException var2) {
            return new String[0];
        }
    }

    public boolean isFrontFacing(String deviceName) {
        CameraCharacteristics characteristics = this.getCameraCharacteristics(deviceName);
        return characteristics != null && (Integer)characteristics.get(CameraCharacteristics.LENS_FACING) == 0;
    }

    public boolean isBackFacing(String deviceName) {
        CameraCharacteristics characteristics = this.getCameraCharacteristics(deviceName);
        return characteristics != null && (Integer)characteristics.get(CameraCharacteristics.LENS_FACING) == 1;
    }

    @Nullable
    public List<CustomCameraEnumerationAndroid.CaptureFormat> getSupportedFormats(String deviceName) {
        return getSupportedFormats(this.context, deviceName);
    }

    public CustomCameraVideoCapturer createCapturer(String deviceName, CustomCameraVideoCapturer.CameraEventsHandler eventsHandler) {
        return new CustomCamera2Capturer(this.context, deviceName, eventsHandler, this.cameraStatusChangeListener);
    }

    @Nullable
    private CameraCharacteristics getCameraCharacteristics(String deviceName) {
        try {
            return this.cameraManager.getCameraCharacteristics(deviceName);
        } catch (AndroidException var3) {
            return null;
        }
    }
    
    public static boolean isSupported(Context context) {
        @SuppressLint("WrongConstant")
        CameraManager cameraManager = (CameraManager) context.getSystemService("camera");
        
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            String[] var3 = cameraIds;
            int var4 = cameraIds.length;
            
            for (int var5 = 0; var5 < var4; ++var5) {
                String id = var3[var5];
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                if ((Integer) characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) == 2) {
                    return false;
                }
            }
            
            return true;
        } catch (AndroidException var8) {
            return false;
        }
    }

    static int getFpsUnitFactor(Range<Integer>[] fpsRanges) {
        if (fpsRanges.length == 0) {
            return 1000;
        } else {
            return (Integer)fpsRanges[0].getUpper() < 1000 ? 1000 : 1;
        }
    }

    static List<Size> getSupportedSizes(CameraCharacteristics cameraCharacteristics) {
        StreamConfigurationMap streamMap = (StreamConfigurationMap)cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        int supportLevel = (Integer)cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        android.util.Size[] nativeSizes = streamMap.getOutputSizes(SurfaceTexture.class);
        List<Size> sizes = convertSizes(nativeSizes);
        if (supportLevel == 2) {
            Rect activeArraySize = (Rect)cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            ArrayList<Size> filteredSizes = new ArrayList();
            Iterator var7 = sizes.iterator();

            while(var7.hasNext()) {
                Size size = (Size)var7.next();
                if (activeArraySize.width() * size.height == activeArraySize.height() * size.width) {
                    filteredSizes.add(size);
                }
            }

            return filteredSizes;
        } else {
            return sizes;
        }
    }

    @SuppressLint("WrongConstant")
    @Nullable
    static List<CustomCameraEnumerationAndroid.CaptureFormat> getSupportedFormats(Context context, String cameraId) {
        return getSupportedFormats((CameraManager)context.getSystemService("camera"), cameraId);
    }

    @Nullable
    static List<CustomCameraEnumerationAndroid.CaptureFormat> getSupportedFormats(CameraManager cameraManager, String cameraId) {
        synchronized(cachedSupportedFormats) {
            if (cachedSupportedFormats.containsKey(cameraId)) {
                return (List)cachedSupportedFormats.get(cameraId);
            } else {
                long startTimeMs = SystemClock.elapsedRealtime();

                CameraCharacteristics cameraCharacteristics;
                try {
                    cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                } catch (Exception var19) {
                    return new ArrayList();
                }

                StreamConfigurationMap streamMap = (StreamConfigurationMap)cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Range<Integer>[] fpsRanges = (Range[])cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                List<CustomCameraEnumerationAndroid.CaptureFormat.FramerateRange> framerateRanges = convertFramerates(fpsRanges, getFpsUnitFactor(fpsRanges));
                List<Size> sizes = getSupportedSizes(cameraCharacteristics);
                int defaultMaxFps = 0;

                CustomCameraEnumerationAndroid.CaptureFormat.FramerateRange framerateRange;
                for(Iterator var11 = framerateRanges.iterator(); var11.hasNext(); defaultMaxFps = Math.max(defaultMaxFps, framerateRange.max)) {
                    framerateRange = (CustomCameraEnumerationAndroid.CaptureFormat.FramerateRange) var11.next();
                }

                List<CustomCameraEnumerationAndroid.CaptureFormat> formatList = new ArrayList();
                Iterator var22 = sizes.iterator();

                while(var22.hasNext()) {
                    Size size = (Size)var22.next();
                    long minFrameDurationNs = 0L;

                    try {
                        minFrameDurationNs = streamMap.getOutputMinFrameDuration(SurfaceTexture.class, new android.util.Size(size.width, size.height));
                    } catch (Exception var18) {
                    }

                    int maxFps = minFrameDurationNs == 0L ? defaultMaxFps : (int) Math.round(1.0E9D / (double)minFrameDurationNs) * 1000;
                    formatList.add(new CustomCameraEnumerationAndroid.CaptureFormat(size.width, size.height, 0, maxFps));
                }

                cachedSupportedFormats.put(cameraId, formatList);
                //long endTimeMs = SystemClock.elapsedRealtime();

                return formatList;
            }
        }
    }

    private static List<Size> convertSizes(android.util.Size[] cameraSizes) {
        List<Size> sizes = new ArrayList();
        android.util.Size[] var2 = cameraSizes;
        int var3 = cameraSizes.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            android.util.Size size = var2[var4];
            sizes.add(new Size(size.getWidth(), size.getHeight()));
        }

        return sizes;
    }

    static List<CustomCameraEnumerationAndroid.CaptureFormat.FramerateRange> convertFramerates(Range<Integer>[] arrayRanges, int unitFactor) {
        List<CustomCameraEnumerationAndroid.CaptureFormat.FramerateRange> ranges = new ArrayList();
        Range[] var3 = arrayRanges;
        int var4 = arrayRanges.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            Range<Integer> range = var3[var5];
            ranges.add(new CustomCameraEnumerationAndroid.CaptureFormat.FramerateRange((Integer)range.getLower() * unitFactor, (Integer)range.getUpper() * unitFactor));
        }

        return ranges;
    }
}


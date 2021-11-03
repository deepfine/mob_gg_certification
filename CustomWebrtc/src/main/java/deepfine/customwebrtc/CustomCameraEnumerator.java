package deepfine.customwebrtc;

import java.util.List;

/**
 * @author hj.kim (DEEP.FINE)
 * @version 1.0.0
 * @Description Class설명
 * @since 2020-12-22
 */
public interface CustomCameraEnumerator {
    String[] getDeviceNames();
    boolean isFrontFacing(String var1);
    boolean isBackFacing(String var1);
    List<CustomCameraEnumerationAndroid.CaptureFormat> getSupportedFormats(String var1);
    CustomCameraVideoCapturer createCapturer(String var1, CustomCameraVideoCapturer.CameraEventsHandler var2);
}
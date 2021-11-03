package deepfine.customwebrtc;

import android.graphics.ImageFormat;

import org.webrtc.Size;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author hj.kim (DEEP.FINE)
 * @version 1.0.0
 * @Description Class설명
 * @since 2020-12-22
 */
public class CustomCameraEnumerationAndroid {
    static final ArrayList<Size> COMMON_RESOLUTIONS = new ArrayList(Arrays.asList(new Size(640, 360),new Size(640, 480), new Size(768, 480), new Size(854, 480), new Size(800, 600), new Size(960, 540), new Size(960, 640), new Size(1024, 576), new Size(1024, 600), new Size(1280, 720), new Size(1280, 1024)));

    public CustomCameraEnumerationAndroid() {
    }

    public static CaptureFormat.FramerateRange getClosestSupportedFramerateRange(List<CaptureFormat.FramerateRange> supportedFramerates, final int requestedFps) {
        return (CaptureFormat.FramerateRange) Collections.min(supportedFramerates, new ClosestComparator<CaptureFormat.FramerateRange>() {
            private int progressivePenalty(int value, int threshold, int lowWeight, int highWeight) {
                return value < threshold ? value * lowWeight : threshold * lowWeight + (value - threshold) * highWeight;
            }

            int diff(CaptureFormat.FramerateRange range) {
                int minFpsError = this.progressivePenalty(range.min, 8000, 1, 4);
                int maxFpsError = this.progressivePenalty(Math.abs(requestedFps * 1000 - range.max), 5000, 1, 3);
                return minFpsError + maxFpsError;
            }
        });
    }

    public static Size getClosestSupportedSize(List<Size> supportedSizes, final int requestedWidth, final int requestedHeight) {
        return (Size)Collections.min(supportedSizes, new ClosestComparator<Size>() {
            int diff(Size size) {
                return Math.abs(requestedWidth - size.width) + Math.abs(requestedHeight - size.height);
            }
        });
    }

    private abstract static class ClosestComparator<T> implements Comparator<T> {
        private ClosestComparator() {
        }

        abstract int diff(T var1);

        public int compare(T t1, T t2) {
            return this.diff(t1) - this.diff(t2);
        }
    }

    public static class CaptureFormat {
        public final int width;
        public final int height;
        public final FramerateRange framerate;
        public final int imageFormat = 17;

        public CaptureFormat(int width, int height, int minFramerate, int maxFramerate) {
            this.width = width;
            this.height = height;
            this.framerate = new FramerateRange(minFramerate, maxFramerate);
        }

        public CaptureFormat(int width, int height, FramerateRange framerate) {
            this.width = width;
            this.height = height;
            this.framerate = framerate;
        }

        public int frameSize() {
            return frameSize(this.width, this.height, 17);
        }

        public static int frameSize(int width, int height, int imageFormat) {
            if (imageFormat != 17) {
                throw new UnsupportedOperationException("Don't know how to calculate the frame size of non-NV21 image formats.");
            } else {
                return width * height * ImageFormat.getBitsPerPixel(imageFormat) / 8;
            }
        }

        public String toString() {
            return this.width + "x" + this.height + "@" + this.framerate;
        }

        public boolean equals(Object other) {
            if (!(other instanceof CaptureFormat)) {
                return false;
            } else {
                CaptureFormat otherFormat = (CaptureFormat)other;
                return this.width == otherFormat.width && this.height == otherFormat.height && this.framerate.equals(otherFormat.framerate);
            }
        }

        public int hashCode() {
            return 1 + (this.width * '\uffd9' + this.height) * 251 + this.framerate.hashCode();
        }

        public static class FramerateRange {
            public int min;
            public int max;

            public FramerateRange(int min, int max) {
                this.min = min;
                this.max = max;
            }

            public String toString() {
                String ls_value = "[" + (float)this.min / 1000.0F + ":" + (float)this.max / 1000.0F + "]";
                return ls_value;
            }

            public boolean equals(Object other) {
                if (!(other instanceof FramerateRange)) {
                    return false;
                } else {
                    FramerateRange otherFramerate = (FramerateRange)other;
                    return this.min == otherFramerate.min && this.max == otherFramerate.max;
                }
            }

            public int hashCode() {
                return 1 + 65537 * this.min + this.max;
            }
        }
    }
}


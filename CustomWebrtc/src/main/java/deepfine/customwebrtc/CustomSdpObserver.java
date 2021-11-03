package deepfine.customwebrtc;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

/**
 * @author hj.kim (DEEP.FINE)
 * @version 1.0.0
 * @Description Class설명
 * @since 2020-12-22
 */
public class CustomSdpObserver implements SdpObserver {
    private String tag;

    public CustomSdpObserver(String logTag) {
        tag = this.getClass().getCanonicalName();
        this.tag = this.tag + " " + logTag;
    }

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
    }

    @Override
    public void onSetSuccess() {
    }

    @Override
    public void onCreateFailure(String s) {
    }

    @Override
    public void onSetFailure(String s) {
    }
}

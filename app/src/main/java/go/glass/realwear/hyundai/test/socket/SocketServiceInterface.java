package go.glass.realwear.hyundai.test.socket;

import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.ArrayList;

import go.glass.realwear.hyundai.test.video.VideoConferenceUser;

public interface SocketServiceInterface {
    void onConnect();
    void onConnectError(String psMsg);
    void onDisConnect(String psMsg);
    void user_joined(String psId, int piCount, ArrayList<VideoConferenceUser> poClient);
    void user_left(String psId);
    void gotMessageFromServer(String psId, JSONObject poData);

    interface PeerCreateLister {
        void onIceCandidate(IceCandidate iceCandidate, String socketId);
        void onSucceed();
    }

    interface PeerCreateOfferListner {
        void onOffer(SessionDescription sessionDescription);
    }

    interface VideoUserDeleteListner {
        void deleteSucc();
    }
}

package go.glass.realwear.hyundai.test.video;

import org.webrtc.AudioTrack;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoTrack;

import deepfine.customwebrtc.CustomPeerConnectionObserver;
import deepfine.customwebrtc.CustomSdpObserver;
import go.glass.realwear.hyundai.test.socket.SocketServiceInterface;
import lombok.Data;

@Data
public class VideoConferenceUser {
    private String socketId = "";
    private PeerConnection peerConnection;
    private VideoTrack videoTrack;
    private AudioTrack audioTrack;
    private MediaStream stream;

    public void createPeerConnection(PeerConnectionFactory poFactory, PeerConnection.RTCConfiguration poRtcConfig, MediaConstraints poPeerConnConstraints, SocketServiceInterface.PeerCreateLister poCallback) {
        peerConnection = poFactory.createPeerConnection(poRtcConfig, poPeerConnConstraints, new CustomPeerConnectionObserver("localPeerCreation") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                poCallback.onIceCandidate(iceCandidate, socketId);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                if ( mediaStream.audioTracks.size() > 0 ) {
                    if ( null != mediaStream.audioTracks.get(0) ) {
                        audioTrack = mediaStream.audioTracks.get(0);
                        audioTrack.setEnabled(true);
                    }
                }

                if ( mediaStream.videoTracks.size() > 0 ) {
                    if ( null != mediaStream.videoTracks.get(0) ) {
                        videoTrack = mediaStream.videoTracks.get(0);

                        if ( videoTrack.enabled() ) {
                            videoTrack.setEnabled(false);
                        }
                    }
                }

                poCallback.onSucceed();
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                super.onIceCandidatesRemoved(iceCandidates);
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                super.onIceConnectionChange(iceConnectionState);
            }
        });
    }

    /**
     * 오퍼 생성
     */
    public void createOffer(MediaConstraints poPeerConnConstraints, SocketServiceInterface.PeerCreateOfferListner poCallback) {
        peerConnection.createOffer(new CustomSdpObserver("localCreateOffer") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                peerConnection.setLocalDescription(new CustomSdpObserver("localSetLocalDesc"), sessionDescription);
                poCallback.onOffer(sessionDescription);
            }

            @Override
            public void onCreateFailure(String s) {
                super.onCreateFailure(s);
            }

            @Override
            public void onSetFailure(String s) {
                super.onSetFailure(s);
            }

            @Override
            public void onSetSuccess() {
                super.onSetSuccess();
            }

        }, poPeerConnConstraints);
    }

    public void remove(AudioTrack poLocalAudioTrack, VideoTrack poLocalVideoTrack, SocketServiceInterface.VideoUserDeleteListner poCallback) {
        try {
            if ( null != stream ) {
                stream.removeTrack(poLocalAudioTrack);
                stream.removeTrack(poLocalVideoTrack);
            }
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
        finally {
            try {
                peerConnection.removeStream(stream);
            }catch (Exception ex){

            }

            stream = null;
        }

        if ( null != videoTrack ) {
            try {
                videoTrack.dispose();
            }
            catch ( Exception e ) {
                e.printStackTrace();
            }
        }

        if ( null != audioTrack ) {
            try {
                audioTrack.dispose();
            }
            catch ( Exception e ) {
                e.printStackTrace();
            }
        }

        if ( null != peerConnection ) {
            try {
                peerConnection.close();
            }
            catch ( Exception e ) {
                e.printStackTrace();
            }
        }

        videoTrack = null;
        audioTrack = null;
        stream = null;
        peerConnection = null;
        socketId = "";

        if ( null != poCallback ) {
            poCallback.deleteSucc();
        }
    }
}

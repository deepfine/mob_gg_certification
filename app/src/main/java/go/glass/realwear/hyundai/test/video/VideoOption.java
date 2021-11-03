package go.glass.realwear.hyundai.test.video;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.audio.AudioDeviceModule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import go.glass.realwear.hyundai.test.DeepFineApplication;
import lombok.Data;

@Data
public class VideoOption {
    protected Context mContext;
    protected PeerConnectionFactory mPeerConnectionFactory;
    protected AudioDeviceModule mAudioAdm;
    protected EglBase mEglBase;
    protected MediaConstraints mPeerConnConstraints;
    protected MediaConstraints mAudioConstraints;
    protected PeerConnection.RTCConfiguration mRtcConfig;
    protected List<PeerConnection.IceServer> mPeerIceServers = new ArrayList<>();
    protected int mVideoWidth = DeepFineApplication.VIDEO_WIDTH;
    protected int mVideoHeight = DeepFineApplication.VIDEO_HEIGHT;
    protected int mVideoFps = DeepFineApplication.VIDEO_FPS;
    protected String mVideoCodec = DeepFineApplication.VIDEO_CODEC;
    protected int mMinBitrate = 200;
    protected int mMaxBitrate = 450;
    protected int mVideoBitrate = 200;
    protected int mAudioBitrate = 32;

    public VideoOption(Context poContext) {
        this.mContext = poContext;
    }

    public void init(EglBase poEglBase) {
        this.mEglBase = poEglBase;

        PeerConnectionFactory.initialize(initPeerConnectionFactoryOptions());
        PeerConnectionFactory.Options peerConnctionFactoryOption = new PeerConnectionFactory.Options();
        peerConnctionFactoryOption.networkIgnoreMask = 0;
        peerConnctionFactoryOption.disableNetworkMonitor = true;

        this.mPeerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(peerConnctionFactoryOption)
                .setAudioDeviceModule(null)
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(mEglBase.getEglBaseContext(), true, false))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(mEglBase.getEglBaseContext()))
                .createPeerConnectionFactory();

        mPeerConnConstraints = new MediaConstraints();
        mPeerConnConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("IceRestart", "true"));
        mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));
        mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(mVideoWidth)));
        mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(mVideoHeight)));
        mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minWidth", Integer.toString(854)));
        mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minHeight", Integer.toString(480)));
        mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(mVideoFps)));
        mPeerConnConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(mVideoFps)));

        mAudioConstraints = new MediaConstraints();
        mAudioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
        mAudioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
        mAudioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("echoCancellation", "true"));
        mAudioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("noiseSuppression", "true"));

        getIceServer();

        mRtcConfig = new PeerConnection.RTCConfiguration(mPeerIceServers);
        mRtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED;
        mRtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT;
        mRtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        mRtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        mRtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        mRtcConfig.iceTransportsType = PeerConnection.IceTransportsType.ALL;
        mRtcConfig.surfaceIceCandidatesOnIceTransportTypeChanged = true;
    }

    public void getIceServer() {
        ArrayList<String> urls = new ArrayList<>();
        urls.add(DeepFineApplication.STUN_SERVER_1);
        urls.add(DeepFineApplication.STUN_SERVER_2);

        PeerConnection.IceServer stun = PeerConnection.IceServer.builder(urls).createIceServer();

        ArrayList<String> turns = new ArrayList<>();
        turns.add(DeepFineApplication.TURN_SERVER_URL + "?transport=udp");
        turns.add(DeepFineApplication.TURN_SERVER_URL + "?transport=tcp");

        PeerConnection.IceServer turn = PeerConnection.IceServer.builder(turns).setUsername(DeepFineApplication.TURN_USER_NAME).setPassword(DeepFineApplication.TURN_USER_PASS).createIceServer();

        mPeerIceServers.add(stun);
        mPeerIceServers.add(turn);
    }

    public PeerConnectionFactory.InitializationOptions initPeerConnectionFactoryOptions() {
        return PeerConnectionFactory.InitializationOptions
                .builder(this.mContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public String changeSdp(String psSdp, String psMediaType, int piBitrate) {
        String lsSdp = psSdp;
        String[] lsSdps = lsSdp.split("\n");

        ArrayList<String> strs = new ArrayList<String>(Arrays.asList(lsSdps));

        int li_line = -1;

        for ( int i = 0; i < strs.size(); i++ ) {
            if ( strs.get(i).indexOf("m="+psMediaType) == 0 ) {
                li_line = i;
                break;
            }
        }

        if ( li_line == -1 ) {
            return lsSdp;
        }

        li_line++;

        while ( strs.get(li_line).indexOf("i=") == 0 || strs.get(li_line).indexOf("c=") == 0 ) {
            li_line++;
        }

        if ( strs.get(li_line).indexOf("b") == 0 ) {
            strs.set(li_line, "b=AS:"+piBitrate);
            lsSdp = String.join("\n", strs);
            return lsSdp;
        }

        List<String> newStrs = strs.subList(0, li_line);
        newStrs.add("b=AS:"+piBitrate);
        newStrs.addAll(strs.subList(li_line+1, strs.size()));

        lsSdp = String.join("\n", newStrs) + "\n";

        return lsSdp;
    }

    /**
     *  Codec 변경
     */
    public String preferCodec(String sdpDescription, String codec, boolean isAudio, int piStartBitrate, int piMinBitrate, int piMaxBitrate) {
        final String[] lines = sdpDescription.split("\r\n");
        final int mLineIndex = findMediaDescriptionLine(isAudio, lines);
        int lineIndex = -1;
        String vpRtpMap = null;

        if (mLineIndex == -1) {
            return sdpDescription;
        }

        final List<String> codecPayloadTypes = new ArrayList<String>();
        final Pattern codecPattern = Pattern.compile("^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$");

        for (int i = 0; i < lines.length; ++i) {
            Matcher codecMatcher = codecPattern.matcher(lines[i]);

            if ( codecMatcher.matches() ) {
                vpRtpMap = codecMatcher.group(1);
                codecPayloadTypes.add(vpRtpMap);
                lineIndex = i;
            }
        }

        if ( codecPayloadTypes.isEmpty() ) {
            return sdpDescription;
        }

        final String newMLine = movePayloadTypesToFront(codecPayloadTypes, lines[mLineIndex]);

        if ( newMLine == null ) {
            return sdpDescription;
        }

        lines[lineIndex+1] = "a=fmtp:" + vpRtpMap + " x-google-start-bitrate="+piStartBitrate+";x-google-min-bitrate="+piMinBitrate+";x-google-max-bitrate="+piMaxBitrate+";x-google-max-quantization="+56; //+ ";useadaptivelayering_v2=true";
        lines[mLineIndex] = newMLine;

        String temp = joinString(Arrays.asList(lines), "\r\n", true /* delimiterAtEnd */);

        return temp;
    }

    public int findMediaDescriptionLine(boolean isAudio, String[] sdpLines) {
        final String mediaDescription = isAudio ? "m=audio " : "m=video ";
        for (int i = 0; i < sdpLines.length; ++i) {
            if (sdpLines[i].startsWith(mediaDescription)) {
                return i;
            }
        }
        return -1;
    }

    public String movePayloadTypesToFront(List<String> preferredPayloadTypes, String mLine) {
        final List<String> origLineParts = Arrays.asList(mLine.split(" "));

        if (origLineParts.size() <= 3) {
            return null;
        }

        final List<String> header = origLineParts.subList(0, 3);
        final List<String> unpreferredPayloadTypes =
                new ArrayList<String>(origLineParts.subList(3, origLineParts.size()));
        unpreferredPayloadTypes.removeAll(preferredPayloadTypes);

        final List<String> newLineParts = new ArrayList<String>();
        newLineParts.addAll(header);
        newLineParts.addAll(preferredPayloadTypes);
        newLineParts.addAll(unpreferredPayloadTypes);

        return joinString(newLineParts, " ", false /* delimiterAtEnd */);
    }

    public String joinString(Iterable<? extends CharSequence> s, String delimiter, boolean delimiterAtEnd) {
        Iterator<? extends CharSequence> iter = s.iterator();

        if (!iter.hasNext()) {
            return "";
        }

        StringBuilder buffer = new StringBuilder(iter.next());

        while (iter.hasNext()) {
            buffer.append(delimiter).append(iter.next());
        }

        if (delimiterAtEnd) {
            buffer.append(delimiter);
        }

        return buffer.toString();
    }

    public void clear() {
        try {
            if ( null != mPeerConnectionFactory ) {
                mPeerConnectionFactory.dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if ( null != mAudioAdm ) {
            mAudioAdm.release();
            mAudioAdm = null;
        }

        mAudioConstraints = null;
        mPeerConnConstraints = null;
        mPeerConnectionFactory = null;
        mEglBase = null;
    }
}

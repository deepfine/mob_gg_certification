package go.glass.realwear.hyundai.test.video;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import deepfine.customwebrtc.CustomCamera1Enumerator;
import deepfine.customwebrtc.CustomCamera2Enumerator;
import deepfine.customwebrtc.CustomCamera2Session;
import deepfine.customwebrtc.CustomCameraEnumerator;
import deepfine.customwebrtc.CustomCameraVideoCapturer;
import deepfine.customwebrtc.CustomSdpObserver;
import deepfine.customwebrtc.CustomVideoCapture;
import go.glass.realwear.hyundai.test.dialog.DeepFineAlertDialog;
import go.glass.realwear.hyundai.test.socket.SocketService;
import go.glass.realwear.hyundai.test.socket.SocketServiceInterface;

public abstract class VideoBaseActivity<B extends ViewDataBinding> extends AppCompatActivity implements SocketServiceInterface {
    protected B binding;
    protected abstract int getLayoutId();
    protected Intent mIntent;
    protected String mRoomId = "";
    protected Context mContext;
    protected EglBase mEglBase;
    public String mCameraType = "CAMERA_1"; // CAMERA_1  CAMERA_2
    protected CustomCameraVideoCapturer.CameraEventsHandler mCameraOpenCallback;
    protected CustomVideoCapture mVideoCapturer;
    protected SurfaceTextureHelper mSurfaceTextureHelper;
    protected VideoOption mVideoOption;
    protected VideoSource mVideoSource;
    protected AudioSource mAudioSource;
    protected AudioTrack mLocalAudioTrack;
    protected VideoTrack mLocalVideoTrack;
    protected String mSocketId = "";
    protected CopyOnWriteArrayList<VideoConferenceUser> mUsers = new CopyOnWriteArrayList<>();
    protected DeepFineAlertDialog mDeepFineAlertDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, getLayoutId());
        setWindowManager();

        mContext = this;

        mDeepFineAlertDialog = new DeepFineAlertDialog(mContext);

        mVideoOption = new VideoOption(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socketClose();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void setWindowManager() {
        if (null != getWindow()) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    public void videoConferenceBaseInit(CustomCameraVideoCapturer.CameraEventsHandler poCameraOpenCallback) {
        mVideoOption.init(mEglBase);
        mCameraOpenCallback = poCameraOpenCallback;

        mSurfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().getName(), mEglBase.getEglBaseContext());
        mVideoCapturer = getCamera();

        mVideoSource = mVideoOption.getMPeerConnectionFactory().createVideoSource(mVideoCapturer.isScreencast());
        mVideoSource.adaptOutputFormat(mVideoOption.getMVideoWidth(), mVideoOption.getMVideoHeight(), mVideoOption.getMVideoFps());

        mVideoCapturer.initialize(mSurfaceTextureHelper, getApplicationContext(), mVideoSource.getCapturerObserver());
        mVideoCapturer.startCapture(mVideoOption.getMVideoWidth(), mVideoOption.getMVideoHeight(), mVideoOption.getMVideoFps());

        mAudioSource = mVideoOption.getMPeerConnectionFactory().createAudioSource(mVideoOption.getMAudioConstraints());

        mLocalAudioTrack = mVideoOption.getMPeerConnectionFactory().createAudioTrack("101", mAudioSource);
        mLocalVideoTrack = mVideoOption.getMPeerConnectionFactory().createVideoTrack("100", mVideoSource);
    }

    /**
     * 카메라
     */
    private CustomVideoCapture getCamera() {
        CustomVideoCapture object = null;

        if ( mCameraType.equals("CAMERA_1") ) {
            object = createCameraCapturer(new CustomCamera1Enumerator(false));
        }
        else if ( mCameraType.equals("CAMERA_2") ) {
            object = createCameraCapturer(new CustomCamera2Enumerator(this, new CustomCamera2Session.CameraStatusChangeListener() {
                @Override
                public void flashStatusChanged(boolean z) {

                }

                @Override
                public void zoomLevelChanged(float f2, boolean z) {

                }
            }));
        }

        return object;
    }

    /**
     * 카메라
     */
    private CustomVideoCapture createCameraCapturer(CustomCameraEnumerator po_enumerator) {
        final String[] deviceNames = po_enumerator.getDeviceNames();

        for ( String deviceName : deviceNames ) {
            if ( !po_enumerator.isFrontFacing(deviceName) ) {
                CustomCameraVideoCapturer videoCapture = po_enumerator.createCapturer(deviceName, mCameraOpenCallback);
                if ( videoCapture != null ) return videoCapture;
            }
        }

        return null;
    }

    /**
     * 피어소켓 인덱스 구하기
     */
    public int getUserIndex(String psSocketId) {
        if ( mUsers.size() < 1 ) {
            return -1;
        }

        int li_index = -1;

        for ( int i = 0; i < mUsers.size(); i++ ) {
            VideoConferenceUser data = mUsers.get(i);

            if ( data.getSocketId().equals(psSocketId) ) {
                li_index = i;
                break;
            }
        }

        return li_index;
    }

    public void videoConferenceFinish() {

    }

    public void userChange() {

    }

    protected void showAlertDialog(String contentText, String confirmButtonText, String cancelButtonText, boolean onBtn, DeepFineAlertDialog.DeepFineAlertDialogListner callBackListener) {
        if (null != mDeepFineAlertDialog) {
            if (mDeepFineAlertDialog.isShowing()) {
                mDeepFineAlertDialog.dismiss();
            }
        } else {
            mDeepFineAlertDialog = new DeepFineAlertDialog(this);
        }

        mDeepFineAlertDialog.setMContent(contentText);
        mDeepFineAlertDialog.setMStext(confirmButtonText);
        mDeepFineAlertDialog.setMCtext(cancelButtonText);
        mDeepFineAlertDialog.setMCallback(callBackListener);
        mDeepFineAlertDialog.setMOneButtonType(onBtn);

        if (!mDeepFineAlertDialog.isShowing()) {
            mDeepFineAlertDialog.show();
        }
    }

    //==============================================================================================
    // 소켓 함수
    //==============================================================================================
    public void socketInit() {
        SocketService.getInstance().init(this);
    }

    public void socketClose() {
        try {
            if ( null !=  SocketService.getInstance().getSocket() || SocketService.getInstance().getSocketStatus() ) {
                SocketService.getInstance().close();
            }
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnect() {
        mSocketId = SocketService.getInstance().getSocketId();
        SocketService.getInstance().getSocket().emit("join_room", mRoomId);
    }

    @Override
    public void onConnectError(String psMsg) {
        socketClose();
    }

    @Override
    public void onDisConnect(String psMsg) {
        socketClose();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void user_joined(String psId, int piCount, ArrayList<VideoConferenceUser> poClient) {
        for ( VideoConferenceUser data : poClient ) {
            if ( data.getSocketId().equals(mSocketId) ) {
                continue;
            }

            if ( getUserIndex(data.getSocketId()) == -1 ) {
                data.createPeerConnection(mVideoOption.getMPeerConnectionFactory(), mVideoOption.getMRtcConfig(), mVideoOption.getMPeerConnConstraints(), new SocketServiceInterface.PeerCreateLister() {
                    @Override
                    public void onIceCandidate(IceCandidate iceCandidate, String socketId) {
                        if (iceCandidate != null) {
                            try {
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("type", "candidate");
                                jsonObject.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                                jsonObject.put("sdpMid", iceCandidate.sdpMid);
                                jsonObject.put("candidate", iceCandidate.sdp);

                                JSONObject jsonObject_content = new JSONObject();
                                jsonObject_content.put("ice", jsonObject);

                                SocketService.getInstance().getSocket().emit("signal", socketId, jsonObject_content.toString());
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onSucceed() {

                    }
                });

                data.setStream(mVideoOption.getMPeerConnectionFactory().createLocalMediaStream("102"));
                data.getStream().addTrack(mLocalVideoTrack);
                data.getStream().addTrack(mLocalAudioTrack);
                data.getPeerConnection().addStream(data.getStream());

                mUsers.addIfAbsent(data);
            }
        }

        userChange();

        if ( piCount >= 2 ) {
            if ( psId.equals(mSocketId) ) {
                return;
            }

            int index = getUserIndex(psId);

            if ( index == -1 ) {
                return;
            }

            mUsers.get(index).createOffer(mVideoOption.getMPeerConnConstraints(), sessionDescription -> {
                try {
                    JSONObject jsonObject = new JSONObject();

                    String ls_sdp = videoSdpChanged(sessionDescription.description, true);

                    jsonObject.put("sdp", ls_sdp);
                    jsonObject.put("type", sessionDescription.type.canonicalForm());

                    JSONObject jsonObject_content = new JSONObject();
                    jsonObject_content.put("sdp", jsonObject);

                    SocketService.getInstance().getSocket().emit("signal", psId, jsonObject_content.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public void user_left(String psId) {
        try {
            int index = getUserIndex(psId);

            if ( index != -1 ) {
                mUsers.get(index).remove(mLocalAudioTrack, mLocalVideoTrack, () -> {
                    mUsers.remove(index);
                });
            }
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
        finally {
            userChange();
        }
    }

    @Override
    public void gotMessageFromServer(String psId, JSONObject signal) {
        int index = getUserIndex(psId);

        if (index == -1) {
            return;
        }

        VideoConferenceUser data = mUsers.get(index);

        if ( !data.getSocketId().equals(mSocketId) ) {
            try {
                if ( signal.has("sdp") ) {
                    JSONObject content_data = new JSONObject(signal.getString("sdp"));
                    String ls_type = content_data.getString("type");

                    data.getPeerConnection().setRemoteDescription(new CustomSdpObserver("localSetRemote"), new SessionDescription((ls_type.equals("answer") ? SessionDescription.Type.ANSWER : SessionDescription.Type.OFFER), content_data.getString("sdp")));

                    if (ls_type.equals("offer")) {
                        data.getPeerConnection().createAnswer(new CustomSdpObserver("localCreateAns") {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void onCreateSuccess(SessionDescription sessionDescription) {
                                super.onCreateSuccess(sessionDescription);

                                try {
                                    data.getPeerConnection().setLocalDescription(new CustomSdpObserver("localSetLocal"), sessionDescription);

                                    JSONObject jsonObject = new JSONObject();

                                    String ls_sdp = videoSdpChanged(sessionDescription.description, true);

                                    jsonObject.put("sdp", ls_sdp);
                                    jsonObject.put("type", sessionDescription.type.canonicalForm());

                                    JSONObject jsonObject_content = new JSONObject();
                                    jsonObject_content.put("sdp", jsonObject);

                                    SocketService.getInstance().getSocket().emit("signal", data.getSocketId(), jsonObject_content.toString());

                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onCreateFailure(String s) {
                                super.onCreateFailure(s);
                            }
                        }, mVideoOption.getMPeerConnConstraints());
                    }
                }

                if (signal.has("ice")) {
                    JSONObject content_data = new JSONObject(signal.getString("ice"));
                    data.getPeerConnection().addIceCandidate(new IceCandidate(content_data.getString("sdpMid"), content_data.getInt("sdpMLineIndex"), content_data.getString("candidate")));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * sdp change
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public String videoSdpChanged(String psSdp, boolean pbBitrateChange) {
        String sdp = psSdp;

        if ( pbBitrateChange ) {
            sdp = mVideoOption.changeSdp(mVideoOption.changeSdp(sdp, "video", mVideoOption.mVideoBitrate), "audio", mVideoOption.mAudioBitrate);
        }

        sdp = mVideoOption.preferCodec(sdp,mVideoOption.mVideoCodec, false, mVideoOption.mMaxBitrate, mVideoOption.mMinBitrate, mVideoOption.mMaxBitrate);

        return sdp;
    }
}

package go.glass.realwear.hyundai.test.video;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.example.glass.ui.GlassGestureDetector;

import org.webrtc.EglBase;
import org.webrtc.RendererCommon;

import deepfine.customwebrtc.CustomCameraVideoCapturer;
import go.glass.realwear.hyundai.test.DeepFineApplication;
import go.glass.realwear.hyundai.test.R;
import go.glass.realwear.hyundai.test.databinding.ActivityVideoBinding;
import go.glass.realwear.hyundai.test.dialog.DeepFineAlertDialog;
import go.glass.realwear.hyundai.test.socket.SocketService;
import go.glass.realwear.hyundai.test.util.Common;
import go.glass.realwear.hyundai.test.util.NetworkManager;

public class VideoActivity extends VideoBaseActivity<ActivityVideoBinding> implements GlassGestureDetector.OnGestureListener {
    //==============================================================================================
    // Get Layout Id
    //==============================================================================================
    @Override
    protected int getLayoutId() { return R.layout.activity_video; }

    //==============================================================================================
    // Constant Define
    //==============================================================================================
    protected AudioManager mAudioManager;

    //==============================================================================================
    // LifeCycle
    //==============================================================================================
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);

        if ( getIntent() != null ) {
            mIntent = getIntent();
            mRoomId = mIntent.getStringExtra("room_id");
        }

        onBind();
    }

    private void onBind() {
        binding.setActivity(this);
        binding.setLifecycleOwner(this);
        videoActivityStart();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            GlassGestureDetector.Gesture keyValue = Common.getKeyEvent(event.getKeyCode());

            if (null != keyValue) {
                onGesture(keyValue);
            }
        }
        return true;
    }

    @Override
    public boolean onGesture(GlassGestureDetector.Gesture gesture) {
        try {
            switch (gesture) {
                case SWIPE_DOWN:
                    finishVideo();
                    return true;
            }
        }
        catch (Exception e){
            return false;
        }

        return false;
    }

    //==============================================================================================
    // User Function
    //==============================================================================================
    /**
     * 회의 종료 alert
     */
    private void finishVideo() {
        showAlertDialog("회의를 종료하시겠습니까?", getString(R.string.common_exit), getString(R.string.common_cancel), false, type -> {
            if (type.equals(DeepFineAlertDialog.DeepFineAlertDialogType.SUCCESS)) {
                videoConferenceFinish();
            }
        });
    }

    /**
     * 회의 시작
     */
    private void videoActivityStart() {
        mEglBase = EglBase.create();
        binding.surfaceview.setFpsReduction(mVideoOption.mVideoFps);
        binding.surfaceview.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        binding.surfaceview.setEnableHardwareScaler(true);
        binding.surfaceview.init(mEglBase.getEglBaseContext(), null);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setSpeakerphoneOn(true);
        mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0);

        binding.setOptionValue(DeepFineApplication.VIDEO_WIDTH + "*" + DeepFineApplication.VIDEO_HEIGHT + " F : " + DeepFineApplication.VIDEO_FPS + " C : " + DeepFineApplication.VIDEO_CODEC);

        userChange();
        videoConferenceBaseInit(mCameraOpenCallback);

        NetworkManager.getInstance(mContext).startNetworkCheck(new NetworkManager.NetworkSpeedListner() {
            @Override
            public void getSpeed(String psText) {
                runOnUiThread(()->{
                    binding.setNetworkStatus(psText);
                });
            }
        });
    }

    /**
     * 카메라 Open Listner
     */
    private CustomCameraVideoCapturer.CameraEventsHandler mCameraOpenCallback = new CustomCameraVideoCapturer.CameraEventsHandler() {
        @Override
        public void onCameraError(String var1) { }
        @Override
        public void onCameraDisconnected() { }
        @Override
        public void onCameraFreezed(String var1) { }
        @Override
        public void onCameraOpening(String var1) { }
        @Override
        public void onFirstFrameAvailable() {
            if (!SocketService.getInstance().getSocketStatus()) {
                mLocalVideoTrack.addSink(binding.surfaceview);
                socketInit();
            }
        }
        @Override
        public void onCameraClosed() { }
    };

    /**
     * 참여자 카운트 변경
     */
    @Override
    public void userChange() {
        binding.setUserCount((mUsers.size() + 1) + "명 참여중");
    }

    /**
     * 회의 종료
     */
    @Override
    public void videoConferenceFinish() {
        NetworkManager.getInstance(mContext).stopNetworkCheck();

        socketClose();

        for ( VideoConferenceUser data : mUsers ) {
            data.remove(mLocalAudioTrack, mLocalVideoTrack, null);
        }

        mUsers.clear();

        try {
            binding.surfaceview.release();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (null != mLocalVideoTrack) {
                mLocalVideoTrack.dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mLocalVideoTrack = null;
        }

        try {
            if (null != mLocalAudioTrack) {
                mLocalAudioTrack.dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mLocalAudioTrack = null;
        }

        try {
            if (null != mVideoCapturer) {
                mVideoCapturer.stopCapture();
                mVideoCapturer.dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mVideoCapturer = null;
        }

        try {
            if (null != mVideoSource) {
                mVideoSource.dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mVideoSource = null;
        }

        try {
            if (null != mAudioSource) {
                mAudioSource.dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mAudioSource = null;
        }

        if (null != mSurfaceTextureHelper) {
            mSurfaceTextureHelper.dispose();
        }

        mSurfaceTextureHelper = null;

        mVideoOption.clear();

        try {
            mEglBase.releaseSurface();
            mEglBase.release();
        } catch (Exception e) {
            e.printStackTrace();
        }

        finish();
    }
}

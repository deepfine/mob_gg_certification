package go.glass.realwear.hyundai.test;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.glass.ui.GlassGestureDetector;

import go.glass.realwear.hyundai.test.dialog.DeepFineAlertDialog;
import go.glass.realwear.hyundai.test.permission.CheckPermissions;
import go.glass.realwear.hyundai.test.permission.PermissionListener;
import go.glass.realwear.hyundai.test.socket.SocketService;
import go.glass.realwear.hyundai.test.util.Common;
import go.glass.realwear.hyundai.test.util.NetworkManager;
import go.glass.realwear.hyundai.test.video.VideoActivity;

public class MainActivity extends AppCompatActivity implements GlassGestureDetector.OnGestureListener, View.OnFocusChangeListener {
    private TextView textView;
    private TextView tvButton_0;
    private TextView tvButton_1;
    private TextView tvButton_2;
    private TextView tvButton_3;
    private TextView tvButton_4;
    private TextView tvButton_5;
    private TextView tvButton_6;
    private TextView tvButton_7;
    private TextView tvButton_8;
    private TextView tvButton_9;
    private TextView tvButtonReset;
    private TextView tvCall;
    private TextView tvSetting;
    private TextView tvSettingValue;

    private TextView[] arrTextViews = new TextView[]{};
    private int currentFocus = 11;
    protected DeepFineAlertDialog mDeepFineAlertDialog;

    private static final String[] permissions = new String[]{
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.INTERNET,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.WAKE_LOCK
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        tvButton_0 = findViewById(R.id.tvButton_0);
        tvButton_1 = findViewById(R.id.tvButton_1);
        tvButton_2 = findViewById(R.id.tvButton_2);
        tvButton_3 = findViewById(R.id.tvButton_3);
        tvButton_4 = findViewById(R.id.tvButton_4);
        tvButton_5 = findViewById(R.id.tvButton_5);
        tvButton_6 = findViewById(R.id.tvButton_6);
        tvButton_7 = findViewById(R.id.tvButton_7);
        tvButton_8 = findViewById(R.id.tvButton_8);
        tvButton_9 = findViewById(R.id.tvButton_9);
        tvButtonReset = findViewById(R.id.tvButtonReset);
        tvCall = findViewById(R.id.tvCall);
        tvSetting = findViewById(R.id.tvSetting);
        tvSettingValue = findViewById(R.id.tvSettingValue);

        textView.setText("1111");

        mDeepFineAlertDialog = new DeepFineAlertDialog(this);

        arrTextViews = new TextView[]{
                tvButton_0, tvButton_1, tvButton_2, tvButton_3, tvButton_4,
                tvButton_5, tvButton_6, tvButton_7, tvButton_8, tvButton_9, tvButtonReset, tvCall, tvSetting
        };

        for ( TextView tv : arrTextViews ) {
            tv.setFocusable(true);
            tv.setFocusableInTouchMode(true);
            tv.setOnFocusChangeListener(this);

            tv.setOnClickListener(v -> {
                textViewButtonClick((TextView)v);
            });
        }

        arrTextViews[currentFocus].post(()-> {
            arrTextViews[currentFocus].requestFocus();
            onFocusChange(arrTextViews[currentFocus], true);
        });

        checkPermission();
    }

    @SuppressLint("StringFormatMatches")
    @Override
    protected void onResume() {
        super.onResume();

        try {
            if ( null !=  SocketService.getInstance().getSocket() || SocketService.getInstance().getSocketStatus() ) {
                SocketService.getInstance().close();
            }
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }

        tvSettingValue.setText(String.format(getString(R.string.video_size_value), DeepFineApplication.VIDEO_WIDTH, DeepFineApplication.VIDEO_HEIGHT, DeepFineApplication.VIDEO_FPS, DeepFineApplication.VIDEO_CODEC));
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
                case SWIPE_UP:
                    startVideoCall();
                    return true;
                case SWIPE_DOWN:
                    finishApp();
                    return true;
                case TAP:
                    getCurrentFocus().performClick();
                    getCurrentFocus().setPressed(true);
                    return true;
                case SWIPE_BACKWARD:
                    currentFocus++;

                    if ( currentFocus >= arrTextViews.length ) {
                        currentFocus = 0;
                    }

                    arrTextViews[currentFocus].requestFocus();
                    return true;
                case SWIPE_FORWARD:
                    currentFocus--;

                    if ( currentFocus < 0 ) {
                        currentFocus = arrTextViews.length-1;
                    }

                    arrTextViews[currentFocus].requestFocus();
                    return true;
            }
        }
        catch (Exception e){
            return false;
        }

        return false;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        for ( TextView textView : arrTextViews ) {
            if ( textView.getId() == v.getId() && hasFocus ) {
                textView.setTextColor(getColor(R.color._FFFFFF));
                textView.setBackgroundColor(getColor(R.color._0089ff));
            }
            else {
                textView.setTextColor(getColor(R.color._000000));
                textView.setBackgroundColor(getColor(R.color._FFFFFF));
            }
        }
    }

    private void textViewButtonClick(TextView poView) {
        if (poView.getHint().toString().equals("reset")) {
            textView.setText("");
        }
        else if (poView.getHint().toString().equals("call")) {
            startVideoCall();
        }
        else if (poView.getHint().toString().equals("videoSetting")) {
            videoSetting();
        }
        else {
            textView.setText(textView.getText().toString() + poView.getHint());
        }
    }

    //==============================================================================================
    // Permissions
    //==============================================================================================
    private void checkPermission() {
        CheckPermissions.checkPermission(this, permissions, new PermissionListener() {
            @Override
            public void onPermissionAllGranted() {
                permissionAllGranted();
            }

            @Override
            public void onPermissionDenied() {
                permissionDenied();
            }
        });
    }

    private void permissionAllGranted() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
            }
        }, 2000);
    }

    private void permissionDenied() {
        finish();
    }

    /**
     * 앱 강제 종료
     */
    protected void finishApp() {
        showAlertDialog(getString(R.string.common_finish_app), getString(R.string.common_exit), getString(R.string.common_cancel), false, type -> {
            if (type.equals(DeepFineAlertDialog.DeepFineAlertDialogType.SUCCESS)) {
                // 소켓중지
                moveTaskToBack(true);
                finishAndRemoveTask();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
    }

    protected void startVideoCall() {
        String roomId = textView.getText().toString();

        if ( roomId.length() < 1 ) {
            Toast.makeText(this, "회의방 번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if ( !NetworkManager.getInstance(this).isConnection() ) {
            Toast.makeText(this, "네트워크 연결을 해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        showAlertDialog("회의방 번호 [ " + roomId + " ]\n\n" + getString(R.string.start_video_call), getString(R.string.common_connect), getString(R.string.common_cancel), false, type -> {
            if (type.equals(DeepFineAlertDialog.DeepFineAlertDialogType.SUCCESS)) {
                callVideo(roomId);
            }
        });
    }

    protected void videoSizeConfirm() {
        showAlertDialog("FULL HD 영상회의 진행 하시겠습니까?\n예) 1920*1080\n아니오) 1280*720", getString(R.string.common_exit), getString(R.string.common_cancel), false, type -> {
            if (type.equals(DeepFineAlertDialog.DeepFineAlertDialogType.SUCCESS)) {

            }
            else {

            }
        });
    }

    protected void callVideo(String psRoomId) {
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra("room_id", psRoomId);

        startActivity(intent);
    }

    protected void videoSetting() {
        Intent intent = new Intent(this, VideoSettingActivity.class);
        startActivity(intent);
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
}
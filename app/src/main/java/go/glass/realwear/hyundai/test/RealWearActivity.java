package go.glass.realwear.hyundai.test;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import go.glass.realwear.hyundai.test.databinding.ActivityRealwearBinding;
import go.glass.realwear.hyundai.test.permission.CheckPermissions;
import go.glass.realwear.hyundai.test.permission.PermissionListener;
import go.glass.realwear.hyundai.test.video.VideoActivity;

public class RealWearActivity extends AppCompatActivity {
    //==============================================================================================
    // Variable
    //==============================================================================================
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

    ActivityRealwearBinding binding;

    //==============================================================================================
    // Override
    //==============================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();
    }

    //==============================================================================================
    // User Function
    //==============================================================================================
    private void bind() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_realwear);
        binding.setView(this);
    }

    //==============================================================================================
    // Actions
    //==============================================================================================
    public void callVideo() {
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra("room_id", "7622");

        startActivity(intent);
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
        bind();
    }

    private void permissionDenied() {
        finish();
    }
}

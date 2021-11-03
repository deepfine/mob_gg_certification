package go.glass.realwear.hyundai.test.permission;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import go.glass.realwear.hyundai.test.R;
import go.glass.realwear.hyundai.test.dialog.DeepFineAlertDialog;

public class CheckPermissions extends AppCompatActivity {
    private static final int REQUEST_PERMISSIONS = 200;
    private static PermissionListener permissionListener;
    private static String[] permissions;

    private DeepFineAlertDialog mDeepFineAlertDialog;

    public static void checkPermission(Context context, String[] permissions, PermissionListener listener) {
        permissionListener = listener;
        CheckPermissions.permissions = permissions;
        Intent intent = new Intent(context, CheckPermissions.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        noTransitionAnimation();
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        checkPermission();
    }

    @Override
    public void finish() {
        super.finish();
        noTransitionAnimation();
    }

    @Override
    public void onBackPressed() {}

    private void noTransitionAnimation() {
        overridePendingTransition(0, 0);
    }

    //==============================================================================================
    // Permissions
    //==============================================================================================
    private void checkPermission() {
        if (checkExistDenyPermission()) {
            showAlertDialog(getString(R.string.permission_explain_message), getString(R.string.common_confirm), getString(R.string.common_cancel), new DeepFineAlertDialog.DeepFineAlertDialogListner() {
                @Override
                public void DeepFineAlertDialogCallback(DeepFineAlertDialog.DeepFineAlertDialogType poType) {
                    switch (poType) {
                        case SUCCESS:
                            ActivityCompat.requestPermissions(CheckPermissions.this, permissions, REQUEST_PERMISSIONS);
                            break;
                        case CANCEL:
                            mDeepFineAlertDialog.dismiss();
                            showPermissionDeniedAlert();
                            break;
                    }
                }
            });
        } else {
            onPermissionAllGranted();
        }
    }

    private boolean checkExistDenyPermission() {
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                if (grantResults.length > 0) {
                    for (int grantResult : grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            showPermissionDeniedAlert();
                            return;
                        }
                    }
                    onPermissionAllGranted();
                } else {
                    showPermissionDeniedAlert();
                }

                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSIONS) {
            checkPermission();
        }
    }

    private void showPermissionDeniedAlert() {
        new Handler().post(() ->
                showAlertDialog(getString(R.string.permission_denied_message), getString(R.string.common_close), getString(R.string.common_system_setting), new DeepFineAlertDialog.DeepFineAlertDialogListner() {
                    @Override
                    public void DeepFineAlertDialogCallback(DeepFineAlertDialog.DeepFineAlertDialogType poType) {
                        switch (poType) {
                            case SUCCESS:
                                permissionListener.onPermissionDenied();
                                finish();
                                break;
                            case CANCEL:
                                startSetting();
                                break;
                        }
                    }
                })
        );
    }

    private void startSetting() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, REQUEST_PERMISSIONS);
    }

    protected void showAlertDialog(String contentText, String confirmButtonText, String cancelButtonText, DeepFineAlertDialog.DeepFineAlertDialogListner callBackListener) {
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

        if (!mDeepFineAlertDialog.isShowing()) {
            mDeepFineAlertDialog.show();
        }
    }

    private void onPermissionAllGranted() {
        permissionListener.onPermissionAllGranted();
        finish();
    }
}

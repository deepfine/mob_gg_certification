package go.glass.realwear.hyundai.test.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.example.glass.ui.GlassGestureDetector;

import java.util.Objects;

import go.glass.realwear.hyundai.test.R;
import go.glass.realwear.hyundai.test.databinding.DialogDeepfineAlertBinding;
import lombok.Setter;

public class DeepFineAlertDialog extends Dialog implements GlassGestureDetector.OnGestureListener, View.OnFocusChangeListener {
    public enum DeepFineAlertDialogType {
        SUCCESS, CANCEL
    }

    public interface DeepFineAlertDialogListner {
        void DeepFineAlertDialogCallback(DeepFineAlertDialogType poType);
    }

    protected DialogDeepfineAlertBinding mBinding;
    protected GlassGestureDetector mGlassGestureDetector;

    @Setter
    protected boolean mOneButtonType = false;

    @Setter
    protected DeepFineAlertDialogListner mCallback;
    protected TextView[] mFocusView = null;
    protected int mCurrentFocusIdx = 0;

    @Setter
    protected String mStext = "";

    @Setter
    protected String mCtext = "";

    @Setter
    protected String mContent = "";

    public DeepFineAlertDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.dialog_deepfine_alert, null, false);
        mBinding.setDeepfineAlertDialog(this);
        setContentView(mBinding.getRoot());
        setCancelable(false);

        try {
            WindowManager.LayoutParams params = Objects.requireNonNull(getWindow()).getAttributes();

            params.width = (int)(getContext().getResources().getDisplayMetrics().density * 420);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            getWindow().setAttributes(params);
            getWindow().setBackgroundDrawable(ContextCompat.getDrawable(getContext(), R.drawable.style_dialog_deepfine_alert_background));
        } catch (NullPointerException npe) {
            Log.e(this.getClass().getSimpleName(), npe.getMessage());
        }


        mGlassGestureDetector = new GlassGestureDetector(getContext(), this);
    }

    @Override
    public void show() {
        super.show();
        init();
    }

    public void init() {
        mBinding.setContent(mContent);

        mBinding.setSuccessText(mStext.equals("") ? getContext().getString(R.string.common_confirm) : mStext);

        if ( mOneButtonType ) {
            mBinding.viewLine1.setVisibility(View.GONE);
            mBinding.tvOk.setVisibility(View.GONE);
            mBinding.tvCancel.setVisibility(View.GONE);
            mBinding.tvOne.setVisibility(View.VISIBLE);
        }
        else {
            mBinding.viewLine1.setVisibility(View.VISIBLE);
            mBinding.tvOk.setVisibility(View.VISIBLE);
            mBinding.tvCancel.setVisibility(View.VISIBLE);
            mBinding.tvOne.setVisibility(View.GONE);
            mBinding.setCancelText(mCtext.equals("") ? getContext().getString(R.string.common_cancel) : mCtext);
        }

        mCurrentFocusIdx = 0;
        mFocusView = new TextView[]{};

        if ( mOneButtonType ) {
            mFocusView = new TextView[]{mBinding.tvOne};
        }
        else {
            mFocusView = new TextView[]{mBinding.tvCancel, mBinding.tvOk};
        }

        for ( TextView textView : mFocusView ) {
            if ( textView.getId() == mBinding.tvOk.getId() ) {
                mBinding.tvOk.setText(mStext);
            }
            else if ( textView.getId() == mBinding.tvCancel.getId() ) {
                mBinding.tvCancel.setText(mCtext);
            }
            else if ( textView.getId() == mBinding.tvOne.getId() ) {
                mBinding.tvOne.setText(mStext);
            }

            textView.setFocusable(true);
            textView.setFocusableInTouchMode(true);
            textView.setOnFocusChangeListener(this);
        }

        mFocusView[mCurrentFocusIdx].post(() -> {
            mFocusView[mCurrentFocusIdx].requestFocus();
            onFocusChange(mFocusView[mCurrentFocusIdx], true);
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if ( null != mGlassGestureDetector ) {
            if ( mGlassGestureDetector.onTouchEvent(ev)) {
                return true;
            }
        }

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onGesture(GlassGestureDetector.Gesture gesture) {
        try {
            switch (gesture) {
                case TAP:
                    getCurrentFocus().performClick();
                    getCurrentFocus().setPressed(true);
                    return true;
                case SWIPE_UP:
                case SWIPE_DOWN:
                    return true;
                case SWIPE_BACKWARD:
                    mCurrentFocusIdx++;

                    if ( mCurrentFocusIdx >= mFocusView.length ) {
                        mCurrentFocusIdx = 0;
                    }

                    mFocusView[mCurrentFocusIdx].requestFocus();
                    return true;
                case SWIPE_FORWARD:
                    mCurrentFocusIdx--;

                    if ( mCurrentFocusIdx < 0 ) {
                        mCurrentFocusIdx = mFocusView.length-1;
                    }

                    mFocusView[mCurrentFocusIdx].requestFocus();
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
        for ( TextView textView : mFocusView ) {
            if ( textView.getId() == v.getId() && hasFocus ) {
                textView.setTextColor(getContext().getColor(R.color._FFFFFF));
                textView.setBackgroundColor(getContext().getColor(R.color._0089ff));
            }
            else {
                textView.setTextColor(getContext().getColor(R.color._000000));
                textView.setBackgroundColor(getContext().getColor(R.color._dddddd));
            }
        }
    }

    public void actionS() {
        mCallback.DeepFineAlertDialogCallback(DeepFineAlertDialogType.SUCCESS);
        dismiss();
    }

    public void actionC() {
        mCallback.DeepFineAlertDialogCallback(DeepFineAlertDialogType.CANCEL);
        dismiss();
    }
}

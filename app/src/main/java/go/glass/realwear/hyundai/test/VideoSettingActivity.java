package go.glass.realwear.hyundai.test;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.glass.ui.GlassGestureDetector;

import go.glass.realwear.hyundai.test.util.Common;

public class VideoSettingActivity extends AppCompatActivity implements GlassGestureDetector.OnGestureListener, View.OnFocusChangeListener {
    private int currentFocus = 0;

    private TextView tvWh;
    private TextView tvFps;
    private TextView tvCodec;
    private TextView tvWh_1;
    private TextView tvWh_2;
    private TextView tvWh_3;
    private TextView tvFps_1;
    private TextView tvFps_2;
    private TextView tvFps_3;
    private TextView tvFps_4;
    private TextView tvCodec_1;
    private TextView tvCodec_2;
    private TextView tvCodec_3;

    private TextView[] arrTextViews = new TextView[]{};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_setting);

        tvWh = findViewById(R.id.tvWh);
        tvFps = findViewById(R.id.tvFps);
        tvCodec = findViewById(R.id.tvCodec);

        tvWh_1 = findViewById(R.id.tvWh_1);
        tvWh_2 = findViewById(R.id.tvWh_2);
        tvWh_3 = findViewById(R.id.tvWh_3);
        tvFps_1 = findViewById(R.id.tvFps_1);
        tvFps_2 = findViewById(R.id.tvFps_2);
        tvFps_3 = findViewById(R.id.tvFps_3);
        tvFps_4 = findViewById(R.id.tvFps_4);
        tvCodec_1 = findViewById(R.id.tvCodec_1);
        tvCodec_2 = findViewById(R.id.tvCodec_2);
        tvCodec_3 = findViewById(R.id.tvCodec_3);

        arrTextViews = new TextView[]{
                tvWh_1, tvWh_2, tvWh_3,
                tvFps_1, tvFps_2, tvFps_3, tvFps_4,
                tvCodec_1, tvCodec_2, tvCodec_3
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        valueChange();
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

    @Override
    public boolean onGesture(GlassGestureDetector.Gesture gesture) {
        try {
            switch (gesture) {
                case SWIPE_UP:
                    return true;
                case SWIPE_DOWN:
                    finish();
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

    private void textViewButtonClick(TextView poView) {
        if (poView.getId() == R.id.tvWh_1) {
            DeepFineApplication.VIDEO_WIDTH = 1920;
            DeepFineApplication.VIDEO_HEIGHT = 1080;
        }
        else if (poView.getId() == R.id.tvWh_2) {
            DeepFineApplication.VIDEO_WIDTH = 1280;
            DeepFineApplication.VIDEO_HEIGHT = 720;
        }
        else if (poView.getId() == R.id.tvWh_3) {
            DeepFineApplication.VIDEO_WIDTH = 854;
            DeepFineApplication.VIDEO_HEIGHT = 480;
        }
        else if (poView.getId() == R.id.tvFps_1) {
            DeepFineApplication.VIDEO_FPS = 5;
        }
        else if (poView.getId() == R.id.tvFps_2) {
            DeepFineApplication.VIDEO_FPS = 10;
        }
        else if (poView.getId() == R.id.tvFps_3) {
            DeepFineApplication.VIDEO_FPS = 15;
        }
        else if (poView.getId() == R.id.tvFps_4) {
            DeepFineApplication.VIDEO_FPS = 20;
        }
        else if (poView.getId() == R.id.tvCodec_1) {
            DeepFineApplication.VIDEO_CODEC = "VP8";
        }
        else if (poView.getId() == R.id.tvCodec_2) {
            DeepFineApplication.VIDEO_CODEC = "VP9";
        }
        else if (poView.getId() == R.id.tvCodec_3) {
            DeepFineApplication.VIDEO_CODEC = "H264";
        }

        valueChange();
    }

    private void valueChange() {
        tvWh.setText(DeepFineApplication.VIDEO_WIDTH + " * " + DeepFineApplication.VIDEO_HEIGHT);
        tvFps.setText("Fps : " + DeepFineApplication.VIDEO_FPS);
        tvCodec.setText("Fps : " + DeepFineApplication.VIDEO_CODEC);
    }
}

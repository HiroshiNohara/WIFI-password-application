package com.app.wifipassword.zxing.android;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.AppCompatImageView;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.app.wifipassword.WifiActivity;
import com.google.zxing.Result;
import com.app.wifipassword.R;
import com.app.wifipassword.zxing.bean.ZxingConfig;
import com.app.wifipassword.zxing.camera.CameraManager;
import com.app.wifipassword.zxing.common.Constant;
import com.app.wifipassword.zxing.decode.DecodeImgCallback;
import com.app.wifipassword.zxing.decode.DecodeImgThread;
import com.app.wifipassword.zxing.decode.ImageUtil;
import com.app.wifipassword.zxing.view.ViewfinderView;

import java.io.IOException;

public class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {

    public ZxingConfig config;
    private SurfaceView previewView;
    private ViewfinderView viewfinderView;
    private AppCompatImageView flashLightButton;
    private boolean hasSurface;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private CameraManager cameraManager;
    private com.app.wifipassword.zxing.android.CaptureActivityHandler handler;
    private SurfaceHolder surfaceHolder;
    private String launchMethod;

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }


    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent launchIntent = getIntent();
        launchMethod = launchIntent.getStringExtra("launch_method");
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.BLACK);
        }

        try {
            config = (ZxingConfig) getIntent().getExtras().get(Constant.INTENT_ZXING_CONFIG);
        } catch (Exception e) {
        }

        if (config == null) {
            config = new ZxingConfig();
        }

        setContentView(R.layout.activity_capture);

        initView();

        hasSurface = false;

        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);
        beepManager.setPlayBeep(config.isPlayBeep());
        beepManager.setVibrate(config.isShake());


    }


    private void initView() {
        previewView = findViewById(R.id.preview_view);
        previewView.setOnClickListener(this);

        viewfinderView = findViewById(R.id.viewfinder_view);
        viewfinderView.setZxingConfig(config);

        flashLightButton = findViewById(R.id.flashLight_button);
        flashLightButton.setOnClickListener(this);

        if (isSupportCameraLedFlash(getPackageManager())) {
            flashLightButton.setVisibility(View.VISIBLE);
        } else {
            flashLightButton.setVisibility(View.GONE);
        }

    }

    public static boolean isSupportCameraLedFlash(PackageManager pm) {
        if (pm != null) {
            FeatureInfo[] features = pm.getSystemAvailableFeatures();
            if (features != null) {
                for (FeatureInfo f : features) {
                    if (f != null && PackageManager.FEATURE_CAMERA_FLASH.equals(f.name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void switchFlashImg(int flashState) {

        if (flashState == Constant.FLASH_OPEN) {
            flashLightButton.setImageResource(R.drawable.ic_open);
        } else {
            flashLightButton.setImageResource(R.drawable.ic_close);
        }

    }

    public void handleDecode(Result rawResult) {

        inactivityTimer.onActivity();
        //beepManager.playBeepSoundAndVibrate();
        Intent intent = getIntent();
        if (rawResult.toString().contains("WIFI:") && rawResult.toString().contains("T:") && rawResult.toString().contains("S:") && rawResult.toString().contains("P:")) {
            if (launchMethod != null) {
                intent.putExtra("QRMessage", rawResult.getText());
                setResult(RESULT_OK, intent);
                this.finish();
            } else {
                Intent WifiIntent = new Intent(CaptureActivity.this, WifiActivity.class);
                WifiIntent.putExtra("QRMessage", rawResult.getText());
                WifiIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(WifiIntent);
            }
        } else {
            Toast.makeText(this, R.string.retry_scan, Toast.LENGTH_SHORT).show();
            final Handler mHandler = new Handler();
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    handler.restartPreviewAndDecode();
                }
            }, 3000);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        cameraManager = new CameraManager(getApplication(), config);

        viewfinderView.setCameraManager(cameraManager);
        handler = null;

        surfaceHolder = previewView.getHolder();
        if (hasSurface) {

            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
        }

        beepManager.updatePrefs();
        inactivityTimer.onResume();

    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager);
            }
        } catch (IOException ioe) {
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, new com.app.wifipassword.zxing.android.FinishListener(this));
        builder.setOnCancelListener(new com.app.wifipassword.zxing.android.FinishListener(this));
        builder.show();
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        beepManager.close();
        cameraManager.closeDriver();

        if (!hasSurface) {

            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        viewfinderView.stopAnimator();
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void onClick(View view) {

        int id = view.getId();
        if (id == R.id.flashLight_button) {
            cameraManager.switchFlashLight(handler);
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constant.REQUEST_IMAGE && resultCode == RESULT_OK) {
            String path = ImageUtil.getImageAbsolutePath(this, data.getData());

            new DecodeImgThread(path, new DecodeImgCallback() {
                @Override
                public void onImageDecodeSuccess(Result result) {
                    handleDecode(result);
                }

                @Override
                public void onImageDecodeFailed() {
                    Toast.makeText(CaptureActivity.this, R.string.scan_failed_tip, Toast.LENGTH_SHORT).show();
                }
            }).run();

        }
    }

}

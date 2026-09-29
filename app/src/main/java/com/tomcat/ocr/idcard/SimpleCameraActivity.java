package com.tomcat.ocr.idcard;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.ocr.decode.OcrDecode;
import com.ocr.decode.OcrDecodeCallback;
import com.ocr.decode.OcrDecodeFactory;

import java.io.ByteArrayOutputStream;

public class SimpleCameraActivity extends Activity {

    private Context context;
    private SurfaceHolder surfaceHolder;
    private ViewfinderView camera_finderView;

    private CameraManager cameraManager;
    private SurfaceView camera_sv;
    private View takePictureButton;

    private OcrDecode ocrDecode;
    private boolean saveImage;
    private int ocrType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(layoutParams);
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        hideSystemBars();

        context = this;

        setContentView(R.layout.activity_simple_camera);


        Intent intent = getIntent();
        saveImage = intent.getBooleanExtra("saveImage", false);
        ocrType = intent.getIntExtra("type", 0);


        camera_sv = findViewById(R.id.camera_sv);
        camera_finderView = findViewById(R.id.camera_finderView);
        surfaceHolder = camera_sv.getHolder();

        takePictureButton = findViewById(R.id.takePicture);
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                take();
            }
        });

        findViewById(R.id.bt_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        //1. 初始化, 建议在Application onCreate中初始化, 只需要调用一次即可
        //OcrDecodeFactory.initOCR(context);

        //2. 创建OCR引擎
        ocrDecode = OcrDecodeFactory.newBuilder(context)
                .saveImage(saveImage)       //是否保存图片, 仅身份证模式有效, 表示自动裁剪身份证头像
                .ocrType(ocrType)           //0身份证, 1驾驶证, 2护照
                .build();


        if(ocrType == 2){
            //如果是扫描护照 , 则放大扫描框
            camera_finderView.setMarginSize(getResources().getDimension(com.msd.ocr.idcard.R.dimen.public_20_dp));
        }

    }

    private ProgressDialog progressDialog;
    private static final long FOCUS_TIMEOUT_MS = 3000L;
    private boolean isTake;
    private boolean isWaitingForFocus;

    private final Runnable focusTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (isTake && isWaitingForFocus) {
                Log.w("ocr", "Manual focus timed out");
                resumeCameraPreview();
                Toast.makeText(context, com.msd.ocr.idcard.R.string.focus_failed,
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void take(){
        if (isTake || cameraManager == null || !cameraManager.isOpen()) {
            return;
        }

        final Camera camera = cameraManager.getCamera();
        if (camera == null) {
            return;
        }

        isTake = true;
        isWaitingForFocus = true;
        setTakePictureEnabled(false);
        cameraManager.stopFocus();

        try {
            camera.cancelAutoFocus();
            Camera.Parameters parameters = camera.getParameters();
            if (parameters.getSupportedFocusModes() != null
                    && parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                CameraConfigurationUtils.setFocusArea(parameters);
                CameraConfigurationUtils.setMetering(parameters);
                camera.setParameters(parameters);
                handler.postDelayed(focusTimeoutRunnable, FOCUS_TIMEOUT_MS);
                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera focusedCamera) {
                        handler.removeCallbacks(focusTimeoutRunnable);
                        if (!isTake || !isWaitingForFocus) {
                            return;
                        }
                        isWaitingForFocus = false;
                        if (success) {
                            takeFocusedPicture();
                        } else {
                            Log.w("ocr", "Manual focus failed");
                            resumeCameraPreview();
                            Toast.makeText(context, com.msd.ocr.idcard.R.string.focus_failed,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                // Fixed-focus devices cannot run autoFocus.
                isWaitingForFocus = false;
                takeFocusedPicture();
            }
        } catch (RuntimeException e) {
            Log.e("ocr", "Unable to start manual focus", e);
            resumeCameraPreview();
            Toast.makeText(context, com.msd.ocr.idcard.R.string.focus_failed,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void takeFocusedPicture() {
        try {
            cameraManager.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(final byte[] jpeg, Camera camera) {
                    if (!isTake) {
                        return;
                    }
                    if (jpeg == null || jpeg.length == 0) {
                        handleCaptureFailure("Camera returned an empty JPEG", null);
                        return;
                    }

                    showProgressDialog();
                    final Rect scanFrame = new Rect(camera_finderView.getFinder());
                    final int finderWidth = camera_finderView.getWidth();
                    final int finderHeight = camera_finderView.getHeight();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            decodeCroppedPicture(jpeg, scanFrame, finderWidth, finderHeight);
                        }
                    }, "simple-ocr-photo-crop").start();
                }
            });
        } catch (RuntimeException e) {
            handleCaptureFailure("Unable to take picture", e);
        }
    }

    private void decodeCroppedPicture(byte[] jpeg, Rect scanFrame,
                                      int finderWidth, int finderHeight) {
        Bitmap source = null;
        Bitmap cropped = null;
        try {
            source = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
            if (source == null) {
                throw new IllegalStateException("Unable to decode captured JPEG");
            }

            Rect cropRect = calculatePhotoCropRect(scanFrame, finderWidth, finderHeight,
                    source.getWidth(), source.getHeight());
            cropped = Bitmap.createBitmap(source, cropRect.left, cropRect.top,
                    cropRect.width(), cropRect.height());

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!cropped.compress(Bitmap.CompressFormat.JPEG, 100, output)) {
                throw new IllegalStateException("Unable to encode cropped JPEG");
            }

            byte[] croppedJpeg = output.toByteArray();
            Rect fullCroppedImage = new Rect(0, 0, cropped.getWidth(), cropped.getHeight());
            ocrDecode.decode(croppedJpeg, fullCroppedImage, callback);
        } catch (RuntimeException | OutOfMemoryError e) {
            handleCaptureFailure("Unable to crop or decode captured image", e);
        } finally {
            if (cropped != null && cropped != source && !cropped.isRecycled()) {
                cropped.recycle();
            }
            if (source != null && !source.isRecycled()) {
                source.recycle();
            }
        }
    }

    private Rect calculatePhotoCropRect(Rect frame, int viewWidth, int viewHeight,
                                        int photoWidth, int photoHeight) {
        if (frame == null || viewWidth <= 0 || viewHeight <= 0
                || photoWidth <= 1 || photoHeight <= 1) {
            throw new IllegalStateException("Invalid view or photo dimensions");
        }

        float scaleX = photoWidth / (float) viewWidth;
        float scaleY = photoHeight / (float) viewHeight;
        int left = clamp(Math.round(frame.left * scaleX), 0, photoWidth - 1);
        int top = clamp(Math.round(frame.top * scaleY), 0, photoHeight - 1);
        int right = clamp(Math.round(frame.right * scaleX), left + 1, photoWidth);
        int bottom = clamp(Math.round(frame.bottom * scaleY), top + 1, photoHeight);
        return new Rect(left, top, right, bottom);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private void handleCaptureFailure(final String message, final Throwable error) {
        if (error == null) {
            Log.e("ocr", message);
        } else {
            Log.e("ocr", message, error);
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                resumeCameraPreview();
                Toast.makeText(context, com.msd.ocr.idcard.R.string.parse_error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resumeCameraPreview() {
        handler.removeCallbacks(focusTimeoutRunnable);
        dismissProgressDialog();
        isTake = false;
        isWaitingForFocus = false;
        setTakePictureEnabled(true);

        if (cameraManager == null || !cameraManager.isOpen()) {
            return;
        }
        try {
            cameraManager.resumePreview(previewCallback);
        } catch (RuntimeException e) {
            Log.e("ocr", "Unable to resume camera preview", e);
        }
    }

    private void showProgressDialog() {
        dismissProgressDialog();
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(getString(com.msd.ocr.idcard.R.string.parsing));
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void setTakePictureEnabled(boolean enabled) {
        if (takePictureButton != null) {
            takePictureButton.setEnabled(enabled);
        }
    }

    private boolean hasSurface;
    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        cameraManager = new CameraManager();


        if (hasSurface) {
            // activity在paused时但不会stopped,因此surface仍旧存在；
            // surfaceCreated()不会调用，因此在这里初始化camera
            initCamera(surfaceHolder);
        } else {
            // 重置callback，等待surfaceCreated()来初始化camera
            surfaceHolder.addCallback(surfaceHolderCallback);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }

        if (cameraManager.isOpen()) {
            return;
        }

        try {
            // 打开Camera硬件设备
            cameraManager.openDriver(surfaceHolder, this);
            // 创建一个handler来打开预览，并抛出一个运行时异常
            cameraManager.startPreview(previewCallback);

            Camera camera = cameraManager.getCamera();
            Camera.Size size = camera.getParameters().getPreviewSize();
            camera_finderView.initFinder(size.width, size.height, handler);
        } catch (Exception ioe) {
            Log.d("zk", ioe.toString());
        }
    }

    private Handler handler = new Handler(Looper.getMainLooper());



    private final Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(final byte[] data, final Camera camera) {
            // Preview frames are only used to keep the legacy autofocus cycle alive.
            // Recognition always uses a focused still JPEG.
        }
    };

    private OcrDecodeCallback callback = new OcrDecodeCallback() {
        @Override
        public void onSuccess(final String json) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    resumeCameraPreview();
                    Toast.makeText(context, "解析成功: " + json, Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onFail(final int cause) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    resumeCameraPreview();
                    Toast.makeText(context, "解析失败: " + cause, Toast.LENGTH_LONG).show();
                }
            });
        }
    };


    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(focusTimeoutRunnable);
        isTake = false;
        isWaitingForFocus = false;
        setTakePictureEnabled(true);
        dismissProgressDialog();
        if (cameraManager != null) {
            cameraManager.stopPreview();
            cameraManager.closeDriver();
        }
        if (!hasSurface) {
            surfaceHolder.removeCallback(surfaceHolderCallback);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
        }
    }

    private void hideSystemBars() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    
    private SurfaceHolder.Callback surfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (!hasSurface) {
                hasSurface = true;
                initCamera(holder);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            hasSurface = false;
        }
    };

}

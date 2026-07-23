package com.tomcat.ocr.idcard;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.ocr.decode.OcrDecode;
import com.ocr.decode.OcrDecodeCallback;
import com.ocr.decode.OcrDecodeFactory;

import java.io.ByteArrayOutputStream;

public class SimpleCameraActivity extends Activity {

    private Context context;
    private SurfaceHolder surfaceHolder;
    private ViewfinderView camera_finderView;

    private CameraManager cameraManager;
    private Camera mCamera;
    private SurfaceView camera_sv;

    private OcrDecode ocrDecode;
    private boolean saveImage;
    private int ocrType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        setContentView(R.layout.activity_simple_camera);


        Intent intent = getIntent();
        saveImage = intent.getBooleanExtra("saveImage", false);
        ocrType = intent.getIntExtra("type", 0);


        camera_sv = findViewById(R.id.camera_sv);
        camera_finderView = findViewById(R.id.camera_finderView);
        surfaceHolder = camera_sv.getHolder();

        findViewById(R.id.takePicture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                take();
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
    private boolean isTake;
    private void take(){
        if(isTake){
            return;
        }
        if(progressDialog != null && progressDialog.isShowing()){
            progressDialog.dismiss();
        }
        progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(getString(com.msd.ocr.idcard.R.string.parsing));
        progressDialog.show();
        isTake = true;



//        //调用相机拍照, 需要预先设定相机的拍照大小, 再根据屏幕的大小, 比率计算出实际预览区域, 再裁剪出来送去识别.
//        cameraManager.takePicture(null, null, new Camera.PictureCallback() {
//            @Override
//            public void onPictureTaken(byte[] jpeg, Camera camera) {
//                cameraManager.stopPreview();
//
//                Camera.Parameters parameters = camera.getParameters();
//                Rect rect  =  camera_finderView.getViewfinder(camera);
//
//                //3. 传递相机数据,请求解码, 注意这个Rect 就是画面预览的框框, 因此我们需要裁剪一下图片, 再去识别.
//                Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
//                ocrDecode.decode(bitmapCutToByte(bitmap, rect), callback);
//            }
//        });
    }

    private boolean hasSurface;
    @Override
    protected void onResume() {
        super.onResume();
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



    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(final byte[] data, final Camera camera) {
            if(isTake) {
                isTake = false;
                Camera.Parameters parameters = camera.getParameters();
                Rect rect  =  camera_finderView.getViewfinder(camera);
                //传递相机数据,请求解码, 注意这个Rect 就是画面预览的框框, 因此我们需要裁剪一下图片, 再去识别.
                FastYUVtoRGB fastYUVtoRGB = new FastYUVtoRGB(context);

                //将相机预览的YUV数据转换成Bitmap
                Bitmap bitmap = fastYUVtoRGB.convertYUVtoRGB(data, parameters.getPreviewSize().width, parameters.getPreviewSize().height);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                //再裁剪Bitmap(根据界面的预览框进行裁剪)后转换成 byte[] jpeg格式
                ocrDecode.decode(baos.toByteArray(), rect, callback);
            }
        }
    };

    private OcrDecodeCallback callback = new OcrDecodeCallback() {
        @Override
        public void onSuccess(final String json) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "解析成功: " + json, Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();
                }
            });
        }

        @Override
        public void onFail(final int cause) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "解析失败: " + cause, Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();
                }
            });
        }
    };


    @Override
    protected void onPause() {
        super.onPause();
        cameraManager.stopPreview();
        cameraManager.closeDriver();
        if (!hasSurface) {
            surfaceHolder.removeCallback(surfaceHolderCallback);
        }
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

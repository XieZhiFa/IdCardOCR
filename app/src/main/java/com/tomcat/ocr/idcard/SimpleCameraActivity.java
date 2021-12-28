package com.tomcat.ocr.idcard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.msd.ocr.idcard.LibraryInitOCR;

public class SimpleCameraActivity extends Activity {

    private Context context;
    private SurfaceHolder surfaceHolder;
    private ViewfinderView camera_finderView;

    private CameraManager cameraManager;
    private Camera mCamera;
    private SurfaceView camera_sv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;

        setContentView(R.layout.activity_simple_camera);
        camera_sv = findViewById(R.id.camera_sv);
        camera_finderView = findViewById(R.id.camera_finderView);
        surfaceHolder = camera_sv.getHolder();


        // 1. 初始化lib库(需要授权)
        LibraryInitOCR.initOCR(context);


        // 2. 初始化解码器
        LibraryInitOCR.initDecode(context, handler, false);

        //请求相机权限, 实际开中, 请先申请了权限再转跳到扫描界面.
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


    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(final byte[] data, final Camera camera) {

            Camera.Parameters parameters = camera.getParameters();
            //Rect rect  =  new Rect(368, 144, 1549, 936);

            Rect rect  =  camera_finderView.getViewfinder(camera);

            //3. 传递相机数据,请求解码
            LibraryInitOCR.decode(rect, parameters.getPreviewSize().width, parameters.getPreviewSize().height, data);
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




    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            //4. 接收解码回调
            Log.i("ocr", "Handler: " + msg.what);

            switch (msg.what){
                case LibraryInitOCR.DECODE_SUCCESS:{
                    Log.i("ocr", "成功: " + msg.obj.toString());
                    Intent result = (Intent) msg.obj;
                    String ocrReulst = result.getStringExtra("OCRResult");
                    Toast.makeText(context, "解析成功: " + ocrReulst, Toast.LENGTH_LONG).show();
                    break;
                }

                default:{
                    break;
                }
            }
        }
    };


    
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

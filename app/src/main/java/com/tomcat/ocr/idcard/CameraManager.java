package com.tomcat.ocr.idcard;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

public class CameraManager {
    private static final String TAG = CameraManager.class.getName();
    private Camera camera;
    private Camera.Parameters parameters;
    private AutoFocusManager autoFocusManager;
    private int requestedCameraId = -1;

    private boolean initialized;
    private boolean previewing;

    private Size mPreviewSize;
    private List<Size> mSupportedPreviewSizes;

    public Camera getCamera(){
        return this.camera;
    }

    /**
     * 打开摄像头
     *
     * @param cameraId 摄像头id
     * @return Camera
     */
    public Camera open(int cameraId, Context mContext) {
        int nucameras = Camera.getNumberOfCameras();
        if (nucameras == 0) {
            Log.e(TAG, "No cameras!");
            return null;
        }
        boolean explicitRequest = cameraId >= 0;
        if (!explicitRequest) {
            // Select a camera if no explicit camera requested
            int index = 0;
            while (index < nucameras) {
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(index, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    break;
                }
                index++;
            }
            cameraId = index;
        }
        Camera camera;
        if (cameraId < nucameras) {
            Log.e(TAG, "Opening camera #" + cameraId);
            camera = Camera.open(cameraId);
        } else {
            if (explicitRequest) {
                Log.e(TAG, "Requested camera does not exist: " + cameraId);
                camera = null;
            } else {
                Log.e(TAG, "No camera facing back; returning camera #0");
                camera = Camera.open(0);
            }
        }
        mSupportedPreviewSizes = camera.getParameters()
                .getSupportedPreviewSizes();
        if (mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(
                    mSupportedPreviewSizes, getScreenWidth(mContext), getScreenHeight(mContext));
        }
        Log.d("zkcam", "w=" + getScreenWidth(mContext));
        Log.d("zkcam", "h=" + getScreenHeight(mContext));
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewSize(mPreviewSize.width,
                mPreviewSize.height);
        Log.d("zkcam", "pw=" + mPreviewSize.width);
        Log.d("zkcam", "ph=" + mPreviewSize.height);
        camera.setParameters(parameters);
        return camera;
    }

    public Camera.Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.2;
        double targetRatio = (double) w / h;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }


        return optimalSize;
    }

    /**
     * 打开camera
     *
     * @param holder SurfaceHolder
     * @throws IOException IOException
     */
    public synchronized void openDriver(SurfaceHolder holder, Context mContext)
            throws IOException {
        Log.e(TAG, "openDriver");
        Camera theCamera = camera;
        if (theCamera == null) {
            theCamera = open(requestedCameraId, mContext);
            if (theCamera == null) {
                throw new IOException();
            }
            camera = theCamera;
        }
        theCamera.setPreviewDisplay(holder);

//        if (!initialized) {
//            initialized = true;
//            parameters = camera.getParameters();
//            parameters.setPreviewSize(800, 600);
//            parameters.setPictureFormat(ImageFormat.JPEG);
//            parameters.setJpegQuality(100);
//            parameters.setPictureSize(800, 600);
////            theCamera.setParameters(parameters);
//        }
    }

    /**
     * camera是否打开
     *
     * @return camera是否打开
     */
    public synchronized boolean isOpen() {
        return camera != null;
    }

    /**
     * 关闭camera
     */
    public synchronized void closeDriver() {
        Log.e(TAG, "closeDriver");
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    /**
     * 开始预览
     */
    public synchronized void startPreview(Camera.PreviewCallback mp) {
        Log.e(TAG, "startPreview");
        Camera theCamera = camera;
        if (theCamera != null && !previewing) {
//            theCamera.setOneShotPreviewCallback(mp);
            theCamera.startPreview();
            previewing = true;
            autoFocusManager = new AutoFocusManager(camera, mp);
        }
//        camera.setPreviewCallback(new Camera.PreviewCallback() {
//            @Override
//            public void onPreviewFrame(byte[] data, Camera camera) {
//                Log.d("zka","onPreviewFrame");
//            }
//        });
    }

    /**
     * 关闭预览
     */
    public synchronized void stopPreview() {
        Log.e(TAG, "stopPreview");
        if (autoFocusManager != null) {
            autoFocusManager.stop();
            autoFocusManager = null;
        }
        if (camera != null && previewing) {
            camera.stopPreview();
            previewing = false;
        }
    }


    public void startFocus(){
        if(autoFocusManager != null) {
            autoFocusManager.start();
        }
    }

    /**
     * 打开闪光灯
     */
    public synchronized void openLight() {
        Log.e(TAG, "openLight");
        if (camera != null) {
            parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(parameters);
        }
    }

    /**
     * 关闭闪光灯
     */
    public synchronized void offLight() {
        Log.e(TAG, "offLight");
        if (camera != null) {
            parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(parameters);
        }
    }

    /**
     * 拍照
     *
     * @param shutter ShutterCallback
     * @param raw     PictureCallback
     * @param jpeg    PictureCallback
     */
    public synchronized void takePicture(final Camera.ShutterCallback shutter, final Camera.PictureCallback raw,
                                         final Camera.PictureCallback jpeg) {

        camera.takePicture(shutter, raw, jpeg);


    }

    public int getScreenWidth(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }

    /**
     * 获得屏幕高度
     *
     * @param context 上下文
     * @return 屏幕除去通知栏的高度
     */
    public int getScreenHeight(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.heightPixels;
    }
}

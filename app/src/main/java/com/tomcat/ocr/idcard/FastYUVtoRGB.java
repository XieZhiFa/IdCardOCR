package com.tomcat.ocr.idcard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

import java.io.ByteArrayOutputStream;

public class FastYUVtoRGB {
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;

    public FastYUVtoRGB(Context context) {
        rs = RenderScript.create(context);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }


    public Bitmap convertYUVtoRGB(byte[] yuvData, int width, int height) {
        if (yuvType == null) {
            yuvType = new Type.Builder(rs, Element.U8(rs)).setX(yuvData.length);
            in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

            rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
            out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
        }
        in.copyFrom(yuvData);
        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        Bitmap bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyTo(bmpout);
        return bmpout;
    }

    public Bitmap zoomBitmap(Bitmap bitmap){
        int targetWidth = 480;                          //表示把图片压缩到这个大小
        float width = bitmap.getWidth();
        float height = bitmap.getHeight();
        if(width < targetWidth && height < targetWidth){    //当前的人脸小于指定大小
            return bitmap;
        }

        double scale = 1; 							    // 记录图片压缩比率
        if (width > height) { 						    // 如果宽比高大,并且大于720,则将压缩比率计算为宽除于640
            scale = width / targetWidth;
        } else if (height > width) { 					// 如果宽比高大,并且大于720,则将压缩比率计算为高除于640
            scale = height / targetWidth;
        }else{
            scale = width / targetWidth;                //宽高相等的话, 无所谓了
        }

        return zoomBitmap(bitmap, width / scale, height / scale);
    }

    /**
     * 缩放图片
     * @param bitmap	原图
     * @param width		你想要缩放的宽
     * @param height	你想要缩放的高
     * @return
     */
    public static Bitmap zoomBitmap(Bitmap bitmap, double width, double height) {
        int w = bitmap.getWidth();							//获取位图的宽
        int h = bitmap.getHeight();							//获取位图的高

        float scaleWidth = ((float) width / w);
        float scaleHeight = ((float) height / h);			//设置缩放倍数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
    }


    public static byte[] bitmapCutToByte(Bitmap bitmap, Rect rect){
        bitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        return bos.toByteArray();
    }
}

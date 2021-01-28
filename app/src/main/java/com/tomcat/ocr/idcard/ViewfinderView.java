package com.tomcat.ocr.idcard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;


/**
 * 视频取景器控件
 *
 * @author fangcm 2012-09-06
 *
 */
public class ViewfinderView extends View {

	private int width , height;
	private Paint paint;
	private Context mContext;
	private int mWidth,mHeight;
	private float lineLeft,lineRight,lineTop,lineBottom;
	private int lineModel = 0;
	private float marginW = 0f;
	private float marginH = 0f;
	private float marginT = 0f;
	private int dLineWidth = 12;
	private int dLen = 60;
	private int m_nImageWidth;
	private int m_nImageHeight;

	boolean l = false, r = false, t = false, b = false , L = false;

	public ViewfinderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.mContext = context;
	}


	public ViewfinderView(Context context){
		super(context);
		this.mContext = context;
	}

	public ViewfinderView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.mContext = context;
	}

	/**
	 * 大小  85.6*54
	 * 长是宽的1.58倍
	 * @param pWidth
	 * @param pHeight
	 */
	public void initFinder(int pWidth,int pHeight,Handler mHandler){
		m_nImageWidth = pWidth;
		m_nImageHeight = pHeight;
		WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		width = display.getWidth();
		height = display.getHeight();
		Log.d("tag", "-1-------->>"+width);

		marginT = mContext.getResources().getDimension(com.msd.ocr.idcard.R.dimen.public_48_dp);

		marginW = (float) ((width - pWidth)/2.0);
		marginH = (float) ((height - pHeight)/2.0);

		mWidth = width/2;
		mHeight = height/2;

		float g = height - marginT * 2;
		float k = g* 1.58f;
		float x = 10.0f;
		Log.d("ocr", k+"<<--k---高度----g--1---->>"+g);
		while(k > pWidth){
			x --;
			k = k * (x/10.0f);
			g = g * (x/10.0f);
		}
		Log.d("ocr", k+"<<--k---高度----g--2---->>"+g);
		lineLeft = (float) (mWidth - k/2.0);
		lineRight = (float) (mWidth + k/2.0);
		lineTop = (float) (mHeight - g/2.0);
		lineBottom = (float) (mHeight + g/2.0);




		int nDisplayWidth = display.getWidth();
		int nDisplayHeight = display.getHeight();

		int nImageWidth = m_nImageWidth;
		int nImageHeight  = m_nImageHeight;
		double nFitWidth;
		double nFitHeight;
		double nUseWidth = 0;
		double nUseHeight = 0;
		double dRealRegionWidth = 0;
//		double dRealRegionHeight = 0;
		if(nImageWidth*nDisplayHeight < nDisplayWidth*nImageHeight){
			nFitHeight = nDisplayHeight;
			nFitWidth = (nImageWidth/(double)nImageHeight)*nFitHeight;
		}else{
			nFitWidth = nDisplayWidth;
			nFitHeight = nFitWidth*(nImageHeight/(double)nImageWidth);
		}
		if(nFitWidth/nFitHeight >= 4/3){
			nUseHeight = nFitHeight;
			nUseWidth = 4*nUseHeight/3.0f;
		}else{
			nUseWidth = nFitWidth;
			nUseHeight = 3*nUseWidth/4.0f;
		}
		dRealRegionWidth = nUseWidth/480.0f*420.0f;
//		dRealRegionHeight = nUseHeight/360.0f*270.0f;

//		lineLeft = (int)((nDisplayWidth - dRealRegionWidth)/2.0f);//- (nDisplayWidth - nFitWidth)/2.0f);
//		lineRight = (int)( nDisplayWidth - lineLeft);//- (nDisplayWidth - nFitWidth) );
//		lineTop = (int)(nDisplayHeight - dRealRegionHeight)/2.0f;
//		lineBottom = nDisplayHeight - lineTop;


		paint = new Paint();
		dLineWidth = (int)dRealRegionWidth/28; 	//30
		dLineWidth = 4;
		paint.setStrokeWidth(dLineWidth);
		dLen = (int)dRealRegionWidth/6; 		//160

	}


	public void initFinder(int w,int h,int d){}

	public Rect getFinder(){
		return new Rect((int)(lineLeft - marginW), (int)(lineTop - marginH), (int)(lineRight + marginW), (int)(lineBottom + marginH));
	}



	public Rect getViewfinder(Camera camera) {
		Rect finderRect = getFinder();
		WindowManager windowManager = (WindowManager)this.mContext.getSystemService(Context.WINDOW_SERVICE);
		Display display = windowManager.getDefaultDisplay();
		float w = (float)display.getWidth();
		float h = (float)display.getHeight();
		int width = camera.getParameters().getPreviewSize().width;
		int height = camera.getParameters().getPreviewSize().height;
		float xs = (float)width / w;
		float ys = (float)height / h;
		Rect rect = new Rect(finderRect);
		rect.left = (int)((float)finderRect.left * xs);
		rect.right = (int)((float)finderRect.right * xs);
		rect.top = (int)((float)finderRect.top * ys);
		rect.bottom = (int)((float)finderRect.bottom * ys);
		return rect;
	}


	public void setLineRect(int model){
		lineModel = model;
		invalidate();
	}


	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);

		if(paint == null){
			return;
		}

		paint.setColor(Color.GREEN);

		canvas.drawLine(lineLeft - dLineWidth/2, lineTop, lineLeft + dLen, lineTop, paint);
		canvas.drawLine(lineLeft, lineTop- dLineWidth/2, lineLeft, lineTop + dLen, paint);

		canvas.drawLine(lineRight, lineTop -  dLineWidth/2, lineRight, lineTop + dLen, paint);
		canvas.drawLine(lineRight +  dLineWidth/2, lineTop, lineRight - dLen, lineTop, paint);

		canvas.drawLine(lineLeft, lineBottom +  dLineWidth/2, lineLeft, lineBottom - dLen, paint);
		canvas.drawLine(lineLeft -  dLineWidth/2, lineBottom, lineLeft + dLen, lineBottom, paint);

		canvas.drawLine(lineRight +  dLineWidth/2, lineBottom, lineRight - dLen, lineBottom, paint);
		canvas.drawLine(lineRight, lineBottom +  dLineWidth/2, lineRight, lineBottom - dLen, paint);

		switch (lineModel) {
			case 0:

				break;
			case 1://左边框线
				canvas.drawLine(lineLeft, lineTop, lineLeft, lineBottom, paint);
				break;
			case 2://右边框线
				canvas.drawLine(lineRight, lineTop, lineRight, lineBottom, paint);
				break;
			case 3://左右
				canvas.drawLine(lineLeft, lineTop, lineLeft, lineBottom, paint);
				canvas.drawLine(lineRight, lineTop, lineRight, lineBottom, paint);
				break;
			case 4://上边框
				canvas.drawLine(lineLeft, lineTop, lineRight, lineTop, paint);
				break;
			case 5://左上
				canvas.drawLine(lineLeft, lineTop, lineLeft, lineBottom, paint);
				canvas.drawLine(lineLeft, lineTop, lineRight, lineTop, paint);
				break;
			case 6://右上
				canvas.drawLine(lineRight, lineTop, lineRight, lineBottom, paint);
				canvas.drawLine(lineLeft, lineTop, lineRight, lineTop, paint);
				break;
			case 7://左右上
				canvas.drawLine(lineLeft, lineTop, lineLeft, lineBottom, paint);
				canvas.drawLine(lineRight, lineTop, lineRight, lineBottom, paint);
				canvas.drawLine(lineLeft, lineTop, lineRight, lineTop, paint);
				break;
			case 8://下边框
				canvas.drawLine(lineLeft, lineBottom, lineRight, lineBottom, paint);
				break;
			case 9://左下
				canvas.drawLine(lineLeft, lineTop, lineLeft, lineBottom, paint);
				canvas.drawLine(lineLeft, lineBottom, lineRight, lineBottom, paint);
				break;
			case 10://右下边框
				canvas.drawLine(lineRight, lineTop, lineRight, lineBottom, paint);
				canvas.drawLine(lineLeft, lineBottom, lineRight, lineBottom, paint);
				break;
			case 11://左右下
				canvas.drawLine(lineLeft, lineTop, lineLeft, lineBottom, paint);
				canvas.drawLine(lineRight, lineTop, lineRight, lineBottom, paint);
				canvas.drawLine(lineLeft, lineBottom, lineRight, lineBottom, paint);
				break;
			case 12://上下
				canvas.drawLine(lineLeft, lineTop, lineRight, lineTop, paint);
				canvas.drawLine(lineLeft, lineBottom, lineRight, lineBottom, paint);
				break;
			case 13://上下左
				canvas.drawLine(lineLeft, lineTop, lineRight, lineTop, paint);
				canvas.drawLine(lineLeft, lineBottom, lineRight, lineBottom, paint);
				canvas.drawLine(lineLeft, lineTop, lineLeft, lineBottom, paint);
				break;
			case 14://上下右
				canvas.drawLine(lineLeft, lineTop, lineRight, lineTop, paint);
				canvas.drawLine(lineLeft, lineBottom, lineRight, lineBottom, paint);
				canvas.drawLine(lineRight, lineTop, lineRight, lineBottom, paint);
				break;
			case 15://全
				canvas.drawLine(lineLeft, lineTop, lineLeft, lineBottom, paint);
				canvas.drawLine(lineRight, lineTop, lineRight, lineBottom, paint);
				canvas.drawLine(lineLeft, lineTop, lineRight, lineTop, paint);
				canvas.drawLine(lineLeft, lineBottom, lineRight, lineBottom, paint);
				break;

			default:

				break;
		}
		paint.setColor(Color.BLACK);
		paint.setAlpha(100);
//		canvas.drawRect(lineLeft + dLineWidth / 2, lineTop + dLineWidth / 2, lineRight - dLineWidth / 2, lineBottom - dLineWidth / 2, paint);

		//画四周
		canvas.drawRect(0, 0, width, lineTop - dLineWidth / 2, paint);

		canvas.drawRect(0, lineTop - dLineWidth / 2, lineLeft - dLineWidth / 2, lineBottom + dLineWidth / 2, paint);

		canvas.drawRect(0, lineBottom + dLineWidth / 2, width, height, paint);

		canvas.drawRect(lineRight + dLineWidth / 2, lineTop - dLineWidth / 2, width, lineBottom + dLineWidth / 2, paint);

	}
}

package cn.tureal.trlzdemo1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class TRZTMatchView extends View {
    private List<TRPenObj> mPenListCalc;
    private TRPenObj mNewPenObj;
    private Canvas mBufferCanvas;
    private Bitmap mBufferBitmap;
    private Bitmap mBGBitmap;

    private int mBufferWidth = 225;
    private int mBufferHeight = 225;



    public TRZTMatchView(Context context){
        super(context);
    }

    public TRZTMatchView(Context context, AttributeSet attributeSet){
        super(context, attributeSet);
        setBackgroundColor(getResources().getColor(R.color.colorWBBG));

        mPenListCalc = new ArrayList<TRPenObj>();
        mBufferBitmap = Bitmap.createBitmap(mBufferWidth, mBufferHeight, Bitmap.Config.ARGB_8888);
        mBufferCanvas = new Canvas(mBufferBitmap);
    }

    protected void reDraw(Canvas canvas){
        if(canvas == null)
            return;

        if(mBGBitmap != null && mBGBitmap.getWidth() > 0 && mBGBitmap.getHeight() > 0){
            canvas.drawBitmap(mBGBitmap, 0, 0, null);
        }
        else{
            canvas.drawColor(getResources().getColor(R.color.colorWBBG));
        }

        for(TRPenObj pen : mPenListCalc){
            pen.drawObj(canvas, mWBPosPenToBufferTrans);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(mBufferBitmap == null || mBufferCanvas == null){
            return;
        }

        if(mBufferBitmap != null){
            Matrix matrix = new Matrix();
            final float w = getWidth();
            final float h = getHeight();
            if(w/mBufferWidth > h/mBufferHeight){
                float scale = h/mBufferHeight;
                float offset = (w - mBufferWidth * scale)/2;
                matrix.setScale(scale, scale);
                matrix.postTranslate(offset, 0);
            }
            else {
                float scale = w/mBufferWidth;
                float offset = (h - mBufferHeight * scale)/2;
                matrix.setScale(scale, scale);
                matrix.postTranslate(0, offset);
            }
            canvas.drawColor(getResources().getColor(R.color.colorBG));
            canvas.drawBitmap(mBufferBitmap, matrix, null);
        }
    }

    private class WBPosPenToBufferTrans implements IWBPosTrans{
        @Override
        public float transPosX(float posX) {
            return posX;
        }

        @Override
        public float transPosY(float posY) {
            return posY;
        }
    }
    public TRZTMatchView.WBPosPenToBufferTrans mWBPosPenToBufferTrans = new TRZTMatchView.WBPosPenToBufferTrans();

    public void AddPenObj(TRPenObj newPenObj){
        if(newPenObj != null){
            mPenListCalc.add(newPenObj);
            reDraw(mBufferCanvas);
            invalidate();
        }
    }

    public void ClearWB(){
        if(mBufferCanvas != null){
            mNewPenObj = null;
            mPenListCalc.clear();
            reDraw(mBufferCanvas);
            invalidate();
        }
    }

    public void SetBG(Bitmap bg){
        mBGBitmap = bg;
        ClearWB();
    }

}

package cn.tureal.trlzdemo1;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TRPenObj implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int nObjType = 1;
    public long mTime;
    public int mColor;
    public float mThick;
    public List<Point> mPenPointList;
    public TRPenObj(int color, float thick){
        mTime = System.currentTimeMillis();
        mPenPointList = new ArrayList<Point>();
        mColor = color;
        mThick = thick;
    }

    public TRPenObj(long time, List<Point> listPenPoint, int color, float thick){
        mTime = time;
        mPenPointList = listPenPoint;
        mColor = color;
        mThick = thick;
    }

    public TRPenObj(TRPenObj pen){
        mTime = pen.mTime;
        mPenPointList = pen.mPenPointList;
        mColor = pen.mColor;
        mThick = pen.mThick;
    }

    public Point getCenterPoint(){
        long sumX = 0;
        long sumY = 0;
        Point ptCenter = new Point(-1, -1);
        if(mPenPointList.size() > 0){
            for(Point pt : mPenPointList){
                sumX += pt.x;
                sumY += pt.y;
            }
            ptCenter.x = (int) (sumX/mPenPointList.size());
            ptCenter.y = (int) (sumY/mPenPointList.size());
        }
        return ptCenter;
    }

    public void addPenPoint(int posX, int posY){
        mPenPointList.add(new Point(posX, posY));
    }

    public boolean drawObj(Canvas canvas, IWBPosTrans trans){
        if(canvas == null || trans == null || mPenPointList == null && mPenPointList.size() < 1){
            return false;
        }

        Paint paint = new Paint();
        paint.setColor(mColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(mThick);
        paint.setAntiAlias(true);

        Path path = new Path();
        boolean bStart = false;
        float wbPathX = 0;
        float wbPathY = 0;
        float wbLastX = 0;
        float wbLastY = 0;
        for (Point ptPen : mPenPointList){
            float wbPosX = trans.transPosX(ptPen.x);
            float wbPosY = trans.transPosY(ptPen.y);
            if(wbPosX < 0 || wbPosY < 0) {
                bStart = false;
            }
            else{
                if(bStart){
                    wbPathX = (wbPosX + wbLastX) / 2;
                    wbPathY = (wbPosY + wbLastY) / 2;
                    wbLastX = wbPosX;
                    wbLastY = wbPosY;
                    path.lineTo(wbPathX, wbPathY);
                }
                else{
                    wbPathX = wbPosX;
                    wbPathY = wbPosY;
                    wbLastX = wbPosX;
                    wbLastY = wbPosY;
                    path.moveTo(wbPathX, wbPathY);
                    bStart = true;
                }
            }
        }

        canvas.drawPath(path, paint);
        return true;
    }
}

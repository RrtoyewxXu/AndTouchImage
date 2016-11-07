package com.rrtoyewx.touchimageview.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import android.view.ScaleGestureDetector;
import android.view.View;


/**
 * Created by Rrtoyewx on 2016/11/2.
 */

public class ScaleView extends View {
    private ScaleGestureDetector.OnScaleGestureListener mOnScaleGestureListener;
    private ScaleGestureDetector mScaleGestureDetector;

    public ScaleView(Context context) {
        this(context, null);
    }

    public ScaleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScaleView(Context context, AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mOnScaleGestureListener = new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {

                Log.e("TAG", " onScale getCurrentSpan(): " + detector.getCurrentSpan()
                        + " detector.getPrevSpan(): " + detector.getPreviousSpan()
                        + " factor: " + (detector.getPreviousSpan() > 0 ? detector.getCurrentSpan() / detector.getPreviousSpan() : -1)
                        + " detector.getFocusX(): " + detector.getFocusX()
                        + " detector.getFocusY(): " + detector.getFocusY()
                        + " detector.getScaleFactor() :" + detector.getScaleFactor());
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                Log.e("TAG", " onScaleBegin getCurrentSpan(): " + detector.getCurrentSpan()
                        + " detector.getCurrentSpanX(): " + detector.getCurrentSpanX()
                        + " detector.getCurrentSpanY(): " + detector.getCurrentSpanY()
                        + " detector.getFocusX(): " + detector.getFocusX()
                        + " detector.getFocusY(): " + detector.getFocusY()
                        + " detector.getScaleFactor() :" + detector.getScaleFactor());
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                Log.e("TAG", " onScaleEnd getCurrentSpan(): " + detector.getCurrentSpan()
                        + " detector.getCurrentSpanX(): " + detector.getCurrentSpanX()
                        + " detector.getCurrentSpanY(): " + detector.getCurrentSpanY()
                        + " detector.getFocusX(): " + detector.getFocusX()
                        + " detector.getFocusY(): " + detector.getFocusY()
                        + " detector.getScaleFactor() :" + detector.getScaleFactor());
            }
        };

        mScaleGestureDetector = new ScaleGestureDetector(context, mOnScaleGestureListener);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mScaleGestureDetector.onTouchEvent(event);
    }
}

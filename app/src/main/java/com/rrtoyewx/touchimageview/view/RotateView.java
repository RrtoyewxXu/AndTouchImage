package com.rrtoyewx.touchimageview.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.rrtoyewx.touchimageviewlibrary.gesturedetector.RotateGestureDetector;

/**
 * Created by Rrtoyewx on 2016/11/3.
 */

public class RotateView extends View {
    private RotateGestureDetector mRotateGestureDetector;
    private RotateGestureDetector.OnRotateGestureListener mOnRotateGestureListener;

    public RotateView(Context context) {
        this(context, null);
    }

    public RotateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RotateView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mOnRotateGestureListener = new RotateGestureDetector.OnRotateGestureListener() {

            @Override
            public boolean onRotateBegin(RotateGestureDetector detector) {
                Log.e("TAG", " onRotateBegin detector.getDegree(): " + detector.getDegree());
                return true;
            }

            @Override
            public boolean onRotate(RotateGestureDetector detector) {
                Log.e("TAG", " onRotate detector.getDegree(): " + detector.getDegree());

                return true;
            }

            @Override
            public void onRotateEnd(RotateGestureDetector detector) {
                Log.e("TAG", " onRotateEnd detector.getDegree(): " + detector.getDegree());
            }
        };

        mRotateGestureDetector = new RotateGestureDetector(context, mOnRotateGestureListener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        return mRotateGestureDetector.onTouchEvent(event);
    }
}

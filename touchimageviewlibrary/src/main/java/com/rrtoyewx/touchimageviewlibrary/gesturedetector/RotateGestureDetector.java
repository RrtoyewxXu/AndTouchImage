package com.rrtoyewx.touchimageviewlibrary.gesturedetector;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.rrtoyewx.touchimageviewlibrary.R;
import com.rrtoyewx.touchimageviewlibrary.util.L;

import static android.view.MotionEvent.*;

/**
 * Created by Rrtoyewx on 2016/11/2.
 */

public class RotateGestureDetector {
    public static final float RADIAN_TO_DEGREE = (float) (180.0 / Math.PI);
    //TODO ：最小的滑动的角度，后期待完成
    private static final float MIN_ROTATE_DEGREE = 0.1f;

    public interface OnRotateGestureListener {
        boolean onRotateBegin(RotateGestureDetector detector);

        boolean onRotate(RotateGestureDetector detector);

        void onRotateEnd(RotateGestureDetector detector);
    }

    public static class SimpleRotateGestureListener implements OnRotateGestureListener {

        @Override
        public boolean onRotateBegin(RotateGestureDetector detector) {
            return true;
        }

        @Override
        public boolean onRotate(RotateGestureDetector detector) {
            return true;
        }

        @Override
        public void onRotateEnd(RotateGestureDetector detector) {

        }
    }

    private final Context mContext;
    private final OnRotateGestureListener mListener;

    private float mFocusX;
    private float mFocusY;

    private float[] mCurrentPointerDegrees;
    private float[] mPrevPointerDegrees;

    private float mInitialSpan;

    private long mCurrentTime;
    private long mPrevTime;

    private boolean mInProgress;

    private int mMinSpan;
    private int mSpanSlop;

    public RotateGestureDetector(Context context, OnRotateGestureListener onRotateGestureListener) {
        this.mContext = context;
        this.mListener = onRotateGestureListener;

        mSpanSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        final Resources resources = context.getResources();
        mMinSpan = resources.getDimensionPixelSize(R.dimen.config_minScalingSpan);
    }

    public boolean onTouchEvent(MotionEvent event) {
        mCurrentTime = event.getEventTime();

        final int action = event.getActionMasked();

        final boolean streamComplete = action == ACTION_UP
                || action == ACTION_CANCEL;

        if (action == ACTION_DOWN || streamComplete) {
            if (mInProgress) {
                mListener.onRotateEnd(this);
                mInProgress = false;
                mInitialSpan = 0;
            }
            if (streamComplete) {
                return true;
            }
        }

        final boolean configChange = action == ACTION_DOWN
                || action == ACTION_POINTER_UP
                || action == ACTION_POINTER_DOWN;

        final boolean pointerUp = action == ACTION_POINTER_UP;
        final int skipIndex = pointerUp ? event.getActionIndex() : -1;

        float sumX = 0;
        float sumY = 0;
        int count = event.getPointerCount();
        final int div = pointerUp ? count - 1 : count;

        for (int i = 0; i < count; i++) {
            if (i == skipIndex) {
                continue;
            }
            sumX += event.getX(i);
            sumY += event.getY(i);
        }

        final float focusX = sumX / div;
        final float focusY = sumY / div;

        final float[] pointerDegrees = new float[count];

        float devSumX = 0;
        float devSumY = 0;
        float tempLength = 0;
        float tempDegree = 0;
        for (int i = 0; i < count; i++) {
            if (i == skipIndex) {
                continue;
            }
            devSumX += Math.abs(event.getX(i) - focusX);
            devSumY += Math.abs(event.getY(i) - focusY);

            //求第一象限的角
            tempLength = (float) Math.hypot(Math.abs(event.getY(i) - focusY), Math.abs(event.getX(i) - focusX));
            //   L.e("index: " + i + " tempLength: " + tempLength);
            if (tempLength == 0) {
                continue;
            }
            tempDegree = (float) (Math.asin(Math.abs(event.getY(i) - focusY) / tempLength) * RADIAN_TO_DEGREE);
            // L.e("第一象限的index：" + i + "  tempDegree : " + tempDegree);
            if (event.getX(i) - focusX >= 0 && event.getY(i) - focusY >= 0) {
                tempDegree = tempDegree;
            } else if (event.getX(i) - focusX > 0 && event.getY(i) - focusY < 0) {
                tempDegree = 360 - tempDegree;
            } else if (event.getX(i) - focusX < 0 && event.getY(i) - focusY > 0) {
                tempDegree = 180 - tempDegree;
            } else if (event.getX(i) - focusX < 0 && event.getY(i) - focusY < 0) {
                tempDegree += 180;
            }

            // L.e("正常转换的角度index：" + i + "  tempDegree : " + tempDegree);
            pointerDegrees[i] = tempDegree;
        }

        final float devX = devSumX / div;
        final float devY = devSumY / div;
        final float spanX = devX * 2;
        final float spanY = devY * 2;
        final float span = (float) Math.hypot(spanX, spanY);

        mFocusX = focusX;
        mFocusY = focusY;

        final boolean wasInProgress = mInProgress;
        if (mInProgress && (span < mMinSpan || configChange)) {
            mListener.onRotateEnd(this);
            mInProgress = false;
            mInitialSpan = span;
        }

        if (configChange) {
            mInitialSpan = span;
            mPrevPointerDegrees = new float[pointerDegrees.length];
            System.arraycopy(pointerDegrees, 0, mPrevPointerDegrees, 0, pointerDegrees.length);

            mCurrentPointerDegrees = new float[pointerDegrees.length];
            System.arraycopy(pointerDegrees, 0, mCurrentPointerDegrees, 0, pointerDegrees.length);
        }

        if (!mInProgress
                && span > mMinSpan
                && (wasInProgress || Math.abs(span - mInitialSpan) > mSpanSlop)) {

            mPrevPointerDegrees = new float[pointerDegrees.length];
            System.arraycopy(pointerDegrees, 0, mPrevPointerDegrees, 0, pointerDegrees.length);

            mCurrentPointerDegrees = new float[pointerDegrees.length];
            System.arraycopy(pointerDegrees, 0, mCurrentPointerDegrees, 0, pointerDegrees.length);

            mPrevTime = mCurrentTime;
            mInProgress = mListener.onRotateBegin(this);
        }

        if (action == ACTION_MOVE) {
            mCurrentPointerDegrees = new float[pointerDegrees.length];
            System.arraycopy(pointerDegrees, 0, mCurrentPointerDegrees, 0, pointerDegrees.length);

            boolean updatePrev = true;
            if (mInProgress) {
                updatePrev = mListener.onRotate(this);
            }

            if (updatePrev) {
                mPrevPointerDegrees = new float[mCurrentPointerDegrees.length];
                System.arraycopy(mCurrentPointerDegrees, 0, mPrevPointerDegrees, 0, mCurrentPointerDegrees.length);

                mPrevTime = mCurrentTime;
            }
        }
        return true;
    }

    public float getDegree() {
        float degree = 0;
        if (mCurrentPointerDegrees != null && mPrevPointerDegrees != null) {
            int count = Math.min(mCurrentPointerDegrees.length, mPrevPointerDegrees.length);
            for (int i = 0; i < count; i++) {
                // fix 0 -> 360 error;
                // fix 360 -> 0 error;
                if (Math.abs(mCurrentPointerDegrees[i] - mPrevPointerDegrees[i]) > 300) {
                    mPrevPointerDegrees[i] = mCurrentPointerDegrees[i];
                }
                degree += mCurrentPointerDegrees[i] - mPrevPointerDegrees[i];
            }
            degree /= count;
        }
        while (degree > 360) {
            degree -= 360;
        }
        while (degree < -360) {
            degree += 360;
        }
        return degree;
    }

    public float getFocusX() {
        return mFocusX;
    }

    public float getFocusY() {
        return mFocusY;
    }
}

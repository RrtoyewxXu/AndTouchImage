package com.rrtoyewx.touchimageviewlibrary.helper;

import android.os.SystemClock;
import android.util.Log;
import android.view.animation.Interpolator;

/**
 * Created by Rrtoyewx on 2016/11/8.
 * Scaler: like  Scroller in the role of scrollBy() and scorllTo();
 */

public class Scaler {
    private static final int DEFAULT_DURATION = 300;
    private Interpolator mInterpolator;

    private float mStartScale;
    private float mFinalScale;
    private float mCurrentScale;

    private long mStartTime;
    private int mDuration;
    private float mDurationReciprocal;
    private boolean mFinished;

    private float mFocusX;
    private float mFocusY;

    public Scaler(Interpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    public void startScale(float startScale, float finalScale, float focusX, float focusY) {
        startScale(startScale, finalScale, DEFAULT_DURATION, focusX, focusY);
    }

    public void startScale(float startScale, float finalScale, int duration, float focusX, float focusY) {
        mFinalScale = finalScale;
        mStartScale = startScale;

        mDuration = duration;
        mFinished = false;

        mStartTime = currentAnimationTimeMillis();
        mDurationReciprocal = 1.0f / mDuration;

        mFocusX = focusX;
        mFocusY = focusY;
    }

    public boolean computeScrollOffset() {
        if (mFinished) {
            return false;
        }
        int timePassed = (int) (currentAnimationTimeMillis() - mStartTime);
        if (timePassed < mDuration) {
            float interpolation = mInterpolator.getInterpolation(timePassed * mDurationReciprocal);
            mCurrentScale = (mFinalScale - mStartScale) * interpolation + mStartScale;
        } else {
            mCurrentScale = mFinalScale;
            mFinished = true;
        }
        return true;
    }

    private long currentAnimationTimeMillis() {
        return SystemClock.uptimeMillis();
    }

    public float getCurrentScale() {
        return mCurrentScale;
    }

    public float getFocusX() {
        return mFocusX;
    }

    public float getFocusY() {
        return mFocusY;
    }
}

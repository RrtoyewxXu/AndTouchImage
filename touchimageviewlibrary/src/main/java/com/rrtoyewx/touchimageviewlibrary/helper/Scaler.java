package com.rrtoyewx.touchimageviewlibrary.helper;

import android.graphics.PointF;
import android.os.SystemClock;
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

    private float mCenterX;
    private float mCenterY;
    private float mCurrentFactor;

    public Scaler(Interpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    public void startScale(float startScale, float finalScale, float centerX, float centerY) {
        startScale(startScale, finalScale, DEFAULT_DURATION, centerX, centerY);
    }

    public void startScale(float startScale, float finalScale, int duration, float centerX, float centerY) {
        mFinalScale = finalScale;
        mCurrentScale = mStartScale = startScale;

        mDuration = duration;
        mFinished = false;

        mStartTime = currentAnimationTimeMillis();
        mDurationReciprocal = 1.0f / mDuration;

        mCenterX = centerX;
        mCenterY = centerY;
        mCurrentFactor = 0;
    }

    public boolean computeScrollOffset() {
        if (mFinished) {
            return false;
        }

        if (mCurrentScale == mFinalScale
                || mCurrentFactor == 1.0) {
            mFinished = true;
        }

        int timePassed = (int) (currentAnimationTimeMillis() - mStartTime);
        if (timePassed < mDuration) {
            mCurrentFactor = mInterpolator.getInterpolation(timePassed * mDurationReciprocal);
            mCurrentScale = (mFinalScale - mStartScale) * mCurrentFactor + mStartScale;
        } else {
            mCurrentScale = mFinalScale;
            mCurrentFactor = 1.0f;
        }
        return true;
    }

    public void abortAnimation() {
        mCurrentScale = mFinalScale;
        mCurrentFactor = 1.0f;
        mFinished = true;
    }

    private long currentAnimationTimeMillis() {
        return SystemClock.uptimeMillis();
    }

    public float getCurrentScale() {
        return mCurrentScale;
    }

    public float getCenterX() {
        return mCenterX;
    }

    public float getCenterY() {
        return mCenterY;
    }

    public float getCurrentFactor() {
        return mCurrentFactor;
    }

    public float getFinalScale() {
        return mFinalScale;
    }

    public float getStartScale() {
        return mStartScale;
    }

}

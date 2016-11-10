package com.rrtoyewx.touchimageviewlibrary.helper;

import android.os.SystemClock;
import android.view.animation.Interpolator;

/**
 * Created by Rrtoyewx on 2016/11/10.
 * Rotater: like  Scroller in the role of scrollBy() and scorllTo();
 */

public class Rotater {
    private static final int DEFAULT_DURATION = 500;
    private Interpolator mInterpolator;

    private float mStartDegree;
    private float mFinalDegree;

    private float mCurrentDegree;

    private long mStartTime;
    private int mDuration;
    private float mDurationReciprocal;
    private boolean mFinished;

    private float mCenterX;
    private float mCenterY;

    public Rotater(Interpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    public void startRotate(float startDegree, float targetDegree, float centerX, float centerY) {
        startRotate(startDegree, targetDegree, DEFAULT_DURATION, centerX, centerY);
    }

    public void startRotate(float startDegree, float targetDegree, int duration, float centerX, float centerY) {
        this.mStartTime = currentAnimationTimeMillis();
        this.mDuration = duration;
        mDurationReciprocal = 1.0f / mDuration;

        mCurrentDegree = mStartDegree = startDegree;
        mFinalDegree = targetDegree;
        mFinished = false;

        this.mCenterX = centerX;
        this.mCenterY = centerY;
    }

    public boolean computeScrollOffset() {
        if (mFinished) {
            return false;
        }

        if (mCurrentDegree == mFinalDegree) {
            mFinished = true;
        }

        int timePassed = (int) (currentAnimationTimeMillis() - mStartTime);
        if (timePassed < mDuration) {
            float currentFactor = mInterpolator.getInterpolation(timePassed * mDurationReciprocal);
            mCurrentDegree = (mFinalDegree - mStartDegree) * currentFactor + mStartDegree;
        } else {
            mCurrentDegree = mFinalDegree;
        }
        return true;
    }

    public void abortAnimation() {
        mCurrentDegree = mFinalDegree;
        mFinished = true;
    }

    private long currentAnimationTimeMillis() {
        return SystemClock.uptimeMillis();
    }

    public float getCurrentDegree() {
        return mCurrentDegree;
    }

    public float getCenterX() {
        return mCenterX;
    }

    public float getCenterY() {
        return mCenterY;
    }
}


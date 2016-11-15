package com.rrtoyewx.touchimageviewlibrary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;

import com.rrtoyewx.touchimageviewlibrary.gesturedetector.RotateGestureDetector;
import com.rrtoyewx.touchimageviewlibrary.helper.Rotater;
import com.rrtoyewx.touchimageviewlibrary.helper.Scaler;
import com.rrtoyewx.touchimageviewlibrary.util.L;

/**
 * Created by Rrtoyewx on 2016/11/2.
 */

public class TouchImageView extends ImageView {
    //-------------------default value-------------------------
    private static final float DEGREE_TO_RADIAN = (float) (Math.PI / 180);
    private static final Interpolator DEFAULT_ANIMATION_INTERPOLATOR = new AccelerateDecelerateInterpolator();
    private static final float[] DEFAULT_ORIGIN_MATRIX_VALUE = new float[]{
            1, 0, 0,
            0, 1, 0,
            0, 0, 1};

    private static final float SUPER_MIN_MULTIPLIER = .75f;
    private static final float SUPER_MAX_MULTIPLIER = 1.25f;

    private static final int VALUE_MIN_SCALE = 1;
    private static final int VALUE_MAX_SCALE = 3;

    //-----------------------constants that remark state-----------
    private static final int STATE_NONE = 0;
    private static final int STATE_DRAG = 1;
    private static final int STATE_ZOOM = 2;
    private static final int STATE_ROTATE = 3;
    private static final int STATE_FLING = 4;
    private static final int STATE_ANIMATE_ZOOM = 5;
    private static final int STATE_ANIMATE_ROTATE = 6;
    private static final int STATE_ANIMATE_TRANSLATE = 7;

    @IntDef({STATE_NONE, STATE_DRAG, STATE_ZOOM, STATE_ROTATE, STATE_FLING, STATE_ANIMATE_ZOOM, STATE_ANIMATE_ROTATE, STATE_ANIMATE_TRANSLATE})
    public @interface STATE {
    }

    @STATE
    private int mState;

    private float mRotateDegree = 0;
    private float mNormalizedScale = 1;

    private float mMinScale;
    private float mMaxScale;
    private float mSuperMinScale;
    private float mSuperMaxScale;

    private float[] mMatrixValues;
    private float[] mOriginMatrixValue;

    private Matrix mMatrix;
    private int mViewHeight;
    private int mViewWidth;
    private float mAdjustedDrawableWidth;
    private float mAdjustedDrawableHeight;

    private Matrix mPrevMatrix;
    private int mPrevViewHeight;
    private int mPrevViewWidth;
    private float mPrevAdjustedDrawableWidth;
    private float mPrevAdjustedDrawableHeight;

    private Context mContext;
    private ScaleType mScaleType;

    private Interpolator mInterpolator;
    private ScrollerCompat mScroller;
    private float mFlingCurrentX;
    private float mFlingCurrentY;

    private float mAnimateScaleLastValue;
    private Scaler mScaler;

    private Rotater mRotater;
    private float mAnimateRotateLastValue = -1;

    private boolean mShouldCatchNormalGesture;
    private boolean mShouldCatchScaleGesture;
    private boolean mShouldCatchRotateGesture;

    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetectorCompat mGestureDetector;
    private RotateGestureDetector mRotateGestureDetector;

    {
        mState = STATE_NONE;

        mMinScale = VALUE_MIN_SCALE;
        mMaxScale = VALUE_MAX_SCALE;

        mSuperMinScale = VALUE_MIN_SCALE * SUPER_MIN_MULTIPLIER;
        mSuperMaxScale = VALUE_MAX_SCALE * SUPER_MAX_MULTIPLIER;

        mInterpolator = DEFAULT_ANIMATION_INTERPOLATOR;

        mMatrix = new Matrix();
        mPrevMatrix = new Matrix();

        mMatrixValues = new float[9];
        mOriginMatrixValue = new float[9];

        mShouldCatchNormalGesture = true;
        mShouldCatchScaleGesture = true;
        mShouldCatchRotateGesture = true;
    }

    public TouchImageView(Context context) {
        this(context, null);
    }

    public TouchImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TouchImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        super.setClickable(true);
        this.mContext = context;
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetectorCompat(context, new GestureListener());
        mRotateGestureDetector = new RotateGestureDetector(context, new RotateListener());

        mScroller = ScrollerCompat.create(mContext, mInterpolator);
        mScaler = new Scaler(mInterpolator);
        mRotater = new Rotater(mInterpolator);

        if (mScaleType == null) {
            mScaleType = ScaleType.FIT_CENTER;
        }

        setImageMatrix(mMatrix);
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable drawable = getDrawable();
        if (drawable == null
                || drawable.getIntrinsicHeight() == 0
                || drawable.getIntrinsicWidth() == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        mViewHeight = getViewSize(heightMode, heightSize, drawableHeight);
        mViewWidth = getViewSize(widthMode, widthSize, drawableWidth);

        setMeasuredDimension(mViewWidth, mViewHeight);

        fillImageToView();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    private int getViewSize(int measureMode, int measureSize, int needSize) {
        int viewSize;
        switch (measureMode) {
            default:
            case MeasureSpec.EXACTLY:
                viewSize = measureSize;
                break;
            case MeasureSpec.AT_MOST:
                viewSize = Math.min(measureSize, needSize);
                break;
            case MeasureSpec.UNSPECIFIED:
                viewSize = needSize;
        }
        return viewSize;
    }

    private void fillImageToView() {
        Drawable drawable = getDrawable();
        if (drawable == null
                || drawable.getIntrinsicWidth() == 0
                || drawable.getIntrinsicHeight() == 0
                || mMatrix == null) {
            return;
        }

        int drawableWidth = drawable.getIntrinsicWidth();
        int drawableHeight = drawable.getIntrinsicHeight();

        float scaleX = (float) mViewWidth / drawableWidth;
        float scaleY = (float) mViewHeight / drawableHeight;

        // 各个ScaleType的效果
        // http://blog.csdn.net/chen825919148/article/details/8845889
        switch (mScaleType) {
            case CENTER:
                scaleX = scaleY = 1;
                break;
            case CENTER_CROP:
                scaleX = scaleY = Math.max(scaleX, scaleY);
                break;
            case CENTER_INSIDE:
                scaleX = scaleY = Math.min(Math.min(scaleX, scaleY), 1);
                break;
            case FIT_XY:
                break;
            default:
            case FIT_CENTER:
                scaleX = scaleY = Math.min(scaleX, scaleY);
                break;
        }

        float xSpace = mViewWidth - (scaleX * drawableWidth);
        float ySpace = mViewHeight - (scaleY * drawableHeight);
        mAdjustedDrawableWidth = scaleX * drawableWidth;
        mAdjustedDrawableHeight = scaleY * drawableHeight;

        initOriginImage(scaleX, scaleY, xSpace / 2, ySpace / 2);
        if (mNormalizedScale != 1.0 || mRotateDegree != 0) {
            if (!checkHasPreviousImageInfos()) {
                savePreviousImageInfos();
            }

            if (checkHasPreviousImageInfos()) {
                mPrevMatrix.postRotate(-mRotateDegree);
                mPrevMatrix.getValues(mMatrixValues);
                float prevTransX = mMatrixValues[Matrix.MTRANS_X];
                float prevTransY = mMatrixValues[Matrix.MTRANS_Y];

                float prevImageShowWidth = mPrevAdjustedDrawableWidth * mNormalizedScale;
                float prevImageShowHeight = mPrevAdjustedDrawableHeight * mNormalizedScale;

                float currentTransX = prevTransX / prevImageShowWidth * getImageWidth();
                float currentTransY = prevTransY / prevImageShowHeight * getImageHeight();
                mPrevMatrix.postRotate(mRotateDegree);

                mMatrix.getValues(mMatrixValues);
                mMatrixValues[Matrix.MSCALE_X] = mNormalizedScale * scaleX;
                mMatrixValues[Matrix.MSCALE_Y] = mNormalizedScale * scaleY;
                mMatrixValues[Matrix.MTRANS_X] = currentTransX;
                mMatrixValues[Matrix.MSCALE_Y] = currentTransY;
                mMatrix.setValues(mMatrixValues);
                mMatrix.postRotate(mRotateDegree);
            }
        }

        setImageMatrix(mMatrix);
    }

    private void initOriginImage(float originScaleX, float originScaleY, float originTransX, float originTransY) {
        mNormalizedScale = 1;
        mRotateDegree = 0;
        mMatrix.setValues(DEFAULT_ORIGIN_MATRIX_VALUE);
        mMatrix.setScale(originScaleX, originScaleY);
        mMatrix.postTranslate(originTransX, originTransY);

        mMatrix.getValues(mOriginMatrixValue);
    }

    private void setState(@STATE int state) {
        this.mState = state;
    }

    private void translateImageInner(float transX, float transY) {
        float currentImageXValue = (float) Math.max(Math.abs(getImageWidth() * Math.cos(mRotateDegree * DEGREE_TO_RADIAN)), Math.abs(getImageHeight() * Math.sin(mRotateDegree * DEGREE_TO_RADIAN)));
        float currentImageYValue = (float) Math.max(Math.abs(getImageWidth() * Math.sin(mRotateDegree * DEGREE_TO_RADIAN)), Math.abs(getImageHeight() * Math.cos(mRotateDegree * DEGREE_TO_RADIAN)));

        if (currentImageXValue <= mViewWidth) {
            transX = 0;
        }
        if (currentImageYValue <= mViewHeight) {
            transY = 0;
        }
        L.d("当图片的长／宽大于View的长／宽时,才可以滑动,滑动距离修正为: distanceX = " + transX + ",distanceY = " + transY);
        mMatrix.postTranslate(transX, transY);
        fixTranslateTrans();
    }

    private void animateTranslateImageInner(float transX, float transY) {
        setState(STATE_ANIMATE_TRANSLATE);

        mMatrix.getValues(mMatrixValues);
        mFlingCurrentX = mMatrixValues[Matrix.MTRANS_X];
        mFlingCurrentY = mMatrixValues[Matrix.MTRANS_Y];

        mScroller.startScroll((int) mFlingCurrentX, (int) mFlingCurrentY, (int) transX, (int) transY);
        postInvalidate();
    }

    private void animateTranslateImageInner(float transX, float transY, int duration) {
        setState(STATE_ANIMATE_TRANSLATE);

        mMatrix.getValues(mMatrixValues);
        mFlingCurrentX = mMatrixValues[Matrix.MTRANS_X];
        mFlingCurrentY = mMatrixValues[Matrix.MTRANS_Y];

        mScroller.startScroll((int) mFlingCurrentX, (int) mFlingCurrentY, (int) transX, (int) transY, duration);
        postInvalidate();
    }

    private void fixTranslateTrans() {
        PointF midPoint = calculateOriginMapping();

        mMatrix.getValues(mMatrixValues);
        float transX = mMatrixValues[Matrix.MTRANS_X];
        float transY = mMatrixValues[Matrix.MTRANS_Y];
        float currentImageXValue = (float) Math.max(Math.abs(getImageWidth() * Math.cos(mRotateDegree * DEGREE_TO_RADIAN)), Math.abs(getImageHeight() * Math.sin(mRotateDegree * DEGREE_TO_RADIAN)));
        float currentImageYValue = (float) Math.max(Math.abs(getImageWidth() * Math.sin(mRotateDegree * DEGREE_TO_RADIAN)), Math.abs(getImageHeight() * Math.cos(mRotateDegree * DEGREE_TO_RADIAN)));

        float minTransY = mViewHeight >= currentImageYValue ? midPoint.y - (mViewHeight - currentImageYValue) / 2 : (mViewHeight - currentImageYValue) / 2 + midPoint.y;
        float maxTransY = mViewHeight >= currentImageYValue ? (mViewHeight - currentImageYValue) / 2 + midPoint.y : midPoint.y - (mViewHeight - currentImageYValue) / 2;

        float minTransX = mViewWidth >= currentImageXValue ? midPoint.x - (mViewWidth - currentImageXValue) / 2 : (mViewWidth - currentImageXValue) / 2 + midPoint.x;
        float maxTransX = mViewWidth >= currentImageXValue ? (mViewWidth - currentImageXValue) / 2 + midPoint.x : midPoint.x - (mViewWidth - currentImageXValue) / 2;

        transX = transX > maxTransX ? maxTransX : (transX < minTransX ? minTransX : transX);
        transY = transY > maxTransY ? maxTransY : (transY < minTransY ? minTransY : transY);

        mMatrixValues[Matrix.MTRANS_X] = transX;
        mMatrixValues[Matrix.MTRANS_Y] = transY;

        mMatrix.setValues(mMatrixValues);
        L.i("图片在平移的过程中,为了让图片长／宽小于View的长／宽时，图片始终居中，修正后的偏移量:offsetX = " + transX + ", offsetY = " + transY);
    }

    private Matrix transformMatrix() {
        mMatrix.getValues(mMatrixValues);
        float skewX = mMatrixValues[Matrix.MSKEW_X];
        float skewY = mMatrixValues[Matrix.MSKEW_Y];
        if (skewX != 0 || skewY != 0) {
            //todo : now center pointer is (mViewWidth/2,mViewHeight/2);
            mMatrix.postRotate(-mRotateDegree, mViewWidth / 2, mViewHeight / 2);
        }
        return mMatrix;
    }

    private void restoreMatrix() {
        mMatrix.postRotate(mRotateDegree, mViewWidth / 2, mViewHeight / 2);
    }

    private PointF calculateOriginMapping() {
        Matrix matrix = new Matrix();
        float[] tempFloat = new float[9];
        matrix.setValues(mOriginMatrixValue);
        matrix.postScale(mNormalizedScale, mNormalizedScale, mViewWidth / 2, mViewHeight / 2);
        matrix.postRotate(mRotateDegree, mViewWidth / 2, mViewHeight / 2);

        matrix.getValues(tempFloat);
        return new PointF(tempFloat[2], tempFloat[5]);
    }

    private void flingImageInner(float velocityX, float velocityY) {
        setState(STATE_FLING);

        transformMatrix();

        mMatrix.getValues(mMatrixValues);
        mFlingCurrentX = (int) mMatrixValues[Matrix.MTRANS_X];
        mFlingCurrentY = (int) mMatrixValues[Matrix.MTRANS_Y];

        float minX = getImageWidth() > mViewWidth ? mViewWidth - getImageWidth() : mFlingCurrentX;
        float maxX = getImageWidth() > mViewWidth ? 0 : mFlingCurrentX;
        float minY = getImageHeight() > mViewHeight ? mViewHeight - getImageHeight() : mFlingCurrentY;
        float maxY = getImageHeight() > mViewHeight ? 0 : mFlingCurrentY;

        L.d("抛动作,修正后的minX = " + minX + ",maxX = " + maxX + ",minY = " + minY + ",maxY = " + maxY);
        mScroller.fling((int) mFlingCurrentX, (int) mFlingCurrentY, (int) velocityX, (int) velocityY, (int) minX, (int) maxX, (int) minY, (int) maxY);

        restoreMatrix();
    }

    private void cancelTranslateAnimate() {
        if (mScroller != null) {
            mScroller.abortAnimation();
            setState(STATE_NONE);
        }

        mFlingCurrentX = 0;
        mFlingCurrentY = 0;
    }

    private void scaleImageInner(float factor, float centerX, float centerY, boolean stretchImageToSuper) {
        float lowerScale = stretchImageToSuper ? mSuperMinScale : mMinScale;
        float upperScale = stretchImageToSuper ? mSuperMaxScale : mMaxScale;

        float originScale = mNormalizedScale;
        mNormalizedScale *= factor;
        if (mNormalizedScale > upperScale) {
            mNormalizedScale = upperScale;
            factor = mNormalizedScale / originScale;
        } else if (mNormalizedScale < lowerScale) {
            mNormalizedScale = lowerScale;
            factor = mNormalizedScale / originScale;
        }

        mMatrix.postScale(factor, factor, centerX, centerY);
        fixScaleTrans();
        fixTranslateTrans();
    }

    private void needToScaleBoundary() {
        boolean needToScaleBoundary = false;
        float targetScale = mNormalizedScale;

        if (mNormalizedScale > mMaxScale) {
            needToScaleBoundary = true;
            targetScale = mMaxScale;
        }

        if (mNormalizedScale < mMinScale) {
            needToScaleBoundary = true;
            targetScale = mMinScale;
        }

        if (needToScaleBoundary) {
            cancelScaleAnimate();
            animateScaleImageInner(targetScale, mViewWidth / 2, mViewHeight / 2);
        }
    }

    private void animateScaleImageInner(float targetScale, float focusX, float focusY) {
        setState(STATE_ANIMATE_ZOOM);

        mAnimateScaleLastValue = mNormalizedScale;
        mScaler.startScale(mNormalizedScale, targetScale, focusX, focusY);

        postInvalidate();
    }

    private void animateScaleImageInner(float targetScale, float focusX, float focusY, int duration) {
        setState(STATE_ANIMATE_ZOOM);

        mAnimateScaleLastValue = mNormalizedScale;
        mScaler.startScale(mNormalizedScale, targetScale, duration, focusX, focusY);

        postInvalidate();
    }

    private void cancelScaleAnimate() {
        if (mScaler != null) {
            mScaler.abortAnimation();
            mAnimateScaleLastValue = 0;
            setState(STATE_NONE);
        }
    }

    private void fixScaleTrans() {
        transformMatrix();

        mMatrix.getValues(mMatrixValues);
        float transX = mMatrixValues[Matrix.MTRANS_X];
        float transY = mMatrixValues[Matrix.MTRANS_Y];

        transX = getImageWidth() < mViewWidth ? (mViewWidth - getImageWidth()) / 2 : transX;
        transY = getImageHeight() < mViewHeight ? (mViewHeight - getImageHeight()) / 2 : transY;
        mMatrixValues[Matrix.MTRANS_X] = transX;
        mMatrixValues[Matrix.MTRANS_Y] = transY;

        mMatrix.setValues(mMatrixValues);
        restoreMatrix();
    }

    private void rotateImageInner(float degree, float centerX, float centerY) {
        mMatrix.postRotate(degree, centerX, centerY);
        mRotateDegree += degree;

        fixRotateTrans();
        fixTranslateTrans();
    }

    private void AnimateRotateImageInner(float targetDegree, float centerX, float centerY) {
        setState(STATE_ANIMATE_ROTATE);

        mAnimateRotateLastValue = mRotateDegree;
        mRotater.startRotate(mRotateDegree, targetDegree, centerX, centerY);
        postInvalidate();
    }

    private void AnimateRotateImageInner(float targetDegree, float centerX, float centerY, int duration) {
        setState(STATE_ANIMATE_ROTATE);

        mAnimateRotateLastValue = mRotateDegree;
        mRotater.startRotate(mRotateDegree, targetDegree, duration, centerX, centerY);
        postInvalidate();
    }

    private void cancelRotateAnimate() {
        if (mRotater != null) {
            mRotater.abortAnimation();
            mAnimateRotateLastValue = -1;
            setState(STATE_NONE);
        }
    }

    private void fixRotateTrans() {
        transformMatrix();

        mMatrix.getValues(mMatrixValues);
        float transX = mMatrixValues[Matrix.MTRANS_X];
        float transY = mMatrixValues[Matrix.MTRANS_Y];

        transX = getImageWidth() < mViewWidth ? (mViewWidth - getImageWidth()) / 2 : transX;
        transY = getImageHeight() < mViewHeight ? (mViewHeight - getImageHeight()) / 2 : transY;
        mMatrixValues[Matrix.MTRANS_X] = transX;
        mMatrixValues[Matrix.MTRANS_Y] = transY;

        mMatrix.setValues(mMatrixValues);
        restoreMatrix();
    }

    private void printlnMatrix(String hintMessage) {
        mMatrix.getValues(mMatrixValues);
        L.e("----------------" + hintMessage + " begin----------------");
        L.e(mMatrixValues[0] + " , " + mMatrixValues[1] + " , " + mMatrixValues[2]);
        L.e(mMatrixValues[3] + " , " + mMatrixValues[4] + " , " + mMatrixValues[5]);
        L.e(mMatrixValues[6] + " , " + mMatrixValues[7] + " , " + mMatrixValues[8]);
        L.e("----------------" + hintMessage + " end-----------------");

    }

    private void savePreviousImageInfos() {
        if (mViewHeight != 0 && mViewWidth != 0) {
            mPrevViewHeight = mViewHeight;
            mPrevViewWidth = mViewWidth;

            mMatrix.getValues(mMatrixValues);
            mPrevMatrix.setValues(mMatrixValues);
        }
    }

    private boolean checkHasPreviousImageInfos() {
        return mPrevMatrix != null && mPrevViewHeight != 0 && mPrevViewWidth != 0;
    }

    @Override
    public void setImageResource(int resId) {
        savePreviousImageInfos();
        super.setImageResource(resId);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mShouldCatchNormalGesture) {
            mGestureDetector.onTouchEvent(event);
        }
        if (mShouldCatchRotateGesture) {
            mRotateGestureDetector.onTouchEvent(event);
        }
        if (mShouldCatchScaleGesture) {
            mScaleGestureDetector.onTouchEvent(event);
        }
        if (mShouldCatchRotateGesture
                || mShouldCatchScaleGesture
                || mShouldCatchNormalGesture) {
            setImageMatrix(mMatrix);
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int newX = mScroller.getCurrX();
            int newY = mScroller.getCurrY();
            int transX = (int) (newX - mFlingCurrentX);
            int transY = (int) (newY - mFlingCurrentY);

            mFlingCurrentX = newX;
            mFlingCurrentY = newY;
            translateImageInner(transX, transY);
            setImageMatrix(mMatrix);
            postInvalidate();
        } else {
            mFlingCurrentY = 0;
            mFlingCurrentX = 0;
        }

        if (mScaler.computeScrollOffset()
                && mAnimateScaleLastValue != 0) {
            final float currentScale = mScaler.getCurrentScale();
            final float focusX = mScaler.getCenterX();
            final float focusY = mScaler.getCenterY();
            float deltaScale = (currentScale) / mAnimateScaleLastValue; //0~1;
            mAnimateScaleLastValue = currentScale;

            scaleImageInner(deltaScale, focusX, focusY, true);
            setImageMatrix(mMatrix);
            postInvalidate();
        } else {
            mAnimateScaleLastValue = 0;
        }

        if (mRotater.computeScrollOffset()
                && mAnimateRotateLastValue != -1) {

            final float currentDegree = mRotater.getCurrentDegree();
            final float centerX = mRotater.getCenterX();
            final float centerY = mRotater.getCenterY();
            rotateImageInner(-mAnimateRotateLastValue, centerX, centerY);
            rotateImageInner(currentDegree, centerX, centerY);
            mAnimateRotateLastValue = currentDegree;
            setImageMatrix(mMatrix);
            postInvalidate();
        } else {
            mAnimateRotateLastValue = -1;
        }
    }

    /**
     * switch this catch normal gesture like fling , scorll , double tab;
     *
     * @param shouldCatchNormalGesture the flag that switch
     */
    public void shouldCatchNormalGesture(boolean shouldCatchNormalGesture) {
        this.mShouldCatchNormalGesture = shouldCatchNormalGesture;
    }

    /**
     * switch this catch scale gesture;
     *
     * @param shouldCatchScaleGesture the flag that switch
     */
    public void shouldCatchScaleGesture(boolean shouldCatchScaleGesture) {
        this.mShouldCatchScaleGesture = shouldCatchScaleGesture;
    }

    /**
     * switch this catch rotate gesture;
     *
     * @param shouldCatchRotateGesture the flag that switch
     */
    public void shouldCatchRotateGesture(boolean shouldCatchRotateGesture) {
        this.mShouldCatchRotateGesture = shouldCatchRotateGesture;
    }

    /**
     * @param targetScale:scale what you want
     * @see #scaleImage(float, float, float, boolean) ;
     */
    public void scaleImage(float targetScale) {
        scaleImage(targetScale, true);
    }

    /**
     * @param targetScale :scale what you want
     * @param animate     : switch animation
     * @see #scaleImage(float, float, float, boolean) ;
     */
    public void scaleImage(float targetScale, boolean animate) {
        scaleImage(targetScale, mViewWidth / 2, mViewHeight / 2, animate);
    }

    /**
     * @param targetScale :scale what you want
     * @param centerX     :zoom      center x
     * @param centerY     :zoom      center y
     * @see #scaleImage(float, float, float, boolean) ;
     */
    public void scaleImage(float targetScale, float centerX, float centerY) {
        scaleImage(targetScale, centerX, centerY, true);
    }

    /**
     * zoom image that you that;
     * zoom range mSuperMinScale ~ mSuperMaxScale
     *
     * @param targetScale :scale what you want
     * @param centerX     :zoom center x
     * @param centerY     :zoom center y
     * @param animate     :switch animation
     */
    public void scaleImage(float targetScale, float centerX, float centerY, boolean animate) {
        if (targetScale > mSuperMaxScale) {
            L.e("设置的scale太大了，已经处理到了mSuperMaxScale");
        }
        if (targetScale < mSuperMinScale) {
            L.e("设置的scale太小了，已经处理到了mSuperMaxScale");
        }

        if (!animate) {
            scaleImageInner(targetScale, centerX, centerY, false);
            setImageMatrix(mMatrix);
        }

        if (animate) {
            cancelScaleAnimate();
            animateScaleImageInner(mNormalizedScale * targetScale, centerX, centerY);
        }
    }

    /**
     * @param targetScale :scale where you want
     * @param duration    :animate  duration
     * @see #scaleImage
     */
    public void scaleImage(float targetScale, int duration) {
        scaleImage(targetScale, mViewWidth / 2, mViewHeight / 2, duration);
    }

    /**
     * zoom image that you that;
     * zoom range mSuperMinScale ~ mSuperMaxScale
     *
     * @param targetScale :scale where you want
     * @param centerX     :zoom center x
     * @param centerY     :zoom center Y
     * @param duration    :animate duration
     */
    public void scaleImage(float targetScale, float centerX, float centerY, int duration) {
        if (targetScale > mSuperMaxScale) {
            L.e("设置的scale太大了，已经处理到了mSuperMaxScale");
        }
        if (targetScale < mSuperMinScale) {
            L.e("设置的scale太小了，已经处理到了mSuperMaxScale");
        }
        cancelScaleAnimate();
        animateScaleImageInner(mNormalizedScale * targetScale, centerX, centerY, duration);
    }

    /**
     * set super max scale
     *
     * @param superMaxScale
     */
    public void setSuperMaxScale(int superMaxScale) {
        if (superMaxScale < mMaxScale) {
            throw new IllegalArgumentException("superMaxScale must be greater maxScale");
        }
        this.mSuperMaxScale = superMaxScale;
    }

    /**
     * set super min scale
     *
     * @param superMinScale
     */
    public void setSuperMinScale(int superMinScale) {
        if (superMinScale > mMaxScale) {
            throw new IllegalArgumentException("superMinScale must be smaller minScale");
        }
        this.mSuperMinScale = superMinScale;
    }

    /**
     * @param degree : the degree that you want to rotate image
     * @see #rotateImageInner(float, float, float)
     */
    public void rotateImage(float degree) {
        rotateImage(degree, true);
    }

    /**
     * rotate image
     *
     * @param degree   :the degree that you want to rotate image
     * @param animate: switch animation
     */
    public void rotateImage(float degree, boolean animate) {
        if (animate) {
            cancelRotateAnimate();
            AnimateRotateImageInner(mRotateDegree + degree, mViewWidth / 2, mViewHeight / 2);
        } else {
            rotateImageInner(degree, mViewWidth / 2, mViewHeight / 2);
            setImageMatrix(mMatrix);
        }
    }

    /**
     * rotate image
     *
     * @param degree   :the degree that you want to rotate image
     * @param duration : animate duration
     */
    public void rotateImage(float degree, int duration) {
        AnimateRotateImageInner(mRotateDegree + degree, mViewWidth / 2, mViewHeight / 2, duration);
    }

    /**
     * @param transX :translate of horizontal direction
     * @param transY :translate of vertical direction
     * @see #translateImage(float, float, boolean)
     */
    public void translateImage(float transX, float transY) {
        translateImage(transX, transY, true);
    }

    /**
     * translate image
     *
     * @param transX  : translate of horizontal direction
     * @param transY  : translate of vertical direction
     * @param animate : switch animation
     */
    public void translateImage(float transX, float transY, boolean animate) {
        if (!animate) {
            translateImageInner(transX, transY);
            setImageMatrix(mMatrix);
        } else {
            cancelTranslateAnimate();
            animateTranslateImageInner(transX, transY);
        }
    }

    /**
     * translate image
     *
     * @param transX   :translate of horizontal direction
     * @param transY   :translate of vertical direction
     * @param duration :animate duration
     */
    public void translateImage(float transX, float transY, int duration) {
        cancelTranslateAnimate();
        animateTranslateImageInner(transX, transY, duration);
    }

    public
    @STATE
    int getState() {
        return mState;
    }

    public float getImageWidth() {
        return mAdjustedDrawableWidth * mNormalizedScale;
    }

    public float getImageHeight() {
        return mAdjustedDrawableHeight * mNormalizedScale;
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            cancelTranslateAnimate();
            setState(STATE_DRAG);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float targetScale = mNormalizedScale > mMinScale ? mMinScale : mMaxScale;
            final float focusX = e.getX();
            final float focusY = e.getY();
            L.d("当前手势为:双击,从" + mNormalizedScale + "到" + targetScale + "进行缩放,缩放中心为:centerX = " + focusX + ",centerY = " + focusY);
            animateScaleImageInner(targetScale, focusX, focusY);
            return super.onDoubleTap(e);
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (mState == STATE_DRAG) {
                L.d("当前手势为:滑动,滑动距离为: distanceX = " + distanceX + ",distanceY = " + distanceY);
                translateImageInner(-distanceX, -distanceY);
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (mState == STATE_DRAG) {
                L.d("当前手势为:抛,当前速度:velocityX = " + velocityX + ",velocityY = " + velocityY);

                cancelTranslateAnimate();
                flingImageInner(velocityX, velocityY);
            }

            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (mState == STATE_ZOOM || mState == STATE_ROTATE) {
                scaleImageInner(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY(), true);
                return true;
            }
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            setState(STATE_ZOOM);
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            needToScaleBoundary();
        }
    }

    class RotateListener implements RotateGestureDetector.OnRotateGestureListener {
        @Override
        public boolean onRotateBegin(RotateGestureDetector detector) {
            setState(STATE_ROTATE);
            return true;
        }

        @Override
        public boolean onRotate(RotateGestureDetector detector) {
            if (mState == STATE_ROTATE
                    || mState == STATE_ZOOM) {
                rotateImageInner(detector.getDegree(), mViewWidth / 2, mViewHeight / 2);
                L.d("当前手势为:旋转,旋转的角度的为:" + detector.getDegree() + "缩放的中心(默认)为: centerX = " + (mViewWidth / 2) + "centerY = " + (mViewHeight / 2));
            }
            return true;
        }

        @Override
        public void onRotateEnd(RotateGestureDetector detector) {
            setState(STATE_NONE);
        }
    }
}

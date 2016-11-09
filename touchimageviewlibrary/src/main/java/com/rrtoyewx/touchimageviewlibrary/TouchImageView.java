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
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;

import com.rrtoyewx.touchimageviewlibrary.gesturedetector.RotateGestureDetector;
import com.rrtoyewx.touchimageviewlibrary.helper.Scaler;
import com.rrtoyewx.touchimageviewlibrary.util.L;

/**
 * Created by Rrtoyewx on 2016/11/2.
 */

public class TouchImageView extends ImageView {
    private static final float SUPER_MIN_MULTIPLIER = .75f;
    private static final float SUPER_MAX_MULTIPLIER = 1.25f;

    private static final int VALUE_MIN_SCALE = 1;
    private static final int VALUE_MAX_SCALE = 3;

    private static final int STATE_NONE = 0;
    private static final int STATE_DRAG = 1;
    private static final int STATE_ZOOM = 2;
    private static final int STATE_ROTATE = 3;
    private static final int STATE_FLING = 4;
    private static final int STATE_ANIMATE_ZOOM = 5;

    private static final Interpolator DEFAULT_ANIMATION_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    @IntDef({STATE_NONE, STATE_DRAG, STATE_ZOOM, STATE_ROTATE, STATE_FLING, STATE_ANIMATE_ZOOM})
    public @interface STATE {
    }

    @STATE
    private int mState = STATE_NONE;

    private float mRotateDegree = 0;
    private float mNormalizedScale = 1;
    private float mMinScale = VALUE_MIN_SCALE;
    private float mMaxScale = VALUE_MAX_SCALE;

    private float mSuperMinScale = VALUE_MIN_SCALE * SUPER_MIN_MULTIPLIER;
    private float mSuperMaxScale = VALUE_MAX_SCALE * SUPER_MAX_MULTIPLIER;

    private Matrix mMatrix;
    private float[] mMatrixValues = new float[9];


    private int mViewHeight;
    private int mViewWidth;
    private float mMathViewWidth;
    private float mMathViewHeight;

    private Context mContext;
    private ScaleType mScaleType;

    private boolean mImageRenderedAtLeastOnce;
    private boolean mOnDrawReady;

    private Interpolator mInterpolator = DEFAULT_ANIMATION_INTERPOLATOR;

    private ScrollerCompat mScroller;
    private float mFlingCurrentX;
    private float mFlingCurrentY;

    private float mScaleCurrentFactor;
    private Scaler mScaler;

    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetectorCompat mGestureDetector;
    private RotateGestureDetector mRotateGestureDetector;

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

        mMatrix = new Matrix();

        if (mScaleType == null) {
            mScaleType = ScaleType.FIT_CENTER;
        }

        setImageMatrix(mMatrix);
        setScaleType(ScaleType.MATRIX);
        mOnDrawReady = false;
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
        mOnDrawReady = true;
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
        mMathViewWidth = scaleX * drawableWidth;
        mMathViewHeight = scaleY * drawableHeight;

        // is not scale
        if (mNormalizedScale == 1) {
            mNormalizedScale = 1;
            mRotateDegree = 0;

            mMatrix.setScale(scaleX, scaleY);
            mMatrix.postTranslate(xSpace / 2, ySpace / 2);
        }

//        fixTrans();
        setImageMatrix(mMatrix);
        printlnMatrix("init");
    }

    private void setState(@STATE int state) {
        this.mState = state;
    }

    private void translateImage(float transX, float transY) {
        if (getImageHeight() <= mViewHeight) {
            transY = 0;
        }

        if (getImageWidth() <= mViewWidth) {
            transX = 0;
        }

        mMatrix.postTranslate(transX, transY);
        fixDragTrans();
        setImageMatrix(mMatrix);
    }

    private void fixDragTrans() {
        transformMatrix();

        mMatrix.getValues(mMatrixValues);
        float transX = mMatrixValues[Matrix.MTRANS_X];
        float transY = mMatrixValues[Matrix.MTRANS_Y];

        float minTransY = mViewHeight >= getImageHeight() ? 0 : mViewHeight - getImageHeight();
        float maxTransY = mViewHeight >= getImageHeight() ? mViewHeight - getImageHeight() : 0;

        float minTransX = mViewWidth >= getImageWidth() ? 0 : mViewWidth - getImageWidth();
        float maxTransX = mViewWidth >= getImageWidth() ? mViewWidth - getImageWidth() : 0;

        transX = transX > maxTransX ? maxTransX : (transX < minTransX ? minTransX : transX);
        transY = transY > maxTransY ? maxTransY : (transY < minTransY ? minTransY : transY);
        mMatrixValues[Matrix.MTRANS_X] = transX;
        mMatrixValues[Matrix.MTRANS_Y] = transY;

        mMatrix.setValues(mMatrixValues);
        restoreMatrix();
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

    private void startFling(float velocityX, float velocityY) {
        setState(STATE_FLING);
        transformMatrix();
        mMatrix.getValues(mMatrixValues);

        int startX = (int) mMatrixValues[Matrix.MTRANS_X];
        int startY = (int) mMatrixValues[Matrix.MTRANS_Y];

        int minX;
        int maxX;
        int minY;
        int maxY;

        //enlarge
        if (getImageWidth() > mViewWidth) {
            minX = (int) (mViewWidth - getImageWidth());
            maxX = 0;
        } else {
            //narrow
            minX = maxX = startX;
        }

        //enlarge
        if (getImageHeight() > mViewHeight) {
            minY = (int) (mViewHeight - getImageHeight());
            maxY = 0;
        } else {
            //narrow
            minY = maxY = startY;
        }

        mScroller.fling(startX, startY, (int) velocityX, (int) velocityY, minX, maxX, minY, maxY);
        mFlingCurrentX = startX;
        mFlingCurrentY = startY;

        restoreMatrix();
    }

    private void cancelFling() {
        if (mScroller != null) {
            mScroller.abortAnimation();
            setState(STATE_NONE);
        }

        mFlingCurrentX = 0;
        mFlingCurrentY = 0;
    }

    private void scaleImage(float factor, float centerX, float centerY, boolean stretchImageToSuper) {
        float lowerScale;
        float upperScale;
        if (stretchImageToSuper) {
            lowerScale = mSuperMinScale;
            upperScale = mSuperMaxScale;
        } else {
            lowerScale = mMinScale;
            upperScale = mMaxScale;
        }

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
        fixDragTrans();
        setImageMatrix(mMatrix);
    }

    private void stretchImageToSuper() {
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
            animateToScaleBoundary(targetScale, mViewWidth / 2, mViewHeight / 2);
        }
    }

    private void animateToScaleBoundary(float targetScale, float focusX, float focusY) {
        setState(STATE_ANIMATE_ZOOM);
        mScaleCurrentFactor = mNormalizedScale;
        mScaler.startScale(mNormalizedScale, targetScale, focusX, focusY);

        postInvalidate();
    }

    private void cancelAnimateToScaleBoundary() {
        mScaler.abortAnimation();
        mScaleCurrentFactor = 0;
        Log.e("TAG", "cancelAnimateToScaleBoundary");
    }

    private PointF transformTouchPositionToBitmapCoordinate(float touchX, float touchY) {
        PointF point = new PointF(touchX, touchY);
        Drawable drawable = getDrawable();
        if (drawable != null) {
            transformMatrix();
            mMatrix.getValues(mMatrixValues);
            final float transX = mMatrixValues[Matrix.MTRANS_X];
            final float transY = mMatrixValues[Matrix.MTRANS_Y];

            point.x = (touchX - transX) / getImageWidth() * drawable.getIntrinsicWidth();
            point.y = (touchY - transY) / getImageHeight() * drawable.getIntrinsicHeight();

            restoreMatrix();
        }
        return point;
    }

    private PointF transformBitmapCoordinateToTouchPosition(float coordinateX, float coordinateY) {
        PointF point = new PointF();
        Drawable drawable = getDrawable();
        if (drawable != null) {
            transformMatrix();
            mMatrix.getValues(mMatrixValues);
            final float transX = mMatrixValues[Matrix.MTRANS_X];
            final float transY = mMatrixValues[Matrix.MTRANS_Y];

            point.x = transX + coordinateX / drawable.getIntrinsicWidth() * getImageWidth();
            point.y = transY + coordinateY / drawable.getIntrinsicHeight() * getImageHeight();

            restoreMatrix();
        }

        return point;
    }

    private void translateImageToCenterTouchPosition(float factor, float touchX, float touchY) {
        PointF originTouchPoint = transformBitmapCoordinateToTouchPosition(touchX, touchY);
        PointF endPoint = new PointF(mViewWidth / 2, mViewHeight / 2);
        final float targetX = originTouchPoint.x + (endPoint.x - originTouchPoint.x) * factor;
        final float targetY = originTouchPoint.y + (endPoint.y - originTouchPoint.y) * factor;
        Log.e("TAG", "translateImageToCenterTouchPosition targetX" + targetX
                + "targetY" + targetY);
        Log.e("TAG", "translateImageToCenterTouchPosition originTouchPoint.x" + originTouchPoint.x
                + "originTouchPoint.y" + originTouchPoint.y);

        mMatrix.postTranslate(targetX - originTouchPoint.x, targetY - originTouchPoint.y);
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

    private void rotateImage(float degree, float centerX, float centerY) {
        mMatrix.getValues(mMatrixValues);
        mMatrix.postRotate(degree, centerX, centerY);
        mRotateDegree += degree;
        setImageMatrix(mMatrix);
    }

    private void printlnMatrix(String hintMessage) {
        mMatrix.getValues(mMatrixValues);
        for (int i = 0; i < mMatrixValues.length; i++) {
            L.e("----------------" + hintMessage + " begin----------------");
            L.e(mMatrixValues[0] + " , " + mMatrixValues[1] + " , " + mMatrixValues[2]);
            L.e(mMatrixValues[3] + " , " + mMatrixValues[4] + " , " + mMatrixValues[5]);
            L.e(mMatrixValues[6] + " , " + mMatrixValues[7] + " , " + mMatrixValues[8]);
            L.e("----------------" + hintMessage + " end-----------------");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        mRotateGestureDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);

        return true;
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

            translateImage(transX, transY);
            postInvalidate();
        } else {
            mFlingCurrentY = 0;
            mFlingCurrentX = 0;
        }

        if (mScaler.computeScrollOffset()
                && mScaleCurrentFactor != 0) {
            final float currentScale = mScaler.getCurrentScale();
            final float focusX = mScaler.getCenterX();
            final float focusY = mScaler.getCenterY();
            final float currentFactor = mScaler.getCurrentFactor();

            float deltaScale = (currentScale - mScaleCurrentFactor) / mScaleCurrentFactor + 1; //0~1;
            mScaleCurrentFactor = currentScale;

            scaleImage(deltaScale, focusX, focusY, true);
            //translateImageToCenterTouchPosition(currentFactor, focusX, focusY);
            postInvalidate();
        } else {
            mScaleCurrentFactor = 0;
        }
    }

    public float getImageWidth() {
        return mMathViewWidth * mNormalizedScale;
    }

    public float getImageHeight() {
        return mMathViewHeight * mNormalizedScale;
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            cancelFling();
            setState(STATE_DRAG);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
//            float targetScale = mNormalizedScale > 1.5f ? 1.5f : mMaxScale;
//            final float focusX = e.getX();
//            final float focusY = e.getY();
//            Log.e("TAG", "focusX:" + focusX + "focusY:" + focusY);
//            cancelAnimateToScaleBoundary();
//            PointF drawableTouchPoint = transformTouchPositionToBitmapCoordinate(focusX, focusY);
//            Log.e("TAG", "drawableTouchPoint.x:" + drawableTouchPoint.x + "drawableTouchPoint.y" + drawableTouchPoint.y);
//            animateToScaleBoundary(targetScale, drawableTouchPoint.x, drawableTouchPoint.y);

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
                translateImage(-distanceX, -distanceY);
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (mState == STATE_DRAG) {
                cancelFling();
                startFling(velocityX, velocityY);
            }

            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (mState == STATE_ZOOM || mState == STATE_ROTATE) {
                L.e("onScale : " + detector.getScaleFactor());
                scaleImage(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY(), true);
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
            setState(STATE_NONE);
            cancelAnimateToScaleBoundary();
            stretchImageToSuper();
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
                rotateImage(detector.getDegree(), mViewWidth / 2, mViewHeight / 2);
            }
            return true;
        }

        @Override
        public void onRotateEnd(RotateGestureDetector detector) {
            setState(STATE_NONE);
        }
    }
}

package com.rrtoyewx.touchimageviewlibrary;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.IntDef;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

import com.rrtoyewx.touchimageviewlibrary.gesturedetector.RotateGestureDetector;
import com.rrtoyewx.touchimageviewlibrary.listener.OnUpListener;
import com.rrtoyewx.touchimageviewlibrary.util.L;

/**
 * Created by Rrtoyewx on 2016/11/2.
 */

public class TouchImageView extends ImageView implements OnUpListener {
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

    @IntDef({STATE_NONE, STATE_DRAG, STATE_ZOOM, STATE_ROTATE, STATE_FLING, STATE_ANIMATE_ZOOM})
    public @interface STATE {
    }

    @STATE
    private int mState = STATE_NONE;

    private float mNormalizedScale = 1;
    private float mMinScale = VALUE_MIN_SCALE;
    private float mMaxScale = VALUE_MAX_SCALE;

    private float mSuperMinScale = VALUE_MIN_SCALE * SUPER_MIN_MULTIPLIER;
    private float mSuperMaxScale = VALUE_MAX_SCALE * SUPER_MAX_MULTIPLIER;

    private Matrix mMatrix;
    private Matrix mPrevMatrix;
    private float[] mMatrixValues = new float[9];

    private int mViewHeight;
    private int mViewWidth;
    private float mMathViewWidth;
    private float mMathViewHeight;

    private int mPrevViewHeight;
    private int mPrevViewWidth;
    private float mPrevMathViewWidth;
    private float mPrevMathViewHeight;

    private Context mContext;
    //private Fling fling;
    private ScaleType mScaleType;

    private boolean mImageRenderedAtLeastOnce;
    private boolean mOnDrawReady;

    private ScrollerCompat mScroller;
    private float mFlingCurrentX;
    private float mFlingCurrentY;

    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetectorCompat mGestureDetector;
    private RotateGestureDetector mRotateGestureDetetor;

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
        mRotateGestureDetetor = new RotateGestureDetector(context, new RotateListener());

        mScroller = ScrollerCompat.create(mContext);

        mMatrix = new Matrix();
        mPrevMatrix = new Matrix();

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
                || mMatrix == null
                || mPrevMatrix == null) {
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
            mMatrix.setScale(scaleX, scaleY);
            mMatrix.postTranslate(xSpace / 2, ySpace / 2);
            mNormalizedScale = 1;
        }

//        fixTrans();
        setImageMatrix(mMatrix);
        L.e("scale:" + scaleX);
    }

    private void savePreviousImageValues() {
        if (mMatrix != null && mPrevMatrix != null) {
            mMatrix.getValues(mMatrixValues);
            mPrevMatrix.setValues(mMatrixValues);
            mPrevViewHeight = mViewHeight;
            mPrevMathViewWidth = mViewWidth;
            mPrevMathViewHeight = mMathViewHeight;
            mPrevMathViewWidth = mMathViewWidth;
        }
    }

    private void setState(@STATE int state) {
        this.mState = state;
    }

    private void translateImage(float transX, float transY) {
        mMatrix.postTranslate(transX, transY);
        setImageMatrix(mMatrix);
    }

    private void springBackTranslate() {
        mMatrix.getValues(mMatrixValues);
        fixDragTrans(mMatrixValues[Matrix.MTRANS_X], mMatrixValues[Matrix.MTRANS_Y]);
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
        L.e("scale image factor:" + factor);
        mMatrix.postScale(factor, factor, centerX, centerY);
        // fixScaleTrans();
        //fixTrans();
        printlnMatrix();
        setImageMatrix(mMatrix);
    }

    private void startFling(float velocityX, float velocityY) {
        setState(STATE_FLING);
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
    }

    private void cancelFling() {
        if (mScroller != null) {
            mScroller.abortAnimation();
            setState(STATE_NONE);
        }

        mFlingCurrentX = 0;
        mFlingCurrentY = 0;
    }

    private void rotateImage(float degree, float centerX, float centerY) {
//        mMatrix.postRotate(degree, centerX, centerY);
        mMatrix.getValues(mMatrixValues);
        mMatrix.postRotate(degree, centerX, centerY);
//        mMatrix.preTranslate(-mMatrixValues[Matrix.MTRANS_X], -mMatrixValues[Matrix.MTRANS_Y]);
//        mMatrix.postTranslate(mMatrixValues[Matrix.MTRANS_X], mMatrixValues[Matrix.MTRANS_Y]);
        setImageMatrix(mMatrix);
        fixRotateTrans();
    }

    private void fixTrans() {
        mMatrix.getValues(mMatrixValues);
        float transX = mMatrixValues[Matrix.MTRANS_X];
        float transY = mMatrixValues[Matrix.MTRANS_Y];

        float fixTransX = getFixTrans(transX, mViewWidth, getImageWidth());
        float fixTransY = getFixTrans(transY, mViewHeight, getImageHeight());

        if (fixTransX != 0 || fixTransY != 0) {
            mMatrix.postTranslate(fixTransX, fixTransY);
        }
    }

    private float getFixTrans(float trans, float viewSize, float contentSize) {
        float minTrans;
        float maxTrans;
        if (contentSize <= viewSize) {
            minTrans = 0;
            maxTrans = viewSize - contentSize;
        } else {
            minTrans = viewSize - contentSize;
            maxTrans = 0;
        }

        if (trans < minTrans) {
            return -trans + minTrans;
        }
        if (trans > maxTrans) {
            return -trans + maxTrans;
        }

        return 0;
    }

    private void fixDragTrans(float transX, float transY) {
        float endTransX = getFixDragTrans(transX, mViewWidth, getImageWidth());
        float endTransY = getFixDragTrans(transY, mViewHeight, getImageHeight());
        L.e("transX: " + transX + "transY: " + transY + " endTransX: " + endTransX + " endTranY: " + endTransY);
        mFlingCurrentX = transX;
        mFlingCurrentY = transY;
        mScroller.startScroll((int) transX,
                (int) transY,
                Math.round(endTransX - transX),
                Math.round(endTransY - transY),
                (int) (Math.hypot(endTransX - transX, endTransY - transY)));
        postInvalidate();
    }

    private float getFixDragTrans(float trans, float viewSize, float contentSize) {
        return viewSize > contentSize ? (viewSize - contentSize) / 2 : 0;
    }

    private void fixScaleTrans() {
        mMatrix.getValues(mMatrixValues);
        float transX = mMatrixValues[Matrix.MTRANS_X];
        float transY = mMatrixValues[Matrix.MTRANS_Y];

        float fixTransX = getFixScaleTrans(transX, mViewWidth, getImageWidth());
        float fixTransY = getFixScaleTrans(transY, mViewHeight, getImageHeight());

        mMatrix.postTranslate(fixTransX, fixTransY);
    }

    private float getFixScaleTrans(float trans, float viewSize, float contentSize) {
        if (viewSize >= contentSize) {
            return (viewSize - contentSize) / 2 - trans;
        }
        return 0;
    }

    private void fixRotateTrans() {
        printlnMatrix();
//        mMatrix.getValues(mMatrixValues);
//        float transX = mMatrixValues[Matrix.MTRANS_X];
//        float transY = mMatrixValues[Matrix.MTRANS_Y];
//
//        float fixTransX = getFixRotateTrans(transX, mViewWidth, getImageWidth());
//        float fixTransY = getFixRotateTrans(transY, mViewHeight, getImageHeight());
//
//        mMatrix.postTranslate(fixTransX, fixTransY);
    }

    private float getFixRotateTrans(float trans, float viewSize, float contentSize) {
        if (viewSize >= contentSize) {
            return (viewSize - contentSize) / 2 - trans;
        }
        return 0;
    }

    private void printlnMatrix() {
        mMatrix.getValues(mMatrixValues);
        for (int i = 0; i < mMatrixValues.length; i++) {
            L.e(mMatrixValues[0] + " , " + mMatrixValues[1] + " , " + mMatrixValues[2]);
            L.e(mMatrixValues[3] + " , " + mMatrixValues[4] + " , " + mMatrixValues[5]);
            L.e(mMatrixValues[6] + " , " + mMatrixValues[7] + " , " + mMatrixValues[8]);
        }
    }

    //------------------------------ change image drawable----------------------
    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        savePreviousImageValues();
        fillImageToView();
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        savePreviousImageValues();
        fillImageToView();
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        savePreviousImageValues();
        fillImageToView();
    }

    @Override
    public void setImageURI(Uri uri) {
        super.setImageURI(uri);
        savePreviousImageValues();
        fillImageToView();
    }

    //--------------------------------change image drawable-----------------------
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        mRotateGestureDetetor.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);

        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_UP) {
         //   onUpEvent();
        }

        return true;
    }

    @Override
    public void onUpEvent() {
        springBackTranslate();
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

            mMatrix.postTranslate(transX, transY);
            //fixTrans();
            setImageMatrix(mMatrix);

            L.e("computeScroll()");

            postInvalidate();
        }
    }

    public float getImageWidth() {
        return mMathViewWidth * mNormalizedScale;
    }

    public float getImageHeight() {
        return mMathViewHeight * mNormalizedScale;
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
        }
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            cancelFling();
            setState(STATE_DRAG);
            return true;
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
                L.e("STATE: = onScroll()");
                translateImage(-distanceX, -distanceY);
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (mState == STATE_DRAG) {
                L.e("STATE: = onFling()");
                // cancelFling();
                // startFling(velocityX, velocityY);
            }

            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }

    class RotateListener implements RotateGestureDetector.OnRotateGestureListener {
        @Override
        public boolean onRotateBegin(RotateGestureDetector detector) {
            setState(STATE_ROTATE);
            cancelFling();
            return true;
        }

        @Override
        public boolean onRotate(RotateGestureDetector detector) {
            if (mState == STATE_ROTATE
                    || mState == STATE_ZOOM) {
                L.e("STATE: = onRotate(), factor: = " + detector.getDegree());
                rotateImage(detector.getDegree(), mViewWidth / 2, mViewHeight / 2);
            }
            return true;
        }

        @Override
        public void onRotateEnd(RotateGestureDetector detector) {
            setState(STATE_NONE);
            L.e("onRotateEnd");
        }
    }

}

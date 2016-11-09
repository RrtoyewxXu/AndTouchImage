# AndTouchImage
2016.11.4
## 当前进度
1. 缩放手势完成
2. 平移手势完成
3. 旋转手势完成（部分完成：旋转中心为中间，需要平移来加修复）
4. 缩放和平移在共同使用完成

## 待完成
1. 缩放，平移，旋转手势共同使用
2. 更换资源图片卡顿
3. 更换资源图片保持上一张图片的状态
4. 缩放的回弹效果
5. 暂时没测出来的bug

---

2016.11.8
## 进度状况
1. 2016.11.08 14:40  基本功能完善 支持缩放，旋转，平移，抛等手势
2. 2016.11.08 16:58  引入自定义Scaler，类似与Scroller来实现图片缩放回弹效果
3. 2016.11.08 17:28  修复图片的缩放回弹无法到边缘的bug
4. 2016.11.09 11:19  修复Scaler中的duration完成过后，mCurrentScale != mFinalScale
5


说明：
通过更改手势来使用测试当前页面
```
        mScaleGestureDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);
        //mRotateGestureDetetor.onTouchEvent(event);
```

```
//缩放图片
scaleImage()
//旋转图片
rotateImage()
```

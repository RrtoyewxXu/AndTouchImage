# AndTouchImage
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

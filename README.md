# AndTouchImage
AndTouchImage，很够很好识别缩放，缩放，旋转，双击放大／缩小等手势，
在TouchImage的基础的改进很多，但是后来发现当引入旋转的手势后，维护图片的中的矩阵太过复杂，不如重新写了一份，这才有了AndTouchView
## 效果图
[!手指直接操作屏幕的效果图](images/pointer_gesture_example.gif)
[!通过代码来操作的效果图](images/command_example.gif)
## 使用情况，
1. 微博／微信的大图浏览模式，对单图片的操作
2. 裁剪图片的使用底部的图片
3. 继承ImageView，便于改动 

##  HOW TO USE
### 打开捕捉手势的开关，默认捕捉缩放，旋转，拖拽，抛，以及双击
```
    //normal gesture
    public void shouldCatchNormalGesture(boolean shouldCatchNormalGesture){...}
    
    //scale gesture
    public void shouldCatchScaleGesture(boolean shouldCatchScaleGesture){...}
       
    //rotate gesture
    public void shouldCatchRotateGesture(boolean shouldCatchRotateGesture){...}
```

### 缩放图片
缩放的范围在mSuperMinScale~mSuperMaxScale之间，显示图片在mMinScale～mMaxScale
```
public void scaleImage(float targetScale){...}

public void scaleImage(float targetScale, boolean animate) {...}
   
public void scaleImage(float targetScale, float centerX, float centerY){...}

public void scaleImage(float targetScale, float centerX, float centerY, boolean animate) {...}

public void scaleImage(float targetScale, int duration) {...}

public void scaleImage(float targetScale, float centerX, float centerY, int duration) {...}
```
参数的说明：
- targetScale：你需要缩放的比例，缩放后的图片的比例为你targetScale * 当前图片的比例
- animate:是否开启动画缩放
- duration:动画持续的时间
- centerX:缩放中心的X坐标，相对于屏幕来说
- centerY:缩放中心的Y坐标，相对于屏幕来说
  
### 旋转图片
```
public void rotateImage(float degree) {...}

public void rotateImage(float degree, int duration) {...}

public void rotateImage(float degree, boolean animate) {...}
```
参数说明：
- degree:你想要旋转的角度，旋转之后的图片的角度为你当前的图片的角度*degree
- animate:是否开启动画旋转
- duration:动画持续时间
旋转中心为视图的中心点，想过去更改这个点，让用户来设，后来发现旋转中心改成别的点较丑，有需求的话会更改的。

### 滑动图片
```
public void translateImage(float transX, float transY) {...}
 
public void translateImage(float transX, float transY, boolean animate) {...}

public void translateImage(float transX, float transY, int duration) {...}

```

参数说明：
- transX:平移的X轴的偏移量
- transY:平移的Y轴的偏移量
- animate:是否开启动画平移
- duration:动画持续时间
-----


## 改进
1. 双击缩放或者扩大的时候，双击按下的点能够尽可能显示在屏幕中间
2. 暴漏出监听
3. 切换图片保存状态


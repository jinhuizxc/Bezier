package com.example.jh.bezier;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AnimationDrawable;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by jinhui on 2017/6/20.
 * 邮箱: 1004260403@qq.com
 *
 * View组件的绘制会调用draw(Canvas canvas)方法，
 * draw过程中主要是先画Drawable背景，对 drawable调用setBounds()，
 * 然后是draw(Canvas c)方法。有点注意的是背景drawable的实际大小会影响view组件的大小，
 * drawable的实际大小通过getIntrinsicWidth()和getIntrinsicHeight()获取，
 * 当背景比较大时view组件大小等于背景drawable的大小。
 * 画完背景后，draw过程会调用onDraw(Canvas canvas)方法，然后就是dispatchDraw(Canvas canvas)方法，dispatchDraw()主要是分发给子组件进行绘制，
 *
 * 我们通常定制组件的时候重写的是onDraw()方法。值得注意的是ViewGroup容器组件的绘制，
 * 当它没有背景时直接调用的是dispatchDraw()方法, 而绕过了draw()方法，
 * 当它有背景的时候就调用draw()方法，而draw()方法里包含了dispatchDraw()方法的调用。
 * 因此要在ViewGroup上绘制东西的时候往往重写的是dispatchDraw()方法而不是onDraw()方法，
 * 或者自定制一个Drawable，
 * 重写它的draw(Canvas c)和 getIntrinsicWidth()，getIntrinsicHeight()方法，然后设为背景。
 *
 * @总结：
 * 绘制VIew本身的内容，通过调用View.onDraw(canvas)函数实现
 * 绘制自己的孩子通过dispatchDraw（canvas）实现
 */

public class RedPoint extends FrameLayout {


    private static final String TAG = "RedPoint";
    // 初始x、y的位置
    public static final int originX = 200;
    public static final int originY = 30;
    private Paint mPaint;//画笔

    private PointF mStart, mCurr;//起始点和当前点
    // 初始化圆的半径为30px
    private static final int RADIUS_DEFAULT = 40;
    private int mR = RADIUS_DEFAULT;//半径40px
    private int y;

    // 初始化路径
    private Path mPath;
    // 初始化文字
    private TextView mTipTextView;
    private static final int TV_PADDIGNG = 6;
    private ImageView explodeView;//播放动画
    private boolean isAnimating =false;
    private boolean mTouch = false;//默认为false

    public RedPoint(@NonNull Context context) {
        super(context);
        initView();
    }

    public RedPoint(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public RedPoint(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        //初始化画笔
        mPaint = new Paint();
        mPaint.setColor(Color.RED);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);//填充样式，这将将内容区域填充成蓝色

        //初始化圆心
        y = originY;
        mStart = new PointF(originX, originY);//起始圆心的位置坐标（100，100）
        mCurr = new PointF();

        //初始化路径，用于画曲线
        mPath = new Path();
        //初始化文字
        LayoutParams params = new LayoutParams(mR * 2, mR * 2);//长宽均为半径的2倍
        params.setMargins(originX - mR / 2, originY - mR / 2, 0, 0);//距离左边和上边100像素，与圆心重合
        mTipTextView = new TextView(getContext());
        mTipTextView.setLayoutParams(params);
        // 设置内边距
        mTipTextView.setPadding(TV_PADDIGNG, TV_PADDIGNG, TV_PADDIGNG, TV_PADDIGNG);
        // 设置textviews的红色背景
        mTipTextView.setBackgroundResource(R.drawable.tv_bg);
        mTipTextView.setTextColor(Color.WHITE);
        mTipTextView.setGravity(Gravity.CENTER);
        mTipTextView.setText("99+");
        //为RedPoint添加子view，即TextView
        addView(mTipTextView);
        //添加爆炸动画效果
        explodeView = new ImageView(getContext());
        explodeView.setLayoutParams(params);
        explodeView.setImageDrawable(getResources().getDrawable(R.drawable.explode));//设置动画资源
        explodeView.setVisibility(INVISIBLE);//不可见
        addView(explodeView);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.e(TAG, "onDraw 方法被执行...");  // onDraw方法不执行
    }

    /**
     * invalidate/postInvalidate后会调用
     * viewGroup中的调用顺序onDraw->dispatchDraw,在没有背景图的时候，跳过onDraw步骤
     * view中的均调用，原则上重写onDraw
     *
     * @param canvas
     */
    @Override
    protected void dispatchDraw(Canvas canvas) {

        Log.e(TAG, "dispatchDraw 方法被执行...");
        //保存第一个参数指定区域的画布内容，之后所有操作仅仅对画布有效
        canvas.saveLayer(new RectF(0, 0, getWidth(), getHeight()), mPaint, Canvas.ALL_SAVE_FLAG);//和save相同，同时开辟并指向图片到屏幕之外。方法比较消耗渲染时间

        //提供圆心，半径,画笔画出当前圆
        if (mTouch && !isAnimating) {
            canvas.drawCircle(mStart.x, mStart.y, mR, mPaint);//添加文本后，需要触摸以后才能调用这段代码
            canvas.drawCircle(mCurr.x, mCurr.y, mR, mPaint);//不断画圆
            //不断画p0,p1,p2，p3构成的曲线
            calPath();//求出p0,p1,p2，p3曲线的路径
            canvas.drawPath(mPath, mPaint);//画出路径
            //设置文字中心坐标
            mTipTextView.setX(mCurr.x - mTipTextView.getWidth() / 2);
            mTipTextView.setY(mCurr.y - mTipTextView.getHeight() / 2);
        }
        else if(!isAnimating){
            mTipTextView.setX(mStart.x - mTipTextView.getWidth() / 2);
            mTipTextView.setY(mStart.y - mTipTextView.getHeight() / 2);
        }
        canvas.restore();//restore和saveLayer一起使用
        /**
         * super.dispatchDraw表示绘制子view，也就是addView中的mTipTextView
         * 先绘制RedPoint再绘制mTipTextView
         */
        super.dispatchDraw(canvas);//!!!!!注意这里保证TextView覆盖在RedPoint控件之上
    }


    //
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.e(TAG, "onTouchEvent 方法被执行...");
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int x = (int) event.getX();
                int y = (int)event.getY();
                if(x >originX - RADIUS_DEFAULT && x <originX + RADIUS_DEFAULT
                        &&y >originY - RADIUS_DEFAULT && y < originY + RADIUS_DEFAULT){
                    Rect rect = new Rect();
                    int location[] = new int[2];//构造长度为2的数组
                    mTipTextView.getLocationOnScreen(location);//得到TextView的左上角在screen中的位置，保存到location数组中
                    rect.left = location[0];//location[0]表示左边的坐标
                    rect.top = location[1];//上
                    rect.right = location[0] + mTipTextView.getWidth();//右边，需要加上文本的宽
                    rect.bottom = location[1] + mTipTextView.getHeight();//下
//                //RawX始终相对于屏幕坐标
                    if (rect.contains((int) event.getRawX(), (int) event.getRawX())) {//判断触点(rawx,rawy)是否包含在文本坐标域中
                        mTouch = true;
                    }
                    mTouch = true;
                }
                break;
            case MotionEvent.ACTION_UP://抬起的时候不画圆
                mTouch = false;
                break;
        }
        mCurr.set(event.getX(), event.getY());//根据用户手指的移动动态设置圆心坐标
        postInvalidate();//刷新视图，handler机制调用，任何线程均可使用，但实时性没有invalidate强
        return true;//事件被消费，不再往上层视图传递
    }

    /**
     * 实时计算并绘制p0->p1->p2->p3的路径轨迹
     */
    private void calPath() {

        //x_1,y_1是移动的坐标，x_0,y_0是固定点坐标
        float centerX1 = mCurr.x;
        float centerY1 = mCurr.y;
        float centerX0 = mStart.x;
        float centerY0 = mStart.y;


        //需要的临时变量
        float dx = centerX1 - centerX0;
        float dy = centerY1 - centerY0;
        double alpha = Math.atan2(dy, dx);//计算反正切
//        double alpha = Math.atan(dy/dx);//计算反正切
        //======添加动态缩小起始圆的功能
        double distanceOfCircle = Math.sqrt(Math.pow((double) Math.abs(centerX1 - centerX0), 2) +
                Math.pow((double) Math.abs(centerY1 - centerY0), (double) 2));

        mR = (int) (RADIUS_DEFAULT - distanceOfCircle / 20);//半径缩小比例和圆心距离成正比，20为调整系数
        Log.e(TAG, "mR =" + mR);
        //设置最小半径10px
        if (mR < 30) {
            mR = 30;//
            //爆炸动画效果,爆炸之后视图消失，可使用标志位指示
            explodeView.setVisibility(VISIBLE);
//            AnimationDrawable是定义在drawable的xml文件中用<animation-list>标签包括的一组资源
            AnimationDrawable animationDrawble = (AnimationDrawable) explodeView.getDrawable();
            explodeView.setX(mCurr.x-explodeView.getWidth()/2);
            explodeView.setY(mCurr.y-explodeView.getHeight()/2);
            animationDrawble.start();
            isAnimating = true;
            mTouch =false;
            mTipTextView.setVisibility(GONE);
        }
        //======结束添加动态缩小起始圆的功能
        float offX = (float) (mR * Math.sin(alpha));
        float offY = (float) (mR * Math.cos(alpha));

        //计算图中的P0(x0,y0),P1(x1,y1),P2(x2,y2),P3(x3,y3)
        //p0
        float x0 = centerX0 + offX;
        float y0 = centerY0 - offY;

        //p1
        float x1 = centerX1 + offX;
        float y1 = centerY1 - offY;

        //p2
        float x2 = centerX1 - offX;
        float y2 = centerY1 + offY;

        //p3
        float x3 = centerX0 - offX;
        float y3 = centerY0 + offY;

        //控制点
        float controlX = (centerX0 + centerX1) / 2;
        float controlY = (centerY0 + centerY1) / 2;


        //清理Path路径上的轨迹
        mPath.reset();

        //移到p0点
        mPath.moveTo(x0, y0);
        //绘制p0,p1曲线
        mPath.quadTo(controlX, controlY, x1, y1);//绘制bezier二阶曲线,前两个参数是控制点，后两个参数是二次曲线的终点坐标
        //连接P1,P2
        mPath.lineTo(x2, y2);
        //绘制p0,p1曲线
        mPath.quadTo(controlX, controlY, x3, y3);
        //连接p3,p0
        mPath.lineTo(x0, y0);
    }



}

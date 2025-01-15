package com.fanbox.wallpapermaker

import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import android.view.View.OnTouchListener
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min


class MultiTouchListenerWithRotate(view: View) : OnTouchListener {
    private var dX = 0f
    private var dY = 0f // 用于单指拖拽时记录坐标偏移

    // 1. 创建缩放手势检测器
    private val scaleDetector =
        ScaleGestureDetector(view.context, ScaleListener(view))
    private var mRotationDegrees = 0f // 当前累计旋转角度
    private var mLastAngle = 0f // 记录双指的前一次角度

    // 记录手指按下时，View 自身已经有的初始旋转角度
    private var mBaseRotation = 0f

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        // 让 scaleDetector 优先处理双指缩放
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // 单指按下时，记录初始位移差
                dX = view.x - event.rawX
                dY = view.y - event.rawY
            }

            MotionEvent.ACTION_POINTER_DOWN ->                 // 第二个手指按下时（即开始双指），初始化旋转角
                if (event.pointerCount == 2) {
                    mBaseRotation = view.rotation // 记录当前 View 已有的旋转角
                    mLastAngle = getAngle(event) // 记录双指的初始角度
                }

            MotionEvent.ACTION_MOVE ->                 // 单指拖拽
                if (event.pointerCount == 1) {
                    view.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                } else if (event.pointerCount == 2) {
                    val currentAngle = getAngle(event) // 当前双指的角度
                    val deltaAngle = currentAngle - mLastAngle

                    // 计算新的旋转角度 = 初始角度 + 本次的变化量
                    mRotationDegrees = mBaseRotation + deltaAngle

                    // 应用到 View
                    view.rotation = mRotationDegrees
                }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {}
            else -> {}
        }
        return true // 消费事件
    }

    /**
     * 计算当前双指的角度（相对于水平方向，以度数为单位）。
     * 假设第 0 个手指和第 1 个手指分别是 x0,y0 与 x1,y1
     */
    private fun getAngle(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val x0 = event.getX(0)
        val y0 = event.getY(0)
        val x1 = event.getX(1)
        val y1 = event.getY(1)

        val deltaX = (x1 - x0).toDouble()
        val deltaY = (y1 - y0).toDouble()
        // atan2(y, x) 得到弧度，转换为角度
        val angle = Math.toDegrees(atan2(deltaY, deltaX))
        return angle.toFloat()
    }

    /**
     * 内部类：处理双指缩放
     */
    private inner class ScaleListener(private val mView: View) : SimpleOnScaleGestureListener() {
        private var scaleFactor = 1.0f

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scale = detector.scaleFactor
            scaleFactor *= scale
            // 限制最大 / 最小缩放倍数
            scaleFactor = max(0.2, min(scaleFactor.toDouble(), 5.0)).toFloat()

            mView.scaleX = scaleFactor
            mView.scaleY = scaleFactor
            return true // 返回 true，表示已处理
        }
    }
}

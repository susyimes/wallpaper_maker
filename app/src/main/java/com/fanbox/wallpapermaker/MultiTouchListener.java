package com.fanbox.wallpapermaker;

import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class MultiTouchListener implements View.OnTouchListener {

    private float dX, dY;               // 用于记录初始手指触摸点与 View 左上角的距离
    private ScaleGestureDetector scaleGestureDetector;

    public MultiTouchListener(View view) {
        // 创建一个缩放手势检测器
        scaleGestureDetector = new ScaleGestureDetector(view.getContext(), new ScaleListener(view));
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // 先让 ScaleGestureDetector 处理多指缩放
        scaleGestureDetector.onTouchEvent(event);

        // 再处理单指拖拽
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dX = view.getX() - event.getRawX();
                dY = view.getY() - event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                // 如果是单指（pointerCount == 1），执行拖拽
                if (event.getPointerCount() == 1) {
                    view.animate()
                            .x(event.getRawX() + dX)
                            .y(event.getRawY() + dY)
                            .setDuration(0)
                            .start();
                }
                break;
            default:
                break;
        }
        return true; // 消费事件
    }

    /**
     * 用于处理双指缩放的 ScaleListener
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private View mView;
        private float scaleFactor = 1.0f;

        ScaleListener(View view) {
            mView = view;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // 获取本次缩放系数
            float scale = detector.getScaleFactor();
            scaleFactor *= scale;
            // 设置缩放的范围避免过大或过小
            scaleFactor = Math.max(0.2f, Math.min(scaleFactor, 5.0f));

            mView.setScaleX(scaleFactor);
            mView.setScaleY(scaleFactor);
            return true;
        }
    }
}

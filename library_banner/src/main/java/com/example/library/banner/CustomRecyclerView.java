package com.example.library.banner;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Create by 2019/3/16
 * Author lmy
 */
public class CustomRecyclerView extends RecyclerView {
    private double mScale;

    public boolean isEnableScoViewPager ;

    public CustomRecyclerView(Context context) {
        super(context);
    }

    public CustomRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setFlingScale(double scale) {
        this.mScale = scale;
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        velocityX *= mScale;
        return super.fling(velocityX, velocityY);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isEnableScoViewPager = true;
                break;
            case MotionEvent.ACTION_MOVE:
                isEnableScoViewPager = true;
                break;
            case MotionEvent.ACTION_UP:
                isEnableScoViewPager = false;
                break;
        }
        if (mOnBannerMoveListener != null) {
            mOnBannerMoveListener.onMoveStatus(isEnableScoViewPager);
        }
        return super.dispatchTouchEvent(ev);
    }

    private OnBannerMoveListener mOnBannerMoveListener;

    public void setOnBannerMoveListener(OnBannerMoveListener onBannerMoveListener){
        this.mOnBannerMoveListener = onBannerMoveListener;
    }

    public interface OnBannerMoveListener{
        void onMoveStatus(boolean isEnableScoViewPager);
    }
}

package com.example.library.banner;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.example.library.R;
import com.example.library.banner.layoutmanager.CenterSnapHelper;
import com.example.library.banner.layoutmanager.BannerLayoutManager;

import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

public class BannerLayout extends FrameLayout {

    private static final int WHAT_AUTO_PLAY = 1000;
    //刷新间隔时间
    private int autoPlayInterval;
    //是否显示指示器
    private boolean showIndicator;
    //指示器的列表
    private RecyclerView indicatorContainer;
    //选中指示器
    private Drawable mSelectedDrawable;
    //未选中指示器
    private Drawable mUnselectedDrawable;
    //指示器列表填充
    private IndicatorAdapter indicatorAdapter;
    //指示器间距
    private int indicatorMargin;
    private CustomRecyclerView mRecyclerView;
    private BannerLayoutManager mLayoutManager;
    private boolean hasInit;
    private int bannerSize = 1;
    private int currentIndex;
    private boolean isPlaying = false;
    private boolean isAutoPlaying;
    int itemSpace;
    float centerScale;
    float moveSpeed;
    protected Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == WHAT_AUTO_PLAY) {
                if (currentIndex == mLayoutManager.getCurrentPosition()) {
                    ++currentIndex;
                    mRecyclerView.smoothScrollToPosition(currentIndex);
                    refreshIndicator();
                    mHandler.sendEmptyMessageDelayed(WHAT_AUTO_PLAY, autoPlayInterval);
                }
            }
            return false;
        }
    });

    private Context mContext;
    private int bannerOrientation;
    /**是否一次只滑动一个卡片*/
    private boolean isFlingScale;

    public BannerLayout(Context context) {
        this(context, null);
    }

    public BannerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BannerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    protected void initView(Context context, AttributeSet attrs) {
        this.mContext = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BannerLayout);
        showIndicator = a.getBoolean(R.styleable.BannerLayout_showIndicator, true);
        isAutoPlaying = a.getBoolean(R.styleable.BannerLayout_autoPlaying, true);
        isFlingScale = a.getBoolean(R.styleable.BannerLayout_flingScale, true);
        autoPlayInterval = a.getInt(R.styleable.BannerLayout_intervalDuration, 3000);
        itemSpace = a.getInt(R.styleable.BannerLayout_itemSpace, 0);
        centerScale = a.getFloat(R.styleable.BannerLayout_centerScale, 1.0f);
        moveSpeed = a.getFloat(R.styleable.BannerLayout_moveSpeed, 1.8f);
        mSelectedDrawable = a.getDrawable(R.styleable.BannerLayout_indicatorDrawableSelected);
        mUnselectedDrawable = a.getDrawable(R.styleable.BannerLayout_indicatorDrawableUnselected);
        bannerOrientation = a.getInt(R.styleable.BannerLayout_orientation, 0);
//        indicatorMargin = a.getInt(R.styleable.BannerLayout_d)

        if (bannerOrientation != 0) {
            bannerOrientation = OrientationHelper.VERTICAL;
        }

        if (mSelectedDrawable == null) {
            //绘制默认选中状态图形
            GradientDrawable selectedGradientDrawable = new GradientDrawable();
            selectedGradientDrawable.setShape(GradientDrawable.OVAL);
            selectedGradientDrawable.setColor(Color.RED);
            selectedGradientDrawable.setSize(dp2px(5), dp2px(5));
            selectedGradientDrawable.setCornerRadius(dp2px(5) / 2);
            mSelectedDrawable = new LayerDrawable(new Drawable[]{selectedGradientDrawable});
        }
        if (mUnselectedDrawable == null) {
            //绘制默认未选中状态图形
            GradientDrawable unSelectedGradientDrawable = new GradientDrawable();
            unSelectedGradientDrawable.setShape(GradientDrawable.OVAL);
            unSelectedGradientDrawable.setColor(Color.GRAY);
            unSelectedGradientDrawable.setSize(dp2px(5), dp2px(5));
            unSelectedGradientDrawable.setCornerRadius(dp2px(5) / 2);
            mUnselectedDrawable = new LayerDrawable(new Drawable[]{unSelectedGradientDrawable});
        }

        indicatorMargin = dp2px(2);
        a.recycle();

        initBannerRecycle();
    }

    /**
     * banner
     */
    public void initBannerRecycle(){
        //轮播图部分
        mRecyclerView = new CustomRecyclerView(mContext);
        LayoutParams vpLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mRecyclerView, vpLayoutParams);
        mLayoutManager = new BannerLayoutManager(getContext(), bannerOrientation);
        mLayoutManager.setItemSpace(itemSpace);
        mLayoutManager.setCenterScale(centerScale);
        mLayoutManager.setMoveSpeed(moveSpeed);
        mRecyclerView.setLayoutManager(mLayoutManager);
        new CenterSnapHelper().attachToRecyclerView(mRecyclerView);
        if (isFlingScale) {
            mRecyclerView.setFlingScale(0.1);
        }

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dx != 0) {
                    setPlaying(false);
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                int first = mLayoutManager.getCurrentPosition();
                if (currentIndex != first) {
                    currentIndex = first;
                }
                if (newState == SCROLL_STATE_IDLE) {
                    setPlaying(true);
                }
                refreshIndicator();
            }
        });
    }

    public CustomRecyclerView getmRecyclerView(){
        return mRecyclerView;
    }

    // 设置是否禁止滚动播放
    public void setAutoPlaying(boolean isAutoPlaying) {
        if (bannerSize > 1) {
            this.isAutoPlaying = isAutoPlaying;
        }else {
            this.isAutoPlaying = false;
        }
        setPlaying(this.isAutoPlaying);
    }

    public void startAutoPlaying(){
        setPlaying(true);
    }

    public void pauseAutoPlaying(){
        setPlaying(false);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    //设置是否显示指示器
    public void setShowIndicator(boolean showIndicator) {
        this.showIndicator = showIndicator;
        if (indicatorContainer != null) {
            indicatorContainer.setVisibility(showIndicator ? VISIBLE : GONE);
        }
    }

    //设置当前图片缩放系数
    public void setCenterScale(float centerScale) {
        this.centerScale = centerScale;
        mLayoutManager.setCenterScale(centerScale);
    }

    //设置跟随手指的移动速度
    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
        mLayoutManager.setMoveSpeed(moveSpeed);
    }

    //设置图片间距
    public void setItemSpace(int itemSpace) {
        this.itemSpace = itemSpace;
        mLayoutManager.setItemSpace(itemSpace);
    }

    /**
     * 设置轮播间隔时间
     *
     * @param autoPlayInterval 时间毫秒
     */
    public void setAutoPlayInterval(int autoPlayInterval) {
        this.autoPlayInterval = autoPlayInterval;
    }

    public void setOrientation(int orientation) {
        mLayoutManager.setOrientation(orientation);
    }

    /**
     * 设置是否自动播放（上锁）
     *
     * @param playing 开始播放
     */
    protected synchronized void setPlaying(boolean playing) {
        if (isAutoPlaying && hasInit) {
            if (!isPlaying && playing) {
                mHandler.removeMessages(WHAT_AUTO_PLAY);
                mHandler.sendEmptyMessageDelayed(WHAT_AUTO_PLAY, autoPlayInterval);
                isPlaying = true;
            } else if (isPlaying && !playing) {
                mHandler.removeMessages(WHAT_AUTO_PLAY);
                isPlaying = false;
            }
        }
    }

    /**
     * 设置轮播数据集
     */
    public void setAdapter(RecyclerView.Adapter adapter) {
        currentIndex = 0;
        hasInit = false;
        mRecyclerView.setAdapter(adapter);






        bannerSize = adapter.getItemCount();
        mLayoutManager.setInfinite(bannerSize >= 2);
        if (bannerSize >= 2) {
            isAutoPlaying = true;
            initIndicatorContainer();
        }
        hasInit = true;
        setPlaying(true);
    }

    /**
     * banner指示器
     */
    public void initIndicatorContainer(){
        if (indicatorContainer != null) {
            removeView(indicatorContainer);
        }

        //指示器部分
        indicatorContainer = new RecyclerView(mContext);
        LinearLayoutManager indicatorLayoutManager = new LinearLayoutManager(mContext, bannerOrientation, false);
        indicatorContainer.setLayoutManager(indicatorLayoutManager);
        indicatorAdapter = new IndicatorAdapter();
        indicatorContainer.setAdapter(indicatorAdapter);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER;
        params.setMargins(0, 0, 0, dp2px(6));
        if (!showIndicator) {
            indicatorContainer.setVisibility(GONE);
        }
        addView(indicatorContainer, params);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setPlaying(false);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setPlaying(true);
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setPlaying(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        setPlaying(false);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            setPlaying(true);
        } else {
            setPlaying(false);
        }
    }

    /**
     * 标示点适配器
     */
    protected class IndicatorAdapter extends RecyclerView.Adapter {

        int currentPosition = 0;

        public void setPosition(int currentPosition) {
            this.currentPosition = currentPosition;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ImageView bannerPoint = new ImageView(getContext());
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(indicatorMargin, indicatorMargin, indicatorMargin, indicatorMargin);
            bannerPoint.setLayoutParams(lp);
            return new RecyclerView.ViewHolder(bannerPoint) {
            };
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (bannerSize == 1) {
                holder.itemView.setVisibility(GONE);
            }else {
                holder.itemView.setVisibility(VISIBLE);
            }

            ImageView bannerPoint = (ImageView) holder.itemView;
            bannerPoint.setImageDrawable(currentPosition == position ? mSelectedDrawable : mUnselectedDrawable);
        }

        @Override
        public int getItemCount() {
            return bannerSize;
        }
    }

    protected int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                Resources.getSystem().getDisplayMetrics());
    }

    /**
     * 改变导航的指示点
     */
    protected synchronized void refreshIndicator() {
        if (indicatorAdapter != null) {
            if (showIndicator && bannerSize > 1) {
                indicatorAdapter.setPosition(currentIndex % bannerSize);
                indicatorAdapter.notifyDataSetChanged();
            }
        }
    }

    public interface OnBannerItemClickListener {
        void onItemClick(int position);
    }
}
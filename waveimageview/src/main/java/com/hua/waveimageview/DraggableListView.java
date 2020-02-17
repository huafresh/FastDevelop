package com.hua.waveimageview;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.collection.ArraySet;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * Author: hua
 * Created: 2017/10/16
 * Description:
 * 可拖拽列表视图控件。
 * 使用方法：
 * 1、在xml中布局或者new出来
 * 2、调用{@link #setAdapter(Adapter)}方法设置数据适配器
 * 3、调用{@link #setState(int)}方法设置控件所处的状态
 * [4]、调用{@link #getDataList()}方法获取调整顺序后的数据列表
 */

public class DraggableListView extends ViewGroup {

    public static final int DEFAULT_SPAN_COUNT = 4;

    private RecyclerView mRecyclerView;
    private Adapter mAdapter;
    private Context mContext;

    private Set<OnStateChangedListener> mStateChangedListeners;

    private ItemTouchHelper.Callback mCallBack;
    private ItemTouchHelper mTouchHelper;

    public static final int STATE_NORMAL = 1;
    public static final int STATE_DRAGGABLE = 2;

    @IntDef({STATE_NORMAL, STATE_DRAGGABLE})
    private @interface State {
    }

    private int mCurState;

    public static final int ITEM_STYLE_VERTICAL = 1;
    public static final int ITEM_STYLE_HORIZONTAL = 2;
    public static final int ITEM_STYLE_GRID = 3;

    @IntDef({ITEM_STYLE_VERTICAL, ITEM_STYLE_HORIZONTAL, ITEM_STYLE_GRID})
    private @interface ItemStyle {
    }

    private int mItemStyle;

    public DraggableListView(Context context) {
        this(context, null);
    }

    public DraggableListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DraggableListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        mContext = context;

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.DraggableListView);
        mItemStyle = array.getInt(R.styleable.DraggableListView_style, ITEM_STYLE_VERTICAL);
        array.recycle();

        mRecyclerView = new RecyclerView(context);
        addView(mRecyclerView);

        mCurState = STATE_NORMAL;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        //this viewGroup host only one child
        View child = getChildAt(0);

        if (child != null) {
            MarginLayoutParams params = (MarginLayoutParams) child.getLayoutParams();
            child.layout(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin);
        }
    }

    /**
     * 设置item展示的样式
     *
     * @param style 可取值：
     *              {@link #ITEM_STYLE_VERTICAL} 垂直线性显示
     *              {@link #ITEM_STYLE_HORIZONTAL} 水平线性显示
     *              {@link #ITEM_STYLE_GRID} 网格显示
     */
    public void setItemStyle(@ItemStyle int style) {
        mItemStyle = style;
    }

    /**
     * 设置进入可拖拽状态
     *
     * @param state 可取值：
     *              {@link #STATE_NORMAL} 正常状态
     *              {@link #STATE_DRAGGABLE} 可拖拽状态
     */
    public void setState(@State int state) {
        if (mCurState != state) {
            mCurState = state;
            notifyStateChanged(state);
        }
    }

    /**
     * 获取当前的状态
     *
     * @return 当前的状态
     * @see #setState(int)
     */
    public int getState() {
        return mCurState;
    }

    /**
     * 状态改变监听
     *
     * @see #setState(int)
     */
    public interface OnStateChangedListener {
        void onStateChanged(int curState);
    }

    /**
     * 添加状态监听
     *
     * @param listener 状态监听
     * @see #setState(int)
     */
    public void addOnStateChangedListener(OnStateChangedListener listener) {
        if (mStateChangedListeners == null) {
            mStateChangedListeners = new ArraySet<>();
        }
        if (listener != null) {
            mStateChangedListeners.add(listener);
        }
    }

    /**
     * 设置数据适配器
     *
     * @param adapter 数据适配器
     */
    public void setAdapter(Adapter adapter) {
        if (adapter == null) {
            return;
        }

        mAdapter = adapter;
        mRecyclerView.setAdapter(adapter);

        initRecyclerView(adapter);

        if (mTouchHelper == null) {
            setItemTouchDragCallBack(new DefaultItemTouchCallBack(adapter));
        }

        setItemListeners();
    }

    private void initRecyclerView(Adapter adapter) {
        RecyclerView.LayoutManager layoutManager = null;
        switch (mItemStyle) {
            case ITEM_STYLE_VERTICAL:
                layoutManager = new LinearLayoutManager(mContext);
                break;
            case ITEM_STYLE_HORIZONTAL:
                LinearLayoutManager manager = new LinearLayoutManager(mContext);
                manager.setOrientation(LinearLayoutManager.HORIZONTAL);
                layoutManager = manager;
                break;
            case ITEM_STYLE_GRID:
                layoutManager = new GridLayoutManager(mContext, adapter.getColumnNum());
                break;
        }
        mRecyclerView.setLayoutManager(layoutManager);
    }

    private void setItemListeners() {
        int itemCount = mAdapter.getItemCount();
        for (int i = 0; i < itemCount; i++) {
            final RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(i);
            holder.itemView.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mTouchHelper.startDrag(holder);
                    mCurState = STATE_DRAGGABLE;
                    notifyStateChanged(mCurState);
                    return true;
                }
            });
            holder.itemView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int actionMasked = event.getActionMasked();
                    switch (actionMasked) {
                        case MotionEvent.ACTION_DOWN:
                            if (mCurState == STATE_DRAGGABLE) {
                                mTouchHelper.startDrag(holder);
                            }
                            break;
                    }
                    return false;
                }
            });
        }
    }

    @SuppressWarnings("unchecked")
    private void notifyStateChanged(int curState) {
        if (mStateChangedListeners != null) {
            for (OnStateChangedListener listener : mStateChangedListeners) {
                listener.onStateChanged(curState);
            }
        }
        if (mAdapter != null) {
            int itemCount = mAdapter.getItemCount();
            for (int i = 0; i < itemCount; i++) {
                RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(i);
                mAdapter.onStateChanged(holder.itemView, mAdapter.mDataList.get(i), i, curState);
            }
        }
    }

    /**
     * 通过继承此类实现{@link DraggableListView}的数据适配器
     *
     * @param <T> 每个item对应的数据实体类型
     */
    public static abstract class Adapter<T> extends RecyclerView.Adapter {
        protected List<T> mDataList;
        private @LayoutRes
        int mLayoutId;
        private Context mContext;

        public Adapter(Context context, List<T> dataList, int mLayoutId) {
            this.mContext = context;
            this.mDataList = dataList;
            this.mLayoutId = mLayoutId;
        }

        /**
         * 当数据与视图绑定时调用
         *
         * @param convertView item视图
         * @param object      数据实体对象
         * @param position    item的位置
         */
        public abstract void convert(@NonNull View convertView, T object, int position);

        /**
         * 状态改变时调用
         *
         * @param curState 当前状态
         * @see #setState(int)
         */
        protected void onStateChanged(@NonNull View convertView, T object, int position, int curState) {

        }

        /**
         * 是否使能滑动删除。默认是false
         *
         * @return 是否使能滑动删除
         */
        protected boolean enableSwipeDrop() {
            return false;
        }

        /**
         * 如果item是Grid样式，此方法的返回值会决定列数
         *
         * @return Grid的列数
         */
        protected int getColumnNum() {
            return DEFAULT_SPAN_COUNT;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            View itemView = inflater.inflate(mLayoutId, null);
            ViewHolder viewHolder = new ViewHolder(itemView);
            itemView.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    return true;
                }
            });
            return viewHolder;

        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            convert(holder.itemView, mDataList.get(position), position);
        }

        @Override
        public int getItemCount() {
            return mDataList != null ? mDataList.size() : 0;
        }

        private class ViewHolder extends RecyclerView.ViewHolder {

            private ViewHolder(View itemView) {
                super(itemView);
            }
        }
    }


    /**
     * 设置拖拽回调。这个回调是拖拽实现的核心类
     *
     * @param callBack 拖拽回调
     * @see ItemTouchHelper.Callback
     */
    public void setItemTouchDragCallBack(ItemTouchHelper.Callback callBack) {
        if (callBack == null) {
            return;
        }
        mCallBack = callBack;
        mTouchHelper = new ItemTouchHelper(callBack);
        mTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    public static class DefaultItemTouchCallBack extends ItemTouchHelper.Callback {
        private Adapter adapter;

        public DefaultItemTouchCallBack(Adapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            int dragFlags = 0;
            int swipeFlags = 0;
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager instanceof GridLayoutManager) {
                swipeFlags = dragFlags = ItemTouchHelper.LEFT | ItemTouchHelper.UP |
                        ItemTouchHelper.RIGHT | ItemTouchHelper.DOWN;
            } else if (layoutManager instanceof LinearLayoutManager) {
                int orientation = ((LinearLayoutManager) layoutManager).getOrientation();
                if (orientation == LinearLayoutManager.VERTICAL) {
                    dragFlags = swipeFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                } else if (orientation == LinearLayoutManager.HORIZONTAL) {
                    dragFlags = swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                }
            }
            return makeMovementFlags(dragFlags, swipeFlags);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                              RecyclerView.ViewHolder target) {
            Collections.swap(adapter.mDataList, viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            int pos = viewHolder.getAdapterPosition();
            if (adapter.mDataList != null) {
                adapter.mDataList.remove(pos);
            }
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return adapter.enableSwipeDrop();
        }
    }


    @SuppressWarnings("unchecked")
    public <T> List<T> getDataList() {
        if (mAdapter != null) {
            return mAdapter.mDataList;
        }
        return null;
    }

}

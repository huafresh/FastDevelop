package com.hua.kotlin_tools.view

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import com.hua.kotlin_tools.R

/**
 * 带圆环的FrameLayout
 *
 * @author zhangsh
 * @version V1.0
 * @date 2019/5/29 8:25 PM
 */

class RingFrameLayout(context: Context, attrs: AttributeSet?, defStyle: Int) : FrameLayout(context, attrs, defStyle) {

    companion object {
        private const val DEFAULT_RING_COLOR = Color.WHITE
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private val DEFAULT_RING_RADIUS by lazy { dip(10) }
    private val DEFAULT_RING_WIDTH by lazy { dip(3) }

    private var mCornerRadius = 0f
    private var mRingColor = 0
    private val mPaint = Paint()
    private var mRingWidth = 0f
    private val mPath = Path()
    private val mRectF = RectF()

    init {

        val array = context.obtainStyledAttributes(attrs, R.styleable.RingFrameLayout)
        mCornerRadius = array.getDimension(R.styleable.RingFrameLayout_ring_corner_radius, DEFAULT_RING_RADIUS)
        mRingWidth = array.getDimension(R.styleable.RingFrameLayout_ring_width, DEFAULT_RING_WIDTH)
        mRingColor = array.getColor(R.styleable.RingFrameLayout_ring_color, DEFAULT_RING_COLOR)
        array.recycle()

        mPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = mRingWidth
            color = mRingColor
            isAntiAlias = true
        }

        setWillNotDraw(false)
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()

        mPath.reset()
        mRectF.set(
                mRingWidth / 2.0f, mRingWidth / 2.0f,
                width.toFloat() - mRingWidth / 2, height.toFloat() - mRingWidth / 2
        )
        val inRadius = mCornerRadius - mRingWidth / 2
        mPath.addRoundRect(mRectF, inRadius, inRadius, Path.Direction.CCW)
        canvas.clipPath(mPath)
        super.dispatchDraw(canvas)

        canvas.restore()
    }

    override fun onDraw(canvas: Canvas) {
        mPath.reset()
        mRectF.set(0f, 0f, width.toFloat(), height.toFloat())
        mPath.addRoundRect(mRectF, mCornerRadius, mCornerRadius, Path.Direction.CCW)
        canvas.drawPath(mPath, mPaint)
    }

    fun dip(dpValue: Int): Float {
        val density = context.resources.displayMetrics.density
        return (dpValue * density)
    }
}
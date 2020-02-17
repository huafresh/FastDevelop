package com.hua.kotlin_tools.view

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.*
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import com.hua.kotlin_tools.LogUtil
import com.hua.kotlin_tools.R
import com.hua.kotlin_tools.setVisible
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException


/**
 * 可以指定目标控件，使其高亮，调用[show]方法显示
 *
 * @author zhangsh
 * @version V1.0
 * @date 2019-10-10 15:34
 */

class UserGuideView(context: Context, attrs: AttributeSet?, defStyle: Int)
    : View(context, attrs, defStyle) {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private val mDashedPaint = Paint()
    private val mClearPaint = Paint()
    private val mMaskColor = parseColorAlpha("#000000", 0.6f)

    private var mFgBitmap: Bitmap? = null
    private lateinit var mFgCanvas: Canvas
    private lateinit var mArrowBitmap: Bitmap
    private val mArrowWidth by lazy { dip(19) }
    private val mArrowHeight by lazy { dip(49) }
    private val mTempRect = Rect()
    private val mMatrix = Matrix()

    private val mTextPaint = Paint()
    private var mGuideText = ""

    private var mTargetView: View? = null
    private var mDashedShape = SHAPE_ROUND

    private var mScreenWidth = 0
    private var mScreenHeight = 0

    var onDismissListener: (() -> Unit)? = null

    private var mEnablePadding = true

    private lateinit var mGestureDetector: GestureDetector

    private var saveOnClickListener: View.OnClickListener? = null

    private val targetLayoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        invalidate()
    }

    init {
        init()
    }

    fun show(target: View,
             guideText: String,
             dashedShape: Int = SHAPE_ROUND,
             enablePadding: Boolean = true) {
        LogUtil.d("show custom guide view. width = $width, height = $height")
        if (mTargetView != target) {
            mTargetView?.removeOnLayoutChangeListener(targetLayoutListener)

            // setupFgCanvas()
            mTargetView = target
            mDashedShape = dashedShape
            mGuideText = guideText
            mEnablePadding = enablePadding
            target.hasOnClickListeners()

            target.addOnLayoutChangeListener(targetLayoutListener)
        }
        setVisible(true)
        // invalidate()
    }

    fun dismiss() {
        setVisible(false)
        onDismissListener?.invoke()
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
    }

    private fun init() {
        mDashedPaint.run {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = dip(2).toFloat()
            isAntiAlias = true
            pathEffect = DashPathEffect(floatArrayOf(16f, 8f), 0f)
        }

        mClearPaint.run {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }

        mTextPaint.run {
            color = Color.WHITE
            textSize = sp(15f).toFloat()
            isAntiAlias = true
        }

        mArrowBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_home_guide_arrow)

        mGestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                dismiss()
                return true
            }

            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }
        })

        // fitsSystemWindows = true

        dismiss()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
    }

    private fun setupFgCanvas() {
        if (mFgBitmap == null) {
            mFgBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            mFgCanvas = Canvas(mFgBitmap!!)
        }
        drawMask(mFgCanvas)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        setupFgCanvas()

        LogUtil.d("draw custom guide view")

        val target = mTargetView ?: throw IllegalStateException("必须设置高亮的目标控件")

        drawFgBitmapWithTargetView(mFgCanvas, target)

        canvas.drawBitmap(mFgBitmap!!, 0f, 0f, null)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return mGestureDetector.onTouchEvent(event)
    }

    /**
     * 绘制高亮虚线、文字等
     */
    private fun drawFgBitmapWithTargetView(canvas: Canvas, target: View) {
        // 绘制高亮虚线
        val dashedRect = drawDashed(target, canvas)

        if (mGuideText.isEmpty()) {
            return
        }

        // 绘制箭头
        val arrowPos = drawArrowBitmap(canvas, dashedRect)
        // 绘制文字
        drawGuideText(canvas, arrowPos.rect, arrowPos.quadrant)
    }

    private fun drawDashed(target: View, canvas: Canvas): Rect {
        val targetLocationInWindow = IntArray(2)
        target.getLocationInWindow(targetLocationInWindow)
        val guideLocationInWindow = IntArray(2)
        getLocationInWindow(guideLocationInWindow)

        val drawLocation = IntArray(2)
        drawLocation[0] = targetLocationInWindow[0] - guideLocationInWindow[0]
        drawLocation[1] = targetLocationInWindow[1] - guideLocationInWindow[1]

        val targetWidth = target.width
        val targetHeight = target.height

        val dashedPadding = if (mEnablePadding) dip(DASHED_PADDING) else 0

        val dashedRect = mTempRect
        when (mDashedShape) {
            SHAPE_ROUND -> {
                val cx = drawLocation[0] + targetWidth / 2.0f
                val cy = drawLocation[1] + targetHeight / 2.0f
                val radius = Math.max(targetWidth / 2, targetHeight / 2).toFloat() + dashedPadding
                canvas.drawCircle(cx, cy, radius, mClearPaint)
                canvas.drawCircle(cx, cy, radius, mDashedPaint)
                dashedRect.set(drawLocation[0], drawLocation[1],
                        (drawLocation[0] + 2 * radius).toInt(),
                        (drawLocation[1] + 2 * radius).toInt())
            }
            SHAPE_RECT -> {
                val left = drawLocation[0] - dashedPadding
                val top = drawLocation[1] - /*context.getStatusBarHeight() -*/ dashedPadding
                dashedRect.set(left, top,
                        left + targetWidth + 2 * dashedPadding,
                        top + targetHeight + 2 * dashedPadding)
                canvas.drawRect(dashedRect, mClearPaint)
                canvas.drawRect(dashedRect, mDashedPaint)
            }
            else -> throw IllegalArgumentException("invalid dashed shape")
        }

        return dashedRect
    }


    /**
     * 绘制箭头图片，[dashedRect]是虚线框的位置
     */
    private fun drawArrowBitmap(canvas: Canvas, dashedRect: Rect): ArrowPos {
        val dashedCx = (dashedRect.left + dashedRect.right) / 2
        val dashedCy = (dashedRect.top + dashedRect.bottom) / 2

        val arrowRect = mTempRect

        // quadrant是指虚线框所在的象限
        val quadrant = if (dashedCx > mScreenWidth / 2) {
            if (dashedCy > mScreenHeight / 2) {
                0
            } else {
                1
            }
        } else {
            if (dashedCy > mScreenHeight / 2) {
                3
            } else {
                2
            }
        }

        when (quadrant) {
            0 -> { // 右下
                val arrowLeft = (dashedCx - mArrowWidth / 2f).toInt()
                val arrowBottom = dashedRect.top - dip(9)
                arrowRect.set(arrowLeft, arrowBottom - mArrowHeight,
                        arrowLeft + mArrowWidth,
                        arrowBottom)
                mMatrix.reset()
            }
            1 -> { // 右上
                val arrowLeft = (dashedCx - mArrowWidth / 2f).toInt()
                val arrowTop = dashedRect.bottom + dip(9)
                arrowRect.set(arrowLeft, arrowTop,
                        arrowLeft + mArrowWidth,
                        arrowTop + mArrowHeight)
                // x轴对称
                val values = floatArrayOf(
                        1f, 0.0f, 0.0f,
                        0.0f, -1f, 0.0f,
                        0.0f, 0.0f, 1.0f)
                mMatrix.setValues(values)
            }
            2 -> { // 左上
                val arrowLeft = (dashedCx - mArrowWidth / 2f).toInt()
                val arrowTop = dashedRect.bottom + dip(9)
                arrowRect.set(arrowLeft, arrowTop,
                        arrowLeft + mArrowWidth,
                        arrowTop + mArrowHeight)
                // 原点对称
                val values = floatArrayOf(
                        -1f, 0.0f, 0.0f,
                        0.0f, -1f, 0.0f,
                        0.0f, 0.0f, 1.0f)
                mMatrix.setValues(values)
            }
            3 -> { // 左下
                val arrowLeft = (dashedCx - mArrowWidth / 2f).toInt()
                val arrowBottom = dashedRect.top - dip(9)
                arrowRect.set(arrowLeft, arrowBottom - mArrowHeight,
                        arrowLeft + mArrowWidth,
                        arrowBottom)
                // y轴对称
                val values = floatArrayOf(
                        -1f, 0.0f, 0.0f,
                        0.0f, 1f, 0.0f,
                        0.0f, 0.0f, 1.0f)
                mMatrix.setValues(values)
            }
            else -> {
            }
        }

        val transformBitmap = Bitmap.createBitmap(mArrowBitmap, 0, 0,
                mArrowBitmap.width, mArrowBitmap.height,
                mMatrix, true)
        canvas.drawBitmap(transformBitmap, null, arrowRect, null)

        return ArrowPos(arrowRect, quadrant)
    }

    /**
     * 绘制引导文字
     */
    private fun drawGuideText(canvas: Canvas, arrowRect: Rect, dashedQuadrant: Int) {
        val textWidth = mTextPaint.measureText(mGuideText)
        val textHeight = mTextPaint.textSize

        val baselineDelta = Math.abs(mTextPaint.fontMetrics.ascent) - textHeight / 2
        when (dashedQuadrant) {
            0 -> { // 右下
                val x = arrowRect.left - dip(6)
                val y = arrowRect.top + baselineDelta
                mTextPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(mGuideText, x.toFloat(), y, mTextPaint)
            }
            1 -> { // 右上
                val x = arrowRect.left - dip(6)
                val y = arrowRect.bottom + baselineDelta
                mTextPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(mGuideText, x.toFloat(), y, mTextPaint)
            }
            2 -> { // 左上
                val x = arrowRect.right + dip(6)
                val y = arrowRect.bottom + baselineDelta
                mTextPaint.fontMetrics.top
                mTextPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(mGuideText, x.toFloat(), y, mTextPaint)
            }
            3 -> { // 左下
                val x = arrowRect.right + dip(6)
                val y = arrowRect.top + baselineDelta
                mTextPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(mGuideText, x.toFloat(), y, mTextPaint)
            }
            else -> {
            }
        }
    }

    /**
     * 绘制全屏遮罩
     */
    private fun drawMask(canvas: Canvas) {
        canvas.drawColor(mMaskColor)
    }

    private fun dip(dpValue: Int): Int {
        val density = context.resources.displayMetrics.density
        return Math.round(dpValue * density)
    }

    fun sp(sp: Float): Int {
        val density = context.resources.displayMetrics.scaledDensity
        return Math.round(sp * density)
    }

    companion object {
        /**
         * 虚线框的形状
         */
        const val SHAPE_ROUND = 0
        const val SHAPE_RECT = 1

        private const val DASHED_PADDING = 5 // dp
    }
}

data class ArrowPos(
        val rect: Rect,
        val quadrant: Int
)

@ColorInt
fun parseColorAlpha(colorStr: String, @FloatRange(from = 0.0, to = 1.0) alpha: Float? = null): Int {
    val colorInt = Color.parseColor(colorStr)
    if (alpha != null) {
        val red = Color.red(colorInt)
        val green = Color.green(colorInt)
        val blue = Color.blue(colorInt)
        return Color.argb((alpha * 0xff).toInt(), red, green, blue)
    }
    return colorInt
}

fun getScreenSize(context: Context): IntArray {
    val metrics = DisplayMetrics()
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowManager.defaultDisplay.getMetrics(metrics)
    return intArrayOf(metrics.widthPixels, metrics.heightPixels)
}

fun Context.getStatusBarHeight(): Int {
    var result = 0
    val resId = this.getResources().getIdentifier("status_bar_height",
            "dimen", "android")
    if (resId > 0) {
        result = this.getResources().getDimensionPixelSize(resId)
    }
    return result
}
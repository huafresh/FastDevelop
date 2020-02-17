package com.hua.kotlin_tools.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.hua.kotlin_tools.LogUtil
import com.hua.kotlin_tools.PrefUtil
import com.hua.kotlin_tools.setVisible
import com.hua.kotlin_tools.view.UserGuideView.Companion.SHAPE_ROUND
import java.lang.IllegalArgumentException

/**
 * @author zhangsh
 * @version V1.0
 * @date 2019-10-28 14:43
 */

class UserGuideLayout(context: Context, attrs: AttributeSet?, defStyle: Int)
    : FrameLayout(context, attrs, defStyle) {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private val mCachedChildrenMap = mutableMapOf<Class<*>, View>()

    private val mCustomGuideView by lazy {
        UserGuideView(context).also { addView(it, LayoutParams(-1, -1)) }
    }

    private val mGuideQueue = mutableListOf<GuideInfo>()

    var onGuideLayoutDissmissed: (() -> Unit)? = null

    init {
        setVisible(false)
    }

    fun showGuide(type: Class<*>) {
        mGuideQueue.add(GuideInfo().apply {
            typeClass = type
        })
        if (isShowing().not()) {
            nextGuide()
        }
    }

    @JvmOverloads
    fun showCustomGuideView(target: View,
                            guideText: String,
                            dashedShape: Int = SHAPE_ROUND,
                            enablePadding: Boolean = true) {
        mGuideQueue.add(GuideInfo().apply {
            this.targetView = target
            this.guideText = guideText
            this.dashedType = dashedShape
            this.enablePadding = enablePadding
        })
        if (isShowing().not()) {
            nextGuide()
        }
    }

    private fun nextGuide() {
        if (mGuideQueue.isNotEmpty()) {
            _showGuide(mGuideQueue.removeAt(0))
        } else {
            dismissGuide()
        }
    }

    private fun isShowing(): Boolean {
        return visibility == View.VISIBLE
    }

    private fun _showGuide(guideInfo: GuideInfo) {
        LogUtil.d("show guide, typeClass = ${guideInfo.typeClass}, targetView = ${guideInfo.targetView}")

        if (guideInfo.typeClass != null) {
            val type = guideInfo.typeClass!!
            val showed = PrefUtil.getBoolean(generatePreKey(type), false, context)
            if (showed) {
                nextGuide()
                return
            }

            val IType = GuideTypeProvider.getGuideType(type)
                    ?: throw IllegalArgumentException("call GuideTypeProvider.addGuideType() first")
            var child = mCachedChildrenMap[type]
            if (child == null) {
                child = IType.getView(context, LayoutInflater.from(context), this)
                addView(child)
            }
            child.setOnClickListener {
                nextGuide()
            }

            goneAllChildren()
            child.setVisible(true)

            PrefUtil.put(generatePreKey(type), true, context)
        } else {
            val target = guideInfo.targetView!!
            val guideText = guideInfo.guideText
            val dashedShape = guideInfo.dashedType
            val enablePadding = guideInfo.enablePadding

            val showed = PrefUtil.getBoolean(generatePreKey(target.javaClass), false, context)
            if (showed) {
                nextGuide()
                return
            }

            goneAllChildren()
            mCustomGuideView.show(target, guideText, dashedShape, enablePadding)
            mCustomGuideView.onDismissListener = {
                nextGuide()
            }

            PrefUtil.put(generatePreKey(target.javaClass), true, context)
        }

        setVisible(true)
    }

    private fun goneAllChildren() {
        for (i in 0 until childCount) {
            getChildAt(i).setVisible(false)
        }
    }

    fun dismissGuide(any: Any? = null) {
        if (any != null) {
            val iterator = mGuideQueue.iterator()
            while (iterator.hasNext()) {
                val guideInfo = iterator.next()
                if ((guideInfo.targetView == any ||
                                guideInfo.typeClass == any)) {
                    iterator.remove()
                    break
                }
            }
        } else {
            mGuideQueue.clear()
        }

        if (mGuideQueue.isEmpty()) {
            setVisible(false)
            onGuideLayoutDissmissed?.invoke()
        }
    }

    private fun generatePreKey(clz: Class<*>): String {
        return KEY_PREFIX + clz.canonicalName
    }

    companion object {
        private const val KEY_PREFIX = "user_guide_"
    }
}

internal class GuideInfo {
    internal var typeClass: Class<*>? = null

    internal var targetView: View? = null
    internal var guideText: String = ""
    internal var dashedType: Int = UserGuideView.SHAPE_ROUND
    internal var enablePadding: Boolean = true
}

object GuideTypeProvider {

    private val mGuideTypeMap = mutableMapOf<Class<*>, IGuideType>()

    fun addGuideType(type: IGuideType) {
        mGuideTypeMap.put(type.javaClass, type)
    }

    internal fun getGuideType(clz: Class<*>): IGuideType? {
        return mGuideTypeMap[clz]
    }
}

interface IGuideType {
    fun getView(context: Context, inflater: LayoutInflater, parent: UserGuideLayout): View
}

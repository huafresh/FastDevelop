package com.hua.kotlin_tools

import android.os.Build
import android.util.Log
import com.orhanobut.logger.Logger
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * 增加这一层方便AOP增加逻辑。
 *
 * @author zhangsh
 * @version V1.0
 * @date 2019-07-03 19:05
 */

object LogUtil {
    private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")

    private const val MAX_TAG_LENGTH = 23

    @JvmStatic
    fun d(tag: String?, msg: String) {
        if (tag == null) {
            Logger.t("@@@hua-${inferTag()}").d(msg)
        } else {
            Logger.t("@@@hua-$tag-${Thread.currentThread().name}").d(msg)
        }
    }

    @JvmStatic
    fun d(msg: String) {
        d(null, msg)
    }

    @JvmStatic
    fun d(tag: String?, jsonObject: JSONObject) {
        Logger.t("@@@hua-$tag-${Thread.currentThread().name}").json(jsonObject.toString())
    }

    @JvmStatic
    fun w(tag: String?, msg: String) {
        Logger.t("@@@hua-$tag").w(msg)
    }

    @JvmStatic
    fun e(tag: String?, msg: String) {
        e("${tag}-${Thread.currentThread().name}", msg, null)
    }

    @JvmStatic
    fun e(tag: String?, msg: String?, throwable: Throwable? = null) {
        Logger.log(Log.ERROR, "@@@hua-$tag", msg, throwable)
    }

    @JvmStatic
    fun e(msg: String, throwable: Throwable? = null) {
        e(inferTag(), msg, throwable)
    }

    @JvmStatic
    fun e(throwable: Throwable) {
        e(inferTag(), null, throwable)
    }

    @JvmStatic
    fun w(throwable: Throwable) {
        Logger.log(Log.WARN, inferTag(), null, throwable)
    }

    private fun inferTag(): String {
        val elements = Throwable().stackTrace
        var element: StackTraceElement? = null
        for (e in elements) {
            if (e.className.contains("LogUtil").not()) {
                element = e
                break
            }
        }
        if (element != null) {
            val className = element.fileName
            val split = className.split("$")
            var inferTag = split[0]
            val methodName = element.methodName
//            val m = ANONYMOUS_CLASS.matcher(inferTag)
//            if (m.find()) {
//                inferTag = m.replaceAll("")
//            }
//            inferTag = inferTag.substring(inferTag.lastIndexOf('.') + 1)
//            // Tag length limit was removed in API 24.
//            inferTag = if (inferTag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                inferTag
//            } else inferTag.substring(0, MAX_TAG_LENGTH)
            //return "${inferTag}-${methodName}-${Thread.currentThread().name}"
            return "${inferTag}-${Thread.currentThread().name}"
        }
        return "未知tag"
    }
}


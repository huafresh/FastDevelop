package com.hua.kotlin_tools

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.transition.Transition
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.lifecycle.MutableLiveData
import java.security.AccessController.getContext

/**
 * @author zhangsh
 * @version V1.0
 * @date 2020-02-17 10:33
 */

fun View.setVisible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

fun View.isVisible(): Boolean {
    return visibility == View.VISIBLE
}

fun Int.toDoubleFormatStr(): String {
    return if (this < 10) "0$this" else "$this"
}

fun dip2Px(dpValue: Float, context: Context): Int {
    val density = context.resources.displayMetrics.density
    return Math.round(dpValue * density)
}

fun sp2Px(spValue: Float, context: Context): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
            spValue, context.resources.displayMetrics).toInt()
}

fun isLocationServiceEnable(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    // getting GPS uploadStatus
    val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    // getting network uploadStatus
    val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    return isGPSEnabled || isNetworkEnabled
}

@JvmOverloads
fun debugToast(msg: String?, duration: Int = Toast.LENGTH_SHORT, context: Context) {
    if (BuildConfig.DEBUG && msg != null) {
        if (isMainThread()) {
            Toast.makeText(context, msg, duration).show()
        } else {
            mMainHandler.post { Toast.makeText(context, msg, duration).show() }
        }
    }
}

fun isDebug(): Boolean {
    return BuildConfig.DEBUG
}

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

val mMainHandler = Handler(Looper.getMainLooper())
fun isMainThread() = Thread.currentThread() == Looper.getMainLooper().thread
fun ensureMainThread(block: () -> Unit) {
    if (isMainThread()) {
        block()
    } else {
        mMainHandler.post { block() }
    }
}

fun Intent?.toKeyValueString(): String {
    val extras = this?.extras
    val builder = StringBuilder("[")
    extras?.keySet()?.forEach { key ->
        builder.append("$key:${extras.get(key)}")
    }
    builder.append("]")
    return builder.toString()
}

fun String?.toIntSafe(default: Int = -1): Int {
    return try {
        this?.toInt() ?: default
    } catch (e: NumberFormatException) {
        default
    }
}

fun View.isAttachedToWindowCompat(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        isAttachedToWindow
    } else {
        false
    }
}

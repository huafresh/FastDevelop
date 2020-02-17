package com.hua.kotlin_tools

import android.app.Application
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.databinding.ObservableField
import androidx.lifecycle.*
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import tv.focal.base.kotlin_ext.isMainThread
import tv.focal.base.thirdparty.rxbus.RxBus2
import tv.focal.base.util.LogUtil
import java.lang.Runnable
import java.util.function.Consumer

/**
 * @author zhangsh
 * @version V1.0
 * @date 2019/4/3 11:02 AM
 */

open class BaseViewModel(private val app: Application) : AndroidViewModel(app) {

    var isMaintenance = false

    protected val handler by lazy { Handler(Looper.getMainLooper()) }

    private val mDisposables = CompositeDisposable()

    fun getString(@StringRes resId: Int): String {
        return app.getString(resId)
    }

    fun getColor(@ColorRes resId: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            app.getColor(resId)
        } else {
            app.resources.getColor(resId)
        }
    }

    fun getDrawable(@DrawableRes resId: Int): Drawable? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            app.getDrawable(resId)
        } else {
            app.resources.getDrawable(resId)
        }
    }

    protected fun launch(block: suspend CoroutineScope.() -> Unit,
                         error: ((Throwable) -> Unit)? = null): Job {
        val handler = CoroutineExceptionHandler { context, e ->
            error?.invoke(e) ?: LogUtil.e(e)
        }
        return viewModelScope.launch(handler) {
            block.invoke(this)
        }
    }

    protected fun launchIO(block: suspend (CoroutineScope) -> Unit,
                           error: (suspend (Throwable) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                block.invoke(this)
            } catch (e: Throwable) {
                error?.invoke(e)
            }
        }
    }

    protected suspend fun runOnMainSync(block: suspend () -> Unit) {
        withContext(Dispatchers.Main) {
            block.invoke()
        }
    }

    protected suspend fun runSafe(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            LogUtil.e("runSafe error", e)
        }
    }

    /**
     * 一般用[runOnMainSync]可以实现线程切换了，但是某些场景需要通过handler来控制程序执行逻辑，
     * 那么就可以用这个方法。
     */
    @Deprecated("use handler directly")
    protected suspend fun postOnMain(runnable: Runnable, delay: Long = 0) {
        handler.postDelayed({
            runnable.run()
        }, delay)
    }

    // onClear时取消订阅
    protected fun <T> onEvent(eventClass: Class<T>, onEvent: ((T) -> Unit)) {
        val disposable = RxBus2.getDefault().onEvent(eventClass)
                .subscribeSafe(onEvent)
        mDisposables.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        mDisposables.dispose()
    }
}

fun <T> MutableLiveData<T>.set(value: T?) {
    if (isMainThread()) {
        this.value = value
    } else {
        this.postValue(value)
    }
}

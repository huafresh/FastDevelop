package com.hua.kotlin_tools

import android.os.Looper
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*

import java.io.Serializable
import java.lang.RuntimeException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * @author zhangsh
 * @version V1.0
 * @date 2019-06-23 09:52
 */

fun <T> Observable<T>.async(): Observable<T> {
    return this.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
}

/**
 * scope实例化放到子线程，提升启动速度。
 */
val sGlobalMainScope by lazy {
    CoroutineScope(SupervisorJob() + Dispatchers.Main)
}

val sGlobalIOScope by lazy {
    CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

/**
 * 这里的global的含义并非Coroutine中的[GlobalScope]
 * 而是为了区别于[BaseViewModel]中launch，此方法的目的就是使代码的执行环境变成协程。
 */
fun globalLaunchMain(block: suspend () -> Unit,
                     error: (suspend (Throwable) -> Unit)? = null) {
    sGlobalMainScope.launch {
        try {
            block.invoke()
        } catch (e: Throwable) {
            error?.invoke(e)
        }
    }
}

fun globalLaunchIO(block: suspend CoroutineScope.() -> Unit,
                   error: ((Throwable) -> Unit)? = null) {
    val handler = CoroutineExceptionHandler { context, e ->
        error?.invoke(e) ?: LogUtil.e(e)
    }
    sGlobalIOScope.launch(handler) {
        block.invoke(this)
    }
}

/**
 * 挂起协程，等待Observable发射数据或者异常
 */
suspend fun <T> Observable<T>.await(defaultValue: T? = null): T {
    return suspendCancellableCoroutine { continuation ->
        subscribe({
            continuation.resume(it)
        }, {
            if (defaultValue != null) {
                continuation.resume(defaultValue)
            } else {
                continuation.resumeWithException(it)
            }
        }, {
            // 某些Observable是有可能只发complete事件的，
            try {
                if (defaultValue != null) {
                    continuation.resume(defaultValue)
                } else {
                    continuation.resumeWithException(RxException("accident completed"))
                }
            } catch (e: IllegalStateException) {
                // Already resumed
            }
        })
    }
}

suspend fun <T> Observable<T>.awaitAsync(defaultValue: T? = null): T {
    return this.async().await(defaultValue)
}

suspend fun <T> Observable<T>.awaitNullable(): T? {
    return suspendCancellableCoroutine { continuation ->
        subscribe({
            continuation.resume(it)
        }, {
            continuation.resume(null)
        }, {
            // 某些Observable是有可能只发complete事件的，
            try {
                continuation.resume(null)
            } catch (e: IllegalStateException) {
                // Already resumed
            }
        })
    }
}

suspend fun <T> Deferred<T>.awaitOrException(errorHandler: ((Throwable) -> Unit)): T {
    return try {
        this.await()
    } catch (e: Throwable) {
        errorHandler(e)
        throw e
    }
}

/**
 * 挂起线程，等待Observable发射数据或者异常，超时时间60s。
 * 请注意必须在子线程中调用。
 */
fun <T> Observable<T>.await2(): T? {
    if (Looper.getMainLooper() == Looper.myLooper()) {
        throw RuntimeException("请在子线程中调用，避免卡顿")
    }

    return runBlocking<T?> {
        withTimeout<T?>(60000) {
            try {
                await()
            } catch (e: Throwable) {
                null
            }
        }
    }
}


open class RxException(msg: String) : Exception(msg)

open class RxBusinessException(msg: String) : RxException(msg)

open class RxErrorException(msg: String) : RxException(msg)


class ResultSubjectHelper<T> {

    private var resultSubject: PublishSubject<T>? = null

    fun createSubject(): Observable<T> {
        resultSubject?.onComplete()
        return PublishSubject.create<T>().also { resultSubject = it }
    }

    fun notifyResult(result: T) {
        resultSubject?.onNext(result)
        resultSubject?.onComplete()
        resultSubject = null
    }

    fun notifyClose() {
        resultSubject?.onComplete()
        resultSubject = null
    }

}

/**
 * [block]出现异常时将返回null
 */
fun <T> CoroutineScope.asyncSafe(
        block: suspend CoroutineScope.() -> T?,
        error: (suspend Throwable.() -> Unit)? = null
): Deferred<T?> {
    return async {
        try {
            block.invoke(this)
        } catch (e: Exception) {
            error?.invoke(e)
            null
        }
    }
}

fun <T> Observable<T>.subscribeSuspend(consume: suspend ((T) -> Unit),
                                       error: (suspend (Throwable) -> Unit)? = null): Disposable {
    return this.subscribe({
        globalLaunchMain({
            consume(it)
        })
    }, {
        globalLaunchMain({
            error?.invoke(it)
        })
    })
}

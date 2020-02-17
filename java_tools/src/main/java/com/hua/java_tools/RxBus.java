package com.hua.java_tools;

import com.hua.kotlin_tools.LogUtil;
import com.trello.lifecycle2.android.lifecycle.AndroidLifecycle;
import com.trello.rxlifecycle3.LifecycleProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.observers.LambdaObserver;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * 结合rxLifeAndroid库解决了生命周期的问题。
 *
 * @author zhangsh
 * @version V1.0
 * @date 2019/3/20 3:30 PM
 */

public class RxBus {
    private static volatile RxBus mDefaultInstance;
    private final Subject<Object> mBus;
    private final Map<Class<?>, Object> mStickyEventMap;

    private RxBus() {
        mBus = PublishSubject.create();
        mStickyEventMap = new ConcurrentHashMap<>();
    }

    public static RxBus getDefault() {
        if (mDefaultInstance == null) {
            synchronized (RxBus.class) {
                if (mDefaultInstance == null) {
                    mDefaultInstance = new RxBus();
                }
            }
        }
        return mDefaultInstance;
    }

    /**
     * 发送事件
     * @deprecated 用send，好理解
     */
    public void post(Object event) {
        mBus.onNext(event);
    }

    /**
     * 发送事件
     */
    public void send(Object event) {
        mBus.onNext(event);
    }

    /**
     * 根据传递的 eventType 类型返回特定类型(eventType)的 被观察者
     *
     * @deprecated 用onEvent方法，更好理解
     */
    public <T> LifecycleObservable<T> toObservable(Class<T> eventType) {
        return new LifecycleObservable<>(mBus.ofType(eventType));
    }

    public <T> LifecycleObservable<T> onEvent(Class<T> eventType) {
        return toObservable(eventType);
    }

    /**
     * 判断是否有订阅者
     */
    public boolean hasObservers() {
        return mBus.hasObservers();
    }

    /**
     * 发送一个新Sticky事件
     */
    public void postSticky(Object event) {
        synchronized (mStickyEventMap) {
            mStickyEventMap.put(event.getClass(), event);
        }
        post(event);
    }

    /**
     * 根据传递的 eventType 类型返回特定类型(eventType)的 被观察者
     *
     * @deprecated 用onStickyEvent, 更好理解
     */
    public <T> LifecycleObservable<T> toObservableSticky(final Class<T> eventType) {
        synchronized (mStickyEventMap) {
            LifecycleObservable<T> observable = toObservable(eventType);
            final Object event = mStickyEventMap.get(eventType);

            if (event != null) {
                Observable<T> source = observable.mergeWith(Observable.create(new ObservableOnSubscribe<T>() {
                    @Override
                    public void subscribe(ObservableEmitter<T> e) throws Exception {
                        e.onNext(eventType.cast(event));
                    }
                }));
                return new LifecycleObservable<>(source);
            } else {
                return observable;
            }
        }
    }

    public <T> LifecycleObservable<T> onStickyEvent(final Class<T> eventType) {
        return toObservableSticky(eventType);
    }

//    /**
//     * 根据eventType获取Sticky事件
//     */
//    public <T> T getStickyEvent(Class<T> eventType) {
//        synchronized (mStickyEventMap) {
//            return eventType.cast(mStickyEventMap.get(eventType));
//        }
//    }

    /**
     * 移除指定eventType的Sticky事件
     */
    public <T> T removeStickyEvent(Class<T> eventType) {
        synchronized (mStickyEventMap) {
            return eventType.cast(mStickyEventMap.remove(eventType));
        }
    }

    /**
     * 移除所有的Sticky事件
     */
    public void removeAllStickyEvents() {
        synchronized (mStickyEventMap) {
            mStickyEventMap.clear();
        }
    }

    public static class LifecycleObservable<T> extends Observable<T> {
        private static final String TAG = LifecycleObservable.class.getSimpleName();

        private Observable<T> source;

        public LifecycleObservable(Observable<T> source) {
            this.source = source;
        }

        public LifecycleObservable<T> bindLifeOwner(LifecycleOwner owner) {
            LifecycleProvider<Lifecycle.Event> provider = AndroidLifecycle.createLifecycleProvider(owner);
            return new LifecycleObservable<>(compose(provider.<T>bindToLifecycle()));
        }

        public LifecycleObservable<T> bindLifeOwner(LifecycleOwner owner, Lifecycle.Event event) {
            LifecycleProvider<Lifecycle.Event> provider = AndroidLifecycle.createLifecycleProvider(owner);
            return new LifecycleObservable<>(compose(provider.<T>bindUntilEvent(event)));
        }

        public LifecycleObservable<T> bindLifeProvider(LifecycleProvider<?> provider) {
            return new LifecycleObservable<>(compose(provider.<T>bindToLifecycle()));
        }

        @Override
        protected void subscribeActual(Observer<? super T> observer) {
            source.subscribe(observer);
        }

        /**
         * 修复RxBus的一个问题，即：
         * 当onNext执行异常时，RxJava会自动取消订阅关系，所以会产生有时可以收到消息，有时不可以的问题。
         */
        public Disposable subscribeSafe(final Consumer<T> onNext) {
            return subscribe(new Consumer<T>() {
                @Override
                public void accept(T t) throws Exception {
                    try {
                        onNext.accept(t);
                    } catch (Exception e) {
                        //e.printStackTrace();
                        LogUtil.e(TAG, "RxBus onNext error.", e);
                    }
                }
            });
        }
    }

    public static class LambdaObserverWrap<T> implements Observer<T> {
        private LambdaObserver<T> origin;

        public LambdaObserverWrap(LambdaObserver<T> origin) {
            this.origin = origin;
        }

        @Override
        public void onSubscribe(Disposable d) {
            origin.onSubscribe(d);
        }

        @Override
        public void onNext(T t) {
            origin.onNext(t);
        }

        @Override
        public void onError(Throwable e) {
            origin.onError(e);
        }

        @Override
        public void onComplete() {
            origin.onComplete();
        }
    }
}

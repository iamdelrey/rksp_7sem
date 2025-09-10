package rksp.practices.pr3;

import io.reactivex.rxjava3.core.MaybeObserver;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;

import java.util.concurrent.ThreadLocalRandom;

public class Task2_3_3_LastOnly {
    public static void main(String[] args) {
        Observable<Integer> src = Observable.defer(() -> {
            int n = ThreadLocalRandom.current().nextInt(0, 1001);
            return Observable.range(0, n)
                    .map(i -> ThreadLocalRandom.current().nextInt(0, 1001));
        });

        src.lastElement()
                .subscribe(new MaybeObserver<Integer>() {
                    @Override public void onSubscribe(Disposable d) { }
                    @Override public void onSuccess(Integer v) {
                        System.out.println("LAST = " + v);
                    }
                    @Override public void onError(Throwable e) {
                        e.printStackTrace();
                    }
                    @Override public void onComplete() {
                        System.out.println("Поток пуст, последнего элемента нет");
                    }
                });
    }
}

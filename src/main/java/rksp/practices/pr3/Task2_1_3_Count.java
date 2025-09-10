package rksp.practices.pr3;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;

import java.util.concurrent.ThreadLocalRandom;

public class Task2_1_3_Count {
    public static void main(String[] args) {
        Observable<Integer> source = Observable.defer(() -> {
            int n = ThreadLocalRandom.current().nextInt(0, 1001);
            return Observable.range(0, n)
                    .map(i -> ThreadLocalRandom.current().nextInt(0, 1001));
        });

        source.count()
                .map(Long::intValue)
                .toObservable()
                .subscribe(new Observer<Integer>() {
                    @Override public void onSubscribe(Disposable d) { }
                    @Override public void onNext(Integer cnt) {
                        System.out.println("COUNT = " + cnt);
                    }
                    @Override public void onError(Throwable e) { e.printStackTrace(); }
                    @Override public void onComplete() { System.out.println("Done"); }
                });
    }
}

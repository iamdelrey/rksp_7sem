package rksp.practices.pr3;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Task2_2_3_ParallelMerge {
    public static void main(String[] args) {
        Observable<Integer> a = Observable.range(0, 1000)
                .map(i -> ThreadLocalRandom.current().nextInt(0, 10))
                .subscribeOn(Schedulers.computation())
                .concatMap(x -> Observable.just(x).delay(1, TimeUnit.MILLISECONDS));

        Observable<Integer> b = Observable.range(0, 1000)
                .map(i -> ThreadLocalRandom.current().nextInt(0, 10))
                .subscribeOn(Schedulers.io())
                .concatMap(x -> Observable.just(x).delay(1, TimeUnit.MILLISECONDS));

        Observable<Integer> merged = Observable.merge(a, b);

        merged.take(20)
                .blockingSubscribe(
                        v -> System.out.print(v + " "),
                        Throwable::printStackTrace,
                        () -> System.out.println("\nDone")
                );
    }
}

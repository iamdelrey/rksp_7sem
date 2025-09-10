package rksp.practices.pr3;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Task1_SensorsAlarm {
    private static final int TEMP_NORM = 25;
    private static final int CO2_NORM = 70;

    public static void main(String[] args) {
        int samples = 20;

        Observable<Integer> tempSensor = Observable.interval(1, TimeUnit.SECONDS)
                .map(t -> ThreadLocalRandom.current().nextInt(15, 31))
                .doOnNext(t -> System.out.println("[TEMP] " + t));

        Observable<Integer> co2Sensor = Observable.interval(1, TimeUnit.SECONDS)
                .map(t -> ThreadLocalRandom.current().nextInt(30, 101))
                .doOnNext(c -> System.out.println(" [CO2] " + c));

        Observable<String> alarmStream = Observable
                .combineLatest(tempSensor, co2Sensor, (t, c) -> {
                    boolean badT = t > TEMP_NORM;
                    boolean badC = c > CO2_NORM;
                    if (badT && badC) return "ALARM!!! t=" + t + " c=" + c;
                    if (badT) return "Warn: temperature high (" + t + ")";
                    if (badC) return "Warn: CO2 high (" + c + ")";
                    return "OK";
                })
                .distinctUntilChanged();

        alarmStream
                .take(samples)
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        System.out.println("Alarm ON");
                    }

                    @Override
                    public void onNext(String msg) {
                        System.out.println("[ALARM] " + msg);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("Alarm OFF");
                    }
                });

        try {
            Thread.sleep((samples + 2) * 1000L);
        } catch (InterruptedException ignored) {
        }
    }
}

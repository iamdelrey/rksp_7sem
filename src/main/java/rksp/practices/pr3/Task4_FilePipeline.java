package rksp.practices.pr3;

import io.reactivex.rxjava3.core.BackpressureOverflowStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Task4_FilePipeline {

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        int total = 30;

        Flowable<MyFile> generator = Flowable
                .rangeLong(1, total)
                .concatMap(id -> Flowable.just(newRandomFile(id))
                        .delay(ThreadLocalRandom.current().nextInt(100, 1001), TimeUnit.MILLISECONDS));

        Flowable<MyFile> queue = generator
                .onBackpressureBuffer(
                        5,
                        () -> System.out.println("[QUEUE] overflow -> drop oldest"),
                        BackpressureOverflowStrategy.DROP_OLDEST
                )
                .observeOn(Schedulers.computation());

        queue
                .groupBy(f -> f.type)
                .flatMap(group -> group
                                .concatMap(f -> process(f)
                                        .doOnSubscribe(d -> System.out.printf("[HANDLER %s] start %s%n", group.getKey(), f))
                                        .doOnNext(done -> System.out.printf("[HANDLER %s] done  %s%n", group.getKey(), done))
                                )
                        , 3)
                .doOnComplete(() -> System.out.println("All files processed."))
                .blockingSubscribe(
                        f -> {
                        },
                        Throwable::printStackTrace
                );
    }

    private static MyFile newRandomFile(long id) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        FileType type = FileType.values()[r.nextInt(FileType.values().length)];
        int size = r.nextInt(10, 101);
        System.out.printf("[GEN] %s%n", new MyFile(id, type, size));
        return new MyFile(id, type, size);
    }

    private static Flowable<MyFile> process(MyFile f) {
        return Flowable.just(f).delay(f.size * 7L, TimeUnit.MILLISECONDS);
    }

    enum FileType {XML, JSON, XLS}

    static class MyFile {
        final long id;
        final FileType type;
        final int size;

        MyFile(long id, FileType type, int size) {
            this.id = id;
            this.type = type;
            this.size = size;
        }

        @Override
        public String toString() {
            return "File#%d[%s,%dKB]".formatted(id, type, size);
        }
    }
}

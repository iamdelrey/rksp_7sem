package rksp.practices.pr3;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class Task3_UserFriends {

    private static UserFriend[] DB;

    public static void main(String[] args) {
        DB = buildDb(20, 5);

        Integer[] userIds = IntStream.range(0, 10)
                .map(i -> ThreadLocalRandom.current().nextInt(1, 21))
                .boxed().toArray(Integer[]::new);

        System.out.println("userIds = " + Arrays.toString(userIds));

        Observable.fromArray(userIds)
                .flatMap(Task3_UserFriends::getFriends)
                .subscribe(new Observer<UserFriend>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        System.out.println("Subscribed");
                    }

                    @Override
                    public void onNext(UserFriend uf) {
                        System.out.println(uf);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("Done.");
                    }
                });
    }

    public static Observable<UserFriend> getFriends(int userId) {
        return Observable.fromArray(DB)
                .filter(uf -> uf.userId == userId);
    }

    private static UserFriend[] buildDb(int users, int maxFriendsPerUser) {
        List<UserFriend> list = new ArrayList<>();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int u = 1; u <= users; u++) {
            int cnt = rnd.nextInt(0, maxFriendsPerUser + 1);
            for (int i = 0; i < cnt; i++) {
                int f;
                do {
                    f = rnd.nextInt(1, users + 1);
                } while (f == u);
                list.add(new UserFriend(u, f));
            }
        }
        return list.toArray(UserFriend[]::new);
    }

    public static class UserFriend {
        public final int userId;
        public final int friendId;

        public UserFriend(int userId, int friendId) {
            this.userId = userId;
            this.friendId = friendId;
        }

        @Override
        public String toString() {
            return "UserFriend{userId=%d, friendId=%d}".formatted(userId, friendId);
        }
    }
}

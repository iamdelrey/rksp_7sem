package rksp.practices.pr4;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
public class RSocketApi {

    private final ReadingRepository repo;
    private final Sinks.Many<Reading> sink = Sinks.many().multicast().onBackpressureBuffer();
    private final AtomicInteger channelId = new AtomicInteger(0);

    public RSocketApi(ReadingRepository repo) {
        this.repo = repo;
    }

    @MessageMapping("fnf.log")
    public Mono<Void> logReading(@Payload Reading reading) {
        if (reading == null) return Mono.empty();
        if (reading.getTs() == null) reading.setTs(Instant.now());
        return repo.save(reading)
                .doOnNext(sink::tryEmitNext)
                .then();
    }

    @MessageMapping("rr.lastByType")
    public Mono<Reading> lastByType(@Payload String type) {
        return repo.findTopByTypeOrderByTsDesc(type)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No readings for type=" + type)));
    }

    @MessageMapping("stream.all")
    public Flux<Reading> streamAll() {
        return repo.findAll()
                .sort((a, b) -> a.getTs().compareTo(b.getTs()))
                .takeLast(10)
                .concatWith(sink.asFlux())
                .filter(Objects::nonNull);
    }

    @MessageMapping("channel.sum")
    public Flux<Integer> channelSum(@Payload Flux<Integer> inbound) {
        int id = channelId.incrementAndGet();
        return inbound
                .map(i -> i == null ? 0 : i)
                .filter(i -> i >= 0)
                .scan(0, Integer::sum)
                .skip(1)
                .doOnSubscribe(s -> System.out.println("[channel#" + id + "] start"))
                .doOnNext(s -> System.out.println("[channel#" + id + "] sum=" + s))
                .doFinally(s -> System.out.println("[channel#" + id + "] completed"));
    }
}

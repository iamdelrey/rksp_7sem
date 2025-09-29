package rksp.practices.pr4;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(
        classes = ServerApp.class,
        properties = "spring.rsocket.server.port=0"
)
class ServerRSocketTests {

    @Autowired RSocketStrategies strategies;
    @Autowired ReadingRepository repo;
    @Autowired Environment env;

    RSocketRequester requester;

    @BeforeEach
    void setup() {
        Integer port = env.getProperty("local.rsocket.server.port", Integer.class);
        assertNotNull(port, "RSocket port was not assigned");
        requester = RSocketRequester.builder()
                .rsocketStrategies(strategies)
                .dataMimeType(MimeTypeUtils.APPLICATION_JSON)
                .tcp("localhost", port);
    }

    @Test
    void rr_lastByType_returnsLastReading() {
        var r1 = new Reading("TEMP", 20, Instant.now().minusSeconds(2));
        var r2 = new Reading("TEMP", 30, Instant.now());
        StepVerifier.create(repo.saveAll(List.of(r1, r2)).then()).verifyComplete();

        Mono<Reading> mono = requester.route("rr.lastByType").data("TEMP").retrieveMono(Reading.class);
        StepVerifier.create(mono)
                .assertNext(r -> {
                    assertThat(r.getType()).isEqualTo("TEMP");
                    assertThat(r.getValue()).isEqualTo(30);
                })
                .verifyComplete();
    }

    @Test
    void channel_sum_returnsRunningSum() {
        Flux<Integer> input = Flux.just(1, 2, 3);
        var result = requester.route("channel.sum")
                .data(input, Integer.class)
                .retrieveFlux(Integer.class)
                .take(3);
        StepVerifier.create(result)
                .expectNext(1, 3, 6)
                .verifyComplete();
    }
}

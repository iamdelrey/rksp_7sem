package rksp.practices.pr4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.rsocket.RSocketServerAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@SpringBootApplication(exclude = RSocketServerAutoConfiguration.class) // <- не поднимать RSocket-сервер
public class ClientApp {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ClientApp.class);
        app.setWebApplicationType(WebApplicationType.NONE);             // <- без HTTP
        try (ConfigurableApplicationContext ctx = app.run(args)) {
            var strategies = ctx.getBean(RSocketStrategies.class);
            int port = Integer.getInteger("rsocket.port", 7777);

            RSocketRequester requester = RSocketRequester.builder()
                    .rsocketStrategies(strategies)
                    .dataMimeType(MimeTypeUtils.APPLICATION_JSON)
                    .tcp("localhost", port);

            var now = Instant.now();
            Flux<Reading> seed = Flux.just(
                    new Reading("TEMP", 24, now.minusSeconds(5)),
                    new Reading("CO2",  65, now.minusSeconds(4)),
                    new Reading("TEMP", 27, now.minusSeconds(3)),
                    new Reading("CO2",  80, now.minusSeconds(2))
            );
            seed.flatMap(r -> requester.route("fnf.log").data(r).send()).blockLast();

            Mono<Reading> lastTemp = requester.route("rr.lastByType").data("TEMP").retrieveMono(Reading.class);
            System.out.println("RR last TEMP = " + lastTemp.block());

            requester.route("stream.all").retrieveFlux(Reading.class)
                    .take(5)
                    .doOnNext(r -> System.out.println("STREAM -> " + r))
                    .blockLast();

            List<Integer> input = List.of(1, 2, 3, 4, 5);
            var sums = requester.route("channel.sum")
                    .data(Flux.fromIterable(input).delayElements(Duration.ofMillis(150)), Integer.class)
                    .retrieveFlux(Integer.class)
                    .take(input.size())
                    .collectList()
                    .block();
            System.out.println("CHANNEL sums: " + sums);
        }
    }
}

package rksp.practices.pr4;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ReadingRepository extends ReactiveCrudRepository<Reading, Long> {
    Mono<Reading> findTopByTypeOrderByTsDesc(String type);
}

package rksp.practices.pr5;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.nio.file.*;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class FileService {
    private final Path root;
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy").withZone(ZoneOffset.UTC);

    public FileService(@Value("${app.upload-path:/upload-files}") String uploadPath) {
        this.root = Paths.get(uploadPath);
    }

    public Mono<Void> init() {
        return Mono.fromRunnable(() -> {
            try { Files.createDirectories(root); } catch (Exception e) { throw new RuntimeException(e); }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Flux<String> saveAll(Flux<FilePart> parts) {
        return parts.flatMap(fp -> {
            String newName = UUID.randomUUID() + "_" + fp.filename();
            Path dest = root.resolve(newName);
            return fp.transferTo(dest).thenReturn(newName);
        });
    }

    public Flux<File> listFiles() {
        return Mono.fromCallable(() -> {
                    if (!Files.exists(root)) return Stream.<File>empty();
                    return Files.list(root).map(Path::toFile);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromStream);
    }

    public Path resolve(String name) { return root.resolve(name); }

    public String fmt(long lastModifiedMillis) {
        return fmt.format(java.time.Instant.ofEpochMilli(lastModifiedMillis));
    }
}

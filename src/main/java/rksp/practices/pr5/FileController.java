package rksp.practices.pr5;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rksp.practices.pr5.dto.FileInfoDTO;

import java.io.File;
import java.nio.file.Files;

@RestController
@RequestMapping("/api")
public class FileController {
    private final FileService svc;
    private final String instance;

    public FileController(FileService svc, @Value("${app.name:${NAME_APP:app}}") String instance) {
        this.svc = svc; this.instance = instance;
    }

    @GetMapping("/whoami")
    public Mono<String> whoami() { return Mono.just(instance); }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<?>> upload(@RequestPart("files") Flux<FilePart> files) {
        return svc.init()
                .thenMany(svc.saveAll(files))
                .collectList()
                .map(names -> ResponseEntity.ok().body(names));
    }

    @GetMapping("/files")
    public Flux<FileInfoDTO> list() {
        return svc.init().thenMany(
                svc.listFiles().map(f -> new FileInfoDTO(
                        f.getName(),
                        svc.fmt(f.lastModified()),
                        "/upload-files/" + f.getName(),
                        instance
                ))
        );
    }

    @GetMapping("/files/{name}")
    public Mono<ResponseEntity<FileSystemResource>> download(@PathVariable String name) {
        return Mono.fromCallable(() -> svc.resolve(name).toFile())
                .flatMap(f -> f.exists()
                        ? Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + f.getName() + "\"")
                        .contentType(probeType(f))
                        .body(new FileSystemResource(f)))
                        : Mono.just(ResponseEntity.notFound().build())
                );
    }

    private MediaType probeType(File f) {
        try {
            String t = Files.probeContentType(f.toPath());
            return (t != null) ? MediaType.parseMediaType(t) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) { return MediaType.APPLICATION_OCTET_STREAM; }
    }
}

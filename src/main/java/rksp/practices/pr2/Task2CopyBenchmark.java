package rksp.practices.pr2;

import org.apache.commons.io.FileUtils;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Task2CopyBenchmark {

    record Result(String method, long bytes, long millis, long memBytes) {}

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java pr2.Task2CopyBenchmark <dir> [--sizeMB 100] [--iterations 3]");
            return;
        }
        final Path dir = Paths.get(args[0]);
        final long sizeMB = argLong(args, "--sizeMB", 100);
        final int iterations = (int) argLong(args, "--iterations", 3);
        run(dir, sizeMB, iterations);
    }

    static void run(Path dir, long sizeMB, int iterations) throws IOException {
        Files.createDirectories(dir);
        Path src = dir.resolve("benchmark-src.bin");
        if (!Files.exists(src) || Files.size(src) != sizeMB * 1024L * 1024L) {
            System.out.printf("Generating source file %s (%,d MB)...%n", src, sizeMB);
            generateFile(src, sizeMB);
        } else {
            System.out.printf("Using existing source file %s (%,d bytes)%n", src, Files.size(src));
        }

        List<Result> results = new ArrayList<>();
        for (int i = 1; i <= iterations; i++) {
            final int iter = i;
            System.out.printf("%nIteration %d/%d%n", iter, iterations);

            results.add(benchCopy("Streams (Buffered)", () -> {
                Path dst = dir.resolve("copy-streams-" + iter + ".bin");
                copyUsingStreams(src.toFile(), dst.toFile());
                return dst;
            }));

            results.add(benchCopy("FileChannel.transferTo", () -> {
                Path dst = dir.resolve("copy-channel-transferTo-" + iter + ".bin");
                copyUsingChannelTransferTo(src.toFile(), dst.toFile());
                return dst;
            }));

            results.add(benchCopy("Files.copy (NIO2)", () -> {
                Path dst = dir.resolve("copy-files-" + iter + ".bin");
                copyUsingFiles(src.toFile(), dst.toFile());
                return dst;
            }));

            results.add(benchCopy("Apache Commons IO", () -> {
                Path dst = dir.resolve("copy-commons-" + iter + ".bin");
                copyUsingCommonsIO(src.toFile(), dst.toFile());
                return dst;
            }));
        }

        System.out.println("\nSummary (average per method):");
        var grouped = new LinkedHashMap<String, List<Result>>();
        for (var r : results) grouped.computeIfAbsent(r.method, k -> new ArrayList<>()).add(r);
        System.out.printf("%-24s %12s %12s %14s%n", "Method", "MB/s", "Millis", "ΔMemory (MB)");
        for (var e : grouped.entrySet()) {
            long bytes = e.getValue().stream().mapToLong(v -> v.bytes).sum() / e.getValue().size();
            long millis = (long) e.getValue().stream().mapToLong(v -> v.millis).average().orElse(0);
            long mem = (long) e.getValue().stream().mapToLong(v -> v.memBytes).average().orElse(0);
            double mbps = (bytes / 1024.0 / 1024.0) / (millis / 1000.0);
            System.out.printf("%-24s %12.1f %12d %14.1f%n", e.getKey(), mbps, millis, mem / 1024.0 / 1024.0);
        }
    }

    interface CopyAction { Path run() throws IOException; }

    static Result benchCopy(String name, CopyAction action) throws IOException {
        System.gc();
        sleepQuiet(150);
        long memBefore = usedMem();
        Instant t0 = Instant.now();
        Path dst = action.run();
        long millis = Duration.between(t0, Instant.now()).toMillis();
        long memAfter = usedMem();
        long deltaMem = Math.max(0, memAfter - memBefore);
        long bytes = Files.size(dst);
        System.out.printf("%-24s -> %,d bytes in %d ms, Δmem ~ %.1f MB%n",
                name, bytes, millis, deltaMem / 1024.0 / 1024.0);
        return new Result(name, bytes, millis, deltaMem);
    }

    static void generateFile(Path path, long sizeMB) throws IOException {
        long total = sizeMB * 1024L * 1024L;
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buf = ByteBuffer.allocateDirect(1024 * 1024);
            Random r = new Random(123456789L);
            long written = 0;
            while (written < total) {
                buf.clear();
                int toWrite = (int) Math.min(buf.capacity(), total - written);
                for (int i = 0; i < toWrite; i++) buf.put((byte) r.nextInt(256));
                buf.flip();
                while (buf.hasRemaining()) ch.write(buf);
                written += toWrite;
            }
        }
    }

    static void copyUsingStreams(File src, File dst) throws IOException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(src));
             OutputStream out = new BufferedOutputStream(new FileOutputStream(dst))) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
        }
    }

    static void copyUsingChannelTransferTo(File src, File dst) throws IOException {
        try (FileChannel in = new FileInputStream(src).getChannel();
             FileChannel out = new FileOutputStream(dst).getChannel()) {
            long size = in.size(), pos = 0;
            while (pos < size) {
                long transferred = in.transferTo(pos, size - pos, out);
                if (transferred <= 0) break;
                pos += transferred;
            }
        }
    }

    static void copyUsingFiles(File src, File dst) throws IOException {
        Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    static void copyUsingCommonsIO(File src, File dst) throws IOException {
        FileUtils.copyFile(src, dst);
    }

    static long usedMem() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }
    static void sleepQuiet(long ms) {
        try { TimeUnit.MILLISECONDS.sleep(ms); } catch (InterruptedException ignored) {}
    }

    static long argLong(String[] args, String key, long def) {
        for (int i = 0; i < args.length - 1; i++) if (args[i].equalsIgnoreCase(key)) {
            try { return Long.parseLong(args[i + 1]); } catch (Exception ignore) {}
        }
        return def;
    }
}
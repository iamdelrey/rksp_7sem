package rksp.practices.pr2;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

public class Task4DirectoryWatcher {
    private static final Map<Path, FileSnapshot> snapshots = new HashMap<>();

    static class FileSnapshot {
        final long size;
        final int checksum16;
        final List<String> lines;
        FileSnapshot(long size, int checksum16, List<String> lines) {
            this.size = size;
            this.checksum16 = checksum16;
            this.lines = lines;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java rksp.practices.pr2.Task4DirectoryWatcher <dir-to-watch>");
            return;
        }
        Path dir = Paths.get(args[0]);
        watch(dir);
    }

    public static void watch(Path dir) throws IOException, InterruptedException {
        if (!Files.isDirectory(dir)) {
            System.err.println("Not a directory: " + dir.toAbsolutePath());
            return;
        }
        System.out.println("Watching: " + dir.toAbsolutePath());
        try (WatchService ws = FileSystems.getDefault().newWatchService()) {
            dir.register(ws,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            while (true) {
                WatchKey key = ws.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                    @SuppressWarnings("unchecked")
                    Path name = ((WatchEvent<Path>) event).context();
                    Path full = dir.resolve(name);

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        System.out.println("[CREATE] " + name);
                        snapshot(full);
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        System.out.println("[MODIFY] " + name);
                        diff(full);
                        snapshot(full);
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        FileSnapshot snap = snapshots.get(full);
                        if (snap != null) {
                            System.out.printf("[DELETE] %s size=%d bytes, checksum16=0x%04X%n",
                                    name, snap.size, snap.checksum16 & 0xFFFF);
                        } else {
                            System.out.printf("[DELETE] %s (no snapshot available)%n", name);
                        }
                        snapshots.remove(full);
                    }
                }
                if (!key.reset()) break;
            }
        }
    }

    private static void snapshot(Path path) {
        try {
            if (!Files.exists(path) || Files.isDirectory(path)) return;
            long size = Files.size(path);
            int cs = Task3Checksum16.checksum16(path);
            List<String> lines = readLinesSafe(path);
            snapshots.put(path, new FileSnapshot(size, cs, lines));
        } catch (Exception ignored) {}
    }

    private static void diff(Path path) {
        try {
            if (!Files.exists(path) || Files.isDirectory(path)) return;
            List<String> newLines = readLinesSafe(path);
            FileSnapshot old = snapshots.get(path);

            Set<String> added = new LinkedHashSet<>(newLines);
            Set<String> removed = new LinkedHashSet<>();
            if (old != null) {
                added.removeAll(old.lines);
                removed.addAll(old.lines);
                removed.removeAll(newLines);
            }

            if (added.isEmpty() && removed.isEmpty()) {
                System.out.println("  No line-level changes detected.");
            } else {
                if (!added.isEmpty()) {
                    System.out.println("  + Added lines:");
                    for (String s : added) System.out.println("    + " + s);
                }
                if (!removed.isEmpty()) {
                    System.out.println("  - Removed lines:");
                    for (String s : removed) System.out.println("    - " + s);
                }
            }
        } catch (Exception e) {
            System.out.println("  (diff failed: " + e.getMessage() + ")");
        }
    }

    private static List<String> readLinesSafe(Path path) throws IOException {
        if (!Files.exists(path) || Files.isDirectory(path)) return List.of();
        return Files.readAllLines(path, Charset.forName("UTF-8"));
    }
}

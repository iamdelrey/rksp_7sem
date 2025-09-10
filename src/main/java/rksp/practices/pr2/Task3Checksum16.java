package rksp.practices.pr2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;

public class Task3Checksum16 {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java Task3Checksum16 <path-to-file>");
            return;
        }
        Path path = Paths.get(args[0]);
        if (!Files.exists(path) || Files.isDirectory(path)) {
            System.err.println("File not found or is a directory: " + path.toAbsolutePath());
            return;
        }
        int sum = checksum16(path);
        System.out.printf("Checksum16(ones' complement) of %s = 0x%04X (%d)%n",
                path, sum & 0xFFFF, sum & 0xFFFF);
    }

    public static int checksum16(Path path) throws IOException {
        try (FileChannel ch = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.allocateDirect(64 * 1024);
            int sum = 0;
            boolean high = true;
            int current = 0;
            int read;
            while ((read = ch.read(buf)) != -1) {
                if (read == 0) continue;
                buf.flip();
                while (buf.hasRemaining()) {
                    int b = buf.get() & 0xFF;
                    if (high) { current = (b << 8); high = false; }
                    else {
                        current |= b;
                        sum = add16(sum, current);
                        current = 0; high = true;
                    }
                }
                buf.clear();
            }
            if (!high) sum = add16(sum, current);
            return (~sum) & 0xFFFF;
        }
    }
    private static int add16(int sum, int word) {
        sum += (word & 0xFFFF);
        sum = (sum & 0xFFFF) + (sum >>> 16);
        return sum & 0xFFFF;
    }
}
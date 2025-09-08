package rksp.practices.pr2;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Task1ReadTextNio {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java Task1ReadTextNio <path-to-file.txt>");
            return;
        }
        Path path = Paths.get(args[0]);
        if (!Files.exists(path) || Files.isDirectory(path)) {
            System.err.println("File not found or is a directory: " + path.toAbsolutePath());
            return;
        }
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
}

package de.dal33t.powerfolder.util.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public final class ShardPlanner {

    public static final int DEFAULT_SECONDS = 10;

    private ShardPlanner() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            throw new IllegalArgumentException("usage: ShardPlanner <durations.properties> <classes.txt> <total> <index>");
        }
        Map<String, Integer> durations = readDurations(Paths.get(args[0]));
        List<String> classes = Files.readAllLines(Paths.get(args[1]), StandardCharsets.UTF_8).stream()
            .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        int total = Integer.parseInt(args[2]);
        int index = Integer.parseInt(args[3]);
        System.out.print(String.join(",", plan(classes, durations, total, index)));
    }

    public static Map<String, Integer> readDurations(Path file) throws IOException {
        Map<String, Integer> durations = new HashMap<>();
        if (!Files.exists(file)) {
            return durations;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        }
        for (String name : props.stringPropertyNames()) {
            durations.put(name, Integer.parseInt(props.getProperty(name).trim()));
        }
        return durations;
    }

    public static List<String> plan(List<String> classes, Map<String, Integer> durations, int total, int index) {
        if (total < 1 || index < 0 || index >= total) {
            throw new IllegalArgumentException("shard " + index + " of " + total);
        }
        List<String> ordered = new ArrayList<>(classes);
        Collections.sort(ordered);
        ordered.sort(Comparator.comparingInt((String c) -> seconds(durations, c)).reversed());
        long[] load = new long[total];
        List<List<String>> shards = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            shards.add(new ArrayList<>());
        }
        for (String c : ordered) {
            int lightest = 0;
            for (int i = 1; i < total; i++) {
                if (load[i] < load[lightest]) {
                    lightest = i;
                }
            }
            shards.get(lightest).add(c);
            load[lightest] += seconds(durations, c);
        }
        return shards.get(index);
    }

    private static int seconds(Map<String, Integer> durations, String className) {
        Integer known = durations.get(className);
        return known == null ? DEFAULT_SECONDS : Math.max(1, known);
    }
}

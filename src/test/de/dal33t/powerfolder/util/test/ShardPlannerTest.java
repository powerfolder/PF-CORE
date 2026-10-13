package de.dal33t.powerfolder.util.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class ShardPlannerTest {

    @Test
    public void testShardsAreDisjointAndCoverEveryClass() {
        List<String> classes = classes(173);
        Map<String, Integer> durations = new HashMap<>();
        for (int total = 1; total <= 6; total++) {
            Set<String> seen = new HashSet<>();
            int count = 0;
            for (int index = 0; index < total; index++) {
                List<String> shard = ShardPlanner.plan(classes, durations, total, index);
                count += shard.size();
                seen.addAll(shard);
            }
            assertEquals(classes.size(), count, "no class runs twice with " + total + " shards");
            assertEquals(new HashSet<>(classes), seen, "every class runs with " + total + " shards");
        }
    }

    @Test
    public void testPlanIsIndependentOfInputOrder() {
        List<String> classes = classes(40);
        List<String> shuffled = new ArrayList<>(classes);
        java.util.Collections.shuffle(shuffled, new java.util.Random(7));
        Map<String, Integer> durations = new HashMap<>();
        durations.put("de.pf.Test03", 300);
        durations.put("de.pf.Test17", 120);
        for (int index = 0; index < 3; index++) {
            assertEquals(ShardPlanner.plan(classes, durations, 3, index),
                ShardPlanner.plan(shuffled, durations, 3, index));
        }
    }

    @Test
    public void testLongClassesAreSpreadOverShards() {
        List<String> classes = classes(12);
        Map<String, Integer> durations = new HashMap<>();
        durations.put("de.pf.Test00", 900);
        durations.put("de.pf.Test01", 500);
        durations.put("de.pf.Test02", 450);
        durations.put("de.pf.Test03", 400);
        long[] load = new long[4];
        for (int index = 0; index < 4; index++) {
            for (String c : ShardPlanner.plan(classes, durations, 4, index)) {
                load[index] += durations.getOrDefault(c, ShardPlanner.DEFAULT_SECONDS);
            }
        }
        long max = 0;
        long min = Long.MAX_VALUE;
        for (long l : load) {
            max = Math.max(max, l);
            min = Math.min(min, l);
        }
        assertEquals(900, max, "the longest class sits alone on its shard");
        assertTrue(min >= 400, "no shard is left idle while another carries two long classes: " + java.util.Arrays.toString(load));
    }

    @Test
    public void testSingleShardRunsLongestFirst() {
        List<String> classes = classes(5);
        Map<String, Integer> durations = new HashMap<>();
        durations.put("de.pf.Test04", 50);
        durations.put("de.pf.Test02", 20);
        List<String> plan = ShardPlanner.plan(classes, durations, 1, 0);
        assertEquals("de.pf.Test04", plan.get(0));
        assertEquals("de.pf.Test02", plan.get(1));
        assertEquals(5, plan.size());
    }

    private static List<String> classes(int n) {
        List<String> classes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            classes.add(String.format("de.pf.Test%02d", i));
        }
        return classes;
    }
}

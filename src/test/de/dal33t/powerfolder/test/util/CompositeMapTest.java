package de.dal33t.powerfolder.test.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import junit.framework.TestCase;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.collection.CompositeMap;

public class CompositeMapTest extends TestCase {

    private static Semaphore LOCK = new Semaphore(3);

    public void testConcurrentAccess() {
        Map<String, String> map1 = new ConcurrentHashMap<String, String>();
        Map<String, String> map2 = new ConcurrentHashMap<String, String>();
        Map<String, String> map3 = new ConcurrentHashMap<String, String>();
        Map<String, String> composite = new CompositeMap<String, String>(map1,
            map2, map3);

        new Modifier(map1).start();
        new Modifier(map2).start();
        new Modifier(map3).start();

        for (int i = 0; i < 10000; i++) {
            for (Map.Entry<String, String> entry : composite.entrySet()) {
                if (!entry.getKey().equals(entry.getValue())) {
                    throw new IllegalStateException("Key: " + entry.getKey()
                        + " Value: " + entry.getValue());
                }
                if (LOCK.tryAcquire()) {
                    return;
                }
            }
        }
    }

    private class Modifier extends Thread {
        Map<String, String> map;

        public Modifier(Map<String, String> map) {
            super();
            this.map = map;
        }

        @Override
        public void run() {
            try {
                LOCK.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < 100000; i++) {
                String v = IdGenerator.makeId();
                map.put(v, v);
                if (i % 100 == 0) {
                    System.out.print(".");
                }
            }

            for (String key : map.keySet()) {
                map.remove(key);
            }

            System.out.println("COMPLETED");
            LOCK.release();
        }

    }
}

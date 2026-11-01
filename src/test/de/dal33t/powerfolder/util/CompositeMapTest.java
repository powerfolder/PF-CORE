/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 */
package de.dal33t.powerfolder.util;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.collection.CompositeMap;

public class CompositeMapTest {

    private static Semaphore LOCK = new Semaphore(3);

    @Test
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
            }

            for (String key : map.keySet()) {
                map.remove(key);
            }

            LOCK.release();
        }

    }
}

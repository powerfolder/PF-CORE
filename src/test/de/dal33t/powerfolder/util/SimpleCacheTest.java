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
 *
 */
package de.dal33t.powerfolder.util;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.SimpleCache;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.test.TestHelper;
public class SimpleCacheTest {

    @Test
    public void testValidCache() {
        Collection<String> expected = new LinkedList<>();
        SimpleCache<String, String> cache = new SimpleCache<>(
            Util.createConcurrentHashMap(), 100, TimeUnit.MILLISECONDS);

        for (int i = 0; i < 1000; i++) {
            String id = IdGenerator.makeId();
            expected.add(id);
            cache.put(id, id);

            String aId = cache.getValidEntry(id);
            assertEquals(id, aId);
        }
        assertEquals(1000, cache.getCacheHits());
        assertEquals(0, cache.getCacheMisses());

        TestHelper.waitMilliSeconds(200);

        for (String eId : expected) {
            String aId = cache.getValidEntry(eId);
            assertNull(aId);
        }
        assertEquals(1000, cache.getCacheHits());
        assertEquals(1000, cache.getCacheMisses());
    }

    @Test
    public void testIllegalTimeout() {
        assertThrows(IllegalArgumentException.class, () -> new SimpleCache<String, String>(0, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> new SimpleCache<String, String>(-1, TimeUnit.SECONDS));
    }

    /** INT-1809: a bounded cache drops the expired entries first, then any but the new one - never beyond its limit. */
    @Test
    public void testMaxEntries() {
        SimpleCache<String, String> cache = new SimpleCache<>(1, TimeUnit.HOURS, 3);
        for (int i = 0; i < 10; i++) {
            cache.put("key" + i, "value" + i);
            assertTrue(cache.size() <= 3, "Size " + cache.size() + " after " + (i + 1) + " puts");
            assertEquals("value" + i, cache.getValidEntry("key" + i), "The entry just added stays");
        }
        assertThrows(IllegalArgumentException.class, () -> new SimpleCache<String, String>(1, TimeUnit.HOURS, 0));
    }

    @Test
    public void testMaxEntriesShrinksWithRoomToSpare() {
        SimpleCache<String, String> cache = new SimpleCache<>(1, TimeUnit.HOURS, 100);
        for (int i = 0; i <= 100; i++) {
            cache.put("key" + i, "value" + i);
        }
        assertEquals(90, cache.size(), "Over the limit the cache shrinks to 90 % - not one entry per put");
    }

    @Test
    public void testMaxEntriesDropsExpiredFirst() {
        SimpleCache<String, String> cache = new SimpleCache<>(50, TimeUnit.MILLISECONDS, 2);
        cache.put("old1", "x");
        cache.put("old2", "x");
        TestHelper.waitMilliSeconds(100);
        cache.put("new", "y");
        assertEquals(1, cache.size(), "Both expired entries are gone, the new one stays");
        assertEquals("y", cache.getValidEntry("new"));
    }
}

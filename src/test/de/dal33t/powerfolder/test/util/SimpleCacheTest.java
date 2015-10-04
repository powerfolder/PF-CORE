/*
 * Copyright 2004 - 2015 Christian Sprajc. All rights reserved.
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
 * $Id$
 */
package de.dal33t.powerfolder.test.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.WeakHashMap;

import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.SimpleCache;
import de.dal33t.powerfolder.util.test.TestHelper;
import junit.framework.TestCase;

public class SimpleCacheTest extends TestCase {

    public void testValidCache() {
        Collection<String> expected = new LinkedList<>();
        SimpleCache<String, String> cache = new SimpleCache<>(
            new WeakHashMap<>(), 100);

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
}

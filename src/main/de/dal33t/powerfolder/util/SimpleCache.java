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
package de.dal33t.powerfolder.util;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import de.dal33t.powerfolder.util.logging.Loggable;

/**
 * A simple, unlimited cache for short term and low volume
 * 
 * @author sprajc
 * @param <K>
 *            the class of the key used for the cache
 * @param <E>
 *            the class of the cache entries
 */
public class SimpleCache<K, E> extends Loggable {
    private long entryTimeout;
    private Map<K, Pair<Date, E>> cache;
    private AtomicInteger cacheHits = new AtomicInteger();
    private AtomicInteger cacheMisses = new AtomicInteger();

    public SimpleCache(Map<K, Pair<Date, E>> backingMap, long duration,
        TimeUnit unit)
    {
        super();
        Reject.ifNull(backingMap, "Backing map");
        Reject.ifFalse(entryTimeout <= 0, "Illegal timeout value");
        this.cache = backingMap;
        this.entryTimeout = unit.toMillis(duration);
    }

    /**
     * Caches a given entry for the given timeout time.
     * 
     * @param key
     *            the key to cache the entry for.
     * @param entry
     *            the entry to cache.
     */
    public void put(K key, E entry) {
        Reject.ifNull(key, "Key is null. Not supported");
        Reject.ifNull(entry, "Value is null. Not supported");
        cache.put(key, new Pair<>(new Date(), entry));
    }

    /**
     * @param key
     *            the key to retrieve the cached value for.
     * @return the cached, valid entry.
     */
    public E getValidEntry(K key) {
        Pair<Date, E> p = cache.get(key);
        if (p == null) {
            cacheMisses.incrementAndGet();
            return null;
        }
        Date lastValid = p.getFirst();
        long ago = System.currentTimeMillis() - lastValid.getTime();
        if (ago > entryTimeout) {
            cache.remove(key);
            cacheMisses.incrementAndGet();
            return null;
        }
        cacheHits.incrementAndGet();
        return p.getSecond();
    }
    
    public void invalidate(K key) {
        cache.remove(key);
    }

    public int getCacheHits() {
        return cacheHits.get();
    }

    public int getCacheMisses() {
        return cacheMisses.get();
    }

    @Override
    public String toString() {
        int accesses = cacheHits.get() + cacheMisses.get();
        String effiStr = "n/a";
        
        if (accesses > 0) {
            double effi = ((double) cacheHits.get()) / accesses;
            effiStr = Format.formatPercent(effi * 100);
        }

        return "SimpleCache [efficiency=" + effiStr + ", entries="
            + cache.size() + ", timeoutMS=" + entryTimeout + ", cacheHits="
            + cacheHits + ", cacheMisses=" + cacheMisses + "]";
    }
}

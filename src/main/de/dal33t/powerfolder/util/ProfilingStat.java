/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id:$
 */
package de.dal33t.powerfolder.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Profiling statistics.
 */
public class ProfilingStat {

    private final String operationName;
    private final AtomicLong count = new AtomicLong();
    private final AtomicLong elapsed = new AtomicLong();

    public ProfilingStat(String operationName, long elapsed) {
        this.operationName = operationName;
        addElapsed(elapsed);
    }

    public void addElapsed (long delta) {
        elapsed.addAndGet(delta);
        count.incrementAndGet();
    }

    public long getCount() {
        return count.get();
    }

    public long getElapsed() {
        return elapsed.get();
    }

    public String getOperationName() {
        return operationName;
    }
}

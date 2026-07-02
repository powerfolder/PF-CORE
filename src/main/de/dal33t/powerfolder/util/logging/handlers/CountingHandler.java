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
package de.dal33t.powerfolder.util.logging.handlers;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * PFS-3596: A handler that counts the number of log messages for each level.
 *
 * @author sprajc
 */
public class CountingHandler extends Handler {
    private AtomicInteger severe = new AtomicInteger(0);
    private AtomicInteger warning = new AtomicInteger(0);

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        if (Level.SEVERE.equals(record.getLevel())) {
            severe.incrementAndGet();
        } else if (Level.WARNING.equals(record.getLevel())) {
            warning.incrementAndGet();
        }
    }

    public int countSevere() {
        return severe.get();
    }

    public int countWarnings() {
        return warning.get();
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}

/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: Controller.java 7040 2009-02-27 01:16:52Z tot $
 */
package de.dal33t.powerfolder.util;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Callable that wraps an deligate and logs all exceptions and errors
 *
 * @author sprajc
 * @param <V>
 */
public class WrappedCallable<V> implements Callable<V> {
    private static final Logger log = Logger.getLogger(WrappedCallable.class
        .getName());

    private Callable<V> deligate;

    public WrappedCallable(Callable<V> deligate) {
        super();
        Reject.ifNull(deligate, "Deligate is null");
        this.deligate = deligate;
    }

    public V call() throws Exception {
        try {
            return deligate.call();
        } catch (Error t) {
            t.printStackTrace();
            log.log(Level.SEVERE, "Error in " + deligate + ": " + t.toString(),
                t);
            throw t;
        } catch (RuntimeException t) {
            t.printStackTrace();
            log.log(Level.SEVERE, "RuntimeException in " + deligate + ": "
                + t.toString(), t);
            throw t;
        }
    }
}

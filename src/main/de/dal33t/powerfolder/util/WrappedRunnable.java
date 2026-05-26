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

import de.dal33t.powerfolder.clientserver.RemoteCallException;
import de.dal33t.powerfolder.security.SecurityException;
import org.hibernate.HibernateException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Runnable that wraps an deligate and logs all exceptions and errors
 *
 * @author sprajc
 */
public class WrappedRunnable implements Runnable {
    private static final Logger LOG = Logger.getLogger(WrappedRunnable.class
        .getName());

    private Runnable deligate;

    public WrappedRunnable(Runnable deligate) {
        super();
        if (deligate instanceof WrappedRunnable) {
            LOG.log(Level.WARNING, "unnecessary wrapped runnable chain!",
                new RuntimeException("here"));
        }
        Reject.ifNull(deligate, "Deligate is null");
        this.deligate = deligate;
    }

    public void run() {
        try {
            deligate.run();
        } catch (OutOfMemoryError oom) {
            // PFS-1722
            oom.printStackTrace();
            LOG.log(Level.SEVERE,
                "Out of memory or process/resource limits reached in " + deligate + ": " + oom.toString(), oom);
            LOG.log(Level.SEVERE,
                "Shutting down java virtual machine. Exit code: 107");

            if (oom.getMessage() != null
                && oom.getMessage().toLowerCase().contains(
                    "unable to create new native thread"))
            {
                LOG.log(Level.WARNING, "Current threads: ");
                LOG.log(Level.WARNING, Debug.dumpCurrentStacktraces(false));
            }

            System.exit(107);
            throw oom;
        } catch (Error t) {
            t.printStackTrace();
            LOG.log(Level.SEVERE, "Error in " + deligate + ": " + t.toString(), t);
            throw t;
        } catch (HibernateException he) {
            LOG.log(Level.SEVERE,
                    "Database connection problem: " + he.getMessage(), he);
        } catch (Waiter.WaiterInterruptedException e) {
            // Shutdown
            LOG.log(Level.INFO, "Interrupted " + deligate);
        } catch (SecurityException | RemoteCallException e) {
            LOG.log(Level.WARNING, deligate + ": " + e, e);
        } catch (RuntimeException t) {
            LOG.log(Level.SEVERE,
                "RuntimeException in " + deligate + ": " + t.toString(), t);
            throw t;
        }
    }

}

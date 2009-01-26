/*
 * Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
 * $Id: MultiSourceDownloadManager.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.transfer.swarm;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Provides utilities for transfers. Currently only centralizes event
 * dispatching in downloads.
 * <p>
 * TODO Remove this. Synchronize with locks instead. This queue can explode
 * without notice.
 * <p>
 * TODO This never gets shut down.
 * 
 * @author Dennis "Bytekeeper" Waldherr
 */
public class TransferUtil {
    private static ExecutorService dispatcher = Executors
        .newSingleThreadExecutor();

    public static void invokeLater(Runnable runnable) {
        dispatcher.execute(runnable);
    }
}

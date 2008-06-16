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
* $Id$
*/
package de.dal33t.powerfolder.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class for simple profiling.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class Profile {
    private static final Logger LOG = Logger.getLogger(Profile.class);
    private static final Map<String, Long> DATA = new ConcurrentHashMap<String, Long>();

    private Profile() {
    }

    /**
     * Starts a profiling measurement.
     * 
     * @param id
     */
    public static void start(String id) {
        Reject.ifBlank(id, "id is blank");
        DATA.put(id, System.currentTimeMillis());
    }

    /**
     * Ends the profiling for this id and prints out the result.
     * 
     * @param id
     * @return the MS the profiling took.
     */
    public static long end(String id) {
        Reject.ifBlank(id, "id is blank");
        Long start = DATA.get(id);
        Reject.ifNull(start, "No profiling started for '" + id + "'");
        long took = System.currentTimeMillis() - start;
        LOG.info("'" + id + "' took " + took + "ms");
        return took;
    }
}

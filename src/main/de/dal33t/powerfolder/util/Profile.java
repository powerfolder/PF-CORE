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

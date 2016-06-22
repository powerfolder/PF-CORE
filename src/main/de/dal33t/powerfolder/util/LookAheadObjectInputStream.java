package de.dal33t.powerfolder.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.logging.Logger;

/**
 * PFC-2874: Look-ahead Java deserialization:
 * http://www.ibm.com/developerworks/library/se-lookahead/
 * 
 * @author sprajc
 */
public class LookAheadObjectInputStream extends ObjectInputStream {
    private static final Logger LOG = Logger
        .getLogger(LookAheadObjectInputStream.class.getName());

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
        throws IOException, ClassNotFoundException
    {
        if (AntiSerializationVulnerability.isBlacklisted(desc.getName())) {
            throw new InvalidClassException(
                "Unauthorized deserialization attempt", desc.getName());
        }
        if (!AntiSerializationVulnerability.isWhitelisted(desc.getName())) {
            LOG.warning("Possible unauthorized deserialization attempt: " + desc.getName());
        }
        return super.resolveClass(desc);
    }

    public LookAheadObjectInputStream(InputStream in) throws IOException {
        super(in);
    }
}

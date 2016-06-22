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
        for (String excluded : AntiSerializationVulnerability.BLACKLIST) {
            if (desc.getName().startsWith(excluded)) {
                throw new InvalidClassException(
                    "Unauthorized deserialization attempt", desc.getName());
            }
        }
        if (!isWhitelisted(desc.getName())) {
            LOG.warning("Class not whitelisted: " + desc.getName());
        }
        return super.resolveClass(desc);
    }

    private boolean isWhitelisted(String className) {
        for (String whitelisted : AntiSerializationVulnerability.WHITELIST) {
            if (className.startsWith(whitelisted)) {
                return true;
            }
        }     
        return false;
    }

    public LookAheadObjectInputStream(InputStream in) throws IOException {
        super(in);
    }

}

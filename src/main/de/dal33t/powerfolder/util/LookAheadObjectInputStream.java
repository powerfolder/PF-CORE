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
package de.dal33t.powerfolder.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.logging.Level;
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
            LOG.log(Level.WARNING,
                "Unauthorized deserialization attempt: " + desc.getName());
            throw new InvalidClassException(
                "Unauthorized deserialization attempt", desc.getName());
        }
        if (!AntiSerializationVulnerability.isWhitelisted(desc.getName())) {
            LOG.log(Level.WARNING,
                "Unauthorized deserialization attempt: " + desc.getName());
            LOG.log(Level.FINE,
                    "Unauthorized deserialization attempt: " + desc.getName(), new StackDump());
            throw new InvalidClassException(
                "Unauthorized deserialization attempt", desc.getName());
        }
        return super.resolveClass(desc);
    }

    public LookAheadObjectInputStream(InputStream in) throws IOException {
        super(in);
    }
}

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
 * $Id: FolderService.java 4655 2008-07-19 15:32:32Z bytekeeper $
 */
package de.dal33t.powerfolder.clientserver;

import java.security.PublicKey;

import de.dal33t.powerfolder.light.MemberInfo;

/**
 * Service to retrieve public keys from the server.
 *
 * @author Christian Sprajc
 * @version $Revision$
 */
public interface PublicKeyService {
    /**
     * @param node
     *            the node to retrive the key for.
     * @return the public key of the node in the keystore of the server. might
     *         be null if not in keystore.
     */
    PublicKey getPublicKey(MemberInfo node);
}

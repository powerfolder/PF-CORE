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
package de.dal33t.powerfolder.pro.encryption;

import java.net.URI;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.util.Debug;
import de.dal33t.powerfolder.util.Reject;
import de.schlichtherle.truezip.crypto.raes.RaesKeyException;
import de.schlichtherle.truezip.crypto.raes.RaesParameters;
import de.schlichtherle.truezip.crypto.raes.Type0RaesParameters;
import de.schlichtherle.truezip.fs.FsController;
import de.schlichtherle.truezip.fs.FsModel;
import de.schlichtherle.truezip.fs.archive.zip.raes.SafeZipRaesDriver;
import de.schlichtherle.truezip.key.sl.KeyManagerLocator;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;

public class CustomZipRaesDriver extends SafeZipRaesDriver {

    private CustomRaesParameters param;
    private Controller controller;

    public CustomZipRaesDriver(Controller controller) {
        super(IOPoolLocator.SINGLETON, KeyManagerLocator.SINGLETON);
        Reject.ifNull(controller, "Controller");
        this.controller = controller;
        param = new CustomRaesParameters("xxx".toCharArray());
    }

    @Override
    protected RaesParameters raesParameters(FsModel model) {
        // If you need the URI of the particular archive file, then call
        // model.getMountPoint().toUri().
        // If you need a more user friendly form of this URI, then call
        // model.getMountPoint().toHierarchicalUri().

        // Let's not use the key manager but instead our custom parameters.
        System.err.println(Thread.currentThread().toString());
        System.err.println(Debug.getCurrentStackTrace());
        System.err.println(getFolder(model.getMountPoint().toUri()));
        return param;
    }

    private Folder getFolder(URI baseURI) {
        System.err.println("baseURI: " + baseURI);
        for (Folder folder : controller.getFolderRepository().getFolders()) {
            URI folderURI = folder.getLocalBase().toURI();
            System.err.println("folderURI: " + folderURI);
            if (folderURI.equals(baseURI)) {
                return folder;
            }
        }
        return null;
    }

    @Override
    public FsController<?> newController(FsModel model, FsController<?> parent)
    {
        // This is a minor improvement: The default implementation decorates
        // the default file system controller chain with a package private
        // file system controller which keeps track of the encryption keys.
        // Because we are not using the key manager, we don't need this
        // special purpose file system controller and can use the default
        // file system controller chain instead.
        return superNewController(model, parent);
    }

    private static final class CustomRaesParameters implements
        Type0RaesParameters
    {
        final char[] password;

        CustomRaesParameters(final char[] password) {
            this.password = password.clone();
        }

        public char[] getWritePassword() throws RaesKeyException {
            System.err.println("getWritePassword");
            return password.clone();
        }

        public char[] getReadPassword(boolean invalid) throws RaesKeyException {
            System.err.println("getReadPassword");
            if (invalid)
                throw new RaesKeyException("Invalid password!");
            return password.clone();
        }

        public KeyStrength getKeyStrength() throws RaesKeyException {
            return KeyStrength.BITS_256;
        }

        public void setKeyStrength(KeyStrength keyStrength)
            throws RaesKeyException
        {
            // We have been using only 128 bits to create archive entries.
            assert KeyStrength.BITS_256 == keyStrength;
        }
    }
}

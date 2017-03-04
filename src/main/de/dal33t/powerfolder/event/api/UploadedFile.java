/*
 * Copyright 2004 - 2017 Christian Sprajc. All rights reserved.
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
package de.dal33t.powerfolder.event.api;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;

import static de.dal33t.powerfolder.ConfigurationEntry.EVENT_API_URL_UPLOADED_FILE_CLIENT;

/**
 * PFS-1766: File/directory was uploaded
 *
 * @author Christian Sprajc
 */
public class UploadedFile extends FileEvent {

    public UploadedFile(Controller controller) {
        super(controller, EVENT_API_URL_UPLOADED_FILE_CLIENT);
    }

    protected UploadedFile(Controller controller, ConfigurationEntry urlEntry) {
        super(controller, urlEntry);
    }

}

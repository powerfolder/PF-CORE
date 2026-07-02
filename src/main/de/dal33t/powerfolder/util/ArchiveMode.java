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

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.disk.FileArchiverImpl;
import de.dal33t.powerfolder.disk.Folder;

import java.nio.file.Path;
import java.util.logging.Logger;

public enum ArchiveMode {

    FULL_BACKUP("archive.full_backup") {
        @Override
        public FileArchiver getInstance(Folder f) {
            Path archive = f.getSystemSubDir().resolve(
                    ConfigurationEntry.ARCHIVE_DIRECTORY_NAME.getValue(f.getController()));
            return new FileArchiverImpl(archive, f.getController().getMySelf()
                    .getInfo());
        }
    };

    private static Logger log = Logger.getLogger(ArchiveMode.class.getName());
    private final String key;

    ArchiveMode(String key) {
        assert StringUtils.isNotEmpty(key);
        this.key = key;
    }

    /**
     * Simplifies usage in GUI
     */
    @Override
    public String toString() {
        return Translation.get(key);
    }

    public abstract FileArchiver getInstance(Folder f);
}

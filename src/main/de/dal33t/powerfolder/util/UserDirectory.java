/*
 * Copyright 2004 - 2011 Christian Sprajc. All rights reserved.
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
 * $Id: ChooseDiskLocationPanel.java 9522 2009-09-11 16:47:01Z harry $
 */
package de.dal33t.powerfolder.util;

import java.nio.file.Path;


public class UserDirectory {
    private String translatedName;
    private String placeholder;
    private Path directory;

    UserDirectory(String translatedName, String placeholder, Path directory) {
        this.translatedName = translatedName;
        this.placeholder = placeholder;
        this.directory = directory;
    }

    public String getTranslatedName() {
        return translatedName;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public Path getDirectory() {
        return directory;
    }

    public String toString() {
        return translatedName + '/' + placeholder + " @ " + directory;
    }
}

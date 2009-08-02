/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
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
 * $Id: BlackMoonSkin.java 8103 2009-05-27 23:56:11Z tot $
 */
package de.dal33t.powerfolder.skin;

public class PowerFolder2Skin extends AbstractSyntheticaSkin {

    @Override
    public String getName() {
        return "PowerFolder Skin 2";
    }

    @Override
    public String getID() {
        return "PowerFolderSkin2";
    }

    @Override
    public String getIconsPropertiesFileName() {
        return "de/dal33t/powerfolder/skin/powerfolder2/icons.properties";
    }

    @Override
    public String getSynthXMLFileName() {
        return "/de/dal33t/powerfolder/skin/powerfolder2/synth.xml";
    }

}

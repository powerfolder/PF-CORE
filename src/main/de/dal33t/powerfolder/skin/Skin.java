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
 * $Id: ServerClient.java 6435 2009-01-21 23:35:04Z harry $
 */
package de.dal33t.powerfolder.skin;

import java.text.ParseException;
import java.util.Properties;

import javax.swing.LookAndFeel;

/**
 * Interface which describes the information that can be retrieved from a skin,
 * which contains look-and-feel and image information.
 *
 * @author harry
 * @version $Revision$
 */
public interface Skin {
    /**
     * @return Descriptive name of the skin.
     */
    String getName();

    /**
     * @return LookAndFeel.
     * @throws ParseException
     *             If something went wrong on parsing the skin.
     */
    LookAndFeel getLookAndFeel() throws ParseException;

    /**
     * @return icon properties to use. Return <code>null</code> if default set
     *         should be used.
     */
    Properties getIconsProperties();

}

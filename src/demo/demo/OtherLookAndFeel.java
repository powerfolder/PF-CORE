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
 * $Id: CustomLookAndFeel.java 7690 2009-04-18 04:03:46Z tot $
 */

package demo;

import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;

import java.text.ParseException;

/**
 * Simple custom look and feel to demonstrate Skins.
 */
public class OtherLookAndFeel extends SyntheticaLookAndFeel {

    public OtherLookAndFeel() throws ParseException {
        super("/demo/synth2.xml");
    }

    public String getID() {
        return "OtherLookAndFeel";
    }

    public String getName() {
        return "Other Look And Feel";
    }
}
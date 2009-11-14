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

import java.text.ParseException;

import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;

/**
 * Base class for own skin with synthetica LAF
 * 
 * @author sprajc
 */
public abstract class AbstractSyntheticaSkin implements Skin {

    public abstract String getName();

    public abstract String getID();

    public abstract String getIconsPropertiesFileName();

    public abstract String getSynthXMLFileName();

    protected String getSynthXMLFileName0() {
        String fn = getSynthXMLFileName();
        if (!fn.startsWith("/")) {
            fn = '/' + fn;
        }
        return fn;
    }

    public final LookAndFeel getLookAndFeel() throws ParseException {
        return new LookAndFeel();
    }

    public class LookAndFeel extends SyntheticaLookAndFeel {
        private static final long serialVersionUID = 1L;

        public LookAndFeel() throws ParseException {
            super(AbstractSyntheticaSkin.this.getSynthXMLFileName0());
        }

        @Override
        public String getID() {
            return AbstractSyntheticaSkin.this.getID();
        }

        @Override
        public String getName() {
            return AbstractSyntheticaSkin.this.getName();
        }
    }
}

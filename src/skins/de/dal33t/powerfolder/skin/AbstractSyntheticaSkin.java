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
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.JComponent;

import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;
import de.javasoft.util.IVersion;

/**
 * Base class for own skin with synthetica LAF
 *
 * @author sprajc
 */
public abstract class AbstractSyntheticaSkin implements Skin {

    public abstract Properties getIconsProperties();

    public abstract String getName();

    public abstract String getID();

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

        public Icon getDisabledIcon(JComponent component, Icon icon) {
            // We don't do it.
            return null;

            // if (icon instanceof ImageIcon) {
            // ImageIcon i = (ImageIcon) icon;
            // GrayFilter filter = new GrayFilter(true, 100);
            // ImageProducer prod = new FilteredImageSource(i.getImage()
            // .getSource(), filter);
            // Image grayImage = Toolkit.getDefaultToolkit().createImage(prod);
            // return new ImageIconUIResource(grayImage);
            // }
            // return null;
        }

        @Override
        public IVersion getVersion() {
            final int major = 1;
            final int minor = 0;
            final int revision = 0;
            final int build = 1;

            return new IVersion() {
                public int getMajor() {
                    return major;
                }

                public int getMinor() {
                    return minor;
                }

                public int getRevision() {
                    return revision;
                }

                public int getBuild() {
                    return build;
                }

                public String toString() {
                    return major + "." + minor + "." + revision + " Build "
                        + build;
                }
            };
        }
    }
}

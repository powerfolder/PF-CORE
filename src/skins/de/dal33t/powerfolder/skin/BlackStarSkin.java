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
 * $Id: BlackStarSkin.java 8103 2009-05-27 23:56:11Z tot $
 */
package de.dal33t.powerfolder.skin;

import de.dal33t.powerfolder.util.Translation;
import de.javasoft.plaf.synthetica.SyntheticaBlackStarLookAndFeel;

public class BlackStarSkin implements Skin {

    public String getName() {
        return Translation.getTranslation("skin.black_star");
    }

    public Class<SyntheticaBlackStarLookAndFeel> getLookAndFeelClass() {
        return SyntheticaBlackStarLookAndFeel.class;
    }

    public String getIconsPropertiesFileName() {
        return "skin/BlackStarIcons.properties";
    }
}

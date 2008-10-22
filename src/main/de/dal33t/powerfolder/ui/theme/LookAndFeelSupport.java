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
* $Id$
*/
package de.dal33t.powerfolder.ui.theme;

import de.javasoft.plaf.synthetica.SyntheticaBlackMoonLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaBlackStarLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaBlueIceLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaBlueMoonLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaBlueSteelLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaGreenDreamLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaMauveMetallicLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaOrangeMetallicLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaSilverMoonLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaSkyMetallicLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaWhiteVisionLookAndFeel;

import javax.swing.LookAndFeel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class which offers several helper methods for handling with LookAndFeels.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class LookAndFeelSupport {

    private static final Logger log = Logger.getLogger(LookAndFeelSupport.class.getName());

    /**
     * All avaiable lookAndFeels
     */
    private static final Class[] AVAILABLE_LAF_CLASSES = new Class[]{
            SyntheticaStandardLookAndFeel.class,
            SyntheticaBlackMoonLookAndFeel.class,
            SyntheticaBlackStarLookAndFeel.class,
            SyntheticaBlueIceLookAndFeel.class,
            SyntheticaBlueMoonLookAndFeel.class,
            SyntheticaBlueSteelLookAndFeel.class,
            SyntheticaGreenDreamLookAndFeel.class,
            SyntheticaMauveMetallicLookAndFeel.class,
            SyntheticaOrangeMetallicLookAndFeel.class,
            SyntheticaSilverMoonLookAndFeel.class,
            SyntheticaSkyMetallicLookAndFeel.class,
            SyntheticaWhiteVisionLookAndFeel.class};

    private static LookAndFeel[] availableLafs;

    private LookAndFeelSupport() {
        // Only static methods available
    }

    /**
     * Returns all availble look and feels
     *
     * @return
     */
    public static synchronized LookAndFeel[] getAvailableLookAndFeels() {
        if (availableLafs == null) {
            availableLafs = new LookAndFeel[AVAILABLE_LAF_CLASSES.length];
            for (int i = 0; i < AVAILABLE_LAF_CLASSES.length; i++) {
                try {
                    availableLafs[i] = (LookAndFeel) AVAILABLE_LAF_CLASSES[i]
                            .newInstance();
                } catch (InstantiationException e) {
                    log.log(Level.SEVERE, "Unable to initalize look and feel", e);
                } catch (IllegalAccessException e) {
                    log.log(Level.SEVERE, "Unable to initalize look and feel", e);
                }
            }
        }
        return availableLafs;
    }

}
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
package de.dal33t.powerfolder.ui;

import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.skin.Skin;
import de.javasoft.plaf.synthetica.*;

import javax.swing.*;
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

    private LookAndFeelSupport() {
        // Only static methods available
    }

    /**
     * Returns all availble look and feel names, I18N
     *
     * @return
     */
    public static synchronized String[] getAvailableLookAndFeelNames(UIController uiController) {
        SyntheticaLookAndFeel customLaf = getCustomLaf(uiController);
        String[] availableLafNames;
        if (customLaf == null) {
            availableLafNames = new String[AVAILABLE_LAF_CLASSES.length];
        } else {
            availableLafNames = new String[AVAILABLE_LAF_CLASSES.length + 1];
        }

        for (int i = 0; i < AVAILABLE_LAF_CLASSES.length; i++) {
            try {
                Class lafClass = AVAILABLE_LAF_CLASSES[i];
                String name;
                if (lafClass.equals(SyntheticaStandardLookAndFeel.class)) {
                    name = Translation.getTranslation("look_and_feel.standard");
                } else if (lafClass.equals(SyntheticaBlackMoonLookAndFeel.class)) {
                    name = Translation.getTranslation("look_and_feel.black_moon");
                } else if (lafClass.equals(SyntheticaBlackStarLookAndFeel.class)) {
                    name = Translation.getTranslation("look_and_feel.black_star");
                } else if (lafClass.equals(SyntheticaBlueIceLookAndFeel.class)) {
                    name = Translation.getTranslation("look_and_feel.blue_ice");
                } else if (lafClass.equals(SyntheticaBlueMoonLookAndFeel.class)) {
                    name = Translation.getTranslation("look_and_feel.blue_moon");
                } else if (lafClass.equals(SyntheticaBlueSteelLookAndFeel.class)) {
                    name = Translation.getTranslation("look_and_feel.blue_steel");
                } else if (lafClass.equals(SyntheticaGreenDreamLookAndFeel.class)) {
                    name = Translation.getTranslation("look_and_feel.green_dream");
                } else if (lafClass.equals(SyntheticaMauveMetallicLookAndFeel.class)) {
                    name = Translation.getTranslation("look_and_feel.mauve_metallic");
                } else if (lafClass.equals(SyntheticaOrangeMetallicLookAndFeel.class)) {
                    name = Translation.getTranslation("look_and_feel.orange_metallic");
                } else if (lafClass.equals(SyntheticaSilverMoonLookAndFeel.class)) {
                    name = Translation.getTranslation("look_and_feel.silver_moon");
                } else if (lafClass.equals(SyntheticaSkyMetallicLookAndFeel.class)) {
                    name = Translation.getTranslation("look_and_feel.sky_metallic");
                } else if (lafClass.equals(SyntheticaWhiteVisionLookAndFeel.class)) {
                    name = Translation.getTranslation("look_and_feel.white_vision");
                } else {
                    LookAndFeel laf = (LookAndFeel) lafClass.newInstance();
                    name = laf.getName();
                }
                availableLafNames[i] = name;
            } catch (InstantiationException e) {
                log.log(Level.SEVERE, "Unable to initalize look and feel name", e);
            } catch (IllegalAccessException e) {
                log.log(Level.SEVERE, "Unable to initalize look and feel name", e);
            }
        }
        if (customLaf != null) {
            availableLafNames[availableLafNames.length - 1] = customLaf.getName();
        }
        return availableLafNames;
    }

    public static SyntheticaLookAndFeel getCustomLaf(UIController uiController) {
        Skin skin = uiController.getActiveSkin();
        if (skin != null) {
            Class clazz = skin.getLookAndFeelClass();
            try {
                Object instance = clazz.newInstance();
                if (instance instanceof SyntheticaLookAndFeel) {
                    return (SyntheticaLookAndFeel) instance;
                }
            } catch (InstantiationException e) {
                log.severe("Could not instantiate custom Synthetica class " + clazz.getName());
            } catch (IllegalAccessException e) {
                log.severe("Could not access custom Synthetica class " + clazz.getName());
            }
        }

        return null;
    }

    /**
     * Returns all available look and feels
     *
     * @return
     */
    public static synchronized LookAndFeel[] getAvailableLookAndFeels(UIController uiController) {
        SyntheticaLookAndFeel customLaf = getCustomLaf(uiController);
        LookAndFeel[] availableLafs;
        if (customLaf == null) {
            availableLafs = new LookAndFeel[AVAILABLE_LAF_CLASSES.length];
        } else {
            availableLafs = new LookAndFeel[AVAILABLE_LAF_CLASSES.length + 1];
        }
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
        if (customLaf != null) {
            availableLafs[availableLafs.length - 1] = customLaf;
        }
        return availableLafs;
    }

    /**
     * Sets the look and feel, and sets font so that Asian fonts display okay.
     * See http://www.javasoft.de/jsf/public/products/synthetica/faq#q12
     *
     * @param laf
     * @throws UnsupportedLookAndFeelException
     *
     */
    public static void setLookAndFeel(LookAndFeel laf)
            throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(laf);
        SyntheticaLookAndFeel.setFont("Dialog", 11);
    }
}
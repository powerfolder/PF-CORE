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

import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.DesertBlue;
import com.jgoodies.looks.plastic.theme.ExperienceBlue;
import com.jgoodies.looks.plastic.theme.Silver;
import com.jgoodies.looks.plastic.theme.SkyBlue;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class which offers several helper methods for handling with Themes
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class ThemeSupport {

    private static final Logger log = Logger.getLogger(ThemeSupport.class.getName());

    /** All avaiable themes */
    private static final Class[] AVAILABLE_THEME_CLASSES = new Class[]{
        FireBurst.class,
        // Inverted themes disabled: BrownSugar.class, DarkStar.class,
        DesertBlue.class, ExperienceBlue.class, Silver.class, SkyBlue.class};

    private static PlasticTheme[] availableThemes;

    private ThemeSupport() {
        // Only static methods available
    }

    /**
     * Returns all availble themes
     * 
     * @return
     */
    public static synchronized PlasticTheme[] getAvailableThemes() {
        if (availableThemes == null) {
            availableThemes = new PlasticTheme[AVAILABLE_THEME_CLASSES.length];
            for (int i = 0; i < AVAILABLE_THEME_CLASSES.length; i++) {
                try {
                    availableThemes[i] = (PlasticTheme) AVAILABLE_THEME_CLASSES[i]
                        .newInstance();
                } catch (InstantiationException e) {
                    log.log(Level.SEVERE, "Unable to initalize theme", e);
                } catch (IllegalAccessException e) {
                    log.log(Level.SEVERE, "Unable to initalize theme", e);
                }
            }
        }
        return availableThemes;
    }

    /**
     * Returns the currently active plastic theme. Or null if no plastic theme
     * is set
     * 
     * @return the theme
     */
    public static PlasticTheme getActivePlasticTheme() {
        if (UIManager.getLookAndFeel() instanceof PlasticXPLookAndFeel) {
            return PlasticXPLookAndFeel.getPlasticTheme();
        }
        return null;
    }

    /**
     * Sets a theme if current look and feel is PlasticXPLookAndFeel
     * 
     * @see PlasticXPLookAndFeel
     * @param theme
     *            the theme
     * @param comp1
     *            the first component to update (optional)
     * @param comp3
     *            the second component to update (optional)
     * @return true if succeeded
     */
    public static boolean setPlasticTheme(PlasticTheme theme, Component comp1,
        Component comp2)
    {
        log.info("Setting theme: " + theme.getName());
        if (UIManager.getLookAndFeel() instanceof PlasticXPLookAndFeel) {
            PlasticXPLookAndFeel.setPlasticTheme(theme);
            try {
                UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
            } catch (UnsupportedLookAndFeelException e) {
                log.log(Level.WARNING, "Unable to set look and feel", e);
            }
            if (comp1 != null) {
                SwingUtilities.updateComponentTreeUI(comp1);
            }
            if (comp2 != null) {
                SwingUtilities.updateComponentTreeUI(comp2);
            }
            return true;
        }

        return false;
    }

}
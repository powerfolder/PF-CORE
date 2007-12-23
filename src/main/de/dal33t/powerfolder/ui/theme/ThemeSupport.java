/* $Id: ThemeSupport.java,v 1.6 2005/06/15 13:02:31 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.theme;

import java.awt.Component;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.DesertBlue;
import com.jgoodies.looks.plastic.theme.ExperienceBlue;
import com.jgoodies.looks.plastic.theme.Silver;
import com.jgoodies.looks.plastic.theme.SkyBlue;

import de.dal33t.powerfolder.util.Logger;

/**
 * Class which offers several helper methods for handling with Themes
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.6 $
 */
public class ThemeSupport {
    // The logger
    private static final Logger LOG = Logger.getLogger(ThemeSupport.class);

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
    public synchronized static PlasticTheme[] getAvailableThemes() {
        if (availableThemes == null) {
            availableThemes = new PlasticTheme[AVAILABLE_THEME_CLASSES.length];
            for (int i = 0; i < AVAILABLE_THEME_CLASSES.length; i++) {
                try {
                    availableThemes[i] = (PlasticTheme) AVAILABLE_THEME_CLASSES[i]
                        .newInstance();
                } catch (InstantiationException e) {
                    LOG.error("Unable to initalize theme", e);
                } catch (IllegalAccessException e) {
                    LOG.error("Unable to initalize theme", e);
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
        LOG.info("Setting theme: " + theme.getName());
        if (UIManager.getLookAndFeel() instanceof PlasticXPLookAndFeel) {
            PlasticXPLookAndFeel.setPlasticTheme(theme);
            try {
                UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
            } catch (UnsupportedLookAndFeelException e) {
                LOG.warn("Unable to set look and feel", e);
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
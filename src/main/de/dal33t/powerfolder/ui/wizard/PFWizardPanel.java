/* $Id: PFWizardPanel.java,v 1.4 2005/11/20 00:22:10 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import java.awt.Color;
import java.awt.Font;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

import jwf.WizardPanel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.Logger;

/**
 * Base class for wizard panels
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public abstract class PFWizardPanel extends WizardPanel {
    private Controller controller;
    private Logger log;

    /**
     * Initalization
     * 
     * @param controller
     *            the controller
     */
    public PFWizardPanel(Controller controller) {
        super();
        if (controller == null) {
            throw new NullPointerException("Controller is null");
        }
        this.controller = controller;
        // Set white background for all folder panels
        setBackground(Color.WHITE);
    }

    // We do not need validiation *********************************************

    public boolean validateNext(List list) {
        return true;
    }

    public boolean validateFinish(List list) {
        return true;
    }

    /**
     * We have help. Open docs in browser
     * 
     * @return true
     */
    public boolean hasHelp() {
        return true;
    }

    /** Always open online docu */
    public void help() {
        Help.openHelp("node/documentation");
    }

    // Helper code ************************************************************

    /**
     * Sets the correct colors to the component
     * 
     * @param comp
     */
    protected void setColors(JComponent comp) {
        comp.setBackground(Color.WHITE);
    }

    /**
     * Creates a label which can be used as title. Has smoothed font
     * 
     * @param text
     * @return
     */
    protected JComponent createTitleLabel(String text) {
        JLabel label = new JLabel(text);
        Font font = new Font(label.getFont().getFontName(), 0,
            PFWizard.HEADER_FONT_SIZE);
        label.setFont(font);
        return label;
    }

    // General ****************************************************************

    /**
     * Returns the controller
     * 
     * @return
     */
    protected Controller getController() {
        return controller;
    }

    /**
     * Returns a logger for this panel
     * 
     * @return
     */
    protected Logger log() {
        if (log == null) {
            log = Logger.getLogger(this);
        }
        return log;
    }
}
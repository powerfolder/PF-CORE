/* $Id: PFWizardPanel.java,v 1.4 2005/11/20 00:22:10 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.widget.AntialiasedLabel;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import jwf.WizardPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Base class for wizard panels
 * All subclasses have a title, optional picto and a content area.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public abstract class PFWizardPanel extends WizardPanel {
    private Controller controller;
    private Logger log;
    private boolean initalized;
    private Icon picto;

    /**
     * Initalization
     * 
     * @param controller
     *            the controller
     */
    protected PFWizardPanel(Controller controller) {
        if (controller == null) {
            throw new NullPointerException("Controller is null");
        }
        this.controller = controller;
        // Set white background for all folder panels
        setBackground(Color.WHITE);
    }

    public final synchronized void display() {
        if (!initalized) {
            buildUI();
        }
        afterDisplay();
    }

    public boolean canFinish() {
        return false;
    }

    public boolean validateNext(List list) {
        return true;
    }

    public boolean validateFinish(List list) {
        return true;
    }

    public void finish() {
    }

    protected abstract JPanel buildContent();

    /**
     * Any work done after the wizard is displayed
     */
    protected void afterDisplay() {
    }

    protected void setPicto(Icon picto) {
        this.picto = picto;
    }

    protected abstract String getTitle();

    /**
     * Set up title, picto (optional) and the content panel.
     */
    protected abstract void initComponents();

    /**
     * Builds the ui
     */
    private void buildUI() {

        // init
        initComponents();

        JPanel content = buildContent();
        content.setOpaque(true);
        content.setBackground(Color.white);

        String title = getTitle();

        Reject.ifBlank(title, "Title is blank");
        Reject.ifNull(content, "Content is null");

        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout("20dlu, pref, 15dlu, left:pref",
            "5dlu, pref, 15dlu, pref, pref:grow");
        PanelBuilder pageBuilder = new PanelBuilder(layout, this);
        CellConstraints cc = new CellConstraints();

        pageBuilder.add(createTitleLabel(title), cc.xy(4, 2));

        // Add current wizard pico
        if (picto != null) {
            pageBuilder.add(new JLabel(picto), cc.xy(2, 4, CellConstraints.DEFAULT,
                CellConstraints.TOP));
        }

        pageBuilder.add(content, cc.xy(4, 4, CellConstraints.DEFAULT,
                CellConstraints.TOP));

        // initalized
        initalized = true;
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
        Help.openHelp("documentation.html");
    }

    // Helper code ************************************************************

    /**
     * @param text
     * @return  a label which can be used as title. Has smoothed font
     */
    private static JComponent createTitleLabel(String text) {
        AntialiasedLabel label = new AntialiasedLabel(text);
        SimpleComponentFactory.setFontSize(label, PFWizard.HEADER_FONT_SIZE);
        return label;
    }

    // General ****************************************************************

    /**
     * @return the controller
     */
    protected Controller getController() {
        return controller;
    }

    /**
     * @return a logger for this panel
     */
    protected Logger log() {
        if (log == null) {
            log = Logger.getLogger(this);
        }
        return log;
    }
}
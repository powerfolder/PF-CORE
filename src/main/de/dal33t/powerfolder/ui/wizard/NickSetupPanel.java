/* $Id: NickSetupPanel.java,v 1.1 2005/06/30 22:25:05 totmacherr Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import javax.swing.Icon;
import javax.swing.JLabel;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.LinkLabel;

/**
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.1 $
 */
public class NickSetupPanel extends PFWizardPanel {
    private boolean initalized = false;

    /**
     * @param controller
     */
    public NickSetupPanel(Controller controller) {
        super(controller);
    }

    // From WizardPanel *******************************************************

    public void display() {
        if (!initalized) {
            buildUI();
        }
    }

    public boolean hasNext() {
        return true;
    }

    public WizardPanel next() {
        // Next on always what to do panel
        return new WhatToDoPanel(getController());
    }

    public boolean canFinish() {
        return false;
    }

    public void finish() {
    }

    // UI building ************************************************************

    /**
     * Builds the ui
     */
    private void buildUI() {
        // init
        initComponents();

        //setBorder(new TitledBorder("Setup (dynamic) dns"));
        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout(
            "20dlu, pref, 15dlu, left:pref, 60dlu, pref:grow",
            "5dlu, pref, 15dlu, pref, pref, pref, pref, 4dlu, pref, 4dlu, pref, pref:grow");

        PanelBuilder builder = new PanelBuilder(this, layout);
        CellConstraints cc = new CellConstraints();

        builder.add(createTitleLabel(Translation
            .getTranslation("wizard.setupdns.title")), cc.xy(4, 2));

        // Add current wizard pico
        builder.add(new JLabel((Icon) getWizardContext().getAttribute(
            PFWizard.PICTO_ICON)), cc.xywh(2, 4, 1, 3, CellConstraints.DEFAULT,
            CellConstraints.TOP));

        builder.addLabel(Translation
            .getTranslation("wizard.setupdns.nodnssetup"), cc.xy(4, 4));

        builder.addLabel(Translation
            .getTranslation("wizard.setupdns.improvesread"), cc.xy(4, 5));

        LinkLabel dyndnsLink = new LinkLabel(Translation
            .getTranslation("wizard.setupdns.dyndnshomepage"),
            "http://www.dyndns.org");
        builder.add(dyndnsLink, cc.xy(4, 6));

        builder
            .addLabel(Translation.getTranslation("wizard.setupdns.enterdns"),
                cc.xy(4, 7));

//        builder.add(dnsField, cc.xy(4, 9));
//        builder.add(dnsValidateBtn, cc.xy(4, 11));

        // initalized
        initalized = true;
    }

    /**
     * Initalizes all nessesary components
     */
    private void initComponents() {

    }

}
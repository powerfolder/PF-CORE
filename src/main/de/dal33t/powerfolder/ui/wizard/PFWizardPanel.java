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
package de.dal33t.powerfolder.ui.wizard;

import javax.swing.JComponent;
import javax.swing.JLabel;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.LayoutMap;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.util.Help;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.ui.widget.AntialiasedLabel;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;

/**
 * Base class for wizard panels All subclasses have a title, optional picto and
 * a content area.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.4 $
 */
public abstract class PFWizardPanel extends WizardPanel {
    private Controller controller;
    private boolean initalized;
    private JLabel titleLabel;

    /**
     * Initialization
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
        // setBackground(Color.WHITE);
        setOpaque(false);

        // HACK: Find a global place
        LayoutMap.getRoot().columnPut("wlabel", "70dlu");
        LayoutMap.getRoot().columnPut("wfield", "100dlu");
    }

    /**
     * Shows the wizard panel.
     */
    public final synchronized void display() {
        if (!initalized) {
            buildUI();
        }
        afterDisplay();
    }

    /**
     * Override if this panel can finish
     * 
     * @return
     */
    public boolean canFinish() {
        return false;
    }

    /**
     * Override if this panel can cancel
     * 
     * @return
     */
    public boolean canCancel() {
        return true;
    }

    /**
     * Override if validation is required.
     * 
     * @param list
     * @return
     */
    public boolean validateNext() {
        return true;
    }

    /**
     * Override if finish validation is required.
     * 
     * @param list
     * @return
     */
    public boolean validateFinish() {
        return true;
    }

    /**
     * Override if finish processing is required.
     */
    public void finish() {
    }

    /**
     * This builds the actual content panel that is displayed below the title.
     * 
     * @return
     */
    protected abstract JComponent buildContent();

    /**
     * Any work done after the wizard is displayed
     */
    protected void afterDisplay() {
    }

    /**
     * Returns the title for the panel
     * 
     * @return
     */
    protected abstract String getTitle();

    /**
     * Set up title, picto (optional) and the content panel.
     */
    protected abstract void initComponents();

    /**
     * We have help. Open docs in browser
     * 
     * @return true
     */
    public boolean hasHelp() {
        return !getWizard().isTiny()
            && StringUtils
                .isNotBlank(ConfigurationEntry.PROVIDER_QUICKSTART_URL
                    .getValue(getController()));
    }

    /** Always open online docu */
    public void help() {
        Help.openQuickstartGuides(controller);
    }

    // Helper code ************************************************************

    /**
     * @param text
     * @return a label which can be used as title. Has smoothed font
     */
    private static JLabel createTitleLabel(String text) {
        AntialiasedLabel label = new AntialiasedLabel(text);
        SimpleComponentFactory.setFontSize(label, PFWizard.HEADER_FONT_SIZE);
        return label;
    }

    /**
     * Method for updating the buttons
     */
    protected void updateTitle() {
        if (titleLabel == null) {
            return;
        }
        titleLabel.setText(getTitle());
    }

    // General ****************************************************************

    /**
     * @return the controller
     */
    protected Controller getController() {
        return controller;
    }

    // Internal ***************************************************************

    /**
     * Builds the ui
     */
    private void buildUI() {

        // init
        initComponents();

        JComponent content = buildContent();
        content.setOpaque(false);
        // content.setBackground(Color.WHITE);

        String title = getTitle();

        Reject.ifBlank(title, "Title is blank");
        Reject.ifNull(content, "Content is null");

        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout("13px, fill:pref:grow",
            "pref, 7dlu, pref, 3dlu, fill:pref:grow");
        PanelBuilder pageBuilder = new PanelBuilder(layout, this);
        pageBuilder.setBorder(Borders
            .createEmptyBorder("7dlu, 20dlu, 0dlu, 20dlu"));
        CellConstraints cc = new CellConstraints();
        int row = 1;

        pageBuilder.add(new JLabel(Icons.getIconById(Icons.LOGO400UI)), cc
            .xywh(1, row, 2, 1, "left, default"));
        row += 2;
        
        titleLabel = createTitleLabel(title);
        pageBuilder.add(titleLabel, cc.xy(2, row));
        row += 2;

        pageBuilder.add(content, cc.xy(2, row, CellConstraints.DEFAULT,
            CellConstraints.TOP));

        // initalized
        initalized = true;
    }
}
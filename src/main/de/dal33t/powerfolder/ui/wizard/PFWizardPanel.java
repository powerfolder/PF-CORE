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

import java.awt.Color;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.LayoutMap;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.widget.AntialiasedLabel;
import de.dal33t.powerfolder.util.Help;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

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
     * Override if validation is required.
     * 
     * @param list
     * @return
     */
    public boolean validateNext(List list) {
        return true;
    }

    /**
     * Override if finish validation is required.
     * 
     * @param list
     * @return
     */
    public boolean validateFinish(List list) {
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
     * Returns the picto for the panel. Can be null.
     * 
     * @return
     * @deprecated use #getPictoComponent() instead
     */
    protected Icon getPicto() {
        return null;
    }

    /**
     * @return the component representing the picto
     */
    protected JComponent getPictoComponent() {
        if (getPicto() == null) {
            return null;
        }
        return new JLabel(getPicto());
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
        return true;
    }

    /** Always open online docu */
    public void help() {
        Help.openQuickstartGuides();
    }

    // Helper code ************************************************************

    /**
     * @return the default picto which is set on the WizardCotext.
     */
    protected JComponent getContextPictoComponent() {
        if (getContextPicto() == null) {
            return null;
        }
        return new JLabel(getContextPicto());
    }

    /**
     * @return the component with the default picto which is set on the
     *         WizardCotext.
     */
    protected Icon getContextPicto() {
        return (Icon) getWizardContext().getAttribute(PFWizard.PICTO_ICON);
    }

    /**
     * @param text
     * @return a label which can be used as title. Has smoothed font
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

    // Internal ***************************************************************

    /**
     * Builds the ui
     */
    private void buildUI() {

        // init
        initComponents();

        JComponent content = buildContent();
        content.setOpaque(true);
        content.setBackground(Color.WHITE);

        String title = getTitle();

        Reject.ifBlank(title, "Title is blank");
        Reject.ifNull(content, "Content is null");

        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout("pref, 15dlu, fill:pref:grow",
            "pref, 15dlu, pref, fill:pref:grow");
        PanelBuilder pageBuilder = new PanelBuilder(layout, this);
        pageBuilder.setBorder(Borders
            .createEmptyBorder("5dlu, 20dlu, 0dlu, 20dlu"));
        CellConstraints cc = new CellConstraints();

        pageBuilder.add(createTitleLabel(title), cc.xy(3, 1));

        // Add current wizard pico
        JComponent picto = getPictoComponent();
        if (picto != null) {
            pageBuilder.add(picto, cc.xy(1, 3, CellConstraints.DEFAULT,
                CellConstraints.TOP));
        }

        pageBuilder.add(content, cc.xy(3, 3, CellConstraints.DEFAULT,
            CellConstraints.TOP));

        // initalized
        initalized = true;
    }

}
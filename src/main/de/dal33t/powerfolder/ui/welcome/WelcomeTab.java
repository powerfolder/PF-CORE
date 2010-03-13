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
 * $Id: WelcomeTab.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.welcome;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.widget.GradientPanel;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Class for the Home tab in the main tab area of the UI.
 */
public class WelcomeTab extends PFUIComponent {

    private JPanel uiComponent;

    /**
     * Constructor
     *
     * @param controller
     */
    public WelcomeTab(Controller controller) {
        super(controller);
    }

    /**
     * @return the UI component after optionally building it.
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        return uiComponent;
    }

    /**
     * One-off build of UI component.
     */
    private void buildUI() {
        initComponents();

        FormLayout layout = new FormLayout("3dlu, pref:grow, 3dlu",
            "3dlu, pref, 3dlu, pref, 3dlu, fill:0:grow");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        // Toolbar
        JPanel toolbar = createToolBar();
        toolbar.setOpaque(false);
        builder.add(toolbar, cc.xy(2, 2));
        builder.addSeparator(null, cc.xyw(1, 4, 2));

        // Main panel in scroll pane
        JPanel mainPanel = buildMainPanel();
        mainPanel.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        UIUtil.removeBorder(scrollPane);
        builder.add(scrollPane, cc.xyw(1, 6, 2));

        uiComponent = GradientPanel.create(builder.getPanel());
    }

    /**
     * Initialise class components.
     */
    private void initComponents() {
    }

    /**
     * Build the main panel with all the detail lines.
     *
     * @return
     */
    private JPanel buildMainPanel() {
        FormLayout layout = new FormLayout("pref:grow", "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        // Bottom border
        builder.setBorder(Borders.createEmptyBorder("1dlu, 3dlu, 2dlu, 3dlu"));
        CellConstraints cc = new CellConstraints();
        int row = 1;
        return builder.getPanel();
    }

    /**
     * Cretes the toolbar.
     *
     * @return the toolbar
     */
    private JPanel createToolBar() {

        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();

        JButton newFolderButton = new JButton(getApplicationModel()
            .getActionModel().getNewFolderAction());
        bar.addGridded(newFolderButton);
        if (!getController().isBackupOnly()) {
            JButton searchComputerButton = new JButton(getApplicationModel()
                .getActionModel().getFindComputersAction());
            bar.addRelatedGap();
            bar.addGridded(searchComputerButton);
        }

        return bar.getPanel();
    }

}
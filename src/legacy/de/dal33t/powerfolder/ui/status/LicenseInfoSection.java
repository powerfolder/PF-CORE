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
 * $Id: TrialInfoSection.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.status;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.util.Translation;

/**
 * Class to render the online storage trial info on the status tab.
 */
public class LicenseInfoSection extends PFUIComponent {

    private JPanel uiComponent;

    private JProgressBar progressBar;
    private ActionLabel infoLabel;

    /**
     * Constructor.
     * 
     * @param controller
     */
    public LicenseInfoSection(Controller controller) {
        super(controller);
    }

    /**
     * @return the uiComponent, creating first if necessary.
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            initComponents();
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * Build the uiComponent. Adds separator, progress bar and label.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("100dlu, pref:grow", "pref, pref");
        // space prog label
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(progressBar, cc.xy(1, 1));
        builder.add(infoLabel.getUIComponent(), cc.xywh(1, 2, 2, 1));

        uiComponent = builder.getPanel();
    }

    /**
     * Initialize the required components.
     */
    private void initComponents() {
        MyClickListener clickListener = new MyClickListener();
        progressBar = new JProgressBar(0, 0, 100);
        // progressBar.setBorder(Borders.createEmptyBorder("0, 0, 3dlu, 0"));
        progressBar.addMouseListener(clickListener);
        infoLabel = new ActionLabel(getController(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                getApplicationModel().getLicenseModel().getActivationAction()
                    .actionPerformed(e);
            }
        });
        infoLabel.getUIComponent().setBorder(
            Borders.createEmptyBorder("3dlu, 0, 0, 0"));

        setDaysValid(-1);
    }

    void setDaysValid(int days) {
        boolean aboutToExpire = days != -1 && days < 30
            && !getController().getOSClient().getAccount().willAutoRenew();
        boolean disabled = !getController().getNodeManager().isStarted();

        if (disabled) {
            infoLabel.setIcon(Icons.getIconById(Icons.WARNING));
            infoLabel.setText(Translation
                .getTranslation("pro.status_tab.disabled"));
            infoLabel.setToolTipText(Translation
                .getTranslation("pro.status_tab.disabled.tips"));
            infoLabel.getUIComponent().setVisible(true);
            progressBar.setVisible(false);
        } else if (aboutToExpire) {
            if (days < 5) {
                infoLabel.setIcon(Icons.getIconById(Icons.WARNING));
            } else {
                infoLabel.setIcon(null);
            }
            infoLabel.setText(Translation.getTranslation(
                "pro.status_tab.remaining", String.valueOf(days)));
            infoLabel.setToolTipText(Translation.getTranslation(
                "pro.status_tab.remaining.tips", String.valueOf(days)));
            infoLabel.getUIComponent().setVisible(true);
            progressBar.setValue(100 * days / 30);
            progressBar.setToolTipText(Translation.getTranslation(
                "pro.status_tab.remaining.tips", String.valueOf(days)));
            progressBar.setVisible(true);
        } else {
            infoLabel.setIcon(null);
            infoLabel.getUIComponent().setVisible(false);
            progressBar.setVisible(false);
        }

    }

    private class MyClickListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (getApplicationModel().getLicenseModel().getActivationAction() != null)
            {
                getApplicationModel()
                    .getLicenseModel()
                    .getActivationAction()
                    .actionPerformed(
                        new ActionEvent(e.getSource(), 0, "clicked"));
            }
        }

    }

}

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
package de.dal33t.powerfolder.ui.home;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.event.NodeManagerAdapter;
import de.dal33t.powerfolder.event.NodeManagerEvent;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.ProUtil;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Class to render the online storage trial info on the home tab.
 */
public class LicenseInfoSection extends PFUIComponent {

    private JPanel uiComponent;

    private JProgressBar progressBar;
    private ActionLabel infoLabel;
    private LinkLabel buyNowLabel;

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
        FormLayout layout = new FormLayout("100dlu, pref:grow",
            "pref, pref, 20dlu, pref");
        // space prog label
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(progressBar, cc.xy(1, 1));
        builder.add(infoLabel.getUIComponent(), cc.xywh(1, 2, 2, 1));
        builder.add(buyNowLabel.getUIComponent(), cc.xyw(1, 4, 2));

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

        buyNowLabel = new LinkLabel(getController(), Translation
            .getTranslation("pro.home_tab.upgrade_to_full"),
            ConfigurationEntry.PROVIDER_BUY_URL.getValue(getController()));
        UIUtil.convertToBigLabel((JLabel) buyNowLabel.getUIComponent());

        getApplicationModel().getLicenseModel().getDaysValidModel()
            .addValueChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    Integer daysValid = (Integer) getApplicationModel()
                        .getLicenseModel().getDaysValidModel().getValue();
                    if (daysValid != null) {
                        setDaysValid(daysValid);
                    }
                }
            });
        setDaysValid(-1);

        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());
    }

    private void setDaysValid(int days) {
        boolean trial = ProUtil.isTrial(getController());
        boolean aboutToExpire = days != -1 && days < 30;
        boolean disabled = !getController().getNodeManager().isStarted();

        if (trial) {
            showBuyNowLink(Translation
                .getTranslation("pro.home_tab.upgrade_to_full"));
        } else if (aboutToExpire) {
            showBuyNowLink(Translation
                .getTranslation("pro.home_tab.renew_license"));
        } else {
            showBuyNowLink(null);
        }

        if (disabled) {

            infoLabel.setIcon(Icons.getIconById(Icons.WARNING));

            infoLabel.setText(Translation
                .getTranslation("pro.home_tab.disabled"));
            infoLabel.setToolTipText(Translation
                .getTranslation("pro.home_tab.disabled.tips"));
            infoLabel.getUIComponent().setVisible(true);
            progressBar.setVisible(false);
        } else if (aboutToExpire) {
            if (days < 5) {
                infoLabel.setIcon(Icons.getIconById(Icons.WARNING));
            } else {
                infoLabel.setIcon(null);
            }
            infoLabel.setText(Translation.getTranslation(
                "pro.home_tab.remaining", String.valueOf(days)));
            infoLabel.setToolTipText(Translation.getTranslation(
                "pro.home_tab.remaining.tips", String.valueOf(days)));
            infoLabel.getUIComponent().setVisible(true);
            progressBar.setValue(100 * days / 30);
            progressBar.setToolTipText(Translation.getTranslation(
                "pro.home_tab.remaining.tips", String.valueOf(days)));
            progressBar.setVisible(true);
        } else {
            infoLabel.setIcon(null);
            infoLabel.getUIComponent().setVisible(false);
            progressBar.setVisible(false);
        }

    }

    private void showBuyNowLink(String text) {
        if (StringUtils.isBlank(text)) {
            buyNowLabel.getUIComponent().setVisible(false);
            return;
        }
        buyNowLabel.setTextAndURL(text, ConfigurationEntry.PROVIDER_BUY_URL
            .getValue(getController()));
        buyNowLabel.getUIComponent().setVisible(true);
    }

    private class MyClickListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (getApplicationModel().getLicenseModel().getActivationAction() != null)
            {
                getApplicationModel().getLicenseModel().getActivationAction()
                    .actionPerformed(
                        new ActionEvent(e.getSource(), 0, "clicked"));
            }
        }

    }

    private class MyNodeManagerListener extends NodeManagerAdapter {

        public boolean fireInEventDispatchThread() {
            return true;
        }

        @Override
        public void startStop(NodeManagerEvent e) {
            Integer daysValid = (Integer) getApplicationModel()
                .getLicenseModel().getDaysValidModel().getValue();
            if (daysValid != null) {
                setDaysValid(daysValid);
            }
        }

    }

}

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
 * $Id: OnlineStorageSection.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.home;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;

/**
 * Class to render the online storage info on the home tab.
 */
public class OnlineStorageSection extends PFUIComponent {

    private JPanel uiComponent;

    private JProgressBar usagePB;
    private JLabel usageLabel;
    private TrialInfoSection trialInfoSection;

    /**
     * Constructor
     *
     * @param controller
     */
    public OnlineStorageSection(Controller controller) {
        super(controller);
    }

    /**
     * Sets the trial info details, hiding if necessary.
     *
     * @param totalStorage
     * @param spaceUsed
     * @param trial
     * @param daysLeft
     */
    public void setInfo(long totalStorage, long spaceUsed, boolean trial, int daysLeft) {

        double percentageUsed = 0;
        if (totalStorage > 0) {
            percentageUsed = 100.0d * (double) spaceUsed /
                    (double) totalStorage;
        }
        if (percentageUsed < 0.0d) {
            percentageUsed = 0.0d;
        }
        if (percentageUsed > 100.0d) {
            percentageUsed = 100.0d;
        }

        if (trial) {
            trialInfoSection.getUIComponent().setVisible(true);
            trialInfoSection.setTrialPeriod(daysLeft);
        } else {
            trialInfoSection.getUIComponent().setVisible(false);
        }

        usagePB.setValue((int) percentageUsed);
        usagePB.setToolTipText(Format.formatBytesShort(spaceUsed) + " / " +
        Format.formatBytesShort(totalStorage));

        usageLabel.setText(Translation.getTranslation(
                "home_tab.online_storage.usage",
                Format.formatNumber(percentageUsed)));
        usageLabel.setToolTipText(Format.formatBytesShort(spaceUsed) + " / " +
        Format.formatBytesShort(totalStorage));
    }

    /**
     * Gets the ui component, building first if necessary.
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            initComponents();
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * Builds the uiComponent.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("100dlu, pref:grow",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu");
              // prog        usage       trial
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(usagePB, cc.xy(1, 1));
        builder.add(usageLabel, cc.xywh(1, 3, 2, 1));
        builder.add(trialInfoSection.getUIComponent(), cc.xywh(1, 5, 2, 1));
        uiComponent = builder.getPanel();
    }

    /**
     * Initializes the components.
     */
    private void initComponents() {
        usagePB = new JProgressBar(0, 0, 100);
        usageLabel = new JLabel("");
        trialInfoSection = new TrialInfoSection(getController());
    }

}

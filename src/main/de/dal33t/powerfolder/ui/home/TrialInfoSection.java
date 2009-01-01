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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;

/**
 * Class to render the online storage trial info on the home tab.
 */
public class TrialInfoSection extends PFUIComponent {

    private JPanel uiComponent;

    private JProgressBar trialPB;
    private JLabel trialLabel;

    /**
     * Constructor.
     *
     * @param controller
     */
    public TrialInfoSection(Controller controller) {
        super(controller);
    }

    /**
     * Set details from the HomeTab. Note that the home tab is responsable for
     * hiding the uiComponent if required.
     *
     * @param days
     */
    public void setTrialPeriod(int days) {
        trialLabel.setText(Translation.getTranslation(
                "home_tab.online_storage.remaining", days));
        trialLabel.setToolTipText(Translation.getTranslation(
                "home_tab.online_storage.remaining.tips", days));
        trialPB.setValue(100 * days / 30);
        trialPB.setToolTipText(Translation.getTranslation(
                "home_tab.online_storage.remaining.tips", days));
    }

    /**
     * Gets the uiComponent, creating first if necessary.
     *
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
     * Build the uiComponent. Adds separator, progress bar and label.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("100dlu, pref:grow",
            "pref, 3dlu, pref, 3dlu, pref, 3dlu");
          // space       prog        label
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addSeparator(null, cc.xywh(1, 1, 2, 1));
        builder.add(trialPB, cc.xy(1, 3));
        builder.add(trialLabel, cc.xywh(1, 5, 2, 1));
        uiComponent = builder.getPanel();
    }

    /**
     * Initialize the required components.
     */
    private void initComponents() {
        trialPB = new JProgressBar(0, 0, 100);
        trialLabel = new JLabel("");
    }

}

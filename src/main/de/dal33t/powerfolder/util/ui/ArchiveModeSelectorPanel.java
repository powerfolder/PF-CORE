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
package de.dal33t.powerfolder.util.ui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.PFUIPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ArchiveMode;

import javax.swing.*;
import java.awt.Component;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.TreeSet;

/**
 * Panel for displaying and selecting archive mode. Attached are a pair of
 * ValueModels that get notified of selection changes (One for mode, one for
 * version history).
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 2.01 $
 */
public class ArchiveModeSelectorPanel extends PFUIPanel {

    /**
     * Map of available modes.
     */
    private static final Set<NameValuePair> PAIRS
            = new TreeSet<NameValuePair>();
    static {
        PAIRS.add(new NameValuePair(0, Translation.getTranslation(
                "archive_mode_selector_panel.none"), 0));
        PAIRS.add(new NameValuePair(1, Translation.getTranslation(
                "archive_mode_selector_panel.version", "1"), 1));
        PAIRS.add(new NameValuePair(2, Translation.getTranslation(
                "archive_mode_selector_panel.versions", "2"), 2));
        PAIRS.add(new NameValuePair(3, Translation.getTranslation(
                "archive_mode_selector_panel.versions", "3"), 3));
        PAIRS.add(new NameValuePair(4, Translation.getTranslation(
                "archive_mode_selector_panel.versions", "4"), 4));
        PAIRS.add(new NameValuePair(5, Translation.getTranslation(
                "archive_mode_selector_panel.versions", "5"), 5));
        PAIRS.add(new NameValuePair(6, Translation.getTranslation(
                "archive_mode_selector_panel.versions", "7"), 7));
        PAIRS.add(new NameValuePair(7, Translation.getTranslation(
                "archive_mode_selector_panel.versions", "10"), 10));
        PAIRS.add(new NameValuePair(8, Translation.getTranslation(
                "archive_mode_selector_panel.versions", "20"), 20));
        PAIRS.add(new NameValuePair(9, Translation.getTranslation(
                "archive_mode_selector_panel.versions", "50"), 50));
        PAIRS.add(new NameValuePair(10, Translation.getTranslation(
                "archive_mode_selector_panel.versions", "100"), 100));
        PAIRS.add(new NameValuePair(11, Translation.getTranslation(
                "archive_mode_selector_panel.unlimited"), -1));
    }

    private JComboBox archiveCombo;
    private JPanel panel;
    private ValueModel modeModel; // <ArchiveMode>
    private ValueModel versionModel; // <Integer>
    private ActionListener purgeListener;
    /**
     * Constructor
     *
     * @param controller
     *             the necessary evil...
     * @param modeModel
     *             ValueModel<ArchiveMode> that gets notified of mode changes.
     * @param versionModel
     *             ValueModel<Integer> that gets notified of version history
     *             changes.
     */
    public ArchiveModeSelectorPanel(Controller controller, ValueModel modeModel,
        ValueModel versionModel, ActionListener purgeListener) {
        super(controller);
        this.modeModel = modeModel;
        this.versionModel = versionModel;
        this.purgeListener = purgeListener;
        initComponents();
    }

    public ArchiveModeSelectorPanel(Controller controller, ValueModel modeModel,
        ValueModel versionModel) {
        super(controller);
        this.modeModel = modeModel;
        this.versionModel = versionModel;
        initComponents();
    }

    /**
     * Set the archive mode and verions history for the panel. Value models
     * are not notified of changes during the set operation.
     *
     * @param archiveMode
     * @param versionHistory
     */
    public void setArchiveMode(ArchiveMode archiveMode, int versionHistory) {
        if (archiveMode == ArchiveMode.NO_BACKUP) {
            archiveCombo.setSelectedIndex(0); // No Backup
        } else if (versionHistory == -1) {
            archiveCombo.setSelectedIndex(PAIRS.size() - 1); // Unlimited
        } else {
            int index = 0;
            for (NameValuePair nvp : PAIRS) {
                if (index != 0 && versionHistory <= nvp.getValue()) {
                    archiveCombo.setSelectedIndex(index);
                    return;
                }
                index++;
            }

            // versionHistory > max ==> Unlimited
            archiveCombo.setSelectedIndex(PAIRS.size() - 1);
        }

        // Make sure that the models have the correct values, in case nothing
        // gets changed.
        fireChange();
    }

    /**
     * Builds panel and returns the component.
     *
     * @return
     */
    public Component getUIComponent() {
        if (panel == null) {
            buildPanel();
        }
        return panel;
    }

    /**
     * Initialize the visual components.
     */
    private void initComponents() {
        String[] names = new String[PAIRS.size()];
        int i = 0;
        for (NameValuePair pair : PAIRS) {
            names[i++] = pair.getName();
        }
        archiveCombo = new JComboBox(names);
        archiveCombo.addItemListener(new MyItemListener());
    }

    /**
     * Notifiy the value models of selection changes.
     */
    private void fireChange() {
        int index = archiveCombo.getSelectedIndex();
        if (index == 0) { // No Backup
            versionModel.setValue(0);
            modeModel.setValue(ArchiveMode.NO_BACKUP);
        } else {
            versionModel.setValue(PAIRS.toArray(new NameValuePair[
                    PAIRS.size()])[index].getValue());
            modeModel.setValue(ArchiveMode.FULL_BACKUP);
        }
    }

    /**
     * Builds the visible panel.
     */
    private void buildPanel() {
        FormLayout layout = new FormLayout("70dlu, 3dlu, pref, pref:grow", "pref");
        panel = new JPanel(layout);

        CellConstraints cc = new CellConstraints();

        panel.add(archiveCombo, cc.xy(1, 1));
        if (purgeListener != null) {
            JButtonMini purgeButton = new JButtonMini(
                    Icons.getIconById(Icons.DELETE), Translation.getTranslation(
                            "archive_mode_selector_panel.purge.tip"));
            purgeButton.addActionListener(purgeListener);
            panel.add(purgeButton, cc.xy(3, 1));
        }
        panel.setOpaque(false);
    }

    /**
     * archive combo item change listener
     */
    private class MyItemListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            if (e.getSource() == archiveCombo) {
                fireChange();
            }
        }
    }

    // /////////////
    // Inner classes
    // /////////////

    private static class NameValuePair implements Comparable<NameValuePair> {

        private final int order;
        private final String name;
        private final int value;

        private NameValuePair(int order, String name, int value) {
            this.order = order;
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }

        public int compareTo(NameValuePair o) {
            return order - o.order;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            NameValuePair that = (NameValuePair) obj;

            if (order != that.order) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return order;
        }
    }
}
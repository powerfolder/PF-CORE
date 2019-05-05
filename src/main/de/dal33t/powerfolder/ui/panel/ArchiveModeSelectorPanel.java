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
package de.dal33t.powerfolder.ui.panel;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.Translation;
import org.jetbrains.annotations.NotNull;

/**
 * Panel for displaying and selecting archive mode. Attached are a pair of
 * ValueModels that get notified of selection changes (One for mode, one for
 * version history).
 *
 * @author <a href="mailto:hglasgow@powerfolder.com">Harry Glasgow</a>
 */
public class ArchiveModeSelectorPanel extends PFUIComponent {

    /**
     * Map of available modes.
     */
    private final Set<NameValuePair> pairs = new TreeSet<>();

    private JComboBox<String> archiveCombo;
    private JButtonMini purgeButton;
    private JPanel panel;
    private List<ValueModel> versionModels; // {Integer}
    private ActionListener purgeListener;

    /**
     * Constructor
     *
     * @param controller
     *            the necessary evil...
     * @param versionModels
     *            List<ValueModel{Integer}> that gets notified of version history
     *            changes.
     * @param purgeListener
     *            Listener to the user clicking the purge archive button.
     */
    public ArchiveModeSelectorPanel(Controller controller,
        List<ValueModel> versionModels,
        ActionListener purgeListener) {
        super(controller);
        this.versionModels = versionModels;
        this.purgeListener = purgeListener;
        initComponents();
    }

    public ArchiveModeSelectorPanel(Controller controller,
        ValueModel versionModel) {
        super(controller);
        versionModels = Collections.singletonList(versionModel);
        initComponents();
    }

    public void setChangeable(boolean changeable) {
        archiveCombo.setEnabled(changeable);
        purgeButton.setVisible(changeable);
    }

    /**
     * Set the archive mode and verions history for the panel. Value models are
     * not notified of changes during the set operation.
     *
     * @param versionHistory The number of versions
     */
    public void setArchiveMode(int versionHistory) {
        if (versionHistory == 0) {
            archiveCombo.setSelectedIndex(0); // No Backup
        } else if (versionHistory == -1) {
            archiveCombo.setSelectedIndex(pairs.size() - 1); // Unlimited
        } else {
            int index = 0;
            for (NameValuePair nvp : pairs) {
                if (index != 0 && versionHistory <= nvp.getValue()) {
                    archiveCombo.setSelectedIndex(index);
                    return;
                }
                index++;
            }

            // versionHistory > max ==> Unlimited
            archiveCombo.setSelectedIndex(pairs.size() - 1);
        }

        // Make sure that the models have the correct values, in case nothing
        // gets changed.
        fireChange();
    }

    /**
     * Builds panel and returns the component.
     *
     * @return The complete panel
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
        pairs.clear();

        pairs.add(new NameValuePair(0, Translation
            .get("archive_mode_selector_panel.none"), 0));
        pairs.add(new NameValuePair(1, Translation.get(
            "archive_mode_selector_panel.version", "1"), 1));
        pairs.add(new NameValuePair(5, Translation.get(
            "archive_mode_selector_panel.versions", "5"), 5));
        pairs.add(new NameValuePair(9, Translation.get(
            "archive_mode_selector_panel.versions", "25"), 25));
        pairs.add(new NameValuePair(10, Translation.get(
            "archive_mode_selector_panel.versions", "100"), 100));
        pairs.add(new NameValuePair(11, Translation
            .get("archive_mode_selector_panel.unlimited"), -1));

        String[] names = new String[pairs.size()];
        int i = 0;
        for (NameValuePair pair : pairs) {
            names[i++] = pair.getName();
        }
        archiveCombo = new JComboBox<>(names);
        archiveCombo.addItemListener(new MyItemListener());
    }

    /**
     * Notifiy the value models of selection changes.
     */
    private void fireChange() {
        int index = archiveCombo.getSelectedIndex();
        if (index <= 0) { // No Backup
            for (ValueModel versionModel : versionModels) {
                versionModel.setValue(0);
            }
        } else {
            for (ValueModel versionModel : versionModels) {
                versionModel.setValue(pairs.toArray(
                    new NameValuePair[0])[index].getValue());
            }
        }
    }

    /**
     * Builds the visible panel.
     */
    private void buildPanel() {
        String layoutColumn;
        if (purgeListener != null) {
            layoutColumn = "pref, 3dlu, pref, pref:grow";
        } else {
            layoutColumn = "pref:grow";
        }
        FormLayout layout = new FormLayout(layoutColumn,
            "pref");
        panel = new JPanel(layout);

        CellConstraints cc = new CellConstraints();

        panel.add(archiveCombo, cc.xy(1, 1));
        if (purgeListener != null) {
            purgeButton = new JButtonMini(Icons
                .getIconById(Icons.DELETE), Translation
                .get("archive_mode_selector_panel.purge.tip"));
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

        public int compareTo(@NotNull NameValuePair o) {
            return order - o.order;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NameValuePair)) {
                return false;
            }

            NameValuePair that = (NameValuePair) obj;

            return order == that.order;
        }

        @Override
        public int hashCode() {
            return order;
        }
    }
}
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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Panel with a combobox for selecting the line speed and a textfield for
 * entering upload speed. Editing the textfield is only possible if Custom
 * line_speed was chosen first.
 *
 * @author Bytekeeper
 * @version $revision$
 */
public class LineSpeedSelectionPanel extends JPanel {

    private static final int UNLIMITED = 0;
    private static final int AUTO_DETECT = -1;

    private JComboBox speedSelectionBox;
    private JComponent customSpeedPanel;
    private SpinnerNumberModel customUploadSpeedSpinnerModel;
    private SpinnerNumberModel customDownloadSpeedSpinnerModel;
    private JSpinner customUploadSpeedSpinner;
    private JLabel customUploadSpeedText;
    private JSpinner customDownloadSpeedSpinner;
    private JLabel customDownloadSpeedText;
    private boolean showCustomEntry;
    private JLabel customUploadKbPerSLabel;
    private JLabel customDownloadKbPerSLabel;

    /**
     * Constructs a new LineSpeedSelectionPanel.
     *
     * @param wan WAN, not LAN.
     * @param showCustomEntry true if the entry panels for custom upload/download speed
     *                                    selection should be shown all the time, otherwise they only
     *                                    visible on demand.
     */
    public LineSpeedSelectionPanel(boolean wan, boolean showCustomEntry) {
        this.showCustomEntry = showCustomEntry;
        initComponents(wan);
        buildPanel();
    }

    private void buildPanel() {
        FormLayout layout = new FormLayout("pref:grow", "pref, 1dlu, pref");
        setLayout(layout);

        CellConstraints cc = new CellConstraints();
        customSpeedPanel.setBorder(Borders.createEmptyBorder("0, 0, 3dlu, 0"));
        JPanel speedSelectionPanel = createSpeedSelectionPanel();
        add(speedSelectionPanel, cc.xy(1, 1));
        add(customSpeedPanel, cc.xy(1, 3));
    }

    private void initComponents(boolean wan) {
        setOpaque(false);

        customUploadSpeedSpinnerModel =
                new SpinnerNumberModel(0, 0, 999999, 5);
        customDownloadSpeedSpinnerModel =
                new SpinnerNumberModel(0, 0, 999999, 5);
        customUploadSpeedText = new JLabel("");
        customUploadSpeedSpinner =
                new JSpinner(customUploadSpeedSpinnerModel);
        customDownloadSpeedText = new JLabel("");
        customDownloadSpeedSpinner =
                new JSpinner(customDownloadSpeedSpinnerModel);

        customUploadKbPerSLabel = new JLabel(Translation.getTranslation(
                "general.kbPerS"));
        customDownloadKbPerSLabel = new JLabel(Translation.getTranslation(
                "general.kbPerS"));

        customUploadSpeedSpinnerModel.addChangeListener(new
                MyChangeListener(customUploadSpeedText));
        customDownloadSpeedSpinnerModel.addChangeListener(new
                MyChangeListener(customDownloadSpeedText));

        customSpeedPanel = createCustomSpeedInputFieldPanel();

        speedSelectionBox = new JComboBox();
        if (wan) {
            loadWanSelection();
        } else {
            loadLanSelection();
        }
        speedSelectionBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                configureUpDownComponents();
            }
        });
        configureUpDownComponents();
    }

    private void configureUpDownComponents() {
        customSpeedPanel.setVisible(showCustomEntry);
        if (((LineSpeed) speedSelectionBox.getSelectedItem()).isEditable()) {
            // Custom line.
            customUploadSpeedSpinner.setVisible(true);
            customDownloadSpeedSpinner.setVisible(true);
            customUploadKbPerSLabel.setVisible(true);
            customDownloadKbPerSLabel.setVisible(true);
            customUploadSpeedText.setVisible(false);
            customDownloadSpeedText.setVisible(false);

            customUploadSpeedSpinnerModel.setValue(0);
            customDownloadSpeedSpinnerModel.setValue(0);
        } else {
            // Presret line.
            customUploadSpeedSpinner.setVisible(false);
            customDownloadSpeedSpinner.setVisible(false);
            customUploadKbPerSLabel.setVisible(false);
            customDownloadKbPerSLabel.setVisible(false);
            customUploadSpeedText.setVisible(true);
            customDownloadSpeedText.setVisible(true);

            customUploadSpeedSpinnerModel.setValue(((LineSpeed)
                    speedSelectionBox.getSelectedItem()).getUploadSpeed());
            customDownloadSpeedSpinnerModel.setValue(((LineSpeed)
                    speedSelectionBox.getSelectedItem()).getDownloadSpeed());
        }
    }

    private JPanel createSpeedSelectionPanel() {
        FormLayout layout = new FormLayout(
                "140dlu, pref:grow", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(speedSelectionBox, cc.xy(1, 1));

        JPanel panel = builder.getPanel();
        panel.setOpaque(false);
        return panel;
    }

    private JPanel createCustomSpeedInputFieldPanel() {
        FormLayout layout = new FormLayout(
                "pref, 3dlu, pref, 3dlu, pref:grow",
                "pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(new JLabel(Translation
                .getTranslation("line_speed.download_speed")), cc.xy(1, 1));
        builder.add(customDownloadSpeedSpinner, cc.xy(3, 1));
        builder.add(customDownloadKbPerSLabel, cc.xy(5, 1));
        builder.add(customDownloadSpeedText, cc.xyw(3, 1, 3));

        builder.add(new JLabel(Translation
                .getTranslation("line_speed.upload_speed")), cc.xy(1, 3));
        builder.add(customUploadSpeedSpinner, cc.xy(3, 3));
        builder.add(customUploadKbPerSLabel, cc.xy(5, 3));
        builder.add(customUploadSpeedText, cc.xyw(3, 3, 3));

        JPanel panel = builder.getPanel();
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Loads the selection with the default values for LAN
     */
    private void loadLanSelection() {
        addLineSpeed("line_speed.lan10", 1000, UNLIMITED);
        addLineSpeed("line_speed.lan100", 10000, UNLIMITED);
        addLineSpeed("line_speed.lan1000", 100000, UNLIMITED);
        addLineSpeed("line_speed.unlimited", UNLIMITED, UNLIMITED);
        if (showCustomEntry) {
            addLineSpeed("line_speed.custom_speed", UNLIMITED, UNLIMITED, true);
        }
    }

    /**
     * Loads the selection with the default values for WAN
     */
    private void loadWanSelection() {
        addLineSpeed("line_speed.auto_speed", AUTO_DETECT, AUTO_DETECT);
        addLineSpeed("line_speed.adsl128", 11, UNLIMITED);
        addLineSpeed("line_speed.adsl256", 23, UNLIMITED);
        addLineSpeed("line_speed.adsl512", 46, UNLIMITED);
        addLineSpeed("line_speed.adsl768", 69, UNLIMITED);
        addLineSpeed("line_speed.adsl1024", 128, UNLIMITED);
        addLineSpeed("line_speed.adsl1536", 192, UNLIMITED);
        addLineSpeed("line_speed.T1", 140, UNLIMITED);
        addLineSpeed("line_speed.T3", 3930, UNLIMITED);
        addLineSpeed("line_speed.unlimited", UNLIMITED, UNLIMITED);
        if (showCustomEntry) {
            addLineSpeed("line_speed.custom_speed", UNLIMITED, UNLIMITED, true);
        }
    }

    /**
     * Creates a LineSpeed instance with the given parameters then adds and
     * returns it.
     *
     * @param descr         the translation property's name whose value will be used
     * @param uploadSpeed   the upload speed in kb/s, 0 for unlimited
     * @param downloadSpeed the download speed in kb/s, 0 for unlimited
     * @return
     */
    private LineSpeed addLineSpeed(String descr, long uploadSpeed,
                                   long downloadSpeed) {
        return addLineSpeed(descr, uploadSpeed, downloadSpeed, false);
    }

    /**
     * Creates a LineSpeed instance with the given parameters then adds and
     * returns it.
     *
     * @param descr         the translation property's name whose value will be used
     * @param uploadSpeed   the upload speed in kb/s, 0 for unlimited
     * @param downloadSpeed the download speed in kb/s, 0 for unlimited
     * @param editable      true if the user should be allowed to modify the upload speed
     *                      setting. (The value of LineSpeed.uploadSpeed remains
     *                      untouched)
     * @return the line_speed entry.
     */
    private LineSpeed addLineSpeed(String descr, long uploadSpeed,
                                   long downloadSpeed, boolean editable) {
        LineSpeed ls = new LineSpeed(Translation.getTranslation(descr),
                uploadSpeed, downloadSpeed, editable);
        addLineSpeed(ls);
        return ls;
    }

    /**
     * Adds the given LineSpeed to the selection list.
     *
     * @param speed
     */
    public void addLineSpeed(LineSpeed speed) {
        speedSelectionBox.addItem(speed);
    }

    /**
     * Removes the given LineSpeed from the selection list.
     *
     * @param speed
     */
    public void removeLineSpeed(LineSpeed speed) {
        speedSelectionBox.removeItem(speed);
    }

    /**
     * Updates the panel by selecting the correct Item for the given speed and
     * also updates the custom value field with that value. TODO: Since some
     * lines might have the same upload limit (like ISDN/DSL) this method
     * currenlty selects the first matching item.
     *
     * @param uploadSpeed   the upload speed in kb/s, 0 for unlimited
     * @param downloadSpeed the download speed in kb/s, 0 for unlimited
     */
    public void setSpeedKBPS(long uploadSpeed, long downloadSpeed) {
        // Find the "best" item to select for the given speed
        // if none matches, falls thru to "Custom".
        int count = speedSelectionBox.getItemCount();
        for (int i = 0; i < count; i++) {
            LineSpeed ls = (LineSpeed) speedSelectionBox.getItemAt(i);
            if (ls.getUploadSpeed() == uploadSpeed
                    && ls.getDownloadSpeed() == downloadSpeed) {
                speedSelectionBox.setSelectedItem(ls);
                return;
            }
        }

        // It's not in the list of presets, it looks like a custom.
        if (showCustomEntry) {
            speedSelectionBox.setSelectedIndex(count - 1); // Custom is last item.
            customUploadSpeedSpinner.setValue(uploadSpeed);
            customDownloadSpeedSpinner.setValue(downloadSpeed);
        }
    }

    public boolean isAutodetect() {
        return customUploadSpeedSpinnerModel.getNumber().longValue() == AUTO_DETECT;
    }

    /**
     * Returns the currently selected upload speed.
     *
     * @return The upload speed in kb/s or AUTO_DETECT if an error occured
     */
    public long getUploadSpeedKBPS() {
        return customUploadSpeedSpinnerModel.getNumber().longValue() * 1024;
    }

    /**
     * Returns the currently selected download speed.
     *
     * @return The download speed in kb/s or AUTO_DETECT if an error occured
     */
    public long getDownloadSpeedKBPS() {
        return customDownloadSpeedSpinnerModel.getNumber().longValue() *
                1024;
    }

    @Override
    public void setEnabled(boolean enabled) {
        customSpeedPanel.setEnabled(enabled);
        speedSelectionBox.setEnabled(enabled);
        customUploadSpeedSpinner.setEnabled(enabled);
        customDownloadSpeedSpinner.setEnabled(enabled);
        customUploadSpeedText.setEnabled(enabled);
        customDownloadSpeedText.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    private static void updateLabel(JLabel label, long kbPerS) {
        if (kbPerS == -1) {
            label.setText(Translation.getTranslation("line_speed.auto_speed"));
        } else if (kbPerS == 0) {
            label.setText(Translation.getTranslation("line_speed.unlimited"));
        } else {
            label.setText(kbPerS + " " +
                    Translation.getTranslation("general.kbPerS"));
        }
    }

    // Inner classes **********************************************************

    /**
     * Container holding the description and upload rate.
     */
    private static class LineSpeed {
        private long uploadSpeed;
        private long downloadSpeed;
        private String desc;
        private boolean editable;

        /**
         * Creates a new LineSpeed
         *
         * @param desc        the "name" of the speed value
         * @param uploadSpeed a value >= 0. If this value is below 0 the user may enter
         *                    a speed in Kilobytes per second.
         */
        LineSpeed(String desc, long uploadSpeed, long downloadSpeed,
                  boolean editable) {
            this.desc = desc;
            this.uploadSpeed = uploadSpeed;
            this.downloadSpeed = downloadSpeed;
            this.editable = editable;
        }

        public long getUploadSpeed() {
            return uploadSpeed;
        }

        public String toString() {
            return desc;
        }

        public boolean isEditable() {
            return editable;
        }

        public long getDownloadSpeed() {
            return downloadSpeed;
        }
    }

    private static class MyChangeListener implements ChangeListener {

        private final JLabel label;

        private MyChangeListener(JLabel label) {
            this.label = label;
        }

        public void stateChanged(ChangeEvent e) {
            SpinnerNumberModel model = (SpinnerNumberModel) e.getSource();
            long kbPerS = model.getNumber().longValue();
            updateLabel(label, kbPerS);
        }
    }
}

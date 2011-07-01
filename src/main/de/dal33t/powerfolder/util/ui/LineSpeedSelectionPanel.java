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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.Feature;

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
    private JSpinner customDownloadSpeedSpinner;
    private LineSpeed defaultSpeed;
    private boolean alwaysShowCustomEntryPanels;
    private JLabel customUploadLabel;
    private JLabel customDownloadLabel;

    /**
     * Constructs a new LineSpeedSelectionPanel.
     * 
     * @param alwaysShowCustomEntryPanels
     *            true if the entry panels for custom upload/download speed
     *            selection should be shown all the time, otherwise they only
     *            visible on demand.
     */
    public LineSpeedSelectionPanel(boolean alwaysShowCustomEntryPanels) {
        initComponents();
        buildPanel();
        this.alwaysShowCustomEntryPanels = alwaysShowCustomEntryPanels;
    }

    private void buildPanel() {
        FormLayout layout = new FormLayout("pref:grow", "pref, 1dlu, pref");
        setLayout(layout);

        CellConstraints cc = new CellConstraints();
        customSpeedPanel = createCustomSpeedInputFieldPanel();
        customSpeedPanel.setBorder(Borders.createEmptyBorder("0, 0, 3dlu, 0"));
        JPanel speedSelectionPanel = createSpeedSelectionPanel();
        add(speedSelectionPanel, cc.xy(1, 1));
        add(customSpeedPanel, cc.xy(1, 3));
    }

    private void initComponents() {
        setOpaque(false);

        if (Feature.AUTO_SPEED_DETECT.isEnabled()) {
            customUploadSpeedSpinnerModel = new SpinnerNumberModel(0, -1, 999999, 1);
            customDownloadSpeedSpinnerModel = new SpinnerNumberModel(0, -1, 999999, 1);
        } else {
            customUploadSpeedSpinnerModel = new SpinnerNumberModel(0, 0, 999999, 1);
            customDownloadSpeedSpinnerModel = new SpinnerNumberModel(0, 0, 999999, 1);
        }
        customUploadSpeedSpinner = new JSpinner(customUploadSpeedSpinnerModel);
        customDownloadSpeedSpinner = new JSpinner(customDownloadSpeedSpinnerModel);

        customUploadLabel = new JLabel();
        customDownloadLabel = new JLabel();

        customUploadSpeedSpinnerModel.addChangeListener(new MyChangeListener(customUploadLabel));
        customDownloadSpeedSpinnerModel.addChangeListener(new MyChangeListener(customDownloadLabel));

        speedSelectionBox = new JComboBox();
        speedSelectionBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (((LineSpeed) speedSelectionBox.getSelectedItem())
                    .isEditable())
                {
                    customUploadSpeedSpinner.setEnabled(true);
                    customDownloadSpeedSpinner.setEnabled(true);
                    customUploadLabel.setEnabled(true);
                    customDownloadLabel.setEnabled(true);
                    if (!alwaysShowCustomEntryPanels) {
                        customSpeedPanel.setVisible(true);
                    }

                } else {
                    customUploadSpeedSpinner.setEnabled(false);
                    customDownloadSpeedSpinner.setEnabled(false);
                    customUploadLabel.setEnabled(false);
                    customDownloadLabel.setEnabled(false);
                    if (!alwaysShowCustomEntryPanels) {
                        customSpeedPanel.setVisible(false);
                    }

                    customUploadSpeedSpinnerModel.setValue(((LineSpeed)
                            speedSelectionBox.getSelectedItem()).getUploadSpeed());
                    customDownloadSpeedSpinnerModel.setValue(((LineSpeed)
                            speedSelectionBox.getSelectedItem()).getDownloadSpeed());
                }
            }
        });
    }

    private JPanel createSpeedSelectionPanel() {
        FormLayout layout = new FormLayout(
            "140dlu, pref:grow", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(speedSelectionBox, cc.xy(1,1));

        JPanel panel = builder.getPanel();
        panel.setOpaque(false);
        return panel;
    }

    private JPanel createCustomSpeedInputFieldPanel() {
        FormLayout layout = new FormLayout(
            "pref, 3dlu, pref, 3dlu, pref",
            "pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(new JLabel(Translation
            .getTranslation("line_speed.download_speed")), cc.xy(1, 1));
        builder.add(customDownloadSpeedSpinner, cc.xy(3, 1));
        builder.add(customDownloadLabel, cc.xy(5, 1));

        builder.add(new JLabel(Translation
            .getTranslation("line_speed.upload_speed")), cc.xy(1, 3));
        builder.add(customUploadSpeedSpinner, cc.xy(3, 3));
        builder.add(customUploadLabel, cc.xy(5, 3));

        JPanel panel = builder.getPanel();
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Loads the selection with the default values for LAN
     */
    public void loadLANSelection() {
        if (Feature.AUTO_SPEED_DETECT.isEnabled()) {
            defaultSpeed = addLineSpeed("line_speed.auto_speed", AUTO_DETECT,
                    UNLIMITED);
        }
        addLineSpeed("line_speed.lan10", 1000, UNLIMITED);
        addLineSpeed("line_speed.lan100", 10000, UNLIMITED);
        addLineSpeed("line_speed.lan1000", 100000, UNLIMITED);
        addLineSpeed("line_speed.unlimited", UNLIMITED, UNLIMITED);
        if (Feature.AUTO_SPEED_DETECT.isEnabled()) {
            addLineSpeed("line_speed.custom_speed", UNLIMITED, UNLIMITED, true);
        } else {
            defaultSpeed = addLineSpeed("line_speed.custom_speed", UNLIMITED,
                    UNLIMITED, true);
        }
    }

    /**
     * Loads the selection with the default values for WAN
     */
    public void loadWANSelection() {
        if (Feature.AUTO_SPEED_DETECT.isEnabled()) {
            defaultSpeed = addLineSpeed("line_speed.auto_speed", AUTO_DETECT,
                    UNLIMITED);
        }
        addLineSpeed("line_speed.adsl128", 11, UNLIMITED);
        addLineSpeed("line_speed.adsl256", 23, UNLIMITED);
        addLineSpeed("line_speed.adsl512", 46, UNLIMITED);
        addLineSpeed("line_speed.adsl768", 69, UNLIMITED);
        addLineSpeed("line_speed.adsl1024", 128, UNLIMITED);
        addLineSpeed("line_speed.adsl1536", 192, UNLIMITED);
        addLineSpeed("line_speed.T1", 140, UNLIMITED);
        addLineSpeed("line_speed.T3", 3930, UNLIMITED);
        addLineSpeed("line_speed.unlimited", UNLIMITED, UNLIMITED);
        if (Feature.AUTO_SPEED_DETECT.isEnabled()) {
            addLineSpeed("line_speed.custom_speed", UNLIMITED, UNLIMITED, true);
        } else {
            defaultSpeed = addLineSpeed("line_speed.custom_speed", UNLIMITED,
                    UNLIMITED, true);
        }
    }

    /**
     * @return the default "fallback" line_speed if one was set, otherwise
     *         returns the current selected speed.
     */
    private LineSpeed getDefaultLineSpeed() {
        return defaultSpeed != null
            ? defaultSpeed
            : (LineSpeed) speedSelectionBox.getSelectedItem();
    }

    /**
     * Sets the default "fallback" line_speed.
     * 
     * @param speed
     *            the LineSpeed or null it should be cleared
     */
    public void setDefaultLineSpeed(LineSpeed speed) {
        defaultSpeed = speed;
    }

    /**
     * Creates a LineSpeed instance with the given parameters then adds and
     * returns it.
     * 
     * @param descr
     *            the translation property's name whose value will be used
     * @param uploadSpeed
     *            the upload speed in kb/s, 0 for unlimited
     * @param downloadSpeed
     *            the download speed in kb/s, 0 for unlimited
     * @return
     */
    private LineSpeed addLineSpeed(String descr, long uploadSpeed,
        long downloadSpeed)
    {
        return addLineSpeed(descr, uploadSpeed, downloadSpeed, false);
    }

    /**
     * Creates a LineSpeed instance with the given parameters then adds and
     * returns it.
     * 
     * @param descr
     *            the translation property's name whose value will be used
     * @param uploadSpeed
     *            the upload speed in kb/s, 0 for unlimited
     * @param downloadSpeed
     *            the download speed in kb/s, 0 for unlimited
     * @param editable
     *            true if the user should be allowed to modify the upload speed
     *            setting. (The value of LineSpeed.uploadSpeed remains
     *            untouched)
     * @return the line_speed entry.
     */
    private LineSpeed addLineSpeed(String descr, long uploadSpeed,
        long downloadSpeed, boolean editable)
    {
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
     * @param uploadSpeed
     *            the upload speed in kb/s, 0 for unlimited
     * @param downloadSpeed
     *            the download speed in kb/s, 0 for unlimited
     */
    public void setSpeedKBPS(long uploadSpeed, long downloadSpeed) {
        // Find the "best" item to select for the given speed
        // if none matches, falls thru tu "Custom"
        for (int i = 0; i < speedSelectionBox.getItemCount(); i++) {
            LineSpeed ls = (LineSpeed) speedSelectionBox.getItemAt(i);
            if (ls.getUploadSpeed() == uploadSpeed
                && ls.getDownloadSpeed() == downloadSpeed)
            {
                speedSelectionBox.setSelectedItem(ls);
                break;
            }
        }

        customUploadSpeedSpinner.setValue(uploadSpeed);
        customDownloadSpeedSpinner.setValue(downloadSpeed);
        if (((LineSpeed) speedSelectionBox.getSelectedItem()).getUploadSpeed() != uploadSpeed
            || ((LineSpeed) speedSelectionBox.getSelectedItem())
                .getDownloadSpeed() != downloadSpeed)
        {
            speedSelectionBox.setSelectedItem(getDefaultLineSpeed());
        }
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
        customUploadLabel.setEnabled(enabled);
        customDownloadLabel.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    private static void updateLabel(JLabel label, long lng) {
        if (lng == -1) {
            label.setText('(' +
                    Translation.getTranslation("line_speed.auto_speed") + ')');
        } else if (lng == 0) {
            label.setText('(' +
                    Translation.getTranslation("line_speed.unlimited") + ')');
        } else {
            label.setText(Translation.getTranslation("general.kbPerS"));
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
         * @param desc
         *            the "name" of the speed value
         * @param uploadSpeed
         *            a value >= 0. If this value is below 0 the user may enter
         *            a speed in Kilobytes per second.
         */
        LineSpeed(String desc, long uploadSpeed, long downloadSpeed,
            boolean editable)
        {
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

    private class MyChangeListener implements ChangeListener {

        private final JLabel label;

        private MyChangeListener(JLabel label) {
            this.label = label;
        }

        public void stateChanged(ChangeEvent e) {
            SpinnerNumberModel model = (SpinnerNumberModel) e.getSource();
            long lng = model.getNumber().longValue();
            updateLabel(label, lng);
        }
    }
}

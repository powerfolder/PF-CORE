package de.dal33t.powerfolder.util.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.util.Logger;
import de.dal33t.powerfolder.util.Translation;

/**
 * Panel with a combobox for selecting the line speed and a textfield for
 * entering upload speed. Editing the textfield is only possible if Custom
 * linespeed was chosen first.
 * 
 * @author Bytekeeper
 * @version $revision$
 */
public class LineSpeedSelectionPanel extends JPanel {
    private static final Logger LOG = Logger.getLogger(LineSpeedSelectionPanel.class);

    private JComboBox speedSelectionBox;
    private JComponent customSpeedPanel;
    private JFormattedTextField customUploadSpeedField;
    private JFormattedTextField customDownloadSpeedField;
    private LineSpeed defaultSpeed;
    private boolean alwaysShowCustomEntryPanels;

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
        FormLayout layout = new FormLayout("pref:grow", "pref, 3dlu, pref");
        setLayout(layout);

        CellConstraints cc = new CellConstraints();
        customSpeedPanel = createCustomSpeedInputFieldPanel();

        add(speedSelectionBox, cc.xy(1, 1));
        add(customSpeedPanel, cc.xy(1, 3));
    }

    private void initComponents() {
        setOpaque(false);

        customUploadSpeedField = new JFormattedTextField();
        customDownloadSpeedField = new JFormattedTextField();

        speedSelectionBox = new JComboBox();
        speedSelectionBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (((LineSpeed) speedSelectionBox.getSelectedItem())
                    .isEditable())
                {
                    customUploadSpeedField.setEnabled(true);
                    customDownloadSpeedField.setEnabled(true);
                    if (!alwaysShowCustomEntryPanels) {
                        customSpeedPanel.setVisible(true);
                    }

                } else {
                    customUploadSpeedField.setEnabled(false);
                    customDownloadSpeedField.setEnabled(false);
                    if (!alwaysShowCustomEntryPanels) {
                        customSpeedPanel.setVisible(false);
                    }

                    customUploadSpeedField.setText(Long
                        .toString(((LineSpeed) speedSelectionBox
                            .getSelectedItem()).getUploadSpeed()));
                    customDownloadSpeedField.setText(Long
                        .toString(((LineSpeed) speedSelectionBox
                            .getSelectedItem()).getDownloadSpeed()));
                }
            }
        });
    }

    private JPanel createCustomSpeedInputFieldPanel() {
        FormLayout layout = new FormLayout(
            "right:pref, 3dlu, pref:grow, 3dlu, pref", "pref, 3dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(new JLabel(Translation
            .getTranslation("linespeed.downloadspeed")), cc.xy(1, 1));
        builder.add(customDownloadSpeedField, cc.xy(3, 1));
        builder.add(new JLabel("KB/s"), cc.xy(5, 1));

        builder.add(new JLabel(Translation
            .getTranslation("linespeed.uploadspeed")), cc.xy(1, 3));
        builder.add(customUploadSpeedField, cc.xy(3, 3));
        builder.add(new JLabel("KB/s"), cc.xy(5, 3));

        JPanel panel = builder.getPanel();
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Loads the selection with the default values for LAN
     */
    public void loadLANSelection() {
        addLineSpeed("linespeed.lan10", 1000, 0);
        addLineSpeed("linespeed.lan100", 10000, 0);
        addLineSpeed("linespeed.lan1000", 100000, 0);
        addLineSpeed("linespeed.unlimited", 0, 0);
        defaultSpeed = addLineSpeed("linespeed.customspeed", 0, 0, true);
    }

    /**
     * Loads the selection with the default values for WAN
     */
    public void loadWANSelection() {
        addLineSpeed("linespeed.isdnspeed64", 5, 0);
        addLineSpeed("linespeed.isdnspeed128", 11, 0);
        addLineSpeed("linespeed.adsl128", 11, 0);
        addLineSpeed("linespeed.adsl256", 23, 0);
        addLineSpeed("linespeed.adsl512", 46, 0);
        addLineSpeed("linespeed.adsl768", 69, 0);
        addLineSpeed("linespeed.T1", 140, 0);
        addLineSpeed("linespeed.T3", 3930, 0);
        addLineSpeed("linespeed.unlimited", 0, 0);
        defaultSpeed = addLineSpeed("linespeed.customspeed", 0, 0, true);
    }

    /**
     * @return the default "fallback" linespeed if one was set, otherwise
     *         returns the current selected speed.
     */
    private LineSpeed getDefaultLineSpeed() {
        return defaultSpeed != null
            ? defaultSpeed
            : (LineSpeed) speedSelectionBox.getSelectedItem();
    }

    /**
     * Sets the default "fallback" linespeed.
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
     * @param speed
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
     * @param speed
     * @param editable
     *            true if the user should be allowed to modify the upload speed
     *            setting. (The value of LineSpeed.uploadSpeed remains
     *            untouched)
     * @return the linespeed entry.
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
     * @param speed
     *            the speed in kb/s, 0 for unlimited
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

        customUploadSpeedField.setValue(uploadSpeed);
        customDownloadSpeedField.setValue(downloadSpeed);
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
     * @return The upload speed in kb/s or a number < 0 if an error occured
     */
    public long getUploadSpeedKBPS() {
        try {
            return (Long) customUploadSpeedField.getFormatter().stringToValue(
                customUploadSpeedField.getText()) * 1024;
        } catch (ParseException e) {
            LOG.warn("Unable to parse uploadlimit '"
                + customUploadSpeedField.getText() + '\'');
        }
        return -1;
    }

    /**
     * Returns the currently selected download speed.
     * 
     * @return The download speed in kb/s or a number < 0 if an error occured
     */
    public long getDownloadSpeedKBPS() {
        try {
            return (Long) customDownloadSpeedField.getFormatter()
                .stringToValue(customDownloadSpeedField.getText()) * 1024;
        } catch (ParseException e) {
            LOG.warn("Unable to parse downloadlimit '"
                + customDownloadSpeedField.getText() + '\'');
        }
        return -1;
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
}

package de.dal33t.powerfolder.ui.preferences;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.LineSpeedSelectionPanel;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

public class NetworkSettingsTab extends PFComponent implements PreferenceTab {
    private static final int PRIVATE_MODE_INDEX = 0;
    private static final int LANONLY_MODE_INDEX = 1;

    private JPanel panel;
    private JComboBox networkingMode;
    private JLabel myDnsLabel;
    private JTextField myDnsField;
    private ValueModel mydnsndsModel;
    private LineSpeedSelectionPanel wanSpeed;
    private LineSpeedSelectionPanel lanSpeed;
    private JSlider silentModeThrottle;
    private boolean needsRestart = false;
    private JLabel silentThrottleLabel;

    public NetworkSettingsTab(Controller controller, ValueModel mydnsndsModel) {
        super(controller);
        this.mydnsndsModel = mydnsndsModel;
        initComponents();
    }

    public String getTabName() {
        return Translation.getTranslation("preferences.dialog.network.title");
    }

    public boolean needsRestart() {
        return needsRestart;
    }

    public void undoChanges() {

    }

    public boolean validate() {
        return true;
    }

    private void initComponents() {
        String[] options = new String[2];
        options[PRIVATE_MODE_INDEX] = Translation
            .getTranslation("preferences.dialog.networkmode.private");
        options[LANONLY_MODE_INDEX] = Translation
            .getTranslation("preferences.dialog.networkmode.lanonly");
        networkingMode = new JComboBox(options);
        if (getController().isLanOnly()) {
            networkingMode.setSelectedIndex(LANONLY_MODE_INDEX);
            networkingMode
                .setToolTipText(Translation
                    .getTranslation("preferences.dialog.networkmode.lanonly.tooltip"));
        } else { // private
            networkingMode.setSelectedIndex(PRIVATE_MODE_INDEX);
            networkingMode
                .setToolTipText(Translation
                    .getTranslation("preferences.dialog.networkmode.private.tooltip"));
        }

        networkingMode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String tooltip = null;
                switch (networkingMode.getSelectedIndex()) {
                    case PRIVATE_MODE_INDEX : {
                        tooltip = Translation
                            .getTranslation("preferences.dialog.networkmode.private.tooltip");
                        break;
                    }
                    case LANONLY_MODE_INDEX : {
                        tooltip = Translation
                            .getTranslation("preferences.dialog.networkmode.lanonly.tooltip");
                        break;
                    }
                }
                networkingMode.setToolTipText(tooltip);
            }

        });

        // DynDns
        myDnsLabel = new LinkLabel(Translation
            .getTranslation("preferences.dialog.dyndns"),
            "http://www.powerfolder.com/node/guide_supernode");

        myDnsField = BasicComponentFactory.createTextField(mydnsndsModel, false);

        wanSpeed = new LineSpeedSelectionPanel(true);
        wanSpeed.loadWANSelection();
        TransferManager tm = getController().getTransferManager();
        wanSpeed.setSpeedKBPS(tm.getAllowedUploadCPSForWAN() / 1024, tm
            .getAllowedDownloadCPSForWAN() / 1024);

        lanSpeed = new LineSpeedSelectionPanel(true);
        lanSpeed.loadLANSelection();
        lanSpeed.setSpeedKBPS(tm.getAllowedUploadCPSForLAN() / 1024, tm
            .getAllowedDownloadCPSForLAN() / 1024);

        silentThrottleLabel = new JLabel(Translation
            .getTranslation("preferences.dialog.silentthrottle"));
        silentThrottleLabel.setToolTipText(Translation
            .getTranslation("preferences.dialog.silentthrottle.tooltip"));

        silentModeThrottle = new JSlider();
        silentModeThrottle.setMinimum(10);
        silentModeThrottle.setMajorTickSpacing(25);
        silentModeThrottle.setMinorTickSpacing(5);

        silentModeThrottle.setPaintTicks(true);
        silentModeThrottle.setPaintLabels(true);
        Dictionary<Integer, JLabel> smtT = new Hashtable<Integer, JLabel>();
        for (int i = 0; i <= 100; i += silentModeThrottle.getMajorTickSpacing()) {
            smtT.put(i, new JLabel(Integer.toString(i) + "%"));
        }
        smtT.put(silentModeThrottle.getMinimum(), 
        		new JLabel(silentModeThrottle.getMinimum() + "%"));
        smtT.put(silentModeThrottle.getMaximum(), 
        		new JLabel(silentModeThrottle.getMaximum() + "%"));
        silentModeThrottle.setLabelTable(smtT);

        int smt = 25;
        try {
            smt = Math.min(100, Math.max(10, Integer
                .parseInt(ConfigurationEntry.UPLOADLIMIT_SILENTMODE_THROTTLE
                    .getValue(getController()))));
        } catch (NumberFormatException e) {
            log().debug("silentmodethrottle" + e);
        }
        silentModeThrottle.setValue(smt);
    }

    /**
     * Creates the JPanel for advanced settings
     * 
     * @return the created panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout(
                "right:100dlu, 3dlu, 30dlu, 3dlu, 15dlu, 10dlu, 30dlu, 30dlu, pref",
                "pref, 3dlu, pref, 3dlu, top:pref, 3dlu, top:pref, 3dlu, top:pref, 3dlu, top:pref:grow, 3dlu");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders
                .createEmptyBorder("3dlu, 0dlu, 0dlu, 0dlu"));
            CellConstraints cc = new CellConstraints();

            int row = 1;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.networkmode.name"), cc.xy(
                1, row));
            builder.add(networkingMode, cc.xywh(3, row, 7, 1));

            row += 2;
            builder.add(myDnsLabel, cc.xy(1, row));
            builder.add(myDnsField, cc.xywh(3, row, 7, 1));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.linesettings"), cc.xy(1,
                row));
            builder.add(wanSpeed, cc.xywh(3, row, 7, 1));

            row += 2;
            builder.addLabel(Translation
                .getTranslation("preferences.dialog.lanlinesettings"), cc.xy(1,
                row));
            builder.add(lanSpeed, cc.xywh(3, row, 7, 1));

            row += 2;
            // TODO BYTEKEEPER Please don't mix initalization
            // and panel building. Create a private field for this
            // JLabel and initalize it in initComponents.
            builder.add(silentThrottleLabel, cc.xy(1, row));
            builder.add(silentModeThrottle, cc.xywh(3, row, 7, 1));
            panel = builder.getPanel();
        }
        return panel;
    }

    /**
     * Saves the network settings.
     */
    public void save() {
        NetworkingMode netMode;
        switch (networkingMode.getSelectedIndex()) {
            case PRIVATE_MODE_INDEX : {
                netMode = NetworkingMode.PRIVATEMODE;
                break;
            }
            case LANONLY_MODE_INDEX : {
                netMode = NetworkingMode.LANONLYMODE;
                break;
            }
            default :
                throw new IllegalStateException("invalid index");
        }
        getController().setNetworkingMode(netMode);
        TransferManager tm = getController().getTransferManager();
        tm.setAllowedUploadCPSForWAN(wanSpeed.getUploadSpeedKBPS());
        tm.setAllowedDownloadCPSForWAN(wanSpeed.getDownloadSpeedKBPS());
        tm.setAllowedUploadCPSForLAN(lanSpeed.getUploadSpeedKBPS());
        tm.setAllowedDownloadCPSForLAN(lanSpeed.getDownloadSpeedKBPS());
        String dyndnsHost = (String) mydnsndsModel.getValue();
        // remove the dyndns, this is done here because
        // the save method of "invisible" tabs are not called
        // and if the mydnsndsModel is empty the dyndns tab is "invisible"
        if (StringUtils.isBlank(dyndnsHost)) {
            ConfigurationEntry.DYNDNS_HOSTNAME.removeValue(getController());
        }
        ConfigurationEntry.UPLOADLIMIT_SILENTMODE_THROTTLE.setValue(
            getController(), Integer.toString(silentModeThrottle.getValue()));
    }
}

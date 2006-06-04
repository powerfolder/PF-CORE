package de.dal33t.powerfolder.ui.preferences;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.LineSpeedSelectionPanel;
import de.dal33t.powerfolder.util.ui.LinkLabel;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

public class NetworkSettingsTab extends PFComponent implements PreferenceTab {
    
    private JPanel panel;
    private JCheckBox privateNetworkingBox;
    private JCheckBox lanOnlyBox;
    private JLabel lanLabel;
    private JLabel myDnsLabel;
    private JTextField myDnsField;
    private ValueModel mydnsndsModel;
    private LineSpeedSelectionPanel wanSpeed;
    private LineSpeedSelectionPanel lanSpeed;
    boolean needsRestart = false;

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
        // Public networking option
        privateNetworkingBox = SimpleComponentFactory.createCheckBox();
        privateNetworkingBox.setToolTipText(Translation
            .getTranslation("preferences.dialog.privatenetworking.tooltip"));
        privateNetworkingBox.setSelected(!getController().isPublicNetworking());
        privateNetworkingBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!privateNetworkingBox.isSelected()) {
                    lanOnlyBox.setSelected(false);
                }
                lanOnlyBox.setEnabled(privateNetworkingBox.isSelected());
                lanLabel.setEnabled(privateNetworkingBox.isSelected());
            }
        });

        // Lan only
        lanOnlyBox = SimpleComponentFactory.createCheckBox();
        lanOnlyBox.setToolTipText(Translation
            .getTranslation("preferences.dialog.lanonly.tooltip"));
        lanOnlyBox.setSelected(getController().isLanOnly());
        lanOnlyBox.setEnabled(privateNetworkingBox.isSelected());
        lanLabel = new JLabel(Translation
            .getTranslation("preferences.dialog.lanonly"));
        lanLabel.setToolTipText(Translation
            .getTranslation("preferences.dialog.lanonly.tooltip"));
        lanLabel.setEnabled(privateNetworkingBox.isSelected());
        // DynDns
        myDnsLabel = new LinkLabel(Translation
            .getTranslation("preferences.dialog.dyndns"), Translation
            .getTranslation("preferences.dialog.dyndns.link"));

        myDnsField = SimpleComponentFactory
            .createTextField(mydnsndsModel, true);

        wanSpeed = new LineSpeedSelectionPanel();
        wanSpeed.loadWANSelection();
        wanSpeed.setUploadSpeedKBPS(getController().getTransferManager()
            .getAllowedUploadCPSForWAN() / 1024);

        lanSpeed = new LineSpeedSelectionPanel();
        lanSpeed.loadLANSelection();
        lanSpeed.setUploadSpeedKBPS(getController().getTransferManager()
            .getAllowedUploadCPSForLAN() / 1024);

    }

    /**
     * Creates the JPanel for advanced settings
     * 
     * @return the created panel
     */
    public JPanel getUIPanel() {
        if (panel == null) {
            FormLayout layout = new FormLayout(
                "right:100dlu, 7dlu, 30dlu, 3dlu, 15dlu, 10dlu, 30dlu, 30dlu, pref",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, top:pref, 3dlu, top:pref:grow, 3dlu");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders.createEmptyBorder("3dlu, 0dlu, 0dlu, 0dlu"));
            CellConstraints cc = new CellConstraints();

            int row = 1;
            JLabel pnLabel = builder.addLabel(Translation
                .getTranslation("preferences.dialog.privatenetworking"), cc.xy(
                1, row));
            pnLabel
                .setToolTipText(Translation
                    .getTranslation("preferences.dialog.privatenetworking.tooltip"));
            builder.add(privateNetworkingBox, cc.xy(3, row));

            row += 2;
            builder.add(lanLabel, cc.xy(1, row));

            builder.add(lanOnlyBox, cc.xy(3, row));

            row += 2;
            builder.add(myDnsLabel, cc.xy(1, row));
            builder.add(myDnsField, cc.xywh(3, row, 7, 1));

            row += 2;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.linesettings")), cc.xy(1,
                row));
            builder.add(wanSpeed, cc.xywh(3, row, 7, 1));

            row += 2;
            builder.add(new JLabel(Translation
                .getTranslation("preferences.dialog.lanlinesettings")), cc.xy(
                1, row));
            builder.add(lanSpeed, cc.xywh(3, row, 7, 1));
            panel = builder.getPanel();
        }
        return panel;
    }

    /**
     * Saves the network settings.
     */
    public void save() {
        // Store networking mode
        getController().setPublicNetworking(!privateNetworkingBox.isSelected());
        getController().getTransferManager().setAllowedUploadCPSForWAN(
            wanSpeed.getUploadSpeedKBPS());
        getController().getTransferManager().setAllowedUploadCPSForLAN(
            lanSpeed.getUploadSpeedKBPS());
        boolean currentLanMode = "true".equals(getController().getConfig().getProperty("lanOnly"));
        if (currentLanMode != lanOnlyBox.isSelected()) {
            getController().getConfig().setProperty("lanOnly",
                lanOnlyBox.isSelected() + "");
            needsRestart = true;
        }
        
    }

}

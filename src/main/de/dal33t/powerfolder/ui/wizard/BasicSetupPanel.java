/* $Id: BasicSetupPanel.java,v 1.8 2006/04/23 18:21:18 bytekeeper Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.*;
import jwf.WizardPanel;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Panel for basic setup like nick, networking mode, etc.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.8 $
 */
public class BasicSetupPanel extends PFWizardPanel {

    private ValueModel nameModel;
    private ValueModel networkingModeModel;
    private LineSpeedSelectionPanel wanLineSpeed;
    private JTextField nameField;
    private JComboBox networkingModeChooser;

    public BasicSetupPanel(Controller controller) {
        super(controller);
    }

    public boolean hasNext() {
        return !StringUtils.isBlank((String) nameModel.getValue());
    }

    public boolean validateNext(List list) {
        long uploadSpeedKBPS = wanLineSpeed.getUploadSpeedKBPS();
        long downloadSpeedKBPS = wanLineSpeed.getDownloadSpeedKBPS();
        if (uploadSpeedKBPS == 0 && downloadSpeedKBPS == 0) {
            int result = DialogFactory.genericDialog(getController()
                .getUIController().getMainFrame().getUIComponent(), Translation
                .getTranslation("wizard.basicsetup.upload.title"), Translation
                .getTranslation("wizard.basicsetup.upload.text"), new String[]{
                Translation.getTranslation("general.continue"),
                Translation.getTranslation("general.cancel")}, 0,
                GenericDialogType.WARN); // Default is continue.
            return result == 0; // Continue
        }
        return true;
    }

    protected JPanel buildContent() {

        FormLayout layout = new FormLayout(
            "pref",
            "pref, 5dlu, pref, 10dlu, pref, 5dlu, pref, 10dlu, pref, 5dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.addLabel(Translation
            .getTranslation("wizard.basicsetup.enternick"), cc.xy(1, 1));
        builder.add(nameField, cc.xy(1, 3));
        builder.addLabel(Translation
            .getTranslation("wizard.basicsetup.networking"), cc.xy(1, 5));
        builder.add(networkingModeChooser, cc.xy(1, 7));
        builder.addLabel(Translation
            .getTranslation("preferences.dialog.linesettings"), cc.xy(1, 9));
        builder.add(wanLineSpeed, cc.xy(1, 11));

        return builder.getPanel();
    }

    public WizardPanel next() {
        // Set nick
        String nick = (String) nameModel.getValue();
        if (!StringUtils.isBlank(nick)) {
            getController().changeNick(nick, true);
        }
        // Set networking mode
        boolean privateNetworking = networkingModeModel.getValue() instanceof PrivateNetworking;
        boolean lanOnlyNetworking = networkingModeModel.getValue() instanceof LanOnlyNetworking;

        if (privateNetworking) {
            getController().setNetworkingMode(NetworkingMode.PRIVATEMODE);
        } else if (lanOnlyNetworking) {
            getController().setNetworkingMode(NetworkingMode.LANONLYMODE);
        } else {
            throw new IllegalStateException("invalid net working mode");
        }

        TransferManager tm = getController().getTransferManager();
        tm.setAllowedUploadCPSForWAN(wanLineSpeed.getUploadSpeedKBPS());
        tm.setAllowedDownloadCPSForWAN(wanLineSpeed.getDownloadSpeedKBPS());

        // Now OS panel (no entry required) and the whattodo
        return new LoginOnlineStoragePanel(getController(), new WhatToDoPanel(
            getController()), false);
    }

    /**
     * Initalizes all nessesary components
     */
    protected void initComponents() {
        nameModel = new ValueHolder(getController().getMySelf().getNick());

        nameModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                updateButtons();
            }
        });

        nameField = BasicComponentFactory.createTextField(nameModel, false);
        // Ensure minimum dimension
        UIUtil.ensureMinimumWidth(107, nameField);

        wanLineSpeed = new LineSpeedSelectionPanel(false);
        wanLineSpeed.loadWANSelection();
        TransferManager tm = getController().getTransferManager();
        wanLineSpeed.setSpeedKBPS(tm.getAllowedUploadCPSForWAN() / 1024, tm
            .getAllowedDownloadCPSForWAN() / 1024);

        networkingModeModel = new ValueHolder();
        // Network mode chooser
        networkingModeChooser = SimpleComponentFactory
                .createComboBox(networkingModeModel);
        networkingModeChooser.addItem(new PrivateNetworking());
        networkingModeChooser.addItem(new LanOnlyNetworking());
        NetworkingMode mode = getController().getNetworkingMode();
        switch (mode) {
            case PRIVATEMODE :
                networkingModeChooser.setSelectedIndex(0);
                break;
            case LANONLYMODE :
                networkingModeChooser.setSelectedIndex(1);
                break;
        }
        wanLineSpeed
            .setEnabled(networkingModeChooser.getSelectedItem() instanceof PrivateNetworking);
        networkingModeChooser.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                wanLineSpeed
                    .setEnabled(e.getItem() instanceof PrivateNetworking);
            }
        });

        setPicto(Icons.PROJECT_WORK_PICTO);
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.basicsetup.title");
    }

    // Helper classes *********************************************************

    private static class PrivateNetworking {
        public String toString() {
            return Translation.getTranslation("wizard.basicsetup.private");
        }
    }

    private static class LanOnlyNetworking {
        public String toString() {
            return Translation.getTranslation("wizard.basicsetup.lanonly");
        }
    }

}

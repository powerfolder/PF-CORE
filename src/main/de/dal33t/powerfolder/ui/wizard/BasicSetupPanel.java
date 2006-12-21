/* $Id: BasicSetupPanel.java,v 1.8 2006/04/23 18:21:18 bytekeeper Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import jwf.WizardPanel;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.NetworkingMode;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.LineSpeedSelectionPanel;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Panel for basic setup like nick, networking mode, etc.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.8 $
 */
public class BasicSetupPanel extends PFWizardPanel {
    private boolean initalized = false;

    private JTextField nameField;
    private ValueModel nameModel;
    private ValueModel networkingModeModel;
    private JComboBox networkingModeChooser;

    private LineSpeedSelectionPanel wanLineSpeed;

    public BasicSetupPanel(Controller controller) {
        super(controller);
    }

    // From WizardPanel *******************************************************

    public synchronized void display() {
        if (!initalized) {
            buildUI();
        }
    }

    public boolean hasNext() {
        return !StringUtils.isBlank((String) nameModel.getValue());
    }

    public boolean validateNext(List list) {
        return true;
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

        // What todo comes next
        return new WhatToDoPanel(getController());
    }

    public boolean canFinish() {
        return false;
    }

    public void finish() {
    }

    // UI building ************************************************************

    /**
     * Builds the ui
     */
    private void buildUI() {
        // init
        initComponents();

        // setBorder(new TitledBorder(Translation
        // .getTranslation("wizard.projectname.title"))); //Load invitation
        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout(
            "20dlu, pref, 15dlu, fill:120dlu",
            "5dlu, pref, 15dlu, pref, 3dlu, pref, 10dlu, pref, 3dlu, pref, 10dlu, pref, 3dlu, pref, 10dlu, pref, 3dlu, pref, pref:grow");
        PanelBuilder builder = new PanelBuilder(layout, this);
        CellConstraints cc = new CellConstraints();

        builder.add(createTitleLabel(Translation
            .getTranslation("wizard.basicsetup.title")), cc.xy(4, 2)); // Choose
        // project
        // name

        // Add current wizard pico
        builder.add(new JLabel(Icons.PROJECT_WORK_PICTO), cc.xywh(2, 4, 1, 3,
            CellConstraints.DEFAULT, CellConstraints.TOP));

        builder.addLabel(Translation
            .getTranslation("wizard.basicsetup.enternick"), cc.xy(4, 4));// Enter
        // your
        // nickname
        builder.add(nameField, cc.xy(4, 6));

        builder.addLabel(Translation
            .getTranslation("wizard.basicsetup.networking"), cc.xy(4, 8)); // Work
        // in
        // private mode?
        builder.add(networkingModeChooser, cc.xy(4, 10));

        builder.addLabel(Translation
            .getTranslation("preferences.dialog.linesettings"), cc.xy(4, 12));

        builder.add(wanLineSpeed, cc.xy(4, 14));

        // initalized
        initalized = true;
    }

    /**
     * Initalizes all nessesary components
     */
    private void initComponents() {
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
            case PRIVATEMODE : {
                networkingModeChooser.setSelectedIndex(0);
                break;
            }
            case LANONLYMODE : {
                networkingModeChooser.setSelectedIndex(1);
                break;
            }
        }
    }

    // Helper classes *********************************************************

    private class PrivateNetworking {
        public String toString() {
            return Translation.getTranslation("wizard.basicsetup.private");
        }
    }

    private class LanOnlyNetworking {
        public String toString() {
            return Translation.getTranslation("wizard.basicsetup.lanonly");
        }
    }

}

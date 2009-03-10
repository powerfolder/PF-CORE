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
package de.dal33t.powerfolder.ui.wizard;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import static de.dal33t.powerfolder.ui.wizard.WizardContextAttributes.*;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;
import jwf.WizardPanel;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

/**
 * A generally used wizard panel for choosing a disk location for a folder.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class ConfirmDiskLocationPanel extends PFWizardPanel {

    private File localBase;

    private JCheckBox backupByOnlineStorageBox;
    private JCheckBox createDesktopShortcutBox;
    private JCheckBox sendInviteAfterCB;

    private JLabel folderSizeLabel;

    public ConfirmDiskLocationPanel(Controller controller, File localBase) {
        super(controller);
        this.localBase = localBase;
    }

    public WizardPanel next() {
        getWizardContext().setAttribute(FOLDER_LOCAL_BASE, localBase);
        getWizardContext().setAttribute(INITIAL_FOLDER_NAME,
                localBase.getName());
        return new FolderSetupPanel(getController());
    }

    public boolean hasNext() {
        return true;
    }

    public boolean validateNext() {
        getWizardContext().setAttribute(CREATE_DESKTOP_SHORTCUT,
                createDesktopShortcutBox.isSelected());
        getWizardContext().setAttribute(SEND_INVIATION_AFTER_ATTRIBUTE,
            sendInviteAfterCB.isSelected());
        getWizardContext().setAttribute(BACKUP_ONLINE_STOARGE,
                backupByOnlineStorageBox.isSelected());
        return true;
    }

    protected JPanel buildContent() {

        FormLayout layout = new FormLayout(
                "pref, 3dlu, pref, 3dlu, pref, 0:grow",
                "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        JComponent locationField = new JLabel(Translation.getTranslation(
                "general.directory"));

        int row = 1;

        builder.add(locationField, cc.xy(1, row));
        builder.add(new JLabel(localBase.getAbsolutePath()), cc.xy(3, row));

        row += 2;
        builder.add(folderSizeLabel, cc.xyw(1, row, 3));

        if (!getController().isLanOnly()
                && PreferencesEntry.USE_ONLINE_STORAGE.getValueBoolean(getController())) {
            row += 2;
            builder.add(backupByOnlineStorageBox, cc.xyw(1, row, 3));
        }

        if (OSUtil.isWindowsSystem()) {
            row += 2;
            builder.add(createDesktopShortcutBox, cc.xyw(1, row, 3));
        }

        // Send Invite
        row += 2;
        builder.add(sendInviteAfterCB, cc.xyw(1, row, 3));

        return builder.getPanel();
    }

    /**
     * Initalizes all nessesary components
     */
    protected void initComponents() {

        folderSizeLabel = new JLabel();

        // Online Storage integration
        boolean backupByOS = !getController().isLanOnly()
                && PreferencesEntry.USE_ONLINE_STORAGE.getValueBoolean(getController())
                && Boolean.TRUE.equals(getWizardContext().getAttribute(
                BACKUP_ONLINE_STOARGE));
        backupByOnlineStorageBox = new JCheckBox(Translation
                .getTranslation("wizard.choose_disk_location.backup_by_online_storage"));
        // Is backup suggested?
        if (backupByOS) {

            // Remember last preference...
            Boolean buos = PreferencesEntry.BACKUP_OS.getValueBoolean(
                    getController());
            if (buos == null) {
                // .. or default to if last os client login ok.
                buos = getController().getOSClient().isLastLoginOK();
            }
            backupByOnlineStorageBox.setSelected(buos);
        }
        backupByOnlineStorageBox.getModel().addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                PreferencesEntry.BACKUP_OS.setValue(getController(),
                        backupByOnlineStorageBox.isSelected());
                if (backupByOnlineStorageBox.isSelected()) {
                    getController().getUIController().getApplicationModel()
                            .getServerClientModel().checkAndSetupAccount();
                }
            }
        });
        backupByOnlineStorageBox.setOpaque(false);

        // Create desktop shortcut
        createDesktopShortcutBox = new JCheckBox(Translation
                .getTranslation("wizard.choose_disk_location.create_desktop_shortcut"));

        createDesktopShortcutBox.setOpaque(false);

        // Send Invite
        boolean sendInvite = Boolean.TRUE.equals(getWizardContext()
                .getAttribute(SEND_INVIATION_AFTER_ATTRIBUTE));
        sendInviteAfterCB = SimpleComponentFactory.createCheckBox(Translation
                .getTranslation("wizard.choose_disk_location.send_invitation"));
        sendInviteAfterCB.setOpaque(false);
        sendInviteAfterCB.setSelected(sendInvite);

    }

    protected JComponent getPictoComponent() {
        return new JLabel(Icons.FILE_SHARING_PICTO);
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.choose_disk_location.options");
    }
}
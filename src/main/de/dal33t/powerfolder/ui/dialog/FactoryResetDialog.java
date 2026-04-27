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
package de.dal33t.powerfolder.ui.dialog;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.ui.util.SimpleComponentFactory;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Translation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.prefs.BackingStoreException;

public class FactoryResetDialog extends BaseDialog {

    private JButton resetButton;
    private JButton cancelButton;
    private JCheckBox deleteLocalFilesBox;

    public FactoryResetDialog(Controller controller) {
        super(Senior.MAIN_FRAME, controller, true);
    }

    public String getTitle() {
        return Translation.get("factory_reset.dialog.title");
    }

    protected Icon getIcon() {
        return null;
    }

    protected JComponent getContent() {
        FormLayout layout = new FormLayout("pref:grow",
            "pref, 10dlu, pref, 3dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        int row = 1;

        JLabel messageLabel = new JLabel("<html><body style='width:300px'>"
            + Translation.get("factory_reset.dialog.message") + "</body></html>");
        builder.add(messageLabel, cc.xy(1, row));
        row += 2;

        deleteLocalFilesBox = SimpleComponentFactory.createCheckBox(
            Translation.get("factory_reset.dialog.delete_local_files"));
        deleteLocalFilesBox.setSelected(false);
        deleteLocalFilesBox.setToolTipText(
            Translation.get("factory_reset.dialog.delete_local_files_tip"));
        builder.add(deleteLocalFilesBox, cc.xy(1, row));
        row += 2;

        FolderRepository repo = getController().getFolderRepository();
        Collection<Folder> folders = repo.getFolders();
        if (!folders.isEmpty()) {
            JLabel foldersLabel = new JLabel(
                Translation.get("factory_reset.dialog.synced_folders",
                    String.valueOf(folders.size())));
            foldersLabel.setFont(foldersLabel.getFont().deriveFont(Font.PLAIN, 11f));
            foldersLabel.setForeground(Color.GRAY);
            builder.add(foldersLabel, cc.xy(1, row));
        }

        resetButton = new JButton(Translation.get("factory_reset.dialog.reset_button"));
        resetButton.addActionListener(new ResetActionListener());

        cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        return builder.getPanel();
    }

    protected Component getButtonBar() {
        return ButtonBarFactory.buildCenteredBar(resetButton, cancelButton);
    }

    protected JButton getDefaultButton() {
        return cancelButton;
    }

    private class ResetActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            boolean deleteLocalFiles = deleteLocalFilesBox.isSelected();
            close();
            getUIController().closePreferencesDialog();
            SwingUtilities.invokeLater(() -> performFactoryReset(deleteLocalFiles));
        }
    }

    private void performFactoryReset(boolean deleteLocalFiles) {
        logInfo("Factory reset started. Delete local files: " + deleteLocalFiles);

        FolderRepository repo = getController().getFolderRepository();

        // 1. Suspend folder scanning
        repo.setSuspendNewFolderSearch(true);

        // 2. Collect local folder paths before removal
        List<Path> localPaths = new ArrayList<>();
        if (deleteLocalFiles) {
            for (Folder folder : repo.getFolders()) {
                Path localBase = folder.getLocalBase();
                if (localBase != null && Files.exists(localBase)) {
                    localPaths.add(localBase);
                }
            }
        }

        // 3. Logout from server
        try {
            getController().getOSClient().logout();
            logInfo("Factory reset: logged out from server");
        } catch (Exception e) {
            logWarning("Factory reset: error during logout: " + e);
        }

        // 4. Remove all folders
        Collection<Folder> allFolders = new ArrayList<>(repo.getFolders());
        for (Folder folder : allFolders) {
            try {
                repo.removeFolder(folder, true, false, false);
                logInfo("Factory reset: removed folder " + folder.getName());
            } catch (Exception e) {
                logWarning("Factory reset: error removing folder "
                    + folder.getName() + ": " + e);
            }
        }

        // 5. Clear configuration
        try {
            Properties config = getController().getConfig();
            config.clear();
            getController().saveConfig();
            logInfo("Factory reset: configuration cleared");
        } catch (Exception e) {
            logWarning("Factory reset: error clearing config: " + e);
        }

        // 6. Clear preferences
        try {
            getController().getPreferences().clear();
            getController().getPreferences().flush();
            logInfo("Factory reset: preferences cleared");
        } catch (BackingStoreException e) {
            logWarning("Factory reset: error clearing preferences: " + e);
        }

        // 7. Delete local sync folders if requested
        if (deleteLocalFiles) {
            for (Path path : localPaths) {
                if (!Files.exists(path)) {
                    continue;
                }
                Path normalized = path.toAbsolutePath().normalize();
                if (normalized.equals(normalized.getRoot())
                    || normalized.getNameCount() <= 1) {
                    logWarning("Factory reset: skipping dangerous path " + normalized);
                    continue;
                }
                try {
                    PathUtils.recursiveDelete(normalized);
                    logInfo("Factory reset: deleted local folder " + normalized);
                } catch (IOException e) {
                    logWarning("Factory reset: error deleting " + normalized + ": " + e);
                }
            }
        }

        logInfo("Factory reset completed. Requesting restart.");
        getController().shutdownAndRequestRestart();
    }
}

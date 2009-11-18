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
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.FolderService;
import de.dal33t.powerfolder.disk.FileArchiver;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.GenericDialogType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;

/**
 * Dialog for restoring a selected file archive.
 */
public class RestoreArchiveDialog extends BaseDialog {

    private JButton okButton;
    private JPanel uiComponent;

    private FileInfo fileInfo;
    private FileInfo versionInfo;
    private JRadioButton restoreRB;
    private JRadioButton saveRB;
    private JLabel fileLocationLabel;
    private JTextField fileLocationField;
    private JButtonMini fileLocationButton;

    /**
     * Constructor
     * 
     * @param controller
     * @param fileInfo
     *            the original file
     * @param versionInfo
     *            the info of the file version to restore
     */
    public RestoreArchiveDialog(Controller controller, FileInfo fileInfo,
        FileInfo versionInfo)
    {
        super(controller, true);
        this.versionInfo = versionInfo;
        this.fileInfo = fileInfo;
    }

    protected JComponent getContent() {
        if (uiComponent == null) {

            restoreRB = new JRadioButton(Translation
                .getTranslation("dialog.restore_archive.restore"));
            saveRB = new JRadioButton(Translation
                .getTranslation("dialog.restore_archive.save"));
            ButtonGroup bg = new ButtonGroup();
            bg.add(restoreRB);
            bg.add(saveRB);

            // Layout
            FormLayout layout = new FormLayout(
                "pref, 3dlu, 122dlu, 3dlu, 15dlu, pref:grow",
                "pref, 3dlu, pref, 3dlu, pref");
            PanelBuilder builder = new PanelBuilder(layout);
            CellConstraints cc = new CellConstraints();

            fileLocationLabel = new JLabel(Translation
                .getTranslation("dialog.restore_archive.file_location"));
            fileLocationField = new JTextField();
            fileLocationField.setEnabled(false);
            fileLocationButton = new JButtonMini(Icons
                .getIconById(Icons.DIRECTORY), Translation
                .getTranslation("dialog.restore_archive.file_location.tip"));
            fileLocationButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showFileDialog();
                }
            });

            // Add components
            builder.add(restoreRB, cc.xyw(1, 1, 6));
            builder.add(saveRB, cc.xyw(1, 3, 6));
            builder.add(fileLocationLabel, cc.xy(1, 5));
            builder.add(fileLocationField, cc.xy(3, 5));
            builder.add(fileLocationButton, cc.xy(5, 5));

            restoreRB.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    enableComponents();
                }
            });
            saveRB.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    enableComponents();
                }
            });

            restoreRB.setSelected(true);
            enableComponents();
            uiComponent = builder.getPanel();
        }
        return uiComponent;
    }

    private void showFileDialog() {
        String dir = DialogFactory.chooseDirectory(getController(),
            fileLocationField.getText());
        fileLocationField.setText(dir);
        enableComponents();
    }

    protected Icon getIcon() {
        return null;
    }

    public String getTitle() {
        return Translation.getTranslation("dialog.restore_archive.title");
    }

    protected Component getButtonBar() {
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                restore();
            }
        });
        enableComponents();
        JButton cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    /**
     * Save / restore the archived file.
     */
    private void restore() {

        FolderRepository repo = getController().getFolderRepository();
        final Folder folder = repo.getFolder(versionInfo.getFolderInfo());
        final FileArchiver fileArchiver = folder.getFileArchiver();
        final File restoreTo;

        if (restoreRB.isSelected()) {
            restoreTo = versionInfo.getDiskFile(getController()
                .getFolderRepository());
        } else {
            restoreTo = new File(fileLocationField.getText(),
                    fileInfo.getFilenameOnly());
        }
        boolean restore = true;
        if (restoreTo.exists()) {
            // Check user is okay with overwriting the file.
            int result = DialogFactory.genericDialog(getController(),
                    Translation.getTranslation("dialog.restore_archive.overwrite_title"),
                    Translation.getTranslation("dialog.restore_archive.overwrite_message"),
                    new String[] {
                            Translation.getTranslation("dialog.restore_archive.overwrite"),
                            Translation.getTranslation("general.cancel")
                    }, 0, GenericDialogType.QUESTION);
            if (result != 0) {
                restore = false;
            }
        }
        if (restore) {
            getController().getThreadPool().execute(new Runnable() {
                public void run() {
                    // Run this outside of the EDT, it may take some time.
                    restore0(folder, fileArchiver, restoreTo);
                }
            });
        }
        close();
    }

    /**
     * Restore from the archiver, or failing that from online storage.
     *
     * @param folder
     * @param fileArchiver
     * @param restoreTo
     */
    private void restore0(Folder folder, FileArchiver fileArchiver,
                          File restoreTo) {
        try {
            boolean restored = false;
            if (fileArchiver.restore(versionInfo, restoreTo)) {
                logInfo("Restored from local archive");
                folder.scanChangedFile(versionInfo);
                restored = true;
            } else {
                // Not local. OnlineStorage perhaps?
                boolean online = folder.hasMember(getController()
                        .getOSClient().getServer());
                if (online) {
                    ServerClient client = getController().getOSClient();
                    if (client != null && client.isConnected()
                            && client.isLoggedIn()) {
                        FolderService service = client.getFolderService();
                        if (service != null) {
                            service.restore(versionInfo, true);
                            logInfo("Restored from OS archive");
                            restored = true;
                        }
                    }
                }
            }

            if (!restored) {
                throw new IOException("Restore failed");
            }
        } catch (IOException e) {
            logSevere(e);
            DialogFactory.genericDialog(getController(), Translation
                .getTranslation("dialog.restore_archive.title"), Translation
                .getTranslation("dialog.restore_archive.save_error", e
                    .getMessage()), GenericDialogType.ERROR);
        }
    }

    protected JButton getDefaultButton() {
        return okButton;
    }

    private void enableComponents() {
        boolean enabled = saveRB.isSelected();
        fileLocationLabel.setEnabled(enabled);
        fileLocationButton.setEnabled(enabled);
        if (okButton != null) {
            okButton.setEnabled(restoreRB.isSelected()
                || fileLocationField.getText().length() > 0);
        }
    }
}
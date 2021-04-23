/*
 * Copyright 2004 - 2015 Christian Sprajc. All rights reserved.
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

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.ConfigurationEntry;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.ui.dialog.DialogFactory;
import de.dal33t.powerfolder.ui.dialog.GenericDialogType;
import de.dal33t.powerfolder.ui.util.Help;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.ui.widget.JButtonMini;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.LinuxUtil;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;
import jwf.WizardPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;


@SuppressWarnings("serial")
public class FileSyncSetupPanel extends PFWizardPanel {
    private static final Logger LOG = Logger.getLogger(FileSyncSetupPanel.class.getName());
    private JRadioButton syncThisComRadioButton; // Sync To This Computer Radion Button
    private JRadioButton syncNetDriveRadioButton; // Sync To Network Drive Radion Button
    private JComponent locationField;
    private JTextField syncNetDriveField;
    private JTextField locationTF;
    private ValueModel locationModel;
    private WizardPanel nextFinishPanel;
    private ServerClient serverClient;
    private  String webDAVURL;
    private Date lastFetch;


    public FileSyncSetupPanel(Controller controller, ServerClient client, WizardPanel nextFinishPanel) {
        super(controller);
        Reject.ifNull(nextFinishPanel, "NextFinishPanel is null");
        this.serverClient = client;
        this.nextFinishPanel = nextFinishPanel;
    }

    @Override
    protected void afterDisplay() {
        super.afterDisplay();
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    @Override
    public WizardPanel next() {
        return nextFinishPanel;
    }

    @Override
    public boolean canCancel() {
        return false;
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    protected String getTitle() {
        return Translation.get("wizard.file_sync.title");
    }

    @Override
    protected void initComponents() {
        syncThisComRadioButton = new JRadioButton(Translation.get("wizard.file_sync.local"));
        syncNetDriveRadioButton = new JRadioButton(Translation.get("wizard.file_sync.network"));
        syncThisComRadioButton.setSelected(true);
        ButtonGroup group = new ButtonGroup();
        group.add(syncThisComRadioButton);
        group.add(syncNetDriveRadioButton);
        locationModel = new ValueHolder(getController().getFolderRepository().getFoldersBasedirString());
        locationModel.addValueChangeListener(evt -> updateLocationComponents());
        locationField = createLocationField();
        syncNetDriveField = new JTextField();
        syncNetDriveField.setName("networkDriveField");
        webDAVURL = ConfigurationEntry.SERVER_WEB_URL.getValue(getController()) + "/webdav";
        syncNetDriveField.setEditable(false);
        syncNetDriveField.setDisabledTextColor(Color.GRAY);
    }

    @Override
    protected JComponent buildContent() {
        FormLayout layout = new FormLayout("left:60dlu, 3dlu, 100dlu, 3dlu,100dlu", "pref");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        int row = 1;

        builder.add(syncThisComRadioButton, cc.xy(3, row));
        builder.add(syncNetDriveRadioButton, cc.xy(5, row));

        row += 2;
        builder.appendUnrelatedComponentsGapRow();
        builder.appendRow("pref");
        JLabel localDrive = new JLabel("Location");
        builder.add(localDrive, cc.xy(1, row));
        builder.add(locationField, cc.xyw(3, row, 3));
//        JLabel networkDriveLabel = new JLabel("Network Drive Url");
//        networkDriveLabel.setVisible(false);
//        builder.add(networkDriveLabel, cc.xy(1, row));
//        builder.add(syncNetDriveField, cc.xyw(3, row, 3));

        syncThisComRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                networkDriveLabel.setVisible(false);
                localDrive.setVisible(true);
                Component[] components = builder.getPanel().getComponents();
                for (Component component : components) {
                    String name = component.getName();
                    if (name != null) {
                        if (name.equals("networkDriveField")) {
                            component.setVisible(false);
                        }
                        if (name.equals("locationField")) {
                            component.setVisible(true);
//                                locationModel.setValue(getController().getMySelf().getNick());
                        }
                    }
                }
            }
        });
        syncNetDriveRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                networkDriveLabel.setVisible(true);
                localDrive.setVisible(false);
                Component[] components = builder.getPanel().getComponents();
                for (Component component : components) {
                    String name = component.getName();
                    if (name != null) {
//                        if (name.equals("networkDriveField")) {
//                            component.setVisible(true);
//                        }
                        createWebdavConnection();
                        if (name.equals("locationField")) {
                            component.setVisible(false);
                        }
                    }
                }

            }
        });
        return builder.getPanel();
    }

    private void createWebdavConnection() {
        ActivityVisualizationWorker worker = new ActivityVisualizationWorker(getController().getUIController())
        {
            protected String getTitle() {
                return Translation.get("exp_folder_view.webdav_title");
            }

            protected String getWorkingText() {
                return Translation.get("exp_folder_view.webdav_working_text");
            }

            public Object construct() throws Throwable {
                try {
//                    createWebDAVURL();

                    /* Handle different OSes */
                    if(OSUtil.isLinux()) {
                        return LinuxUtil.mountWebDAV(serverClient, webDAVURL);
                    } else {
                        WinUtils util = WinUtils.getInstance();

                        if (util != null) {
                            return util.mountWebDAV(serverClient, webDAVURL);
                        }
                    }

                    return 'N';
                } catch (Exception e) {
                    // Looks like the link failed, badly :-(
//                    logSevere(e.getMessage(), e);
                    return 'N' + e.getMessage();
                }
            }

            public void finished() {

                // See what happened.
                String result = (String) get();
                if (result != null) {
                    if (result.startsWith("Y")) {
                        String[] parts = result.substring(1).split("\\s");
                        for (final String part : parts) {
                            if (part.length() == 2 && part.charAt(1) == ':') {
                                // Probably the new drive name, so open it.
                                getController().getIOProvider().startIO(
                                        new Runnable() {
                                            public void run() {
                                                PathUtils.openFile(Paths.get(part));
                                            }
                                        });

                                break;
                            }
                        }
                    } else if (result.startsWith("N")) {
                        String[] ops;
                        if (Help.hasWiki(getController())) {
                            ops = new String[]{
                                    Translation.get("general.ok"),
                                    Translation.get("general.help")};
                        } else {
                            ops = new String[]{Translation
                                    .get("general.ok")};
                        }
                        int op = DialogFactory
                                .genericDialog(
                                        getController(),
                                        Translation
                                                .get("exp_folder_view.webdav_failure_title"),
                                        Translation.get(
                                                "exp_folder_view.webdav_failure_text",
                                                result.substring(1)), ops, 0,
                                        GenericDialogType.ERROR);
                        if (op == 1) {
                            Help.openWikiArticle(getController(),
                                    WikiLinks.WEBDAV);
                        }
                    }
                }
            }
        };
        worker.start();
    }
//    private synchronized String createWebDAVURL() {
//        if (!serverClient.isConnected() || !serverClient.isLoggedIn()) {
//            return null;
//        }
//        if (webDAVURL == null) {
//            webDAVURL = serverClient.getFolderService(folderInfo).getWebDAVURL(folderInfo);
//            if (webDAVURL == null) {
//                // Don't fetch again. It's simply not available.
//                webDAVURL = "";
//            }
//        }
//        return webDAVURL;
//    }
    /**
     * Creates a pair of location text field and button.
     *
     * @return The component containing a text field and button.
     */
    private JComponent createLocationField() {
        FormLayout layout = new FormLayout("190dlu, 3dlu, pref", "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        locationTF = new JTextField();
        locationTF.setEditable(false);
        locationTF.setText((String) locationModel.getValue());
        builder.add(locationTF, cc.xy(1, 1));

        JButton locationButton = new JButtonMini(
                Icons.getIconById(Icons.DIRECTORY),
                Translation
                        .get("exp.preferences.expert.select_directory_text"));
        locationButton.addActionListener(new MyActionListener());
        builder.add(locationButton, cc.xy(3, 1));
        builder.getPanel().setName("locationField");
        return builder.getPanel();
    }

    /**
     * Action listener for the location button. Opens a choose dir dialog and
     * sets the location model with the result.
     */
    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String initial = (String) locationModel.getValue();
            List<Path> files = DialogFactory.chooseDirectory(getController()
                    .getUIController(), initial, false);
            if (!files.isEmpty()) {
                Path newLocation = files.get(0);
                // Make sure that the user is not setting this to the base dir
                // of an existing folder.
                for (Folder folder : getController().getFolderRepository()
                        .getFolders(true)) {
                    if (folder.getLocalBase().equals(newLocation)) {
                        DialogFactory.genericDialog(getController(),
                                Translation.get("exp.preferences.expert.duplicate_local_base_title"),
                                Translation.get("exp.preferences.expert.duplicate_local_base_message", folder.getName()),
                                GenericDialogType.ERROR);
                        return;
                    }
                }
                locationModel.setValue(newLocation.toAbsolutePath().toString());
            }
        }
    }

    /**
     * Called when the location model changes value. Sets the location text
     * field value and enables the location button.
     */
    private void updateLocationComponents() {
        String value = (String) locationModel.getValue();
        locationTF.setText(value);
    }
}

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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.util.Translation;

/**
 * The Sync action panel. User can input his sync actions. e.g. scan. scan &
 * download. Now used only if syncprofile is Project work
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.3 $
 */
public class SyncFolderDialog extends BaseDialog {

    private JButton okButton;
    private static final Object SEND_OPTION = new Object();
    private static final Object RECEIVE_OPTION = new Object();
    private static final Object SEND_RECEIVE_OPTION = new Object();
    private Folder folder;
    private JComponent sendChangesButton;
    private JComponent receiveChangesButton;
    private JComponent sendAndReceiveChangesButton;
    private ValueModel optionModel;

    public SyncFolderDialog(Controller controller, Folder folder) {
        // Modal dialog
        super(Senior.NONE, controller, true);
        this.folder = folder;

    }

    // Application logic ******************************************************

    /**
     * Performs the choosen sync options
     */
    private void performSync() {
        de.dal33t.powerfolder.ui.util.SwingWorker worker = new SyncFolderWorker();
        worker.start();
    }

    // Methods for BaseDialog *************************************************

    public String getTitle() {
        return Translation.get("dialog.synchronization.title");
    }

    protected Icon getIcon() {
        return null;
    }

    protected JComponent getContent() {
        // Init
        initComponents();

        FormLayout layout = new FormLayout("pref",
            "pref, 6dlu, pref, 6dlu, pref, pref, pref, 6dlu");
        PanelBuilder builder = new PanelBuilder(layout);

        CellConstraints cc = new CellConstraints();

        builder.addLabel(
            Translation.get("dialog.synchronization.choose"),
            cc.xy(1, 1));
        // Add iconed label
        builder.addLabel(folder.getName(), cc.xy(1, 3));

        builder.add(sendChangesButton, cc.xy(1, 5));
        builder.add(receiveChangesButton, cc.xy(1, 6));
        builder.add(sendAndReceiveChangesButton, cc.xy(1, 7));

        return builder.getPanel();
    }

    protected Component getButtonBar() {
        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                performSync();
                // remove window
                close();
            }
        });

        JButton cancelButton = createCancelButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Don't want to be notified when the scan completes.
                getController().getUIController().getApplicationModel()
                    .getFolderRepositoryModel()
                    .removeInterestedFolderInfo(folder.getInfo());
                close();
            }
        });

        return ButtonBarFactory.buildCenteredBar(okButton, cancelButton);
    }

    protected JButton getDefaultButton() {
        return okButton;
    }

    // UI Building ************************************************************

    /**
     * Initalizes all ui components
     */
    private void initComponents() {
        // Default: Send
        optionModel = new ValueHolder(SEND_RECEIVE_OPTION);

        sendChangesButton = BasicComponentFactory.createRadioButton(
            optionModel, SEND_OPTION, Translation
                .get("dialog.synchronization.send_own_changes"));

        receiveChangesButton = BasicComponentFactory.createRadioButton(
            optionModel, RECEIVE_OPTION, Translation
                .get("dialog.synchronization.receive_changes"));

        sendAndReceiveChangesButton = BasicComponentFactory
            .createRadioButton(
                optionModel,
                SEND_RECEIVE_OPTION,
                Translation
                    .get("dialog.synchronization.send_and_receive_changes"));
    }

    // Working classes ********************************************************

    private final class SyncFolderWorker extends ActivityVisualizationWorker {
        private SyncFolderWorker() {
            super(getUIController());
        }

        @Override
        protected String getTitle() {
            return Translation
                .get("dialog.synchronization.sychronizing");
        }

        @Override
        protected String getWorkingText() {
            return Translation
                .get("dialog.synchronization.sychronizing");
        }

        @Override
        public Object construct() {
            // Force scan on folder (=send)
            if (optionModel.getValue() == SEND_OPTION
                || optionModel.getValue() == SEND_RECEIVE_OPTION)
            {
                logInfo(folder + ": Performing send/scan");
                folder.scanLocalFiles();
            }

            if (optionModel.getValue() == RECEIVE_OPTION
                || optionModel.getValue() == SEND_RECEIVE_OPTION)
            {
                logInfo(folder + ": Performing receive");
                // Perform remote deltions
                folder.syncRemoteDeletedFiles(true);
                // Request ALL files now modified.
                getController().getFolderRepository().getFileRequestor()
                    .requestMissingFiles(folder, false);
            }
            return null;
        }
    }

}
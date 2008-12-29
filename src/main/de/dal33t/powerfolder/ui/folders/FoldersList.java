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
 * $Id: FoldersList.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.folders;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;

import javax.swing.*;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class creates a list combining folder repository and server client
 * folders. Folder repo events are triggered by a listener. Server client
 * changes are detected by a thread that polls the client on a regular basis.
 */
public class FoldersList extends PFUIComponent {

    private static final long DELAY = 10000; // Ten seconds
    private static final long PERIOD = 10000; // Ten seconds

    private JPanel uiComponent;
    private JPanel folderListPanel;
    private final List<ExpandableFolderView> views;
    private final FolderRepository repo;
    private final ServerClient client;

    /**
     * Constructor
     *
     * @param controller
     */
    public FoldersList(Controller controller) {
        super(controller);
        views = new CopyOnWriteArrayList<ExpandableFolderView>();
        repo = getController().getFolderRepository();
        client = getController().getOSClient();
    }

    /**
     * Gets the UI component.
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        return uiComponent;
    }

    /**
     * Inits the components then builds UI.
     */
    private void buildUI() {

        initComponents();

        // Build ui
        FormLayout layout = new FormLayout("pref:grow",
            "pref, pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(folderListPanel, cc.xy(1, 1));
        uiComponent = builder.getPanel();
    }

    /**
     * Initializes the compoonents and starts things going.
     */
    private void initComponents() {
        folderListPanel = new JPanel();
        folderListPanel.setLayout(new BoxLayout(folderListPanel, BoxLayout.PAGE_AXIS));
        updateFolders();
        registerListeners();
        startServerClientTask();
    }

    /**
     * Starts a thread to poll the server clinet for folder changes.
     */
    private void startServerClientTask() {
        getController().scheduleAndRepeat(new ServerClientTask(), DELAY, PERIOD);
    }

    /**
     * Adds a listener for folder repository changes.
     */
    private void registerListeners() {
        getController().getFolderRepository().addFolderRepositoryListener(
                new MyFolderRepositoryListener());
    }

    /**
     * Makes sure that the folder views are correct for folder repositoy and
     * server client folders.
     */
    private void updateFolders() {

        // Get combined list of repo and client folders.
        List<Folder> folders = client.getJoinedFolders();
        folders.addAll(repo.getFoldersAsCollection());

        // Add new folder views if required.
        for (Folder folder : folders) {
            boolean exists = false;
            for (ExpandableFolderView view : views) {
                if (view.getFolderInfo().equals(folder.getInfo())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                // No view for this folder, so create a new one.
                ExpandableFolderView newView = new ExpandableFolderView(getController(), folder);
                folderListPanel.add(newView.getUIComponent());
                views.add(newView);
            }
        }

        // Remove old folder views if required.
        ExpandableFolderView[] list = views.toArray(new ExpandableFolderView[views.size()]);
        for (ExpandableFolderView view : list) {
            boolean exists = false;
            for (Folder folder : folders) {
                if (folder.getInfo().equals(view.getFolderInfo())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                // No folder for this view, so remove it.
                views.remove(view);
                folderListPanel.remove(view.getUIComponent());
            }
        }
    }

    /**
     * Listener for changes to folder repository folder set.
     */
    private class MyFolderRepositoryListener implements FolderRepositoryListener {

        public void folderRemoved(FolderRepositoryEvent e) {
            updateFolders();
        }

        public void folderCreated(FolderRepositoryEvent e) {
            updateFolders();
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    /**
     * Timer task to poll the server client for changes.
     */
    private class ServerClientTask extends TimerTask {

        public void run() {
            updateFolders();
        }
    }
}

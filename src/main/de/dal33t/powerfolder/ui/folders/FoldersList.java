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
import com.jgoodies.binding.value.ValueModel;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.*;

import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * This class creates a list combining folder repository and server client
 * folders.
 */
public class FoldersList extends PFUIComponent {

    private final List<ExpandableFolderView> views;

    private JPanel uiComponent;
    private JPanel folderListPanel;
    private FolderRepository repo;
    private ServerClient client;
    private JScrollPane scrollPane;
    private Integer folderSelectionType;
    private ExpansionListener expansionListener;

    /**
     * Constructor
     *
     * @param controller
     */
    public FoldersList(Controller controller, ValueModel folderSelectionTypeVM) {
        super(controller);

        expansionListener = new MyExpansionListener();

        folderSelectionTypeVM.addValueChangeListener(new MyPropertyChangeListener());
        folderSelectionType = (Integer) folderSelectionTypeVM.getValue();

        views = new CopyOnWriteArrayList<ExpandableFolderView>();

        buildUI();
    }

    /**
     * Gets the UI component.
     * @return
     */
    public JPanel getUIComponent() {
        return uiComponent;
    }

    /**
     * Inits the components then builds UI.
     */
    private void buildUI() {

        repo = getController().getFolderRepository();
        client = getController().getOSClient();

        folderListPanel = new JPanel();
        folderListPanel.setLayout(new BoxLayout(folderListPanel, BoxLayout.PAGE_AXIS));
        registerListeners();
        updateFolders();

        // Build ui
        FormLayout layout = new FormLayout("pref:grow",
            "pref, pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(folderListPanel, cc.xy(1, 1));
        uiComponent = builder.getPanel();
    }

    /**
     * Adds a listener for folder repository changes.
     */
    private void registerListeners() {
        getController().getFolderRepository().addFolderRepositoryListener(
                new MyFolderRepositoryListener());
        getController().getOSClient().addListener(new MyServerClientListener());
    }

    /**
     * Makes sure that the folder views are correct for folder repositoy and
     * server client folders.
     */
    private void updateFolders() {

        synchronized(views) {

            // Get combined list of repo and client folders.
            List<FolderBean> folderBeanList = new ArrayList<FolderBean>();

            for (Folder folder : repo.getFoldersAsCollection()) {
                FolderInfo folderInfo = folder.getInfo();
                FolderBean bean = new FolderBean(folderInfo);
                bean.setFolder(folder);
                bean.setLocal(true);
                folderBeanList.add(bean);
            }

            for (FolderInfo folderInfo : client.getOnlineFolders()) {
                FolderBean bean = new FolderBean(folderInfo);
                if (folderBeanList.contains(bean)) {
                    for (FolderBean existingBean : folderBeanList) {
                        if (existingBean.getFolderInfo().equals(folderInfo)) {
                            existingBean.setOnline(true);
                        }
                    }
                } else {
                    bean.setOnline(true);
                    folderBeanList.add(bean);
                }
            }

            // Filter on selection.
            for (Iterator<FolderBean> iter = folderBeanList.iterator(); iter.hasNext();) {
                FolderBean bean = iter.next();
                if (folderSelectionType == FoldersTab.FOLDER_TYPE_LOCAL &&
                        !bean.isLocal()) {
                    iter.remove();
                } else if (folderSelectionType == FoldersTab.FOLDER_TYPE_ONLINE &&
                        !bean.isOnline()) {
                    iter.remove();
                }
            }

            // Add new folder views if required.
            for (FolderBean folderBean : folderBeanList) {
                boolean exists = false;
                for (ExpandableFolderView view : views) {
                    if (view.getFolderInfo().equals(folderBean.getFolderInfo())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    // No view for this folder info, so create a new one.
                    ExpandableFolderView newView = new ExpandableFolderView(
                            getController(), folderBean.getFolderInfo());
                    newView.configure(folderBean.getFolder(),                            
                        folderBean.isLocal(), folderBean.isOnline());
                    folderListPanel.add(newView.getUIComponent());
                    folderListPanel.invalidate();
                    if (uiComponent != null) {
                        uiComponent.invalidate();
                    }
                    if (scrollPane != null) {
                        scrollPane.repaint();
                    }
                    views.add(newView);
                    newView.addExpansionListener(expansionListener);
                }
            }

            // Remove old folder views if required.
            // Update remaining views with the current folder, which may be null if
            // online only.
            ExpandableFolderView[] list = views.toArray(new ExpandableFolderView[views.size()]);
            for (ExpandableFolderView view : list) {
                boolean exists = false;
                for (FolderBean folderBean : folderBeanList) {
                    if (folderBean.getFolderInfo().equals(view.getFolderInfo())) {
                        exists = true;
                        view.configure(folderBean.getFolder(),
                            folderBean.isLocal(), folderBean.isOnline());
                        break;
                    }
                }
                if (!exists) {
                    // No folder info for this view, so remove it.
                    views.remove(view);
                    view.removeExpansionListener(expansionListener);
                    view.unregisterListeners();
                    folderListPanel.remove(view.getUIComponent());
                    folderListPanel.invalidate();
                    if (uiComponent != null) {
                        uiComponent.invalidate();
                    }
                    if (scrollPane != null) {
                        scrollPane.repaint();
                    }
                }
            }
        }
    }

    /**
     * Requird to make scroller repaint correctly.
     *
     * @param scrollPane
     */
    public void setScroller(JScrollPane scrollPane) {
        this.scrollPane = scrollPane;
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
     * Listener for changes to server client folder set.
     */
    private class MyServerClientListener implements ServerClientListener {

        public void login(ServerClientEvent event) {
            updateFolders();
        }

        public void accountUpdated(ServerClientEvent event) {
            updateFolders();
        }

        public void serverConnected(ServerClientEvent event) {
            updateFolders();
        }

        public void serverDisconnected(ServerClientEvent event) {
            updateFolders();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyPropertyChangeListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            Object object = evt.getNewValue();
            folderSelectionType = (Integer) object;
            updateFolders();
        }
    }

    private class FolderBean {

        private final FolderInfo folderInfo;
        private Folder folder;
        private boolean local;
        private boolean online;

        private FolderBean(FolderInfo folderInfo) {
            this.folderInfo = folderInfo;
        }

        public FolderInfo getFolderInfo() {
            return folderInfo;
        }

        public Folder getFolder() {
            return folder;
        }

        public boolean isLocal() {
            return local;
        }

        public boolean isOnline() {
            return online;
        }

        public void setFolder(Folder folder) {
            this.folder = folder;
        }

        public void setLocal(boolean local) {
            this.local = local;
        }

        public void setOnline(boolean online) {
            this.online = online;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            FolderBean that = (FolderBean) obj;

            if (!folderInfo.equals(that.folderInfo)) {
                return false;
            }

            return true;
        }

        public int hashCode() {
            return folderInfo.hashCode();
        }
    }

    /**
     * Expansion listener to collapse all other views.
     */
    private class MyExpansionListener implements ExpansionListener {

        public void collapseAllButSource(ExpansionEvent e) {
            synchronized (views) {
                for (ExpandableFolderView view : views) {
                    if (!view.equals(e.getSource())) {
                        // Not source, so collapse.
                        view.collapse();
                    }
                }
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }
}

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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.ExpansionEvent;
import de.dal33t.powerfolder.event.ExpansionListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.light.FolderInfo;

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
    private FoldersTab foldersTab;
    private volatile boolean populated;

    /**
     * Constructor
     *
     * @param controller
     */
    public FoldersList(Controller controller, FoldersTab foldersTab,
                       ValueModel folderSelectionTypeVM) {
        super(controller);
        this.foldersTab = foldersTab;
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

        // Build ui
        FormLayout layout = new FormLayout("pref:grow",
            "pref, pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(folderListPanel, cc.xy(1, 1));
        uiComponent = builder.getPanel();

        updateFolders();
    }

    public boolean isEmpty() {
        return views.isEmpty();
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

        // Do nothing until the populate command is received.
        if (!populated) {
            return;
        }

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

            // Only process OS if USE_ONLINE_STORAGE configured.  
            if (PreferencesEntry.USE_ONLINE_STORAGE.getValueBoolean(getController())) {
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
            }

            FolderInfo expandedFolderInfo = null;
            for (ExpandableFolderView view : views) {
                if (view.isExpanded()) {
                    expandedFolderInfo = view.getFolderInfo();
                    break; // There can only be one expanded view.
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

            // Remove old folder views.
            // Update remaining views with the current folder, which may be null
            // if online only.
            ExpandableFolderView[] list = views
                .toArray(new ExpandableFolderView[views.size()]);
            for (ExpandableFolderView view : list) {
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

            // Sort by name
            Collections.sort(folderBeanList, FolderBeanComparator.INSTANCE);
            
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

                    // Was view expanded before?
                    if (expandedFolderInfo != null && folderBean.getFolderInfo()
                            .equals(expandedFolderInfo)) {
                        newView.expand();
                    }
                    newView.addExpansionListener(expansionListener);
                }
            }
        }
        foldersTab.updateEmptyLabel();
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
     * Enable the updateFolders method so that views get processed.
     * This is done so views do not get added before Synthetica has set all the
     * colors, else views look different before and after.
     */
    public void populate() {
        populated = true;
        updateFolders();
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
            return folderInfo.equals(that.folderInfo);
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
    
    private static class FolderBeanComparator implements Comparator<FolderBean> {

        private static final FolderBeanComparator INSTANCE = new FolderBeanComparator();

        private FolderBeanComparator() {
        }

        public int compare(FolderBean o1, FolderBean o2) {
            return o1.getFolderInfo().name.compareToIgnoreCase(o2
                .getFolderInfo().name);
        }
    }
}

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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.disk.problem.ProblemListener;
import de.dal33t.powerfolder.event.ExpansionEvent;
import de.dal33t.powerfolder.event.ExpansionListener;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.TransferManagerAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.FolderCreatePermission;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.model.BoundPermission;
import de.dal33t.powerfolder.ui.widget.GradientPanel;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.UserDirectories;
import de.dal33t.powerfolder.util.ui.DelayedUpdater;

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
    private ExpansionListener expansionListener;
    private FolderMembershipListener membershipListener;
    private FoldersTab foldersTab;
    private boolean empty;
    private volatile boolean populated;

    private boolean collapseLocal;
    private JLabel localLabel;
    private JLabel localIcon;

    private boolean showTypical;
    private boolean collapseTypical;
    private JLabel typicalLabel;
    private JLabel typicalIcon;

    private boolean collapseOnline;
    private JLabel onlineLabel;
    private JLabel onlineIcon;

    private DelayedUpdater transfersUpdater;
    private DelayedUpdater foldersUpdater;
    private BoundPermission folderCreatePermission;

    /**
     * Constructor
     * 
     * @param controller
     */
    public FoldersList(Controller controller, FoldersTab foldersTab) {
        super(controller);
        empty = true;
        showTypical = PreferencesEntry.SHOW_TYPICAL_FOLDERS
            .getValueBoolean(getController());
        this.foldersTab = foldersTab;
        collapseLocal = PreferencesEntry.FOLDER_LOCAL_COLLAPSED
            .getValueBoolean(getController());
        collapseTypical = PreferencesEntry.FOLDER_TYPICAL_COLLAPSED
            .getValueBoolean(getController());
        collapseOnline = PreferencesEntry.FOLDER_ONLINE_COLLAPSED.
                getValueBoolean(getController());
        transfersUpdater = new DelayedUpdater(getController());
        foldersUpdater = new DelayedUpdater(getController());
        expansionListener = new MyExpansionListener();
        membershipListener = new MyFolderMembershipListener();

        views = new CopyOnWriteArrayList<ExpandableFolderView>();

        localLabel = new JLabel(Translation
            .getTranslation("folders_list.local_folders"));
        localIcon = new JLabel(Icons.getIconById(Icons.EXPAND));
        localLabel.addMouseListener(new LocalListener());
        localIcon.addMouseListener(new LocalListener());

        typicalLabel = new JLabel(Translation
            .getTranslation("folders_list.typical_folders"));
        typicalIcon = new JLabel(Icons.getIconById(Icons.COLLAPSE));
        typicalLabel.addMouseListener(new TypicalListener());
        typicalIcon.addMouseListener(new TypicalListener());

        onlineLabel = new JLabel(Translation
            .getTranslation("folders_list.online_folders"));
        onlineIcon = new JLabel(Icons.getIconById(Icons.COLLAPSE));
        onlineLabel.addMouseListener(new OnlineListener());
        onlineIcon.addMouseListener(new OnlineListener());
        buildUI();
        getController().getTransferManager().addListener(
            new MyTransferManagerListener());

        folderCreatePermission = new BoundPermission(getController(),
            FolderCreatePermission.INSTANCE)
        {
            @Override
            public void hasPermission(boolean hasPermission) {
                showTypical = hasPermission
                    && PreferencesEntry.SHOW_TYPICAL_FOLDERS
                        .getValueBoolean(getController());
                updateFolders();
            }
        };
    }

    /**
     * Gets the UI component.
     * 
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
        folderListPanel.setLayout(new BoxLayout(folderListPanel,
            BoxLayout.PAGE_AXIS));
        registerListeners();

        // Build ui
        FormLayout layout = new FormLayout("pref:grow", "pref, pref:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(folderListPanel, cc.xy(1, 1));
        uiComponent = GradientPanel.create(builder.getPanel());

        updateFolders();
    }

    public boolean isEmpty() {
        return empty;
    }

    /**
     * Adds a listener for folder repository changes.
     */
    private void registerListeners() {
        getController().getFolderRepository().addProblemListenerToAllFolders(
            new MyProblemListener());
        getController().getFolderRepository().addFolderRepositoryListener(
            new MyFolderRepositoryListener());
        for (Folder folder : getController().getFolderRepository().getFolders())
        {
            folder.addMembershipListener(membershipListener);
        }
        getController().getOSClient().addListener(new MyServerClientListener());
    }

    private void updateFolders() {
        foldersUpdater.schedule(new Runnable() {
            public void run() {
                updateFolders0();
            }
        });
    }

    /**
     * Makes sure that the folder views are correct for folder repositoy and
     * server client folders.
     */
    private void updateFolders0() {

        // Do nothing until the populate command is received.
        if (!populated) {
            return;
        }
        if (getController().isShuttingDown()) {
            return;
        }

        // Get combined list of repo and account folders.
        List<ExpandableFolderModel> localFolders = new ArrayList<ExpandableFolderModel>();
        List<ExpandableFolderModel> typicalFolders = new ArrayList<ExpandableFolderModel>();
        List<ExpandableFolderModel> onlineFolders = new ArrayList<ExpandableFolderModel>();

        for (Folder folder : repo.getFolders()) {
            FolderInfo folderInfo = folder.getInfo();
            ExpandableFolderModel bean = new ExpandableFolderModel(
                    ExpandableFolderModel.Type.Local, folderInfo, folder,
                    getController().getOSClient().joinedByCloud(folder));
            localFolders.add(bean);
        }
        Collections.sort(localFolders, FolderBeanComparator.INSTANCE);
       
        for (FolderInfo folderInfo : client.getAccountFolders()) {
            ExpandableFolderModel bean = new ExpandableFolderModel(
                ExpandableFolderModel.Type.CloudOnly, folderInfo, null, true);
            if (localFolders.contains(bean)) {
                continue;
            }
            onlineFolders.add(bean);
        }
        Collections.sort(onlineFolders, FolderBeanComparator.INSTANCE);
        
        if (showTypical) {
            boolean showAppData = PreferencesEntry.ADVANCED_MODE
                .getValueBoolean(getController());
            for (String name : UserDirectories.getUserDirectoriesFiltered(
                getController(), showAppData).keySet())
            {
                FolderInfo folderInfo = new FolderInfo(name,
                    '[' + IdGenerator.makeId() + ']');
                ExpandableFolderModel bean = new ExpandableFolderModel(
                    ExpandableFolderModel.Type.Typical, folderInfo, null, false);

                if (!localFolders.contains(bean)
                    && !onlineFolders.contains(bean))
                {
                    typicalFolders.add(bean);
                }
            }
            Collections.sort(typicalFolders, FolderBeanComparator.INSTANCE);
        }
        
        

        empty = onlineFolders.isEmpty() && typicalFolders.isEmpty() &&
                localFolders.isEmpty();

        synchronized (views) {
            FolderInfo expandedFolderInfo = null;
            for (ExpandableFolderView view : views) {
                if (view.isExpanded()) {
                    expandedFolderInfo = view.getFolderInfo();
                    break; // There can only be one expanded view.
                }
            }

            // Remove all folder views.
            for (ExpandableFolderView view : views) {
                views.remove(view);
                view.removeExpansionListener(expansionListener);
                view.unregisterListeners();
            }
            folderListPanel.removeAll();
            if (uiComponent != null) {
                uiComponent.invalidate();
            }
            if (scrollPane != null) {
                scrollPane.repaint();
            }

            // Add new folder views.
            addSeparator(collapseLocal, localIcon, localLabel, false);
            if (!collapseLocal) {
                for (ExpandableFolderModel folderBean : localFolders) {
                    addView(folderBean, expandedFolderInfo);
                }
            }

            addSeparator(collapseOnline, onlineIcon, onlineLabel, true);

            if (!collapseOnline) {
                for (ExpandableFolderModel folderBean : onlineFolders) {
                    addView(folderBean, expandedFolderInfo);
                }
            }

            if (showTypical) {
                addSeparator(collapseTypical, typicalIcon, typicalLabel, true);

                if (!collapseTypical) {
                    for (ExpandableFolderModel folderBean : typicalFolders) {
                        addView(folderBean, expandedFolderInfo);
                    }
                }
            }

        }
        foldersTab.updateEmptyLabel();
    }

    private void addSeparator(boolean collapsed, JLabel icon, JLabel label, boolean showSeparator) {
        FormLayout layout = new FormLayout(
            "3dlu, pref, 3dlu, pref, 3dlu, pref:grow, 3dlu", "pref, 4dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        icon.setIcon(collapsed ? Icons.getIconById(Icons.EXPAND) : Icons
            .getIconById(Icons.COLLAPSE));
        icon.setToolTipText(collapsed ? Translation
            .getTranslation("folders_list.expand_hint") : Translation
            .getTranslation("folders_list.collapse_hint"));
        label.setToolTipText(collapsed ? Translation
            .getTranslation("folders_list.expand_hint") : Translation
            .getTranslation("folders_list.collapse_hint"));
        builder.add(icon, cc.xy(2, 1));
        builder.add(label, cc.xy(4, 1));
        if (showSeparator) {
            builder.add(new JSeparator(), cc.xy(6, 1));
        }
        JPanel panel = builder.getPanel();
        panel.setOpaque(false);
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        folderListPanel.add(panel);
    }

    private void addView(ExpandableFolderModel folderBean, FolderInfo expandedFolderInfo) {
        ExpandableFolderView newView = new ExpandableFolderView(
            getController(), folderBean.getFolderInfo());
        newView.configure(folderBean);
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
        if (expandedFolderInfo != null
            && folderBean.getFolderInfo().equals(expandedFolderInfo))
        {
            newView.expand();
        }
        newView.addExpansionListener(expansionListener);
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
     * Enable the updateFolders method so that views get processed. This is done
     * so views do not get added before Synthetica has set all the colors, else
     * views look different before and after.
     */
    public void populate() {
        populated = true;
        updateFolders();
    }

    /**
     * Update all views with its folder's problems.
     */
    private void updateProblems() {
        for (ExpandableFolderView view : views) {
            view.updateProblems();
        }
    }

    public void storeValues() {
        PreferencesEntry.FOLDER_LOCAL_COLLAPSED.setValue(getController(),
                collapseLocal);
        PreferencesEntry.FOLDER_TYPICAL_COLLAPSED.setValue(getController(),
                collapseTypical);
        PreferencesEntry.FOLDER_ONLINE_COLLAPSED.setValue(getController(),
                collapseOnline);
    }

    // /////////////////
    // Inner classes //
    // /////////////////

    /**
     * Listener for changes to folder repository folder set.
     */
    private class MyFolderRepositoryListener implements
        FolderRepositoryListener
    {

        public void folderRemoved(FolderRepositoryEvent e) {
            updateFolders();
            e.getFolder().removeMembershipListener(membershipListener);
        }

        public void folderCreated(FolderRepositoryEvent e) {
            updateFolders();
            e.getFolder().addMembershipListener(membershipListener);
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
        
        public void nodeServerStatusChanged(ServerClientEvent event) {
            updateFolders();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyFolderMembershipListener implements
        FolderMembershipListener
    {

        public void memberJoined(FolderMembershipEvent folderEvent) {
            if (getController().getOSClient().isCloudServer(
                folderEvent.getMember()))
            {
                updateFolders();
            }
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            if (getController().getOSClient().isCloudServer(
                folderEvent.getMember()))
            {
                updateFolders();
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
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

    private static class FolderBeanComparator implements Comparator<ExpandableFolderModel>
    {

        private static final FolderBeanComparator INSTANCE = new FolderBeanComparator();

        public int compare(ExpandableFolderModel o1, ExpandableFolderModel o2) {
            return o1.getFolderInfo().name.compareToIgnoreCase(o2
                .getFolderInfo().name);
        }
    }

    private class MyProblemListener implements ProblemListener {
        public void problemAdded(Problem problem) {
            updateProblems();
        }

        public void problemRemoved(Problem problem) {
            updateProblems();
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class LocalListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            collapseLocal = !collapseLocal;
            updateFolders();
        }
    }

    private class TypicalListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            collapseTypical = !collapseTypical;
            updateFolders();
        }
    }

    private class OnlineListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            collapseOnline = !collapseOnline;
            updateFolders();
        }
    }

    private class MyTransferManagerListener extends TransferManagerAdapter {

        private void notifyView(TransferManagerEvent event) {
            final FileInfo fileInfo = event.getFile();
            transfersUpdater.schedule(new Runnable() {
                public void run() {
                    FolderInfo folderInfo = fileInfo.getFolderInfo();
                    synchronized (views) {
                        for (ExpandableFolderView view : views) {
                            if (view.getFolderInfo().equals(folderInfo)) {
                                view.updateNameLabel();
                                break;
                            }
                        }
                    }
                }
            });
        }

        public void completedDownloadRemoved(TransferManagerEvent event) {
            notifyView(event);
        }

        public void downloadCompleted(TransferManagerEvent event) {
            notifyView(event);
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

}

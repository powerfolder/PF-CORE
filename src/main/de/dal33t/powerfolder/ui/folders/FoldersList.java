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

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.*;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PreferencesEntry;
import de.dal33t.powerfolder.clientserver.ServerClient;
import de.dal33t.powerfolder.clientserver.ServerClientEvent;
import de.dal33t.powerfolder.clientserver.ServerClientListener;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.problem.Problem;
import de.dal33t.powerfolder.disk.problem.ProblemListener;
import de.dal33t.powerfolder.event.FolderMembershipEvent;
import de.dal33t.powerfolder.event.FolderMembershipListener;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;
import de.dal33t.powerfolder.event.TransferManagerAdapter;
import de.dal33t.powerfolder.event.TransferManagerEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.security.FolderCreatePermission;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.event.ExpansionEvent;
import de.dal33t.powerfolder.ui.event.ExpansionListener;
import de.dal33t.powerfolder.ui.folders.ExpandableFolderModel.Type;
import de.dal33t.powerfolder.ui.model.BoundPermission;
import de.dal33t.powerfolder.ui.util.DelayedUpdater;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.UserDirectories;

/**
 * This class creates a list combining folder repository and server client
 * folders.
 */
public class FoldersList extends PFUIComponent {

    private final List<ExpandableFolderView> views;

    private JPanel uiComponent;
    private JPanel folderListPanel;
    private FolderRepository repository;
    private ServerClient client;
    private JScrollPane scrollPane;
    private ExpansionListener expansionListener;
    private FolderMembershipListener membershipListener;
    private FoldersTab foldersTab;
    private boolean empty;
    private volatile boolean populated;

    private boolean showTypical;

    private DelayedUpdater transfersUpdater;
    private DelayedUpdater foldersUpdater;
    @SuppressWarnings("unused")
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
        transfersUpdater = new DelayedUpdater(getController());
        foldersUpdater = new DelayedUpdater(getController());
        expansionListener = new MyExpansionListener();
        membershipListener = new MyFolderMembershipListener();

        views = new CopyOnWriteArrayList<ExpandableFolderView>();

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

        repository = getController().getFolderRepository();
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
        uiComponent = builder.getPanel();
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
     * Makes sure that the folder views are correct for folder repository and
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

        for (Folder folder : repository.getFolders()) {
            FolderInfo folderInfo = folder.getInfo();
            ExpandableFolderModel bean = new ExpandableFolderModel(
                Type.Local, folderInfo, folder,
                getController().getOSClient().joinedByCloud(folder));
            localFolders.add(bean);
        }

        for (FolderInfo folderInfo : client.getAccountFolders()) {
            ExpandableFolderModel bean = new ExpandableFolderModel(
                Type.CloudOnly, folderInfo, null, true);
            if (localFolders.contains(bean)) {
                continue;
            }
            localFolders.add(bean);
        }

        if (showTypical) {
            boolean showAppData = PreferencesEntry.EXPERT_MODE
                .getValueBoolean(getController());
            for (String name : UserDirectories.getUserDirectoriesFiltered(
                getController(), showAppData).keySet())
            {
                FolderInfo folderInfo = new FolderInfo(name,
                    IdGenerator.makeFolderId());
                ExpandableFolderModel bean = new ExpandableFolderModel(
                    Type.Typical, folderInfo, null, false);

                if (!localFolders.contains(bean)) {
                    localFolders.add(bean);
                }
            }
        }

        Collections.sort(localFolders, FolderBeanComparator.INSTANCE);

        empty = localFolders.isEmpty();

        synchronized (views) {

            // Remember the expanded and focussed views.
            FolderInfo expandedFolderInfo = null;
            FolderInfo focussedFolderInfo = null;
            for (ExpandableFolderView view : views) {
                if (expandedFolderInfo == null && view.isExpanded()) {
                    expandedFolderInfo = view.getFolderInfo();
                }
                if (focussedFolderInfo == null && view.hasFocus()) {
                    focussedFolderInfo = view.getFolderInfo();
                }
            }

            // Remove all folder views, but keep a list to see if any can be
            // recycled in the new display.
            List<ExpandableFolderView> oldViews =
                    new ArrayList<ExpandableFolderView>();
            for (ExpandableFolderView view : views) {
                oldViews.add(view);
                views.remove(view);
            }

            // Add new folder views.
            for (ExpandableFolderModel folderBean : localFolders) {
                addView(folderBean, expandedFolderInfo, focussedFolderInfo,
                        oldViews);
            }

            // Is anything left in the old list? Decommission.
            for (ExpandableFolderView oldView : oldViews) {
                views.remove(oldView);
                oldView.dispose();
                oldView.removeExpansionListener(expansionListener);
                oldView.unregisterListeners();
                folderListPanel.remove(oldView.getUIComponent());
            }

            // Redraw UI with everything gone.
            folderListPanel.invalidate();
            if (uiComponent != null) {
                uiComponent.invalidate();
            }
            if (scrollPane != null) {
                scrollPane.repaint();
            }

        }
        foldersTab.updateEmptyLabel();
    }

    private void addView(ExpandableFolderModel folderBean,
                         FolderInfo expandedFolderInfo,
                         FolderInfo focussedFolderInfo,
                         List<ExpandableFolderView> oldViews) {


        // See if we already have this view.
        ExpandableFolderView newView = null;
        Iterator<ExpandableFolderView> iter = oldViews.iterator();
        while (iter.hasNext()) {
            ExpandableFolderView oldView = iter.next();
            if (oldView.getFolderInfo().equals(folderBean.getFolderInfo())) {
                newView = oldView;

                // Remove from the list so it does not get decommissioned.
                iter.remove();
                break;
            }
        }

        // Do not have it? Create a new one.
        boolean viewCreated = false;
        if (newView == null) {
            newView = new ExpandableFolderView(getController(),
                    folderBean.getFolderInfo());
            viewCreated = true;
        }

        // Update details.
        newView.configure(folderBean);

        // Add to views.
        folderListPanel.add(newView.getUIComponent());
        views.add(newView);

        // Was view expanded before?
        if (expandedFolderInfo != null
                && folderBean.getFolderInfo().equals(expandedFolderInfo)) {
            newView.expand();
        }

        // Was view focussed before?
        if (focussedFolderInfo != null
                && folderBean.getFolderInfo().equals(focussedFolderInfo)) {
            newView.setFocus(true);
        }

        if (viewCreated) {
            newView.addExpansionListener(expansionListener);
        }
    }

    /**
     * Required to make scroller repaint correctly.
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
            view.updateIconAndOS();
        }
    }

    public void folderCreated(final FolderRepositoryEvent e) {

        // New folder created; try to show it in the list.
        Thread t = new Thread() {
            public void run() {

                // At this point, the newly created folder does not exist in the views,
                // so do this later, when the folder has appeared.
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    // Don't care.
                }

                // Run this in the AWT thread later.
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        scrollToFolderInfo(e.getFolderInfo());
                    }
                });
            }
        };
        t.start();
    }

    /**
     * This tries to find a view for a FolderInfo and if found, scrolls to it in the list.
     *
     * @param folderInfo
     */
    private void scrollToFolderInfo(FolderInfo folderInfo) {

        // Sum view heights and calculate my position within that height.
        boolean found = false;
        int totalHeight = 0;
        int myPosition = 0;
        for (ExpandableFolderView view : views) {
            view.getUIComponent().getHeight();
            int height = view.getUIComponent().getHeight();
            totalHeight += height;
            if (view.getFolderInfo().equals(folderInfo)) {
                found = true;
            }
            if (!found) {
                myPosition += height;
            }
        }

        if (found) {

            int viewportHeight = scrollPane.getViewport().getHeight();

            // Are all the views already on the screen?
            if (viewportHeight > totalHeight) {
                // All on screen with no vertical scrolling required. Nothing to do.
                return;
            }

            // Scroll it so mine is at top.
            scrollPane.getVerticalScrollBar().setValue(myPosition);
        }
    }

    // ////////////////
    // Inner classes //
    // ////////////////

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
        private boolean lastLoginSuccess;

        public void login(ServerClientEvent event) {
            if (event.isLoginSuccess()) {
                updateFolders();
                lastLoginSuccess = true;
            } else if (lastLoginSuccess) {
                updateFolders();
            }
        }

        public void accountUpdated(ServerClientEvent event) {
            updateFolders();
        }

        public void serverConnected(ServerClientEvent event) {
        }

        public void serverDisconnected(ServerClientEvent event) {
            if (event.getServerNode().hasJoinedAnyFolder()) {
                updateFolders();
            }
        }

        public void nodeServerStatusChanged(ServerClientEvent event) {
            if (event.getServerNode().hasJoinedAnyFolder()) {
                updateFolders();
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private class MyFolderMembershipListener implements
        FolderMembershipListener
    {

        public void memberJoined(FolderMembershipEvent folderEvent) {
            if (getController().getOSClient().isClusterServer(
                folderEvent.getMember()))
            {
                updateFolders();
            }
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            if (getController().getOSClient().isClusterServer(
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

        public void resetAllButSource(ExpansionEvent e) {
            synchronized (views) {
                for (ExpandableFolderView view : views) {
                    if (!view.equals(e.getSource())) {
                        // Not source, so collapse.
                        view.collapse();
                        view.setFocus(false);
                    }
                }
            }
        }

        public boolean fireInEventDispatchThread() {
            return true;
        }
    }

    private static class FolderBeanComparator implements
        Comparator<ExpandableFolderModel>
    {

        private static final FolderBeanComparator INSTANCE = new FolderBeanComparator();

        public int compare(ExpandableFolderModel o1, ExpandableFolderModel o2) {
            return o1.getFolderInfo().getLocalizedName()
                .compareToIgnoreCase(o2.getFolderInfo().getLocalizedName());
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

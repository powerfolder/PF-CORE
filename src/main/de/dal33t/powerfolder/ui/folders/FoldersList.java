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
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.event.FolderRepositoryEvent;
import de.dal33t.powerfolder.event.FolderRepositoryListener;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

public class FoldersList extends PFUIComponent {

    private JPanel uiComponent;
    private JPanel folderListPanel;
    private final List<ExpandableFolderView> views;

    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        return uiComponent;
    }

    public FoldersList(Controller controller) {
        super(controller);
        views = new ArrayList<ExpandableFolderView>();
    }

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

    private void initComponents() {
        folderListPanel = new JPanel();
        folderListPanel.setLayout(new BoxLayout(folderListPanel, BoxLayout.PAGE_AXIS));
        Folder[] folders = getController().getFolderRepository().getFolders();
        for (Folder folder : folders) {
            ExpandableFolderView folderView = new ExpandableFolderView(getController(), folder);
            folderListPanel.add(folderView.getUIComponent());
        }
        registerListeners();
    }

    private void registerListeners() {
        getController().getFolderRepository().addFolderRepositoryListener(new MyFolderRepositoryListener());
    }

    private class MyFolderRepositoryListener implements FolderRepositoryListener {

        public void folderRemoved(FolderRepositoryEvent e) {
            String folderName = e.getFolder().getName();
            Component[] components = folderListPanel.getComponents();
            for (ExpandableFolderView view : views) {
                if (view.getFolderName().equals(folderName)) {
                    for (Component component : components) {
                        if (component.equals(view.getUIComponent())) {
                            folderListPanel.remove(component);
                            break;
                        }
                    }
                    break;
                }
            }
        }

        public void folderCreated(FolderRepositoryEvent e) {
            ExpandableFolderView view = new ExpandableFolderView(getController(), e.getFolder());
            folderListPanel.add(view.getUIComponent());
            views.add(view);
        }

        public void maintenanceStarted(FolderRepositoryEvent e) {
        }

        public void maintenanceFinished(FolderRepositoryEvent e) {
        }

        public boolean fireInEventDispathThread() {
            return true;
        }
    }

}

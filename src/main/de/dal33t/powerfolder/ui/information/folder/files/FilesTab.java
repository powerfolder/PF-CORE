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
* $Id: FilesTab.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.files;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.uif_lite.component.UIFSplitPane;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.information.folder.FolderInformationTab;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * UI component for the folder files tab
 */
public class FilesTab extends PFUIComponent
        implements FolderInformationTab {

    private JPanel uiComponent;
    private Folder folder;
    private JSplitPane splitPane;
    private FilesTreePanel treePanel;
    private FilesTablePanel tablePanel;

    /**
     * Constructor
     *
     * @param controller
     */
    public FilesTab(Controller controller) {
        super(controller);
        treePanel = new FilesTreePanel(controller);
        tablePanel = new FilesTablePanel(controller);
        splitPane = new UIFSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                treePanel.getUIComponent(), tablePanel.getUIComponent());
        int dividerLocation = getController().getPreferences().getInt("files.tab.location", 50);
        splitPane.setDividerLocation(dividerLocation);
        splitPane.addPropertyChangeListener(new MyPropertyChangeListner());
    }

    /**
     * Set the tab with details for a folder.
     *
     * @param folderInfo
     */
    public void setFolderInfo(FolderInfo folderInfo) {
        folder = getController().getFolderRepository().getFolder(folderInfo);
        update();
    }

    /**
     * Gets the ui component
     *
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * Bulds the ui component.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("3dlu, pref:grow, 3dlu",
                "3dlu, pref, 3dlu, pref, 3dlu, fill:pref:grow, 3dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        CellConstraints cc = new CellConstraints();

        builder.add(createToolBar(), cc.xy(2, 2));
        builder.addSeparator(null, cc.xy(2, 4));

        splitPane.setOneTouchExpandable(false);
        builder.add(splitPane, cc.xy(2, 6));
        uiComponent = builder.getPanel();
    }

    /**
     * @return the toolbar
     */
    private JPanel createToolBar() {
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();
        bar.addGridded(new JToggleButton("temp"));
        return bar.getPanel();
    }

    /** refreshes the UI elements with the current data */
    private void update() {
    }

    /**
     * Detect changes to the split pane location.
     */
    private class MyPropertyChangeListner implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource().equals(splitPane)
                    && evt.getPropertyName().equals("dividerLocation")) {
               getController().getPreferences().putInt("files.tab.location",
                       splitPane.getDividerLocation()); 
            }

        }
    }
}
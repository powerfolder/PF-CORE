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
package de.dal33t.powerfolder.ui.recyclebin;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.RecycleBin;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.QuickInfoPanel;
import de.dal33t.powerfolder.ui.dialog.FileDetailsPanel;
import de.dal33t.powerfolder.ui.actionold.*;
import de.dal33t.powerfolder.ui.builder.ContentPanelBuilder;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.PFUIPanel;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.RecycleDelete;
import de.dal33t.powerfolder.util.ui.DialogFactory;
import de.dal33t.powerfolder.util.ui.PopupMenuOpener;
import de.dal33t.powerfolder.util.ui.SelectionChangeEvent;
import de.dal33t.powerfolder.util.ui.SelectionModel;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Shows a Table with the contents of the internal RecycleBin in a Table with a
 * buttonbar and context menu.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class RecycleBinPanel extends PFUIPanel implements HasDetailsPanel {
    private JComponent panel;
    private QuickInfoPanel quickInfo;
    private RecycleBinTable table;
    private JScrollPane tableScroller;
    private JPanel toolbar;
    /** The currently selected items */
    private SelectionModel selectionModel;
    private RestoreFileAction restoreFileAction;
    private RemoveFromRecycleBinAction removeFromRecycleBinAction;
    private OpenRecycleFolderAction openRecycleFolderAction;
    private JComponent fileDetailsPanelComp;
    private FileDetailsPanel fileDetailsPanel;
    private JToggleButton showHideFileDetailsButton;

    public RecycleBinPanel(Controller controller) {
        super(controller);
        selectionModel = new SelectionModel();
    }

    /** returns this ui component, creates it if not available * */
    public Component getUIComponent() {
        if (panel == null) {
            initComponents();
            ContentPanelBuilder builder = new ContentPanelBuilder();
            builder.setQuickInfo(quickInfo.getUIComponent());
            builder.setToolbar(toolbar);
            builder.setContent(createContentPanel());
            panel = builder.getPanel();
        }
        return panel;
    }

    private JComponent createContentPanel() {
        FormLayout layout = new FormLayout("fill:pref:grow",
            "fill:0:grow, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(tableScroller, cc.xy(1, 1));
        builder.add(fileDetailsPanelComp, cc.xy(1, 2));
        return builder.getPanel();
    }

    private void initComponents() {
        fileDetailsPanelComp = getFileDetailsPanelComp();
        fileDetailsPanelComp.setVisible(false);
        Action showHideFileDetailsAction = new ShowHideFileDetailsAction(this,
                getController());
        showHideFileDetailsButton = new JToggleButton(showHideFileDetailsAction);

        quickInfo = new RecycleBinQuickInfoPanel(getController());
        table = new RecycleBinTable(getController(), new RecycleBinTableModel(getController(),
            getController().getRecycleBin()));
        // Add selection listener for updating selection model
        table.getSelectionModel().addListSelectionListener(
            new RecycleBinListSelectionListener());
        tableScroller = new JScrollPane(table);
        restoreFileAction = new RestoreFileAction(getController(),
            selectionModel);
        removeFromRecycleBinAction = new RemoveFromRecycleBinAction(
            getController(), selectionModel);
        openRecycleFolderAction = new OpenRecycleFolderAction(
            getController(), selectionModel);
        UIUtil.whiteStripTable(table);
        UIUtil.removeBorder(tableScroller);
        UIUtil.setZeroHeight(tableScroller);
        toolbar = createToolBar();
        buildPopupMenus();
    }

    /**
     * TODO #495
     */
    public String getTitle() {
        return Translation.getTranslation("general.recycle_bin");
    }

    private JComponent getFileDetailsPanelComp() {
        if (fileDetailsPanelComp == null) {
            fileDetailsPanelComp = createFileDetailsPanel();
        }
        return fileDetailsPanelComp;
    }

    /**
     * @return the file panel
     */
    private JComponent createFileDetailsPanel() {
        fileDetailsPanel = new FileDetailsPanel(getController());

        FormLayout layout = new FormLayout("fill:pref:grow",
            "pref, 3dlu, pref, fill:pref");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.addSeparator(null, cc.xy(1, 1));
        builder.addSeparator(null, cc.xy(1, 3));
        builder.add(fileDetailsPanel.getEmbeddedPanel(), cc.xy(1, 4));
        return builder.getPanel();
    }

    /**
     * Creates the toolbar
     * 
     * @return
     */
    private JPanel createToolBar() {
        // Create toolbar
        ButtonBarBuilder bar = ButtonBarBuilder.createLeftToRightBuilder();

        bar.addGridded(new JButton(new EmptyRecycleBinAction(getController())));
        bar.addRelatedGap();
        bar.addGridded(new JButton(restoreFileAction));
        bar.addRelatedGap();
        bar.addGridded(showHideFileDetailsButton);
        bar.addRelatedGap();
        // FileUtils.executeFile only works on Win or Mac.
        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
            bar.addGridded(new JButton(openRecycleFolderAction));
            bar.addRelatedGap();
        }
        bar.setBorder(Borders.DLU4_BORDER);
        return bar.getPanel();
    }

    /**
     * Builds the popup menus
     */
    private void buildPopupMenus() {
        JPopupMenu fileMenu = new JPopupMenu();
        fileMenu.add(restoreFileAction);
        fileMenu.add(removeFromRecycleBinAction);
        table.addMouseListener(new PopupMenuOpener(fileMenu));

    }

    public void toggeDetails() {
        // Ensure the component is created.
        JComponent comp = getFileDetailsPanelComp();
        comp.setVisible(!comp.isVisible());
        showHideFileDetailsButton.setSelected(comp.isVisible());
    }

    /** updates the SelectionModel if some selection has changed in the table */
    private class RecycleBinListSelectionListener implements
        ListSelectionListener
    {
        public void valueChanged(ListSelectionEvent e) {
            int[] selectedRows = table.getSelectedRows();
            if (selectedRows.length != 0 && !e.getValueIsAdjusting()) {
                Object[] selectedObjects = new Object[selectedRows.length];
                for (int i = 0; i < selectedRows.length; i++) {
                    selectedObjects[i] = table.getModel().getValueAt(
                        selectedRows[i], 0);
                }
                selectionModel.setSelections(selectedObjects);
                if (selectedObjects.length == 1) {
                    fileDetailsPanel.setFile((FileInfo) selectedObjects[0]);
                }
            } else {
                selectionModel.setSelection(null);
            }
        }
    }

    private static class RemoveFromRecycleBinAction extends SelectionBaseAction {
        RemoveFromRecycleBinAction(Controller controller,
            SelectionModel selectionModel)
        {
            super("remove_from_recycle_bin", controller, selectionModel);
            setEnabled(false);
        }

        /**
         * Called if button RemoveFromRecycleBinAction is clicked, will show a
         * warning, and remove the selected files from the internal RecycleBin. Will
         * delete to the system recycle bin if possible (warning will change
         * accordingly).
         */
        public void actionPerformed(ActionEvent e) {
            Object[] selections = getSelectionModel().getSelections();
            RecycleBin recycleBin = getController().getRecycleBin();
            String titleKey;
            String textKey;
            if (RecycleDelete.isSupported()) {
                titleKey = "remove_from_recycle_bin_confimation.title";
                textKey = "remove_from_recycle_bin_confimation.text";
            } else {
                titleKey = "delete_confimation.title";
                textKey = "delete_confimation.text";
            }
            StringBuilder filesText = new StringBuilder();
            String separetor = "";
            for (Object selection : selections) {
                if (selection instanceof FileInfo) {
                    FileInfo fileInfo = (FileInfo) selection;
                    filesText.append(separetor + fileInfo.getFilenameOnly());
                    separetor = "\n";
                }
            }
            
            int choice = DialogFactory
            .showScrollableOkCancelDialog(
                getController().getUIController().getMainFrame().getUIComponent(),
                Translation.getTranslation(titleKey),
                Translation.getTranslation(textKey), filesText.toString());
                   
            if (choice == 0) { // OK
                for (Object selection : selections) {
                    if (selection instanceof FileInfo) {
                        FileInfo fileInfo = (FileInfo) selection;
                        if (recycleBin.isInRecycleBin(fileInfo)) {
                            recycleBin.delete(fileInfo);
                        }
                    }
                }
            }
        }

        /**
         * called if selections changed in table, enable this
         * RemoveFromRecycleBinAction if something is selected.
         */
        public void selectionChanged(SelectionChangeEvent event) {
            Object[] selections = getSelectionModel().getSelections();
            if (selections == null || selections.length == 0) {
                setEnabled(false);
                return;
            }
            for (Object selection : selections) {
                if (selection == null) {
                    setEnabled(false);
                    return;
                }
                if (!(selection instanceof FileInfo)) {
                    throw new IllegalStateException("Must be FileInfos "
                        + selection);
                }
            }
            setEnabled(true);
        }
    }

    /**
     * Helper class; Opens the recycle folder on an ActionEvent.
     */
    private static class OpenRecycleFolderAction extends SelectionBaseAction {

        /**
         * Constructor.
         *
         * @param controller the PowerFolder controler
         * @param selectionModel the selection model of FileInfo objects
         */
        OpenRecycleFolderAction(Controller controller, SelectionModel selectionModel) {
            super("open_local_folder", controller, selectionModel);
            setEnabled(false);
        }

        /**
         * Opens the recycle folder of the currently selected item in the operating system's file explorer.
         */
        public void actionPerformed(ActionEvent e) {
            RecycleBin recycleBin = getController().getRecycleBin();
            for (Object object : getSelectionModel().getSelections()) {
                if (object  instanceof FileInfo) {
                    FileInfo fileInfo = (FileInfo) object;
                    File diskFile = recycleBin.getDiskFile(fileInfo);
                    File parentDirectory = diskFile.getParentFile();
                    try {
                        FileUtils.openFile(parentDirectory);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        }

        /**
         * Called if selections changed in table.
         * Enables the Open Folder button if the selected object is a FileInfo instance.
         *
         * @param event the selection event
         */
        public void selectionChanged(SelectionChangeEvent event) {
            Object[] selections = getSelectionModel().getSelections();
            if (selections != null && selections.length > 0) {
                for (Object object : getSelectionModel().getSelections()) {
                    if (object instanceof FileInfo) {
                        setEnabled(true);
                        return;
                    }
                }
            }
            setEnabled(false);
        }
    }
}

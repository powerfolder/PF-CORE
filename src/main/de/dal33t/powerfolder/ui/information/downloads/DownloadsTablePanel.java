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
* $Id: DownloadsTablePanel.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.downloads;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.ui.widget.ActivityVisualizationWorker;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DownloadsTablePanel extends PFUIComponent {

    private JPanel uiComponent;
    private JScrollPane tablePane;
    private DownloadManagersTable table;
    private DownloadManagersTableModel tableModel;

    private Action openDownloadAction;
    private Action abortDownloadsAction;
    private Action clearCompletedDownloadsAction;

    private JPopupMenu fileMenu;

    /**
     * Constructor
     *
     * @param controller
     */
    public DownloadsTablePanel(Controller controller, Action openDownloadAction,
                               Action abortDownloadsAction,
                               Action clearCompletedDownloadsAction) {
        super(controller);
        this.openDownloadAction = openDownloadAction;
        this.abortDownloadsAction = abortDownloadsAction;
        this.clearCompletedDownloadsAction = clearCompletedDownloadsAction;
    }

    /**
     * Returns the ui component.
     *
     * @return
     */
    public JComponent getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * Initialize components
     */
    private void initialize() {
        TransferManagerModel transferManagerModel =
                getUIController().getTransferManagerModel();

        table = new DownloadManagersTable(transferManagerModel);
        table.getTableHeader().addMouseListener(new TableHeaderMouseListener());
        tablePane = new JScrollPane(table);
        tableModel = (DownloadManagersTableModel) table.getModel();
        table.addMouseListener(new TableMouseListener());

        // Whitestrip & set sizes
        UIUtil.whiteStripTable(table);
        UIUtil.setZeroHeight(tablePane);
        UIUtil.removeBorder(tablePane);
    }

    /**
     * Add a selection listener to the table.
     *
     * @param l
     */
    public void addListSelectionListener(ListSelectionListener l) {
        table.getSelectionModel().addListSelectionListener(l);
    }

    /**
     * Add a table model listener to the model.
     * 
     * @param l
     */
    public void addTableModelListener(TableModelListener l) {
        initialize();
        tableModel.addTableModelListener(l);
    }

    /**
     * Build the ui component tab pane.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("fill:pref:grow",
            "fill:0:grow");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.add(tablePane, cc.xy(1, 1));

        buildPopupMenus();

        uiComponent = builder.getPanel();
    }

    /**
     * Builds the popup menus
     */
    private void buildPopupMenus() {
        fileMenu = new JPopupMenu();
        if (OSUtil.isWindowsSystem() || OSUtil.isMacOS()) {
            fileMenu.add(openDownloadAction);
        }
        fileMenu.add(abortDownloadsAction);
        fileMenu.add(clearCompletedDownloadsAction);
    }

    /**
     * Clears old downloads.
     */
    public void clearDownloads() {

        ActivityVisualizationWorker avw =
                new ActivityVisualizationWorker(getUIController()) {

                    protected String getTitle() {
                        return Translation.getTranslation("downloads_panel.cleanup_activity.title");
                    }

                    protected String getWorkingText() {
                        return Translation.getTranslation("downloads_panel.cleanup_activity.description");
                    }

                    public Object construct() {
                        int rowCount = table.getRowCount();
                        if (rowCount == 0) {
                            return null;
                        }

                        // If no rows are selected,
                        // arrange for all downloads to be cleared.
                        boolean noneSelected = true;
                        for (int i = 0; i < table.getRowCount(); i++) {
                            if (table.isRowSelected(i)) {
                                noneSelected = false;
                                break;
                            }
                        }

                        // Do in two passes so changes to the model do not affect
                        // the process.
                        List<DownloadManager> downloadManagersToClear =
                                new ArrayList<DownloadManager>();

                        for (int i = 0; i < table.getRowCount(); i++) {
                            if (noneSelected || table.isRowSelected(i)) {
                                DownloadManager dlm = tableModel.getDownloadManagerAtRow(i);
                                if (dlm.isCompleted()) {
                                    downloadManagersToClear.add(dlm);
                                }
                            }
                        }
                        for (DownloadManager dlm : downloadManagersToClear) {
                            getController().getTransferManager()
                                    .clearCompletedDownload(dlm);
                        }
                        return null;
                    }
                };

        // Clear completed downloads
        avw.start();
    }

    /**
     * Returns true if the table has any rows.
     *
     * @return
     */
    public boolean isRowsExist() {
        return table != null && table.getRowCount() > 0;
    }

    /**
     * Returns true if the table has any selected incomplete downloads.
     *
     * @return
     */
    public boolean isIncompleteSelected() {
        if (table == null || tableModel == null) {
            return false;
        }
        int[] rows = table.getSelectedRows();
        boolean rowsSelected = rows.length > 0;
        if (rowsSelected) {
            for (int row : rows) {
                DownloadManager downloadManager = tableModel.getDownloadManagerAtRow(row);
                if (downloadManager == null) {
                    continue;
                }
                if (!downloadManager.isCompleted()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns true if a single completed download is selected.
     *
     * @return
     */
    public boolean isSingleCompleteSelected() {
        if (table == null || tableModel == null) {
            return false;
        }
        int[] rows = table.getSelectedRows();
        boolean singleRowSelected = rows.length == 1;
        if (singleRowSelected) {
            DownloadManager downloadManager = tableModel.getDownloadManagerAtRow(rows[0]);
            if (downloadManager.isCompleted()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Opens the selected, complete download in its native executor
     */
    public void openSelectedDownload() {
        if (table == null || tableModel == null) {
            return;
        }
        int[] rows = table.getSelectedRows();
        boolean singleRowSelected = rows.length == 1;
        if (singleRowSelected) {
            DownloadManager downloadManager = tableModel.getDownloadManagerAtRow(rows[0]);
            if (downloadManager.isCompleted()) {
                File file = downloadManager.getFileInfo().getDiskFile(
                        getController().getFolderRepository());
                if (file != null && file.exists()) {
                    try {
                        FileUtils.openFile(file);
                    } catch (IOException ex) {
                        logSevere(ex);
                    }
                }
            }
        }
    }

    /**
     * Aborts selected incomplete downloads.
     */
    public void abortSelectedDownloads() {
        if (table == null || tableModel == null) {
            return;
        }
        int[] rows = table.getSelectedRows();
        boolean rowsSelected = rows.length > 0;
        if (rowsSelected) {
            for (int row : rows) {
                DownloadManager downloadManager = tableModel.getDownloadManagerAtRow(row);
                if (downloadManager == null) {
                    continue;
                }
                if (!downloadManager.isCompleted()) {
                    downloadManager.abort();
                }
            }
        }
    }

    public FileInfo getSelectdFile() {
        if (table == null || tableModel == null) {
            return null;
        }
        int[] rows = table.getSelectedRows();
        if (rows.length == 1) {
            return tableModel.getDownloadManagerAtRow(rows[0]).getFileInfo();
        }
        return null;
    }

    ///////////////////
    // Inner Classes //
    ///////////////////

    /**
     * Listener on table header, takes care about the sorting of table
     *
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private static class TableHeaderMouseListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                JTableHeader tableHeader = (JTableHeader) e.getSource();
                int columnNo = tableHeader.columnAtPoint(e.getPoint());
                TableColumn column = tableHeader.getColumnModel().getColumn(
                    columnNo);
                int modelColumnNo = column.getModelIndex();
                TableModel model = tableHeader.getTable().getModel();
                if (model instanceof DownloadManagersTableModel) {
                    DownloadManagersTableModel downloadManagersTableModel = (DownloadManagersTableModel) model;
                    boolean freshSorted = downloadManagersTableModel
                        .sortBy(modelColumnNo);
                    if (!freshSorted) {
                        // reverse list
                        downloadManagersTableModel.reverseList();
                    }
                }
            }
        }
    }

    private class TableMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                showContextMenu(e);
            }
        }

        private void showContextMenu(MouseEvent evt) {
            fileMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }

}

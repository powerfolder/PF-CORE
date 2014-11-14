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
 * $Id: UploadsTablePanel.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.information.uploads;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.ui.PFUIComponent;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.ui.util.SwingWorker;
import de.dal33t.powerfolder.ui.util.UIUtil;

public class UploadsTablePanel extends PFUIComponent {

    private JPanel uiComponent;
    private JScrollPane tablePane;
    private UploadsTable table;
    private UploadsTableModel tableModel;
    private Action clearCompletedUploadsAction;
    private Action addIgnoreAction;
    private JPopupMenu fileMenu;

    /**
     * Constructor.
     *
     * @param controller
     */
    public UploadsTablePanel(Controller controller,
        Action clearCompletedUploadsAction, Action addIgnoreAction)
    {
        super(controller);
        this.clearCompletedUploadsAction = clearCompletedUploadsAction;
        this.addIgnoreAction = addIgnoreAction;
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
        TransferManagerModel transferManagerModel = getUIController()
            .getTransferManagerModel();

        table = new UploadsTable(transferManagerModel);
        table.getTableHeader().addMouseListener(new TableHeaderMouseListener());
        tablePane = new JScrollPane(table);
        tableModel = (UploadsTableModel) table.getModel();
        table.addMouseListener(new TableMouseListener());

        table.registerKeyboardAction(new SelectAllAction(), KeyStroke
            .getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK),
            JComponent.WHEN_FOCUSED);

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
        getUIComponent();
        table.getSelectionModel().addListSelectionListener(l);
    }

    /**
     * Add a table model listener to the model.
     *
     * @param l
     */
    public void addTableModelListener(TableModelListener l) {
        getUIComponent();
        tableModel.addTableModelListener(l);
    }

    /**
     * Clears old uploads.
     */
    public void clearUploads() {

        // Clear completed uploads
        SwingWorker worker = new SwingWorker() {

            public Object construct() {
                int rowCount = table.getRowCount();
                if (rowCount == 0) {
                    return null;
                }

                // If no rows are selected,
                // arrange for all uploads to be cleared.
                boolean noneSelected = true;
                for (int i = 0; i < table.getRowCount(); i++) {
                    if (table.isRowSelected(i)) {
                        noneSelected = false;
                        break;
                    }
                }

                // Do in two passes so changes to the model do not affect
                // the process.
                List<Upload> uploadsToClear = new ArrayList<Upload>();

                for (int i = 0; i < table.getRowCount(); i++) {
                    if (noneSelected || table.isRowSelected(i)) {
                        Upload ul = tableModel.getUploadAtRow(i);
                        if (ul.isCompleted()) {
                            uploadsToClear.add(ul);
                        }
                    }
                }
                for (Upload ul : uploadsToClear) {
                    getController().getTransferManager().clearCompletedUpload(
                        ul);
                }
                return null;
            }
        };
        worker.start();
    }

    /**
     * Build the ui component tab pane.
     */
    private void buildUIComponent() {
        FormLayout layout = new FormLayout("fill:pref:grow", "fill:0:grow");
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
        fileMenu.add(clearCompletedUploadsAction);
        fileMenu.add(addIgnoreAction);
    }

    public FileInfo getSelectdFile() {
        if (table == null || tableModel == null) {
            return null;
        }
        int[] rows = table.getSelectedRows();
        if (rows.length == 1) {
            Upload ul = (Upload) tableModel.getValueAt(rows[0],
                UploadsTableModel.COLPROGRESS);
            return ul != null ? ul.getFile() : null;
        }
        return null;
    }

    public int countActiveUploadCount() {
        int count = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Upload ul = tableModel.getUploadAtRow(i);
            if (ul.isStarted() && !ul.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    public int countCompletedUploadCount() {
        int count = 0;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Upload ul = tableModel.getUploadAtRow(i);
            if (ul.isCompleted()) {
                count++;
            }
        }
        return count;
    }

    public Upload[] getSelectedRows() {
        int[] ints = table.getSelectedRows();
        return tableModel.getUploadsAtRows(ints);
    }

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
                if (model instanceof UploadsTableModel) {
                    UploadsTableModel uploadsTableModel = (UploadsTableModel) model;
                    boolean freshSorted = uploadsTableModel
                        .sortBy(modelColumnNo);
                    if (!freshSorted) {
                        // reverse list
                        uploadsTableModel.reverseList();
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

    private class SelectAllAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            table.selectAll();
        }
    }

}

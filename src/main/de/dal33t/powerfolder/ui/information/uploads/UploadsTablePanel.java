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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.ui.model.TransferManagerModel;
import de.dal33t.powerfolder.ui.information.uploads.UploadsTable;
import de.dal33t.powerfolder.ui.information.uploads.UploadsTableModel;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UploadsTablePanel extends PFUIComponent {

    private JPanel uiComponent;
    private UploadsTable table;
    private UploadsTableModel tableModel;
    private JScrollPane tablePane;


    public UploadsTablePanel(Controller controller) {
        super(controller);
    }

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
        uiComponent = new JPanel();
        uiComponent.add(new JLabel("Uploads table panel"));

        TransferManagerModel transferManagerModel =
                getUIController().getTransferManagerModel();

        table = new UploadsTable(transferManagerModel);
        table.getTableHeader().addMouseListener(new TableHeaderMouseListener());
        tableModel = (UploadsTableModel) table.getModel();
        tablePane = new JScrollPane(table);

        // Whitestrip & set sizes
        UIUtil.whiteStripTable(table);
        UIUtil.setZeroHeight(tablePane);
        UIUtil.removeBorder(tablePane);
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
        uiComponent = builder.getPanel();
    }


    /**
     * Listener on table header, takes care about the sorting of table
     *
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class TableHeaderMouseListener extends MouseAdapter {
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


}

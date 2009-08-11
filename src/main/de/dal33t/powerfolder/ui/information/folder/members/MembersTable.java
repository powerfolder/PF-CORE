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
 * $Id: MemberTable.java 5457 2008-10-17 14:25:41Z harry $
 */
package de.dal33t.powerfolder.ui.information.folder.members;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.jgoodies.binding.adapter.BasicComponentFactory;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.render.SortedTableHeaderRenderer;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.ColorUtil;
import de.dal33t.powerfolder.util.ui.UIUtil;

/**
 * Table to display members of a folder.
 */
public class MembersTable extends JTable {

    /**
     * Constructor
     * 
     * @param model
     */
    public MembersTable(MembersTableModel model) {
        super(model);

        setRowHeight(Icons.getIconById(Icons.NODE_FRIEND_CONNECTED)
            .getIconHeight() + 3);
        setColumnSelectionAllowed(false);
        setShowGrid(false);

        setupColumns();

        getTableHeader().addMouseListener(new TableHeaderMouseListener());

        // Setup renderer
        MemberTableCellRenderer memberCellRenderer = new MemberTableCellRenderer();
        setDefaultRenderer(FolderMember.class, memberCellRenderer);
        setDefaultRenderer(FolderPermission.class, memberCellRenderer);
        setDefaultRenderer(String.class, memberCellRenderer);

        // Associate a header renderer with all columns.
        SortedTableHeaderRenderer.associateHeaderRenderer(model,
            getColumnModel(), MembersTableModel.COL_TYPE);

        setDefaultEditor(FolderPermission.class, new DefaultCellEditor(
            createdEditComboBox(model)));
    }

    /**
     * Sets the column sizes of the table
     */
    private void setupColumns() {
        int totalWidth = getWidth();

        // Otherwise the table header is not visible:
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));

        TableColumn column = getColumn(getColumnName(MembersTableModel.COL_TYPE));
        column.setPreferredWidth(28);
        column.setMinWidth(28);
        column.setMaxWidth(28);
        column = getColumn(getColumnName(MembersTableModel.COL_COMPUTER_NAME));
        column.setPreferredWidth(100);
        column = getColumn(getColumnName(MembersTableModel.COL_USERNAME));
        column.setPreferredWidth(100);

        column = getColumn(getColumnName(MembersTableModel.COL_SYNC_STATUS));
        column.setPreferredWidth(20);

        column = getColumn(getColumnName(MembersTableModel.COL_PERMISSION));
        column.setPreferredWidth(100);

        column = getColumn(getColumnName(MembersTableModel.COL_LOCAL_SIZE));
        column.setPreferredWidth(100);
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
                if (model instanceof MembersTableModel) {
                    MembersTableModel membersTableModel = (MembersTableModel) model;
                    membersTableModel.sortBy(modelColumnNo);
                }
            }
        }
    }

    private class MemberTableCellRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            int actualColumn = UIUtil.toModel(table, column);
            MembersTableModel model = (MembersTableModel) MembersTable.this
                .getModel();
            FolderMember folderMember = model.getFolderMemberAt(row);

            Component defaultComp = super.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);

            // Reset to defaults
            setEnabled(true);
            setIcon(null);
            setForeground(ColorUtil.getTextForegroundColor());

            boolean isServer = folderMember.getMember() != null
                && model.getController().getOSClient().isServer(
                    folderMember.getMember());

            if (actualColumn == MembersTableModel.COL_TYPE) {
                Member member = folderMember.getMember();
                Icon icon = member != null ? Icons.getIconFor(member) : Icons
                    .getIconById(Icons.NODE_FRIEND_DISCONNECTED);
                setIcon(icon);
            } else if (actualColumn == MembersTableModel.COL_COMPUTER_NAME) {
                if (folderMember.getMember() != null) {
                    setText(folderMember.getMember().getNick());
                } else {
                    setText("Not syncing");
                    setForeground(Color.GRAY);
                }
            } else if (actualColumn == MembersTableModel.COL_USERNAME) {
                if (folderMember.getAccountInfo() != null) {
                    setText(folderMember.getAccountInfo().getScrabledUsername());
                } else if (!model.getController().getOSClient().isConnected()) {
                    setText("Not connected to server");
                    setForeground(Color.GRAY);
                } else if (isServer) {
                    setText("Server");
                    setForeground(Color.GRAY);
                } else {
                    setText("Not logged in");
                    setForeground(Color.GRAY);
                }
            } else if (actualColumn == MembersTableModel.COL_PERMISSION) {
                boolean editable = model.isCellEditable(row, column);
                if (!editable) {
                    setForeground(Color.GRAY);
                }
                if (!model.isPermissionsRetrieved()) {
                    setText("");
                } else if (folderMember.getPermission() != null) {
                    setText(folderMember.getPermission().getName());
                } else if (isServer) {
                    // Server has read/write by default
                    setText(Translation
                        .getTranslation("permissions.folder.read_write"));
                } else {
                    FolderPermission defPerm = model.getDefaultPermission();
                    if (defPerm != null) {
                        setText(defPerm.getName() + " (default)");
                    } else {
                        setText("No access");
                    }
                }
            }

            if (!isSelected) {
                setBackground(row % 2 == 0
                    ? ColorUtil.EVEN_TABLE_ROW_COLOR
                    : ColorUtil.ODD_TABLE_ROW_COLOR);
            }

            return defaultComp;
        }
    }

    private JComboBox createdEditComboBox(final MembersTableModel model) {
        return BasicComponentFactory.createComboBox(model
            .getPermissionsListModel(), new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list,
                Object value, int index, boolean isSelected,
                boolean cellHasFocus)
            {
                Component comp = super.getListCellRendererComponent(list,
                    value, index, isSelected, cellHasFocus);

                if (value instanceof FolderPermission) {
                    setText(((FolderPermission) value).getName());
                } else {
                    int selectedRow = getSelectedRow();
                    FolderMember selectedMember = selectedRow >= 0 ? model
                        .getFolderMemberAt(selectedRow) : null;
                    if (selectedMember != null
                        && selectedMember.getMember() == null)
                    {
                        setText("No access");
                    } else {
                        FolderPermission defPerm = model.getDefaultPermission();
                        if (defPerm != null) {
                            setText(defPerm.getName() + " (default)");
                        } else {
                            setText("No access (default)");
                        }
                    }

                }
                return comp;
            }
        });
    }

}

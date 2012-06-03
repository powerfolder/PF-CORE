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
package de.dal33t.powerfolder.ui.information.folder.files.table;

import static de.dal33t.powerfolder.disk.SyncProfile.MANUAL_SYNCHRONIZATION;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.io.File;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.DiskItem;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.ui.render.SortedTableHeaderRenderer;
import de.dal33t.powerfolder.ui.util.ColorUtil;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Translation;

/**
 * Table to display files of a folder.
 */
@SuppressWarnings("serial")
public class FilesTable extends JTable {

    private static final Color AVAILABLE = Color.GRAY;
    private static final Color DOWNLOADING = new Color(40, 170, 40);
    private static final Color DELETED = Color.RED;
    private static final Color NORMAL = Color.BLACK;
    private static final Color NEWER_AVAILABLE = Color.BLUE;

    /**
     * Constructor
     * 
     * @param model
     */
    public FilesTable(FilesTableModel model) {
        super(model);

        setRowHeight(Icons.getIconById(Icons.NODE_CONNECTED).getIconHeight() + 3);
        setColumnSelectionAllowed(false);
        setShowGrid(false);

        setupColumns();

        setDefaultRenderer(FileInfo.class,
            new MyDefaultTreeCellRenderer(model.getController()));
        setDefaultRenderer(DirectoryInfo.class, new MyDefaultTreeCellRenderer(
            model.getController()));

        getTableHeader().addMouseListener(new TableHeaderMouseListener());

        // Associate a header renderer with all columns.
        SortedTableHeaderRenderer.associateHeaderRenderer(model,
            getColumnModel(), 1, true);
    }

    /**
     * Sets the column sizes of the table
     */
    private void setupColumns() {
        int totalWidth = getWidth();

        // Otherwise the table header is not visible:
        getTableHeader().setPreferredSize(new Dimension(totalWidth, 20));

        TableColumn column = getColumn(getColumnName(0));
        column.setPreferredWidth(20);
        column.setMinWidth(20);
        column.setMaxWidth(20);
        column = getColumn(getColumnName(1));
        column.setPreferredWidth(200);
        column = getColumn(getColumnName(2));
        column.setPreferredWidth(70);
        column = getColumn(getColumnName(3));
        column.setPreferredWidth(70);
        column = getColumn(getColumnName(4));
        column.setPreferredWidth(60);
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
                if (model instanceof FilesTableModel) {
                    FilesTableModel filesTableModel = (FilesTableModel) model;
                    boolean freshSorted = filesTableModel.sortBy(modelColumnNo);
                    if (!freshSorted) {
                        // reverse list
                        filesTableModel.reverseList();
                    }
                }
            }
        }
    }

    private static class MyDefaultTreeCellRenderer extends
        DefaultTableCellRenderer
    {

        private Controller controller;

        private MyDefaultTreeCellRenderer(Controller controller) {
            this.controller = controller;
        }

        public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus, int row,
            int column)
        {
            DiskItem diskItem = (DiskItem) value;
            String myValue = "";
            boolean strikethrough = false;
            if (diskItem instanceof DirectoryInfo) {
                DirectoryInfo dir = (DirectoryInfo) diskItem;
                Folder folder = dir.getFolder(controller.getFolderRepository());
                myValue = "";
                setIcon(null);
                switch (column) {
                    case 0 : // file type
                        Icon icon = Icons.getIconById(Icons.DIRECTORY);
                        setIcon(icon);
                        setHorizontalAlignment(LEFT);
                        break;
                    case 1 : // dir name
                        myValue = dir.getFilenameOnly();
                        if (folder.getDiskItemFilter().isExcluded(dir)) {
                            setForeground(NORMAL);
                            // File filtered out by blacklist.
                            strikethrough = true;
                        } else if (dir.isDeleted()) {
                            setForeground(DELETED);
                            setIcon(null);
                        } else if (dir.isExpected(controller
                            .getFolderRepository()))
                        {
                            setForeground(AVAILABLE);
                            setIcon(Icons.getIconById(Icons.EXPECTED));
                        } else {
                            setForeground(NORMAL);
                        }
                        setHorizontalAlignment(LEFT);

                        break;
                }
            } else if (diskItem instanceof FileInfo) {
                FileInfo fileInfo = (FileInfo) diskItem;
                Folder folder = controller.getFolderRepository().getFolder(
                    fileInfo.getFolderInfo());
                setIcon(null);
                setForeground(NORMAL);
                switch (column) {
                    case 0 : // file type
                        Icon icon = Icons.getIconFor(fileInfo, controller);
                        setIcon(icon);
                        setHorizontalAlignment(LEFT);
                        break;
                    case 1 : // file name

                        myValue = fileInfo.getFilenameOnly();

                        // Prepare diskfile
                        File diskFile = fileInfo.getDiskFile(controller
                            .getFolderRepository());
                        if (diskFile != null) {
                            if (!diskFile.exists()) {
                                diskFile = null;
                            }
                        }
                        String fileNameForTooltip;
                        if (diskFile != null) {
                            fileNameForTooltip = replaceSpacesWithNBSP(diskFile
                                .getAbsolutePath());
                        } else {
                            fileNameForTooltip = replaceSpacesWithNBSP(fileInfo
                                .getFilenameOnly());
                        }
                        // Obtain the newest version of this file
                        FileInfo newestVersion = null;
                        FileInfo newestDeletedVersion = null;
                        if (fileInfo
                            .getFolder(controller.getFolderRepository()) != null)
                        {
                            newestVersion = fileInfo
                                .getNewestNotDeletedVersion(controller
                                    .getFolderRepository());
                            newestDeletedVersion = fileInfo
                                .getNewestVersion(controller
                                    .getFolderRepository());
                        }
                        setIcon(null);

                        String statusForTooltip = null;
                        if (fileInfo.isDownloading(controller)) {
                            setForeground(DOWNLOADING);
                            DownloadManager dl = controller
                                .getTransferManager().getActiveDownload(
                                    fileInfo);
                            if (dl != null && dl.isStarted()) {
                                StringBuilder b = new StringBuilder();
                                for (Download d : dl.getSources()) {
                                    if (b.length() > 0) {
                                        b.append(", ");
                                    }
                                    b.append(d.getPartner().getNick());
                                }
                                setIcon(Icons
                                    .getIconById(Icons.DOWNLOAD_ACTIVE));
                                statusForTooltip = Translation.getTranslation(
                                    "file_info.downloading_from_member",
                                    b.toString());
                            } else {
                                setIcon(Icons.getIconById(Icons.DOWNLOAD));
                                statusForTooltip = Translation
                                    .getTranslation("transfers.queued");
                            }
                            // preference goes to deleted, then ignored then
                            // available icon
                        } else if (fileInfo.isDeleted()) {
                            setForeground(DELETED);
                            setIcon(null);
                            statusForTooltip = Translation
                                .getTranslation("file_info.deleted");

                        } else if (folder != null
                            && folder.getDiskItemFilter().isExcluded(fileInfo))
                        {
                            // File filtered out by blacklist.
                            statusForTooltip = replaceSpacesWithNBSP(Translation
                                .getTranslation("file_info.ignore"));
                            strikethrough = true;
                        } else if (fileInfo.isExpected(controller
                            .getFolderRepository()))
                        {
                            setForeground(AVAILABLE);
                            setIcon(Icons.getIconById(Icons.EXPECTED));
                            statusForTooltip = Translation
                                .getTranslation("file_info.expected");

                        } else if (newestVersion != null
                            && newestVersion.isNewerThan(fileInfo))
                        {
                            // A newer version is available
                            // FIXME: If newest version (e.g. v10) is deleted,
                            // but a
                            // newer (e.g. v9) is available
                            // DONE? Should be fixed with exclusion of deleted
                            // newer files
                            setForeground(NEWER_AVAILABLE);
                            if (newestVersion.isDeleted()) {
                                if (newestVersion.equals(newestDeletedVersion))
                                {
                                    if (MANUAL_SYNCHRONIZATION.equals(folder
                                        .getSyncProfile()))
                                    {
                                        // Show remote deletions when in project
                                        // work sync
                                        setIcon(Icons.getIconById(Icons.DELETE));
                                        statusForTooltip = Translation
                                            .getTranslation("file_info.remote_deleted");
                                    }
                                }
                            } else {
                                setIcon(Icons.getIconById(Icons.EXPECTED));
                                statusForTooltip = Translation
                                    .getTranslation("file_info.new_version_available");
                            }
                        } else {
                            if (recentlyDownloaded(fileInfo)) {
                                statusForTooltip = Translation
                                    .getTranslation("file_info.recently_downloaded");
                            }
                        }

                        setToolTipText(getToolTip(fileInfo, fileNameForTooltip,
                            statusForTooltip));
                        setHorizontalAlignment(LEFT);
                        break;
                    case 2 : // file size
                        myValue = Format.formatBytesShort(fileInfo.getSize());
                        setToolTipText(Format.formatBytes(fileInfo.getSize()));
                        setHorizontalAlignment(RIGHT);
                        break;
                    case 3 : // member nick
                        MemberInfo member = fileInfo.getModifiedBy();
                        myValue = member.nick;
                        // setIcon(Icons.getSimpleIconFor(member.getNode(controller,
                        // false)));
                        setHorizontalAlignment(LEFT);
                        break;
                    case 4 : // modified date
                        myValue = Format.formatDateShort(fileInfo
                            .getModifiedDate());
                        setHorizontalAlignment(RIGHT);
                        break;
                }
            }

            Component c = super.getTableCellRendererComponent(table, myValue,
                isSelected, hasFocus, row, column);

            // Show new files in bold.
            if (diskItem != null && diskItem.isFile()) {
                if (recentlyDownloaded((FileInfo) diskItem)) {
                    c.setFont(new Font(getFont().getName(), Font.BOLD,
                        getFont().getSize()));
                }
            }

            if (!isSelected) {
                setBackground(row % 2 == 0
                    ? ColorUtil.EVEN_TABLE_ROW_COLOR
                    : ColorUtil.ODD_TABLE_ROW_COLOR);
            }

            if (strikethrough) {
                Font font = c.getFont();
                Map attribs = font.getAttributes();
                attribs.put(TextAttribute.STRIKETHROUGH, true);
                c.setFont(new Font(attribs));
            }

            return c;
        }

        /**
         * Return true if there is a completed download manager for this file
         * info.
         * 
         * @param fileInfo
         * @return
         */
        private boolean recentlyDownloaded(FileInfo fileInfo) {
            return controller.getTransferManager()
                .isCompletedDownload(fileInfo);
        }

        private static String replaceSpacesWithNBSP(String text) {
            return text.replaceAll(" ", "&nbsp;");
        }

        private static String getToolTip(FileInfo fileInfo,
            String fileNameForTooltip, String statusForTooltip)
        {
            StringBuilder textInHTML = new StringBuilder("<HTML><HEAD>");
            textInHTML
                .append("<style TYPE=\"text/css\"><!--BODY {  font-size: 10px; color: #000000; background : #FFFFFF; }");
            textInHTML.append(".normal { font-size: 10px; color: #000000;}");
            textInHTML
                .append(".deleted { font-size: 10px; color: #FF0000;font-weight: bold;}");
            textInHTML
                .append(".new_avail { font-size: 10px; color: #0000FF;font-weight: bold;}");
            textInHTML
                .append(".bold { font-size: 10px; color: #000000;font-weight: bold;}");
            textInHTML.append("--></style>");
            textInHTML.append("</HEAD><BODY>");
            textInHTML.append("<TABLE cellspacing=0 cellpadding=0 border=0>");
            if (!StringUtils.isBlank(fileNameForTooltip)) {
                textInHTML
                    .append("<TR><TD valign=top class=bold colspan=2>&nbsp;"
                        + fileNameForTooltip + "&nbsp;</TD></TR>");
            }

            if (!StringUtils.isBlank(statusForTooltip)) {
                String style;
                if (statusForTooltip.equals(Translation
                    .getTranslation("file_info.deleted")))
                {
                    style = "deleted";
                } else if (statusForTooltip.equals(Translation
                    .getTranslation("file_info.new_version_available")))
                {
                    style = "new_avail";
                } else {
                    style = "bold";
                }
                textInHTML.append("<TR><TD valign=top class=" + style
                    + " colspan=2>&nbsp;" + statusForTooltip
                    + "&nbsp;</TD></TR>");
            }
            textInHTML.append("</TABLE>");
            // add hidden filename to make sure the tooltip is rendered on new
            // spot
            // even if text is the same
            textInHTML.append("<!-- " + fileInfo.getFilenameOnly() + "-->");

            textInHTML.append("</BODY></HTML>");
            return textInHTML.toString();

        }
    }
}
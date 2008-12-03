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
* $Id: FilesTableModel.java 5457 2008-10-17 14:25:41Z harry $
*/
package de.dal33t.powerfolder.ui.information.folder.files.table;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.DiskItem;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.FileInfoHolder;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.information.folder.files.FilteredDirectoryModel;
import de.dal33t.powerfolder.ui.model.SortedTableModel;
import de.dal33t.powerfolder.util.Format;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.UIUtil;

import javax.swing.Icon;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class to model files selected from the tree.
 */
public class FilesTableModel extends PFComponent implements TableModel,
        SortedTableModel {

    private String[] columns = new String[]{"",
        Translation.getTranslation("files_table_model.name"),
        Translation.getTranslation("files_table_model.size"),
        Translation.getTranslation("files_table_model.modified_by"),
        Translation.getTranslation("files_table_model.date"),
        Translation.getTranslation("files_table_model.availability")};

    private Folder folder;
    private File selectedDirectory;
    private final Map<File, List<DiskItem>> directories;
    private final List<DiskItem> diskItems;
    private final List<TableModelListener> listeners;

    /**
     * Constructor
     *
     * @param controller
     */
    public FilesTableModel(Controller controller) {
        super(controller);
        directories = new ConcurrentHashMap<File, List<DiskItem>>();
        diskItems = new CopyOnWriteArrayList<DiskItem>();
        listeners = new CopyOnWriteArrayList<TableModelListener>();
    }

    /**
     * Set the folder for the model to get details from.
     *
     * @param folder
     */
    public void setFolder(Folder folder) {
        this.folder = folder;
        update();
    }

    /**
     * Set the directory selected in the tree.
     *
     * @param selectedDirectory
     */
    public void setSelectedDirectory(File selectedDirectory) {
        this.selectedDirectory = selectedDirectory;
        update();
    }

    /**
     * Pass the filtered directory model to get the file infos from.
     *
     * @param model
     */
    public void setFilteredDirectoryModel(FilteredDirectoryModel model) {
        directories.clear();
        walkFilteredDirectoryModel(model);
        update();
    }

    /**
     * Walk the FilteredDirectoryModel to get map of directory / FileInfos.
     *
     * @param model
     */
    private void walkFilteredDirectoryModel(FilteredDirectoryModel model) {
        File file = model.getFile();
        List<DiskItem> diskItemList = new ArrayList<DiskItem>();
        diskItemList.addAll(model.getFiles());
        directories.put(file, diskItemList);
        for (FilteredDirectoryModel subModel : model.getSubdirectories()) {
            walkFilteredDirectoryModel(subModel);
        }
    }

    /**
     * Update the model in response to a change.
     */
    private void update() {

        if (folder == null) {
            return;
        }

        if (selectedDirectory == null) {
            return;
        }

        if (directories.isEmpty()) {
            return;
        }

        boolean found = false;
        for (File file : directories.keySet()) {
            if (selectedDirectory.equals(file)) {
                logInfo("Found " + selectedDirectory + " in directories");
                found = true;
                break;
            }
        }

        if (!found) {
            return;
        }

        List<DiskItem> tempList = new ArrayList<DiskItem>(diskItems.size());
        tempList.addAll(diskItems);

        // Look for extra items in the selectedDiskItems list to insert.
        List<DiskItem> selectedDiskItems = directories.get(selectedDirectory);
        for (DiskItem selectedDiskItem : selectedDiskItems) {
            found = false;
            int foundRow = -1;
            int rowCount = 0;
            for (DiskItem diskItem : tempList) {
                if (diskItem.equals(selectedDiskItem)) {
                    found = true;
                    foundRow = rowCount;
                    break;
                }
                rowCount++;
            }
            if (!found) {
                diskItems.add(selectedDiskItem);
                modelChanged(foundRow, true);
            }
        }

        // Look for extra items in the diskItem list to remove.
        int rowCount = 0;
        for (DiskItem diskItem : tempList) {
            found = false;
            for (DiskItem selectedDiskItem : selectedDiskItems) {
                if (diskItem.equals(selectedDiskItem)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                diskItems.remove(diskItem);
                modelChanged(rowCount, false);
            }
            rowCount++;
        }
    }

    private void modelChanged(int row, boolean insert) {
        final TableModelEvent e = new TableModelEvent(this, row, row,
                TableModelEvent.ALL_COLUMNS,
                insert ? TableModelEvent.INSERT : TableModelEvent.DELETE);
        Runnable runner = new Runnable() {
            public void run() {
                synchronized (listeners) {
                    for (TableModelListener listener : listeners) {
                        listener.tableChanged(e);
                    }
                }
            }
        };
        UIUtil.invokeLaterInEDT(runner);
    }

    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return Icon.class;
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return String.class;
            default:
                throw new IllegalArgumentException("columnIndex too big: "
                        + columnIndex);

        }
    }
    public int getColumnCount() {
        return columns.length;
    }

    public String getColumnName(int columnIndex) {
        return columns[columnIndex];
    }

    public int getRowCount() {
        return diskItems.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        DiskItem diskItem = diskItems.get(rowIndex);
        if (columnIndex == 0) {
            if (diskItem instanceof FileInfo) {
            return Icons.getIconFor((FileInfo) diskItem, getController());
            } else {
                return Icons.getIconFor((Directory) diskItem, false, getController());
            }
        } else if (columnIndex == 1) {
            return diskItem.getName();
        } else if (columnIndex == 2) {
            return Format.formatBytesShort(diskItem.getSize());
        } else if (columnIndex == 3) {
            return diskItem.getModifiedBy().nick;
        } else if (columnIndex == 4) {
            return String.valueOf(diskItem.getModifiedDate() != null ?
                    Format.formatDate(diskItem.getModifiedDate()) : "-");
        } else if (columnIndex == 5) {
            if (diskItem instanceof FileInfo) {
                FileInfo fileInfo = (FileInfo) diskItem;
                if (fileInfo.isDeleted()
                    && getController().getRecycleBin().isInRecycleBin(fileInfo))
                {
                    return Translation.getTranslation("fileinfo.in_recycle_bin");
                } else {
                    FileInfoHolder holder = folder.getDirectory()
                        .getFileInfoHolder(fileInfo);
                    if (holder == null) {
                        return "0";
                    } else {
                        return String.valueOf(holder.getAvailability());
                    }
                }
            }
        }
        return "";
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new UnsupportedOperationException("Cannot modify FilesTableModel");
    }

    public int getSortColumn() {
        return 1;    // @todo
    }

    public boolean isSortAscending() {
        return false;  // @todo
    }

    public boolean sortBy(int columnIndex) {
        return true;          // @todo
    }
}

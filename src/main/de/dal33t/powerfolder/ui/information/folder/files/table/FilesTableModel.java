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
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.ui.information.folder.files.FilteredDirectoryModel;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilesTableModel extends PFComponent implements TableModel {

    private Folder folder;
    private File selectedDirectory;
    private final Map<File, List<FolderInfo>> directories;

    public FilesTableModel(Controller controller) {
        super(controller);
        directories = new HashMap<File, List<FolderInfo>>();
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
        System.out.println("hghg set folder");
    }

    public void setSelectedDirectory(File selectedDirectory) {
        this.selectedDirectory = selectedDirectory;
        System.out.println("hghg set dir");
    }

    public void setFilteredDirectoryModel(FilteredDirectoryModel model) {
        System.out.println("hghg set model");
    }

    public void addTableModelListener(TableModelListener l) {
    }

    public Class<?> getColumnClass(int columnIndex) {
        return null;
    }

    public int getColumnCount() {
        return 0;
    }

    public String getColumnName(int columnIndex) {
        return null;
    }

    public int getRowCount() {
        return 0;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return null;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public void removeTableModelListener(TableModelListener l) {
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }
}

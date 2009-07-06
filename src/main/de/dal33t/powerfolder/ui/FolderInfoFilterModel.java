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
package de.dal33t.powerfolder.ui;

import java.util.LinkedList;
import java.util.List;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.FolderInfoChangedEvent;
import de.dal33t.powerfolder.event.FolderInfoFilterChangeListener;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.StringUtils;

/**
 * Based on the settings in this model it filters a Folder info List
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.5 $
 */
public class FolderInfoFilterModel extends FilterModel {
    private List<FolderInfo> folderList;
    private List<FolderInfo> filteredFolderList;

    private List<FolderInfoFilterChangeListener> listeners = new LinkedList<FolderInfoFilterChangeListener>();
    private boolean showEmpty;

    public FolderInfoFilterModel(Controller controller) {
        super(controller);
    }

    public boolean isShowEmpty() {
        return showEmpty;
    }

    public void setShowEmpty(boolean showEmpty) {
        this.showEmpty = showEmpty;
        scheduleFiltering();
    }

    public List<FolderInfo> filter(List<FolderInfo> aFolderList) {
        folderList = aFolderList;
        scheduleFiltering();
        return filteredFolderList;
    }

    public void scheduleFiltering() {
        if (folderList != null) {
            if (filteringNeeded()) {
                filteredFolderList = filter0();
                fireFolderInfoFilterChanged();
            } else {
                List<FolderInfo> old = filteredFolderList;
                filteredFolderList = folderList;
                if (old != filteredFolderList) {
                    fireFolderInfoFilterChanged();
                }
            }
        }
    }

    private boolean filteringNeeded() {
        if (!showEmpty) {
            return true;
        }
        String textFilter = (String) getSearchField().getValue();
        return !StringUtils.isBlank(textFilter);
    }

    private List<FolderInfo> filter0() {
        if (folderList == null) {
            throw new IllegalStateException("file list = null");
        }

        if (folderList.isEmpty()) {
            return folderList;
        }

        List<FolderInfo> tmpFilteredFolderInfoList = new LinkedList<FolderInfo>();
        // Prepare keywords from text filter
        String textFilter = (String) getSearchField().getValue();
        String[] keywords = null;
        if (!StringUtils.isBlank(textFilter)) {
            // Match lowercase
            textFilter = textFilter.toLowerCase();
            keywords = textFilter.split(" ");
        }

        for (Object aFolderList : folderList) {
            FolderInfo fInfo = (FolderInfo) aFolderList;

            boolean showFolderInfo = true;
            boolean isEmpty = fInfo.filesCount == 0;

            // text filter
            if (keywords != null) {
                // Check for match
                showFolderInfo = matches(fInfo, keywords);
            }

            if (isEmpty && !showEmpty) {
                showFolderInfo = false;
            }

            if (showFolderInfo) {
                tmpFilteredFolderInfoList.add(fInfo);
            }
        }
        return tmpFilteredFolderInfoList;
    }

    // Helper code ************************************************************

    /**
     * Answers if the folder matches the searching keywords. Keywords have to be
     * in lowercase. A folder must match all keywords. (AND)
     *
     * @param file
     *            the file
     * @param keywords
     *            the keyword array, all lowercase
     * @return the file matches the keywords
     */
    private static boolean matches(FolderInfo folder, String[] keywords) {
        if (keywords == null || keywords.length == 0) {
            return true;
        }

        for (int i = 0; i < keywords.length; i++) {
            String keyword = keywords[i];
            if (keyword == null) {
                throw new NullPointerException("Keyword empty at index " + i);
            }

            // Match for foldername
            String filename = folder.name.toLowerCase();
            if (filename.contains(keyword)) {
                // Match by name. Ok, continue
                continue;
            }

            // Keyword does not match file, break
            return false;
        }

        // All keywords matched !
        return true;
    }

    public void addFolderInfoFilterChangeListener(
        FolderInfoFilterChangeListener l)
    {
        listeners.add(l);
    }

    public void removeFolderInfoFilterChangeListener(
        FolderInfoFilterChangeListener l)
    {
        listeners.remove(l);
    }

    private void fireFolderInfoFilterChanged() {
        if (filteredFolderList != null) {
            FolderInfoChangedEvent event = new FolderInfoChangedEvent(this,
                filteredFolderList);
            for (FolderInfoFilterChangeListener listener : listeners) {

                listener.filterChanged(event);
            }
        }
    }

}

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

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.FolderInfoChangedEvent;
import de.dal33t.powerfolder.event.FolderInfoFilterChangeListener;
import de.dal33t.powerfolder.light.FolderInfo;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Based on the settings in this model it filters a Folder info List
 *
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.5 $
 */
public class FolderInfoFilterModel extends FilterModel {
    private List folderList;
    private List filteredFolderList;

    private List<FolderInfoFilterChangeListener> listeners = new LinkedList<FolderInfoFilterChangeListener>();
    private boolean showEmpty = false;

    public FolderInfoFilterModel(Controller controller) {
        super(controller);
    }

    /** reset to empty filter */
    public void reset() {
        showEmpty = false;
        getSearchField().setValue("");
    }

    public boolean isShowEmpty() {
        return showEmpty;
    }

    public void setShowEmpty(boolean showEmpty) {
        this.showEmpty = showEmpty;
        scheduleFiltering();
    }

    public List filter(List aFolderList) {
        this.folderList = aFolderList;
        scheduleFiltering();
        return filteredFolderList;
    }

    public void scheduleFiltering() {
        if (folderList != null) {
            if (filteringNeeded()) {
                filteredFolderList = filter0();
                fireFolderInfoFilterChanged();
            } else {
                List old = filteredFolderList;
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
        if (!StringUtils.isBlank(textFilter)) {
            return true;
        }
        return false;
    }

    private List filter0() {
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
        if (StringUtils.isBlank(textFilter)) {
            // Set to null to improve performance later in loop
            textFilter = null;
        } else {
            // Match lowercase
            textFilter = textFilter.toLowerCase();
            StringTokenizer nizer = new StringTokenizer(textFilter, " ");
            keywords = new String[nizer.countTokens()];
            int i = 0;
            while (nizer.hasMoreTokens()) {
                keywords[i++] = nizer.nextToken();
            }
        }

        for (int i = 0; i < folderList.size(); i++) {
            FolderInfo fInfo = (FolderInfo) folderList.get(i);

            boolean showFolderInfo = true;
            boolean isEmpty = fInfo.filesCount == 0;

            // text filter
            if (textFilter != null) {
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
    private boolean matches(FolderInfo folder, String[] keywords) {
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
            if (filename.indexOf(keyword) >= 0) {
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
            for (int i = 0; i < listeners.size(); i++) {

                FolderInfoFilterChangeListener listener = listeners.get(i);
                listener.filterChanged(event);
            }
        }
    }

}

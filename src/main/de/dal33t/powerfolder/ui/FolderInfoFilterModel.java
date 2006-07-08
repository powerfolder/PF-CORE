package de.dal33t.powerfolder.ui;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.event.FilterChangedEvent;
import de.dal33t.powerfolder.event.FolderInfoFilterChangeListener;
import de.dal33t.powerfolder.light.FolderDetails;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;

/**
 * Based on the settings in this model it filters a Folder info List
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.5 $
 */
public class FolderInfoFilterModel extends FilterModel {
    private List folderList;
    private List filteredFolderList;

    private List listeners = new LinkedList();
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
        filter();
    }

    public List filter(List aFolderList) {
        this.folderList = aFolderList;
        filter();
        return filteredFolderList;
    }

    public void filter() {
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
        if (folderList == null)
            throw new IllegalStateException("file list = null");

        if (folderList.size() == 0) {
            return folderList;
        }

        List tmpFilteredFolderInfoList = new LinkedList();
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
                showFolderInfo = matches(fInfo, keywords)
                    || matchesMember(fInfo, keywords);
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
    private boolean matchesMember(FolderInfo folder, String[] keywords) {
        if (keywords == null || keywords.length == 0) {
            return true;
        }
        FolderDetails details = folder.getFolderDetails(getController());
        if (details != null) {
            MemberInfo[] members = details.getMembers();

            for (int i = 0; i < keywords.length; i++) {
                String keyword = keywords[i];
                if (keyword == null) {
                    throw new NullPointerException("Keyword empty at index "
                        + i);
                }

                if (match(members, keyword)) {
                    continue; // keyword matches a member
                }
                return false; // no match for this keyword
            }
            return true; // all keywords match
        }
        return false; //no details, so no members
    }

    private boolean match(MemberInfo[] members, String keyword) {
        for (int y = 0; y < members.length; y++) {
            MemberInfo member = members[y];
            if (member.nick != null) {
                if (member.nick.indexOf(keyword) >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

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
            FilterChangedEvent event = new FilterChangedEvent(this,
                filteredFolderList);
            for (int i = 0; i < listeners.size(); i++) {
                
                FolderInfoFilterChangeListener listener = (FolderInfoFilterChangeListener) listeners
                    .get(i);                
                listener.filterChanged(event);
            }
        }
    }

}

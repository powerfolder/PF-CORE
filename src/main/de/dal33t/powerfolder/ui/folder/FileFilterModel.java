package de.dal33t.powerfolder.ui.folder;

import java.util.*;

import org.apache.commons.lang.StringUtils;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Directory;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.event.FileFilterChangeListener;
import de.dal33t.powerfolder.event.FilterChangedEvent;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MP3FileInfo;
import de.dal33t.powerfolder.ui.FilterModel;

/**
 * Based on the settings in this model it filters a filelist.
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.1 $
 */
public class FileFilterModel extends FilterModel {
    private List filelist;
    private List filteredFilelist;
    private Folder folder;

    private List listeners = new LinkedList();
    private boolean showNormal = true;
    private boolean showExpected = true;
    private boolean showDeleted = false;

    // The number of files
    private int deletedCount;
    private int expectedCount;
    private int normalCount;

    public FileFilterModel(Controller controller) {
        super(controller);
    }

    /** reset to empty filter */
    public void reset() {
        showNormal = true;
        showExpected = true;
        showDeleted = false;
        getSearchField().setValue("");
    }

    public boolean isShowDeleted() {
        return showDeleted;
    }

    public void setShowDeleted(boolean showDeleted) {
        this.showDeleted = showDeleted;
        filter();
    }

    public boolean isShowExpected() {
        return showExpected;
    }

    public void setShowExpected(boolean showExpected) {
        this.showExpected = showExpected;
        filter();
    }

    public boolean isShowNormal() {
        return showNormal;
    }

    public void setShowNormal(boolean showNormal) {
        this.showNormal = showNormal;
        filter();
    }

    // Totals *****************************************************************

    public int getDeletedCount() {
        return deletedCount;
    }

    public int getExpectedCount() {
        return expectedCount;
    }

    public int getNormalCount() {
        return normalCount;
    }

    public List filter(Folder folder, List filelist) {
       
        this.folder = folder;
        this.filelist = filelist;
        filter();
        return filteredFilelist;
    }

    Thread filterThread;
    public void filter() {
        if (filelist != null) {
            if (filteringNeeded()) {
                if (filterThread !=null) {                    
                    filterThread.interrupt();
                    filterThread = null;
                }
               
                filterThread = new FilterThread();
                filterThread.setName("FileFilter");
                filterThread.start();
            } else {
                deletedCount = -1;
                expectedCount = -1;
                normalCount = -1;
                countFiles();
                List old = filteredFilelist;
                filteredFilelist = filelist;
                if (old != filteredFilelist) {
                    fireFileFilterChanged();
                }
            }
        }
    }
    
    public class FilterThread extends Thread {
        boolean intteruppedFiltering = false;
        
        public void run() {            
            filteredFilelist = filter0();
            if (!intteruppedFiltering) {                
                fireFileFilterChanged();
            } else {                
            }
            filterThread = null;
        }
        
        private List filter0() {
            if (filelist == null)
                throw new IllegalStateException("file list = null");

            expectedCount = 0;
            deletedCount = 0;
            normalCount = 0;

            if (filelist.size() == 0) {
                return filelist;
            }

            List tmpFilteredFilelist = Collections.synchronizedList(new ArrayList(
                filelist.size()));
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

            FolderRepository repo = getController().getFolderRepository();
            for (int i = 0; i < filelist.size(); i++) {
                if (Thread.interrupted()) {                    
                    intteruppedFiltering = true;
                    break;
                }
                Object obj = filelist.get(i);
                if (obj instanceof FileInfo) {
                    FileInfo fInfo = (FileInfo) filelist.get(i);

                    boolean showFile = true;
                    boolean isDeleted = fInfo.isDeleted();
                    boolean isExpected = fInfo.isExpected(repo);
                    boolean isNormal = !isDeleted && !isExpected;

                    // text filter
                    if (textFilter != null) {
                        // Check for match
                        showFile = matches(fInfo, keywords)
                            || matchesMeta(fInfo, keywords);
                    }

                    if (isDeleted && folder != null && !folder.isKnown(fInfo)) {
                        // Do never show deleted files from remote members
                        showFile = false;
                    }

                    if (isDeleted && !showDeleted) {
                        showFile = false;
                    }
                    if (isExpected && !showExpected) {
                        showFile = false;
                    }
                    if (isNormal && !showNormal) {
                        showFile = false;
                    }
                    // Calculate number of files in category
                    // Deleted only counted if known, (never count deleted from
                    // remote)
                    deletedCount += isDeleted && folder != null
                        && folder.isKnown(fInfo) ? 1 : 0;
                    expectedCount += isExpected ? 1 : 0;
                    normalCount += isNormal ? 1 : 0;

                    if (showFile) {
                        tmpFilteredFilelist.add(fInfo);
                    }
                } else if (obj instanceof Directory) {
                    Directory directory = (Directory) obj;
                    // text filter
                    if (textFilter != null) {
                        // Check for match
                        if (!matches(directory, keywords)) {
                            continue;
                        }
                    }
                    boolean isDeleted = directory.isDeleted();
                    boolean isExpected = directory.isExpected(repo);
                    boolean isNormal = !isDeleted && !isExpected;
                    if (isDeleted && !showDeleted) {
                        continue;
                    }
                    if (isExpected && !showExpected) {
                        continue;
                    }
                    if (isNormal && !showNormal) {
                        continue;
                    }
                    tmpFilteredFilelist.add(directory);
                } else
                    throw new IllegalStateException("Unknown type, cannot filter");
            }
            return tmpFilteredFilelist;
        }

    }

    private void countFiles() {
        Runnable runner = new Runnable() {
            public void run() {
                int tmpExpectedCount = 0;
                int tmpDeletedCount = 0;
                int tmpNormalCount = 0;

                for (int i = 0; i < filelist.size(); i++) {
                    Object obj = filelist.get(i);
                    if (obj instanceof FileInfo) {
                        FileInfo fInfo = (FileInfo) obj;

                        boolean isDeleted = fInfo.isDeleted();
                        boolean isExpected = fInfo.isExpected(getController()
                            .getFolderRepository());
                        boolean isNormal = !isDeleted && !isExpected;

                        // Calculate number of files in category
                        // Deleted only counted if known, (never count deleted
                        // from remote)
                        tmpDeletedCount += isDeleted && folder != null
                            && folder.isKnown(fInfo) ? 1 : 0;
                        tmpExpectedCount += isExpected ? 1 : 0;
                        tmpNormalCount += isNormal ? 1 : 0;
                    }
                }
                expectedCount = tmpExpectedCount;
                deletedCount = tmpDeletedCount;
                normalCount = tmpNormalCount;
                fireFileCountChanged();
            }
        };
        Thread countThread = new Thread(runner);
        countThread.setName("FileFilter.Counter");
        countThread.start();
    }

    private boolean filteringNeeded() {
        if (!showNormal || !showExpected || !showDeleted) {
            return true;
        }
        String textFilter = (String) getSearchField().getValue();
        if (!StringUtils.isBlank(textFilter)) {
            return true;
        }
        return false;
    }
    
    // Helper code ************************************************************
    private static final boolean matches(Directory directory, String[] keywords) {
        if (keywords == null || keywords.length == 0) {
            return true;
        }
        for (int i = 0; i < keywords.length; i++) {
            String keyword = keywords[i];
            if (keyword == null) {
                throw new NullPointerException("Keyword empty at index " + i);
            }

            if (keyword.startsWith("-")) {
                // negative search:
                // remove the "-"
                keyword = keyword.substring(1);
                // must be something left
                if (keyword.length() != 0) {
                    // Match for directoryname
                    String directoryname = directory.getName().toLowerCase();
                    if (directoryname.indexOf(keyword) >= 0) {
                        // matches nagative search
                        return false;
                    }
                    // does not match negative search
                    continue;
                }
                //only a minus sign in the keyword ignore
                continue;
            }

            // Match for directoryname
            String directoryname = directory.getName().toLowerCase();
            if (directoryname.indexOf(keyword) >= 0) {
                // Match by name. Ok, continue
                continue;
            }
            // keyword did not match
            return false;
        }// all keywords matched!
        return true;
    }

    private static final boolean matchesMeta(FileInfo file, String[] keywords) {
        if (keywords == null || keywords.length == 0) {
            return true;
        }
        if (file instanceof MP3FileInfo) {
            return matchesMP3((MP3FileInfo) file, keywords);
        }
        return false;
    }

    private static final boolean matchesMP3(MP3FileInfo file, String[] keywords) {
        for (int i = 0; i < keywords.length; i++) {
            String keyword = keywords[i];
            if (keyword == null) {
                throw new NullPointerException("Keyword empty at index " + i);
            }

            // Match for title
            if (file.getTitle() != null
                && file.getTitle().toLowerCase().indexOf(keyword) >= 0)
            {
                // Match by title. Ok, continue
                continue;
            }

            // Match for album
            if (file.getAlbum() != null
                && file.getAlbum().toLowerCase().indexOf(keyword) >= 0)
            {
                // Match by album. Ok, continue
                continue;
            }
            // Match for artist
            if (file.getArtist() != null
                && file.getArtist().toLowerCase().indexOf(keyword) >= 0)
            {
                // Match by artist. Ok, continue
                continue;
            }

            // Keyword does not match file, break
            return false;
        }
        // all keywords match!
        return true;
    }

    /**
     * Answers if the file matches the searching keywords. Keywords have to be
     * in lowercase. A file must match all keywords. (AND)
     * 
     * @param file
     *            the file
     * @param keywords
     *            the keyword array, all lowercase
     * @return the file matches the keywords
     */
    private static final boolean matches(FileInfo file, String[] keywords) {
        if (keywords == null || keywords.length == 0) {
            return true;
        }

        for (int i = 0; i < keywords.length; i++) {
            String keyword = keywords[i];
            if (keyword == null) {
                throw new NullPointerException("Keyword empty at index " + i);
            }

            if (keyword.startsWith("-")) {
                // negative search:
                keyword = keyword.substring(1);
                if (keyword.length() != 0) {
                    // Match for filename
                    String filename = file.getName().toLowerCase();
                    if (filename.indexOf(keyword) >= 0) {
                        // if negative keyword match we don't want to see this file
                        return false;
                    }
                    // OR for nick
                    String modifierNick = file.getModifiedBy().nick.toLowerCase();
                    if (modifierNick.indexOf(keyword) >= 0) {
                        // if negative keyword match we don't want to see this file
                        return false;
                    }
                    // does not match the negative keyword
                    continue;
                }
                //only a minus sign in the keyword ignore
                continue;
            } // normal search

            // Match for filename
            String filename = file.getName().toLowerCase();
            if (filename.indexOf(keyword) >= 0) {
                // Match by name. Ok, continue
                continue;
            }

            // OR for nick
            String modifierNick = file.getModifiedBy().nick.toLowerCase();
            if (modifierNick.indexOf(keyword) >= 0) {
                // Match by name. Ok, continue
                continue;
            }

            // Keyword does not match file, break
            return false;
        }

        // All keywords matched !
        return true;
    }

    public void addFileFilterChangeListener(FileFilterChangeListener l) {
        listeners.add(l);
    }

    public void removeFileFilterChangeListener(FileFilterChangeListener l) {
        listeners.remove(l);
    }

    private void fireFileFilterChanged() {
        synchronized (this) {
            if (filteredFilelist != null) {
                FilterChangedEvent event = new FilterChangedEvent(this,
                    filteredFilelist);
                for (int i = 0; i < listeners.size(); i++) {
                    FileFilterChangeListener listener = (FileFilterChangeListener) listeners
                        .get(i);
                    listener.filterChanged(event);
                }
            }
        }
    }

    private void fireFileCountChanged() {
        synchronized (this) {
            if (filteredFilelist != null) {
                FilterChangedEvent event = new FilterChangedEvent(this,
                    filteredFilelist);
                for (int i = 0; i < listeners.size(); i++) {
                    FileFilterChangeListener listener = (FileFilterChangeListener) listeners
                        .get(i);
                    listener.countChanged(event);
                }
            }
        }
    }
}

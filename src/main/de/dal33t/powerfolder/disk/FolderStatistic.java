/* $Id: FolderStatistic.java,v 1.22 2006/03/15 16:50:46 schaatser Exp $
 */
package de.dal33t.powerfolder.disk;

import java.util.*;

import org.apache.commons.lang.time.DateUtils;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.PFComponent;
import de.dal33t.powerfolder.event.*;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.TransferCounter;
import de.dal33t.powerfolder.util.Util;

/**
 * Class to hold pre-calculated static data for a folder. Only freshly
 * calculated if needed
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.22 $
 */
public class FolderStatistic extends PFComponent {
    private final Folder folder;

    // Total size of folder in bytes
    private long totalSize;

    // Total number of files
    private int totalFilesCount;

    // Finer values
    private int totalNormalFilesCount;
    private int totalExpectedFilesCount;
    private int totalDeletedFilesCount;

    // The total sync percentage of the folder
    private double totalSyncPercentage;

    // Contains the sync percentages of the members
    // Member -> Double
    private Map syncPercentages = new HashMap();

    // Number of files
    // Member -> Integer
    private Map filesCount = new HashMap();

    // Size of folder per member
    // member -> Long
    private Map sizes = new HashMap();
    
    // Contains this Folder's download progress
    // It differs from other counters in that it does only count
    // the "accepted" traffic. (= If the downloaded chunk was saved to a file)
    // Used to calculate ETA
    private TransferCounter downloadCounter;
    
    /**
     * if the number of files is more than MAX_ITEMS the updates
     * will be delayed to a maximum, one update every minute
     */
    private int MAX_ITEMS = 200;
    private boolean isCalculating = false;
    private final int DELAY = DateUtils.MILLIS_IN_MINUTE;
    private long lastCalc;    
    private MyTimerTask task;

    FolderStatistic(final Folder folder) {
        super(folder.getController());
        if (folder == null) {
            throw new NullPointerException("Folder is null");
        }
        this.folder = folder;

        folder.addFolderListener(new MyFolderListener());
        folder.addMembershipListener(new MyFolderMembershipListener());

        // Add to NodeManager
        getController().getNodeManager().addNodeManagerListener(
            new MyNodeManagerListener());

        // Listener on transfer manager
        folder.getController().getTransferManager().addListener(
            new MyTransferManagerListener());
    }
    
    // Component listener *****************************************************

    /**
     * FolderMembershipListener
     */
    private class MyFolderMembershipListener implements FolderMembershipListener
    {
        public void memberJoined(FolderMembershipEvent folderEvent) {
            // Recalculate statistics
            calculate();
        }

        public void memberLeft(FolderMembershipEvent folderEvent) {
            // Recalculate statistics
            calculate();
        }
    }

    /**
     * FolderListener
     */
    private class MyFolderListener implements FolderListener {
        public void remoteContentsChanged(FolderEvent folderEvent) {
            calculate();
        }

        public void folderChanged(FolderEvent folderEvent) {
            // Recalculate statistics
            calculate();
        }

        public void statisticsCalculated(FolderEvent folderEvent) {
            // do not implement may cause loop!
        }

        public void syncProfileChanged(FolderEvent folderEvent) {
        }
    }

    /**
     * Listener on transfermanager
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class MyTransferManagerListener extends TransferAdapter {
        public void downloadCompleted(TransferManagerEvent event) {
            // Calculate new statistic when download completed
            if (event.getFile().getFolderInfo().equals(folder.getInfo())) {
                calculate();
            }
        }
    }

    /**
     * Listens to the nodemanager and triggers recalculcation if required
     * 
     * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
     */
    private class MyNodeManagerListener implements NodeManagerListener {
        public void nodeRemoved(NodeManagerEvent e) {
            calculateIfRequired(e);
        }

        public void nodeAdded(NodeManagerEvent e) {
            calculateIfRequired(e);
        }

        public void nodeConnected(NodeManagerEvent e) {
            // Do not calculate, since memberJoined is always fired
            //calculateIfRequired(e);
        }

        public void nodeDisconnected(NodeManagerEvent e) {
            calculateIfRequired(e);
        }

        public void friendAdded(NodeManagerEvent e) {
            calculateIfRequired(e);
        }

        public void friendRemoved(NodeManagerEvent e) {
            calculateIfRequired(e);
        }

        public void settingsChanged(NodeManagerEvent e) {
        }

        private void calculateIfRequired(NodeManagerEvent e) {
            if (!folder.hasMember(e.getNode())) {
                // Member not on folder
                return;
            }
            calculate();
        }
    }
    
    //package protected called from Folder
    void calculate() {
        if (isCalculating) {
            log().verbose("calc stats blocked " + folder.getName());
            return;
        }
        long millisPast = System.currentTimeMillis() - lastCalc;
        if (task != null) {
            log().verbose("calc stats blocked2 " + folder.getName());
            return;
        }
        if (millisPast > DELAY
            || totalFilesCount < MAX_ITEMS)
        {
            log().verbose("calc stats direct  " + folder.getName());
            setCalculateIn(0);
        } else {
            log().verbose("calc stats delayed " + folder.getName());
            setCalculateIn(DELAY);
        }
    }
    
    // Timer code *************************************************************

    private class MyTimerTask extends TimerTask {
        public void run() {
            calculate0();
            task = null;
        }
    }

    private void setCalculateIn(int timeToWait) {        
        if (task != null) {
            return;
        }
        task = new MyTimerTask();
        try {
            getController().schedule(task, timeToWait);
        } catch(IllegalStateException ise) {
            //ignore this happends if this shutdown in debug mode
            log().verbose(ise);
        }
    }

    long startTime = System.currentTimeMillis();
    /**
     * Calculates the statistics
     */
    synchronized void calculate0() {
        log().verbose("calc stats  " +folder.getName() + " stats@: " + (System.currentTimeMillis() - startTime));
        isCalculating = true;
        
        //log().verbose("Recalculation statisitcs on " + folder);
        // clear statistics before
        syncPercentages.clear();
        filesCount.clear();
        sizes.clear();

        // Get ALL files, also offline users
        FileInfo[] allFiles = folder.getAllFiles(true);

        totalNormalFilesCount = 0;
        totalExpectedFilesCount = 0;
        totalDeletedFilesCount = 0;

        // Containing all deleted files
        Set deletedFiles = new HashSet();

        for (int i = 0; i < allFiles.length; i++) {
            FileInfo fInfo = allFiles[i];
            if (fInfo.isDeleted()) {
                totalDeletedFilesCount++;
                deletedFiles.add(fInfo);
            } else if (fInfo.isExpected(folder.getController()
                .getFolderRepository()))
            {
                totalExpectedFilesCount++;
            } else {
                totalNormalFilesCount++;
            }
        }

        log().verbose(
            "Got " + deletedFiles.size() + " total deleted files on folder");

        // calculate total sizes
        totalSize = Util.calculateSize(allFiles, true);
        totalFilesCount = allFiles.length;

        int nCalculatedMembers = 0;
        double totalSyncTemp = 0;

        Member[] members = folder.getMembers();
        for (int i = 0; i < members.length; i++) {
            Member member = members[i];
            FileInfo[] memberFileList = folder.getFiles(member);
            if (memberFileList == null) {
                continue;
            }

            long memberSize = Util.calculateSize(memberFileList, true);
            int memberFileCount = memberFileList.length;

            Set memberFiles = new HashSet(Arrays.asList(memberFileList));
            for (Iterator it = deletedFiles.iterator(); it.hasNext();) {
                FileInfo deletedOne = (FileInfo) it.next();
                if (!memberFiles.contains(deletedOne)) {
                    // Add to my size
                    memberSize += deletedOne.getSize();
                    memberFileCount++;
                }
            }
            
            if ((downloadCounter == null || 
            		downloadCounter.getBytesExpected() != totalSize) && member.isMySelf()) {
            	// Initialize downloadCounter with appropriate values
        		downloadCounter = new TransferCounter(memberSize, totalSize);
            }
            
            double syncPercentage = (((double) memberSize) / totalSize) * 100;
            if (totalSize == 0) {
                syncPercentage = 100;
            }

            nCalculatedMembers++;
            totalSyncTemp += syncPercentage;

            syncPercentages.put(member, new Double(syncPercentage));
            filesCount.put(member, new Integer(memberFileCount));
            sizes.put(member, new Long(memberSize));
        }

        // Calculate total sync
        totalSyncPercentage = totalSyncTemp / nCalculatedMembers;

        log().verbose(
            "Recalculated: " + totalNormalFilesCount + " normal, "
                + totalExpectedFilesCount + " expected, "
                + totalDeletedFilesCount + " deleted");

        // Fire event
        folder.fireStatisticsCalculated();
        lastCalc = System.currentTimeMillis();
        isCalculating = false;
        log().verbose("calc stats  " +folder.getName() + " done @: " + (System.currentTimeMillis() - startTime));        
    }

    /**
     * Answers if we have statistics for this member
     * 
     * @param member
     * @return
     */
    public boolean hasStatistic(Member member) {
        return syncPercentages.get(member) != null;
    }

    /**
     * Answers the number of files this member has
     * 
     * @param member
     * @return
     */
    public int getFilesCount(Member member) {
        Integer count = (Integer) filesCount.get(member);
        return count != null ? count.intValue() : 0;
    }

    /**
     * Answsers the members size of this folder
     * 
     * @param member
     * @return
     */
    public long getSize(Member member) {
        Long size = (Long) sizes.get(member);
        return size != null ? size.longValue() : 0;
    }

    /**
     * Answers the sync percentage of a member
     * 
     * @return the sync percentage of the member, -1 if unknown
     */
    public double getSyncPercentage(Member member) {
        Double sync = (Double) syncPercentages.get(member);
        return sync != null ? sync.doubleValue() : -1;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public int getTotalFilesCount() {
        return totalFilesCount;
    }

    public int getTotalDeletedFilesCount() {
        return totalDeletedFilesCount;
    }

    public int getTotalExpectedFilesCount() {
        return totalExpectedFilesCount;
    }

    /** number of local files */
    public int getTotalNormalFilesCount() {
        return totalNormalFilesCount;
    }

    public double getTotalSyncPercentage() {
        return totalSyncPercentage;
    }

    public Folder getFolder() {
        return folder;
    }

	/**
	 * Returns the download-TransferCounter for this Folder 
	 * @return a TransferCounter or null if no such information is available
	 * 			(might be available later)
	 */
	public TransferCounter getDownloadCounter() {
		return downloadCounter;
	}

    // Logging interface ******************************************************

    public String getLoggerName() {
        return "FolderStatistic '" + folder.getName() + "'";
    }

    // General ****************************************************************

    public String toString() {
        return "Folder statistic on '" + folder.getName() + "'";
    }
}
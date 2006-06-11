/* $Id: Debug.java,v 1.30 2006/04/23 18:14:41 bytekeeper Exp $
 */
package de.dal33t.powerfolder.util;

import java.io.*;
import java.util.*;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.message.NodeInformation;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.Upload;

/**
 * Utility class with methods for debugging
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.30 $
 */
public class Debug {
    private static final Logger LOG = Logger.getLogger(Debug.class);

    private Debug() {
        // No instance allowed
    }

    /**
     * Writes a list of files to disk for debuging info. Sorted
     * 
     * @param fileInfos
     * @param header
     * @param logFile
     * @return
     */
    public static boolean writeFileList(Collection<FileInfo> fileInfos,
        String header, File logFile)
    {
        if (logFile == null) {
            throw new NullPointerException("Logfile is null");
        }
        if (fileInfos == null) {
            throw new NullPointerException("Files are null");
        }
        if (!logFile.exists()) {
            try {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            } catch (IOException e) {
                LOG.error("Unable to write filelist to "
                    + logFile.getAbsolutePath());
                LOG.verbose(e);
                return false;
            }
        }
        if (!logFile.canWrite()) {
            LOG.error("Unable to write filelist to "
                + logFile.getAbsolutePath());
            return false;
        }

        // Copy & Sort
        FileInfo[] list = new FileInfo[fileInfos.size()];
        fileInfos.toArray(list);
        Arrays.sort(list, new FileInfoComparator(FileInfoComparator.BY_NAME));

        try {
            LOG.verbose("Writing log list debug file: "
                + logFile.getAbsolutePath());
            OutputStream fOut = new BufferedOutputStream(new FileOutputStream(
                logFile));
            fOut.write(header.getBytes());
            fOut.write("\n".getBytes());
            for (int i = 0; i < list.length; i++) {
                fOut.write((list[i].toDetailString() + "\n").getBytes());
            }

            fOut.flush();
            fOut.close();
            return true;
        } catch (IOException e) {
            LOG.error("Unable to write filelist to "
                + logFile.getAbsolutePath());
            LOG.verbose(e);
        }
        return false;
    }

    /**
     * Builds a debug report for remote analyse
     * 
     * @param controller
     * @return
     */
    public static String buildDebugReport(Controller c) {
        if (c == null) {
            throw new NullPointerException("Controller is null");
        }
        synchronized (c) {
            StringBuffer b = new StringBuffer();
            b.append("PowerFolder debug report\n");
            b.append("------------------------");
            // information about myself and local port binding

            long uptimeMinutes = c.getUptime() / 1000 / 60;

            b.append("\nVersion: " + Controller.PROGRAM_VERSION + " ("
                + c.getBuildTime() + ")");
            b.append("\nConfig: " + c.getConfigName());
            b.append("\nCurrent time: " + new Date());
            b.append("\nLocale: " + Locale.getDefault() + " ("
                + Locale.getDefault().getDisplayCountry() + ")");
            b.append("\nUptime: " + uptimeMinutes + " minutes");
            b.append("\nOS: " + System.getProperty("os.name"));

            b.append("\nNetworking mode: ");
            if (c.isPublicNetworking()) {
                b.append("Public");
            } else if (c.isPrivateNetworking() ){
                b.append("Private");
            } else {
                b.append("Lan Only");
            }

            b.append("\n\nListener status: ");
            if (c.hasLimitedConnectivity()) {
                b.append("\nWARNING: Has only limited connectivity\n");
            }
            if (c.hasListener()) {
                b.append("Listening on ");
                b.append(c.getConnectionListener().getLocalAddress());
                if (c.getMySelf() != null
                    && c.getMySelf().getInfo().isSupernode)
                {
                    b.append(", acting as supernode");
                } else {
                    b.append(", acting as standardnode");
                }
                b.append("\n");
            } else {
                b.append("Not listening on a local port\n");
            }
            b.append("MySelf: ");
            addDetailInfo(b, c.getMySelf());
            if (c.getNodeManager().getMasterNode() != null) {
                b.append("\nMaster: " + c.getNodeManager().getMasterNode());
            }

            b.append("\n");

            if (c.isStarted()) {

                // All folders
                FolderInfo[] folders = c.getFolderRepository()
                    .getJoinedFolderInfos();
                int nNetFolders = c.getFolderRepository()
                    .getNumberOfNetworkFolder();

                b.append("\nFolders (" + folders.length + " joined, "
                    + nNetFolders + " known)");
                for (int i = 0; i < folders.length; i++) {
                    b.append("\n ");
                    Folder folder = c.getFolderRepository().getFolder(
                        folders[i]);
                    addDetailInfo(b, folder);
                }
                b.append("\n");
                if (folders.length == 0) {
                    b.append(" (none)\n");
                }

                TransferManager tm = c.getTransferManager();
                // dump transfers
                Download[] downloads = c.getTransferManager()
                    .getActiveDownloads();
                b.append("\nDownloads ("
                    + downloads.length
                    + " total, "
                    + Format.NUMBER_FORMATS.format(tm.getDownloadCounter()
                        .calculateCurrentKBS())
                    + " Kbytes/s, "
                    + Format.formatBytes(tm.getDownloadCounter()
                        .getBytesTransferred()) + " bytes total):");

                for (int i = 0; i < downloads.length; i++) {
                    b.append("\n ");
                    Download dl = downloads[i];
                    b.append(dl.isStarted() ? "(active)" : (dl.isQueued()
                        ? "(queued)"
                        : "(requested)"));
                    b.append(" " + dl);
                }
                b.append("\n");
                if (downloads.length == 0) {
                    b.append(" (none)\n");
                }

                Upload[] actULs = tm.getActiveUploads();
                Upload[] quedULs = tm.getQueuedUploads();
                b.append("\nUploads ("
                    + tm.getAllowedUploads()
                    + " allowed, "
                    + actULs.length
                    + " active, "
                    + quedULs.length
                    + " queued, "
                    + Format.NUMBER_FORMATS.format(tm.getUploadCounter()
                        .calculateCurrentKBS())
                    + " Kbytes/s, "
                    + Format.NUMBER_FORMATS
                        .format(tm.getAllowedUploadCPSForWAN() / 1024)
                    + " Kbyte/s allowed, "
                    + Format.formatBytes(tm.getUploadCounter()
                        .getBytesTransferred()) + " bytes total):");

                List uploads = new ArrayList(actULs.length + quedULs.length);
                uploads.addAll(Arrays.asList(actULs));
                uploads.addAll(Arrays.asList(quedULs));
                for (Iterator it = uploads.iterator(); it.hasNext();) {
                    Upload upload = (Upload) it.next();
                    b.append("\n ");
                    b.append(upload.isStarted() ? "(active)" : "(queued)");
                    b.append(" " + upload);
                }
                b.append("\n");
                if (actULs.length == 0 && quedULs.length == 0) {
                    b.append(" (none)\n");
                }

                // all members
                Member[] knownMembers = c.getNodeManager().getNodes();
                // Sort
                Arrays.sort(knownMembers, MemberComparator.IN_GUI);
                b.append("\nAll online nodes ("
                    + c.getNodeManager().countConnectedNodes() + " connected, "
                    + c.getNodeManager().countOnlineNodes() + " online, "
                    + c.getNodeManager().getNodes().length + " known, "
                    + c.getNodeManager().countSupernodes() + " supernodes, "
                    + c.getNodeManager().countFriends() + " friend(s)):");
                for (int i = 0; i < knownMembers.length; i++) {
                    if (knownMembers[i].isConnectedToNetwork()) {
                        b.append("\n ");
                        addDetailInfo(b, knownMembers[i]);
                    }
                }
                b.append("\n");
                if (knownMembers.length == 0) {
                    b.append(" (none)\n");
                }
            } else {
                b.append("Controller NOT started yet\n");
            }

            // config dump, even if controller is not started yet
            b.append("\nConfig:");
            Properties config = (Properties) c.getConfig().clone();
            // Sort config by name
            List sortedConfigKeys = new ArrayList(config.keySet());
            Collections.sort(sortedConfigKeys);
            for (Iterator it = sortedConfigKeys.iterator(); it.hasNext();) {
                String key = (String) it.next();
                String value = config.getProperty(key);
                // Erase folder ids, keep it secret!
                if (key.indexOf(".id") >= 5) {
                    value = "XXX-erased-XXX";
                } 
                // Erase all passwords                
                if (key.toLowerCase().indexOf("password") != -1) {
                    value = "XXX-erased-XXX";
                }
                b.append("\n   " + key + " = " + value);
            }
            b.append("\n");

            /*
             * b.append("\nFolder details:"); for (int i = 0; i <
             * folders.length; i++) { b.append("\n "); addFullInfo(b,
             * c.getFolderRepository().getFolder(folders[i])); } b.append("\n");
             */

            return b.toString();
        }
    }

    /**
     * Adds a detailed info about the member to the buffer
     * 
     * @param b
     * @param m
     */
    private static void addDetailInfo(StringBuffer b, Member m) {
        if (b == null || m == null) {
            return;
        }
        if (m.isMySelf()) {
            b.append("(myself) ");
        } else if (m.isConnected()) {
            if (m.isOnLAN()) {
                b.append("(local)  ");
            } else {
                b.append("(i-net)  ");
            }
        } else if (m.isConnectedToNetwork()) {
            b.append("(online) ");
        } else {
            b.append("(offl.)  ");
        }
        if (m.getInfo().isSupernode) {
            b.append("(s) ");
        }
        b.append(m);
        Identity id = m.getIdentity();
        b.append(", ver. " + (id != null ? id.programVersion : "-") + ", ID: "
            + m.getId());
        b.append(", reconnect address " + m.getReconnectAddress());
    }

    /**
     * Adds detailed info about the folder to buffer
     * 
     * @param b
     * @param f
     */
    private static void addDetailInfo(StringBuffer b, Folder f) {
        if (b == null || f == null) {
            return;
        }
        b.append(f);
        if (f.isSecret()) {
            b.append(", ID: XXX-erased-XXX");
        } else {
            b.append(", ID: " + f.getId());
        }
        b.append(", files: " + f.getFilesCount() + ", size: "
            + Format.formatBytes(f.getInfo().bytesTotal) + ", members: "
            + f.getMembers().length + ", sync: " + f.getSyncProfile());
    }

    /**
     * Writes debug report to disk.
     * 
     * @see #loadDebugReport(MemberInfo)
     * @param nodeInfo
     */
    public static boolean writeNodeInformation(NodeInformation nodeInfo) {
        if (nodeInfo == null) {
            throw new NullPointerException("NodeInfo is null");
        }

        String fileName;
        if (nodeInfo.node != null) {
            fileName = "Node." + nodeInfo.node.nick + ".report.txt";
        } else {
            fileName = "Node.-unknown-.report.txt";
        }
        try {
            // Create in debug directory
            // Create dir
            new File("debug/nodeinfos").mkdirs();

            OutputStream fOut = new BufferedOutputStream(new FileOutputStream(
                "debug/nodeinfos/" + fileName));
            fOut.write(nodeInfo.debugReport.getBytes());
            fOut.close();
            return true;
        } catch (IOException e) {
            LOG.error(e);
        }
        return false;
    }

    /**
     * Loads a stored debug report from disk for that node
     * 
     * @see #writeNodeInformation(NodeInformation)
     * @param node
     * @return
     */
    public static String loadDebugReport(MemberInfo node) {
        Reject.ifNull(node, "Node is null");
        String fileName = "Node." + node.nick + ".report.txt";
        try {
            File file = new File("debug/nodeinfos/" + fileName);
            InputStream fIn = new BufferedInputStream(new FileInputStream(file));

            byte[] buffer = new byte[(int) file.length()];
            fIn.read(buffer);
            return new String(buffer);
        } catch (IOException e) {
            LOG.warn("Debug report for " + node.nick + " not found ("
                + fileName + ")");
            LOG.verbose(e);
        }
        return null;
    }

    /**
     * Writes statistics to disk
     * 
     * @param c
     */
    public static void writeStatistics(Controller c) {
        OutputStream fOut = null;
        try {
            fOut = new BufferedOutputStream(new FileOutputStream(c
                .getConfigName()
                + ".netstat.csv", true));
            Date now = new Date();
            String statLine = Format.FULL_DATE_FOMRAT.format(now) + ";"
                + now.getTime() + ";"
                + c.getNodeManager().countConnectedNodes() + ";"
                + c.getNodeManager().countOnlineNodes() + "\n";
            fOut.write(statLine.getBytes());
        } catch (IOException e) {
            LOG.warn("Unable to write network statistics file", e);
            // Ignore
        } finally {
            try {
                fOut.close();
            } catch (Exception e) {
                //ignore
            }
        }
    }
}
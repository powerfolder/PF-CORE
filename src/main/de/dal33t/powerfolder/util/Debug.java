/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.util;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.message.NodeInformation;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.net.ConnectionQuality;
import de.dal33t.powerfolder.search.LuceneIndexManager;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.util.compare.FileInfoComparator;
import de.dal33t.powerfolder.util.compare.MemberComparator;
import de.dal33t.powerfolder.util.logging.LoggingManager;

import java.io.*;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class with methods for debugging
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.30 $
 */
public class Debug {

    private static final Logger log = Logger.getLogger(Debug.class.getName());
    private static final MyThreadLocal DATE_FORMAT = new MyThreadLocal();

    // private static Map<File, Collection<Object>> fileWatch = new
    // HashMap<File, Collection<Object>>();

    private Debug() {
        // No instance allowed
    }

    public static long countDataitems(Controller controller) {
        long dbSize = 0;
        for (Folder folder : controller.getFolderRepository().getFolders(true))
        {
            for (Member member : folder.getMembersAsCollection()) {
                dbSize += folder.getDAO().count(member.getId(), true, false);
            }
        }
        return dbSize;
    }

    /**
     * Dumps the system properties into the debug directory.
     */
    public static void writeSystemProperties() {
        if (!LoggingManager.isLogToFile()) {
            return;
        }
        Path file = LoggingManager.getDebugDir().resolve(
            "system_properties.txt");
        try {
            Properties sysprops = System.getProperties();
            PropertiesUtil.saveConfig(file, sysprops, "Current time: "
                + new Date());
        } catch (FileNotFoundException e) {
            log.severe("Unable to create SystemInfo file");
        } catch (IOException e) {
            log.severe("Unable to Write to '" + file + '\'');
        }
    }

    /**
     * Writes a list of files to disk as CSV file.
     *
     * @param folderName
     * @param memberName
     * @param fileInfos
     * @param header
     * @return the CSV file. Or null if failed
     */
    public static Path writeFileListCSV(String folderName, String memberName,
        Collection<FileInfo> fileInfos, String header)
    {
        Reject.ifBlank(folderName, "folderName is null");
        Reject.ifBlank(memberName, "memberName is null");
        Reject.ifNull(fileInfos, "Files are null");
        Path filelistsDir = LoggingManager.getDebugDir().resolve("filelists");
        try {
            Files.createDirectories(filelistsDir);
        } catch (IOException ioe) {
            return null;
        }
        Path logFile = filelistsDir.resolve(PathUtils
            .removeInvalidFilenameChars(folderName)
            + "/"
            + PathUtils.removeInvalidFilenameChars(memberName) + ".list.txt");
        return writeFileListCSV(logFile, fileInfos, header);
    }

    /**
     * Writes a list of files to disk as CSV file.
     *
     * @param logFile
     * @param fileInfos
     * @param header
     * @return the CSV file or null if failed.
     */
    public static Path writeFileListCSV(Path logFile,
        Collection<FileInfo> fileInfos, String header)
    {
        if (Files.notExists(logFile)) {
            try {
                if (logFile.getParent() != null
                    && Files.notExists(logFile.getParent()))
                {
                    try {
                        Files.createDirectories(logFile.getParent());
                    } catch (IOException ioe) {
                        return null;
                    }
                }
                Files.createFile(logFile);
            } catch (IOException e) {
                log.severe("Unable to write filelist to "
                    + logFile.toAbsolutePath().toString());
                log.log(Level.FINER, "IOException", e);
                return null;
            }
        }
        if (!Files.isWritable(logFile)) {
            log.severe("Unable to write filelist to "
                + logFile.toAbsolutePath().toString());
            return null;
        }

        // Copy & Sort
        FileInfo[] list = fileInfos.toArray(new FileInfo[fileInfos.size()]);
        Arrays.sort(list, new FileInfoComparator(
            FileInfoComparator.BY_MODIFIED_DATE));

        try (OutputStream fOut = Files.newOutputStream(logFile)) {
            fOut.write(("# " + header + "\n\n").getBytes("UTF-8"));
            fOut.write("Change time      ;Filename;Changer;Size;Version\n\n"
                .getBytes());
            for (FileInfo aList : list) {
                fOut.write(toCSVLine(aList).getBytes("UTF-8"));
            }
            return logFile;
        } catch (IOException e) {
            log.severe("Unable to write nodelist to '"
                + logFile.toAbsolutePath().toString() + '\'');
            log.log(Level.FINER, "IOException", e);
        }

        return null;
    }

    /**
     * Details infos about the fileinfo to a comma separated line.
     *
     * @param f
     */
    private static String toCSVLine(FileInfo f) {
        Reject.ifNull(f, "FileInfo is null");
        StringBuilder b = new StringBuilder();

        b.append(f.getModifiedDate() != null ? DATE_FORMAT.get().format(
            f.getModifiedDate()) : "-");
        b.append(" ;");

        if (f.isDeleted()) {
            b.append("(del) ");
        }
        b.append(f.getRelativeName());
        b.append(f.isDiretory() ? " (D)" : "");
        b.append(';');

        b.append(f.getModifiedBy().nick);
        b.append(';');

        b.append(Format.formatBytes(f.getSize()));
        b.append(';');

        b.append(f.getVersion());
        b.append('\n');

        return b.toString();
    }

    /**
     * Builds a debug report for remote analyse
     *
     * @param c
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

            b.append("\nVersion: " + Controller.PROGRAM_VERSION + " ("
                + c.getBuildTime() + ')');
            b.append("\nConfig: " + c.getConfigName());
            b.append("\nCurrent time: " + new Date());
            b.append("\nLocale: " + Locale.getDefault() + " ("
                + Locale.getDefault().getDisplayCountry() + ')');
            b.append("\nUptime: " + Format.formatTimeframe(c.getUptime()));
            b.append("\nOS: " + System.getProperty("os.name"));
            b.append("\nJava: " + JavaVersion.systemVersion().toString() + " ("
                + System.getProperty("java.vendor") + ')');
            long usedMemory =  Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            b.append("\nMemory: " + Format.formatBytesShort(usedMemory)
                    + "/" + Format.formatBytesShort(Runtime.getRuntime().totalMemory())
                    + "/" + Format.formatBytesShort(Runtime.getRuntime().maxMemory()));
            b.append("\nDataitems: " + countDataitems(c));
            if (c.isStarted()) {
                addSearchIndexInfo(b, c.getFolderRepository().getFolders(true));
            }
            b.append("\nNetworking mode: ");
            b.append(c.getNetworkingMode().name());
            double uptimeDays = ((double) c.getUptime()) / 1000 / 60 / 60 / 24;
            int nWarnings = LoggingManager.getCountingHandler().countWarnings();
            b.append("\nWarnings: ");
            b.append(nWarnings);
            b.append(" total (~");
            b.append(Math.round(nWarnings / uptimeDays));
            b.append(" per day)");
            int nSevere = LoggingManager.getCountingHandler().countSevere();
            b.append("\nSevere: ");
            b.append(nSevere);
            b.append(" total (~");
            b.append(Math.round(nSevere / uptimeDays));
            b.append(" per day)");

            double upKBS = c.getTransferManager()
                .getTotalUploadTrafficCounter().calculateCurrentKBS();
            double downKBS = c.getTransferManager()
                .getTotalDownloadTrafficCounter().calculateCurrentKBS();
            long upBytes = c.getTransferManager()
                .getTotalUploadTrafficCounter().getBytesTransferred();
            long downBytes = c.getTransferManager()
                .getTotalDownloadTrafficCounter().getBytesTransferred();
            b.append("\nTotal traffic: DOWN " + Format.formatDecimal(downKBS)
                + " Kbytes/s, " + Format.formatBytes(downBytes)
                + " bytes total, UP " + Format.formatDecimal(upKBS)
                + " Kbytes/s, " + Format.formatBytes(upBytes) + " bytes total");

            if (c.isLimitedConnectivity()) {
                b.append("\nWARNING: Has limited connectivity");
            }

            b.append("\n\nListener status: ");
            if (c.hasConnectionListener()) {
                b.append("Listening on ");
                b.append(c.getConnectionListener().getAddress());
                if (c.getMySelf() != null
                    && c.getMySelf().getInfo().isSupernode)
                {
                    b.append(", acting as supernode");
                } else {
                    b.append(", acting as standardnode");
                }
                b.append('\n');
            } else {
                b.append("Not listening on a local port\n");
            }
            b.append("MySelf: ");
            addDetailInfo(b, c.getMySelf());

            b.append('\n');

            if (c.isStarted()) {
                // All folders
                Collection<Folder> folders = c.getFolderRepository()
                    .getFolders(true);

                b.append("\nFolders (" + folders.size() + " joined)");
                for (Folder folder : folders) {
                    b.append("\n ");
                    addDetailInfo(b, folder);
                }
                b.append('\n');
                if (folders.isEmpty()) {
                    b.append(" (none)\n");
                }

                TransferManager tm = c.getTransferManager();
                // dump transfers
                Collection<DownloadManager> downloads = c.getTransferManager()
                    .getActiveDownloads();
                b.append("\nDownloads ("
                    + downloads.size()
                    + " total, "
                    + Format.formatDecimal(tm.getDownloadCounter()
                        .calculateCurrentKBS())
                    + " Kbytes/s, "
                    + Format.formatBytes(tm.getDownloadCounter()
                        .getBytesTransferred()) + " bytes total):");
                for (DownloadManager man : downloads) {
                    for (Download dl : man.getSources()) {
                        b.append("\n ");
                        b.append(dl.isStarted() ? "(active)" : (dl.isQueued()
                            ? "(queued)"
                            : "(requested)"));
                        b.append(" " + dl);
                    }
                }
                b.append('\n');
                if (downloads.isEmpty()) {
                    b.append(" (none)\n");
                }

                b.append("\nUploads ("
                    + tm.countActiveUploads()
                    + " active, "
                    + tm.countQueuedUploads()
                    + " queued, "
                    + Format.formatDecimal(tm.getUploadCounter()
                        .calculateCurrentKBS())
                    + " Kbytes/s, "
                    + Format.formatDecimal(tm.getUploadCPSForWAN() / 1024)
                    + " Kbyte/s allowed, "
                    + Format.formatBytes(tm.getUploadCounter()
                        .getBytesTransferred()) + " bytes total):");

                List<Upload> uploads = new ArrayList<Upload>();
                uploads.addAll(tm.getActiveUploads());
                uploads.addAll(tm.getQueuedUploads());
                for (Object upload1 : uploads) {
                    Upload upload = (Upload) upload1;
                    b.append("\n ");
                    b.append(upload.isStarted() ? "(active)" : "(queued)");
                    b.append(" " + upload);
                }
                b.append('\n');
                if (uploads.isEmpty()) {
                    b.append(" (none)\n");
                }

                // all members
                Member[] knownMembers = c
                    .getNodeManager()
                    .getNodesAsCollection()
                    .toArray(
                        new Member[c.getNodeManager().getNodesAsCollection()
                            .size()]);
                // Sort
                Arrays.sort(knownMembers, MemberComparator.IN_GUI);
                b.append("\nAll online nodes ("
                    + c.getNodeManager().countConnectedNodes() + " connected, "
                    + c.getNodeManager().countOnlineNodes() + " online, "
                    + c.getNodeManager().getNodesAsCollection().size()
                    + " known, " + c.getNodeManager().countSupernodes()
                    + " supernodes, " + c.getNodeManager().countFriends()
                    + " friend(s)):");
                for (Member knownMember : knownMembers) {
                    if (knownMember.isConnectedToNetwork()) {
                        b.append("\n ");
                        addDetailInfo(b, knownMember);
                    }
                }
                b.append('\n');
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
            for (Object sortedConfigKey : sortedConfigKeys) {
                String key = (String) sortedConfigKey;
                String value = config.getProperty(key);
                // Erase all passwords
                if (key.toLowerCase().contains("password")) {
                    value = "XXX-erased-XXX";
                }
                if (key.toLowerCase().contains("passphrase")) {
                    value = "XXX-erased-XXX";
                }
                if (key.toLowerCase().contains("license")) {
                    value = "XXX-erased-XXX";
                }
                if (key.toLowerCase().contains("login.admin.iplist")) {
                    value = "XXX-erased-XXX";
                }
                if (key.toLowerCase().contains("secret.key")) {
                    value = "XXX-erased-XXX";
                }
                b.append("\n" + key + "=" + value);
            }
            b.append('\n');

            /*
             * b.append("\nFolder details:"); for (int i = 0; i <
             * folders.length; i++) { b.append("\n "); addFullInfo(b,
             * c.getFolderRepository().getFolder(folders[i])); } b.append("\n");
             */

            return b.toString();
        }
    }

    /**
     * Adds the state of the Lucene search indexes: how many files are still queued for indexing. Summed up over all
     * folders - the individual folders are not listed.
     *
     * @param b
     * @param folders
     */
    private static void addSearchIndexInfo(StringBuffer b, Collection<Folder> folders) {
        int indexedFolders = 0;
        int rebuildingFolders = 0;
        int totalPending = 0;
        int totalContentPending = 0;
        long totalEntries = 0;
        for (Folder folder : folders) {
            LuceneIndexManager index = folder.getSearchIndexManager();
            if (index == null) {
                continue;
            }
            indexedFolders++;
            if (index.isRebuilding()) {
                rebuildingFolders++;
            }
            totalPending += index.getPendingCount();
            totalContentPending += index.getContentPendingCount();
            int entries = index.getIndexEntryCount();
            if (entries > 0) {
                totalEntries += entries;
            }
        }
        b.append("\nSearch index: ");
        if (indexedFolders == 0) {
            b.append("disabled");
            return;
        }
        b.append(indexedFolders).append(" of ").append(folders.size()).append(" folders indexed, ").append(totalEntries)
            .append(" entries, ").append(totalPending).append(" file(s) pending, ").append(totalContentPending)
            .append(" file(s) pending content, ").append(rebuildingFolders).append(" folder(s) rebuilding");
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
        b.append(toDetailInfo(m));
    }

    /**
     * Details infos about the member.
     *
     * @param m
     */
    private static String toDetailInfo(Member m) {
        Reject.ifNull(m, "Member is null");
        StringBuilder b = new StringBuilder();
        if (m.isMySelf()) {
            b.append("(me) ");
        } else if (m.isConnected()) {
            if (m.isOnLAN()) {
                b.append("(LAN) ");
            } else {
                ConnectionHandler peer = m.getPeer();
                if (peer != null) {
                    ConnectionQuality q = peer.getConnectionQuality();
                    if (q.equals(ConnectionQuality.GOOD)) {
                        b.append("(***) ");
                    } else if (q.equals(ConnectionQuality.MEDIUM)) {
                        b.append("(** ) ");
                    } else {
                        b.append("(*  ) ");
                    }
                } else {
                    b.append("(???) ");
                }
            }
        } else if (m.isConnectedToNetwork()) {
            b.append("(on ) ");
        } else {
            b.append("(off) ");
        }
        if (m.getInfo().isSupernode) {
            b.append("(s) ");
        }
        b.append(m);
        Identity id = m.getIdentity();
        b.append(", ver. " + (id != null ? id.getProgramVersion() : "-")
            + ", ID: " + m.getId());
        b.append(", reconnect address " + m.getReconnectAddress());
        return b.toString();
    }

    /**
     * Details infos about the member ad a comma separated line.
     *
     * @param m
     */
    private static String toCSVLine(Member m) {
        Reject.ifNull(m, "Member is null");
        StringBuilder b = new StringBuilder();

        if (m.isMySelf()) {
            b.append("myself");
        } else if (m.isConnected()) {
            if (m.isOnLAN()) {
                b.append("connected (local)");
            } else {
                b.append("connected (i-net)");
            }
        } else if (m.isConnectedToNetwork()) {
            b.append("online");
        } else {
            b.append("offline");
        }

        b.append(';');
        if (m.getInfo().isSupernode) {
            b.append('s');
        } else {
            b.append('n');
        }

        b.append(';');
        b.append(m.getNick());

        b.append(';' + m.getId());

        b.append(';');
        Identity id = m.getIdentity();
        b.append(id != null ? id.getProgramVersion() : "-");

        b.append(";" + m.getReconnectAddress());
        b.append(";" + m.getLastConnectTime());
        b.append(";" + m.getLastNetworkConnectTime());
        return b.toString();
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
        b.append(", ID: " + f.getId());
        b.append(", files: " + f.getKnownItemCount() + ", size: "
            + Format.formatBytes(f.getStatistic().getLocalSize())
            + ", members: " + f.getMembersCount() + ", mode: "
            + f.getSyncProfile().getName() + ", sync: "
            + Format.formatPercent(+f.getStatistic().getLocalSyncPercentage()));
    }

    /**
     * Writes debug report to disk.
     *
     * @see #loadDebugReport(MemberInfo)
     * @param nodeInfo
     * @return if succeeded
     */
    public static boolean writeNodeInformation(NodeInformation nodeInfo) {
        if (nodeInfo == null) {
            throw new NullPointerException("NodeInfo is null");
        }
        String fileName;
        if (nodeInfo.node != null) {
            fileName = PathUtils.removeInvalidFilenameChars(nodeInfo.node.nick)
                + ".report.txt";
        } else {
            fileName = "-unknown-.report.txt";
        }
        try {
            // Create in debug directory
            // Create dir
            Path dir = LoggingManager.getDebugDir().resolve("nodeinfos");
            Files.createDirectories(dir);
            OutputStream fOut = new BufferedOutputStream(
                Files.newOutputStream(dir.resolve(fileName)));
            fOut.write(nodeInfo.debugReport.getBytes());
            fOut.close();
            return true;
        } catch (IOException e) {
            log.log(Level.FINER, "IOException", e);
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
            Path file = LoggingManager.getDebugDir().resolve(
                "nodeinfos/" + fileName);
            InputStream fIn = new BufferedInputStream(
                Files.newInputStream(file));

            byte[] buffer = new byte[(int) Files.size(file)];
            fIn.read(buffer);
            return new String(buffer);
        } catch (IOException e) {
            log.warning("Debug report for " + node.nick + " not found ("
                + fileName + ')');
            // Loggable.logFinerStatic(Debug.class, e);
        }
        return null;
    }

    /**
     * Writes a list of nodes to a debut output file.
     *
     * @param nodes
     *            the list of nodes
     * @param fileName
     *            the filename to write to
     */
    public static void writeNodeList(Collection<Member> nodes, String fileName)
    {
        Reject.ifNull(nodes, "Nodelist is null");
        try (OutputStream fOut = Files.newOutputStream(LoggingManager
            .getDebugDir().resolve(fileName))) {
            for (Member node : nodes) {
                fOut.write(toDetailInfo(node).getBytes());
                fOut.write("\n".getBytes());
            }
        } catch (IOException e) {
            log.warning("Unable to write nodelist to '" + fileName + '\'');
            log.log(Level.FINER, "IOException", e);
        }
    }

    /**
     * Writes a list of nodes to a debut output file in csv format.
     *
     * @param nodes
     *            the list of nodes
     * @param fileName
     *            the filename to write to
     */
    public static void writeNodeListCSV(Collection<Member> nodes,
        String fileName)
    {
        Reject.ifNull(nodes, "Nodelist is null");
        try (OutputStream fOut = Files.newOutputStream(LoggingManager
            .getDebugDir().resolve(fileName))) {
            fOut.write("connect;supernode;nick;id;version;address;last connect time;last online time\n"
                .getBytes());
            synchronized (nodes) {
                for (Member node : nodes) {
                    fOut.write(toCSVLine(node).getBytes());
                    fOut.write("\n".getBytes());
                }
            }
        } catch (IOException e) {
            log.warning("Unable to write nodelist to '" + fileName + '\'');
            log.log(Level.FINER, "IOException", e);

        }
    }

    /**
     * Writes statistics to disk
     *
     * @param controller
     */
    public static void writeStatistics(Controller controller) {
        OutputStream fOut = null;
        try {
            Path file = LoggingManager.getDebugDir().resolve(
                controller.getConfigName() + ".netstat.csv");
            Files.createDirectories(file.getParent());
            fOut = new BufferedOutputStream(Files.newOutputStream(file,
                StandardOpenOption.APPEND));
            Date now = new Date();
            String statLine = Format.formatDateShort(now) + ';' + now.getTime()
                + ';' + controller.getNodeManager().countConnectedNodes() + ';'
                + controller.getNodeManager().countOnlineNodes() + ';'
                + controller.getNodeManager().getNodesAsCollection().size()
                + '\n';
            fOut.write(statLine.getBytes());
        } catch (IOException e) {
            log.log(Level.WARNING, "Unable to write network statistics file", e);
            // Ignore
        } finally {
            try {
                if (fOut != null) {
                    fOut.close();
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    // public static void openedFile(File f, Object src) {
    // synchronized (fileWatch) {
    // Collection<Object> o = fileWatch.get(f);
    // if (o == null) {
    // o = new HashSet<Object>();
    // fileWatch.put(f, o);
    // }
    // o.add(src);
    // }
    // }
    //
    // public static void closedFile(File f, Object src) {
    // synchronized (fileWatch) {
    // Collection<Object> o = fileWatch.get(f);
    // if (o == null) {
    // throw new IllegalStateException("File isn't open!");
    // }
    // if (!o.remove(src)) {
    // throw new IllegalStateException("File isn't open by " + src);
    // }
    // }
    // }
    //
    // public static void dumpOpeners(File f) {
    // synchronized (fileWatch) {
    // Collection<Object> o = fileWatch.get(f);
    // if (o != null) {
    // for (Object s : o) {
    // Loggable.logWarningStatic(Debug.class, f + " opened by "
    // + s);
    // }
    // } else {
    // Loggable.logWarningStatic(Debug.class, f + " is not open!");
    // }
    // }
    // System.exit(1);
    // }
    //
    // public static void dumpOpenFiles() {
    // synchronized (fileWatch) {
    // for (File f : fileWatch.keySet()) {
    // dumpOpeners(f);
    // }
    // }
    // }

    public static void dumpThreadStacks() {
        ThreadGroup top = Thread.currentThread().getThreadGroup();
        while (top.getParent() != null) {
            top = top.getParent();
        }
        showGroupInfo(top);
    }

    public static String getStackTrace(StackTraceElement[] stack) {
        StringBuilder b = new StringBuilder();

        for (StackTraceElement e : stack) {
            b.append(e.toString()).append('\n');
        }
        return b.toString();
    }

    public static void dumpCurrentStackTrace() {
        log.fine(getCurrentStackTrace());
    }

    public static String getCurrentStackTrace() {
        return getStackTrace(Thread.currentThread().getStackTrace());
    }

    /**
     * Dumps all threads in the layout HotSpot itself uses, so the result can be read by the usual
     * analysers (jstack output, thread dump analyzers) instead of only by eye.
     * <p>
     * os_prio, tid, nid and the stack pointer that jstack prints are HotSpot internals with no Java
     * API behind them. They are left out rather than filled with substitutes, everything else the
     * platform exposes is here - including the monitor and synchronizer information that the old
     * ThreadGroup walk could not reach.
     *
     * @param hideIdleThreads
     *            only dump threads that are actually doing something
     */
    public static String dumpCurrentStacktraces(boolean hideIdleThreads) {
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        ThreadInfo[] infos;
        try {
            infos = mx.dumpAllThreads(mx.isObjectMonitorUsageSupported(),
                mx.isSynchronizerUsageSupported());
        } catch (UnsupportedOperationException e) {
            // Lock information is optional for a JVM. The stack traces are worth having without it.
            infos = mx.dumpAllThreads(false, false);
        }

        StringBuilder b = new StringBuilder();
        b.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append('\n');
        b.append("Full thread dump ").append(System.getProperty("java.vm.name")).append(" (")
            .append(System.getProperty("java.vm.version")).append(' ')
            .append(System.getProperty("java.vm.info")).append("):\n");

        for (ThreadInfo info : infos) {
            if (info == null) {
                // The thread died between dumpAllThreads and here.
                continue;
            }
            if (hideIdleThreads && isIdle(info)) {
                continue;
            }
            appendThread(b, mx, info);
        }
        appendDeadlocks(b, mx);
        return b.toString();
    }

    private static void appendThread(StringBuilder b, ThreadMXBean mx, ThreadInfo info) {
        b.append('\n').append('"').append(info.getThreadName()).append('"');
        b.append(" #").append(info.getThreadId());
        if (info.isDaemon()) {
            b.append(" daemon");
        }
        b.append(" prio=").append(info.getPriority());
        long cpuNanos = cpuTime(mx, info.getThreadId());
        if (cpuNanos >= 0) {
            b.append(" cpu=").append(cpuNanos / 1000000L).append("ms");
        }
        b.append(' ').append(runStateOf(info)).append('\n');

        b.append("   java.lang.Thread.State: ").append(info.getThreadState());
        String detail = stateDetailOf(info);
        if (detail != null) {
            b.append(" (").append(detail).append(')');
        }
        b.append('\n');

        StackTraceElement[] stack = info.getStackTrace();
        MonitorInfo[] monitors = info.getLockedMonitors();
        for (int i = 0; i < stack.length; i++) {
            b.append("\tat ").append(stack[i]).append('\n');
            if (i == 0 && info.getLockInfo() != null) {
                b.append("\t- ").append(waitVerbOf(info)).append(' ').append(lockText(info.getLockInfo()));
                if (info.getLockOwnerName() != null) {
                    b.append(" owned by \"").append(info.getLockOwnerName()).append("\" #")
                        .append(info.getLockOwnerId());
                }
                b.append('\n');
            }
            for (MonitorInfo monitor : monitors) {
                if (monitor.getLockedStackDepth() == i) {
                    b.append("\t- locked ").append(lockText(monitor)).append('\n');
                }
            }
        }

        b.append('\n').append("   Locked ownable synchronizers:\n");
        LockInfo[] synchronizers = info.getLockedSynchronizers();
        if (synchronizers.length == 0) {
            b.append("\t- None\n");
        } else {
            for (LockInfo synchronizer : synchronizers) {
                b.append("\t- ").append(lockText(synchronizer)).append('\n');
            }
        }
    }

    private static void appendDeadlocks(StringBuilder b, ThreadMXBean mx) {
        long[] deadlocked;
        try {
            deadlocked = mx.findDeadlockedThreads();
        } catch (UnsupportedOperationException e) {
            deadlocked = mx.findMonitorDeadlockedThreads();
        }
        if (deadlocked == null || deadlocked.length == 0) {
            b.append('\n').append("Found no Java-level deadlocks.\n");
            return;
        }
        b.append('\n').append("Found ").append(deadlocked.length)
            .append(" Java-level deadlocked thread(s):\n");
        for (ThreadInfo info : mx.getThreadInfo(deadlocked, true, true)) {
            if (info == null) {
                continue;
            }
            b.append("\t\"").append(info.getThreadName()).append("\" #").append(info.getThreadId());
            if (info.getLockInfo() != null) {
                b.append(" waiting for ").append(lockText(info.getLockInfo()));
            }
            if (info.getLockOwnerName() != null) {
                b.append(" owned by \"").append(info.getLockOwnerName()).append("\" #")
                    .append(info.getLockOwnerId());
            }
            b.append('\n');
        }
    }

    private static long cpuTime(ThreadMXBean mx, long threadId) {
        if (!mx.isThreadCpuTimeSupported() || !mx.isThreadCpuTimeEnabled()) {
            return -1;
        }
        try {
            return mx.getThreadCpuTime(threadId);
        } catch (UnsupportedOperationException e) {
            return -1;
        }
    }

    /**
     * The identity hash code, not the address jstack prints - the Java API exposes no address. It
     * identifies a lock within one dump, which is what matters when following who waits for whom.
     */
    private static String lockText(LockInfo lock) {
        return String.format("<0x%08x> (a %s)", lock.getIdentityHashCode(), lock.getClassName());
    }

    /**
     * The word jstack puts in front of the lock a thread is waiting for, which differs by how it
     * waits: a monitor is entered, a condition is parked on, Object.wait() is waited on.
     */
    private static String waitVerbOf(ThreadInfo info) {
        if (info.getThreadState() == Thread.State.BLOCKED) {
            return "waiting to lock";
        }
        return isParked(info) ? "parking to wait for" : "waiting on";
    }

    /**
     * The description behind the thread header, e.g. "waiting for monitor entry".
     */
    private static String runStateOf(ThreadInfo info) {
        switch (info.getThreadState()) {
            case RUNNABLE:
                return "runnable";
            case BLOCKED:
                return "waiting for monitor entry";
            case WAITING:
            case TIMED_WAITING:
                if (isParked(info)) {
                    return "waiting on condition";
                }
                return isSleeping(info) ? "sleeping" : "in Object.wait()";
            default:
                return info.getThreadState().toString().toLowerCase();
        }
    }

    /**
     * The parenthesis behind java.lang.Thread.State, or {@code null} when there is none.
     */
    private static String stateDetailOf(ThreadInfo info) {
        switch (info.getThreadState()) {
            case BLOCKED:
                return "on object monitor";
            case WAITING:
            case TIMED_WAITING:
                if (isParked(info)) {
                    return "parking";
                }
                return isSleeping(info) ? "sleeping" : "on object monitor";
            default:
                return null;
        }
    }

    private static boolean isParked(ThreadInfo info) {
        return topFrameIs(info, "jdk.internal.misc.Unsafe", "park")
            || topFrameIs(info, "sun.misc.Unsafe", "park");
    }

    /**
     * Thread.sleep sits behind version dependent frames - sleep, sleepNanos, sleepNanos0 - so the
     * top frames are scanned for the method prefix instead of matching one exact name.
     */
    private static boolean isSleeping(ThreadInfo info) {
        StackTraceElement[] stack = info.getStackTrace();
        for (int i = 0; i < Math.min(3, stack.length); i++) {
            if ("java.lang.Thread".equals(stack[i].getClassName())
                && stack[i].getMethodName().startsWith("sleep"))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean topFrameIs(ThreadInfo info, String className, String methodName) {
        StackTraceElement[] stack = info.getStackTrace();
        return stack.length > 0 && className.equals(stack[0].getClassName())
            && methodName.equals(stack[0].getMethodName());
    }

    /**
     * Whether a thread is only sitting in one of the usual waits and has nothing to tell. Same
     * intent as the filter this replaces: keep what runs or is blocked, drop the parked pools,
     * pollers and acceptors.
     */
    private static boolean isIdle(ThreadInfo info) {
        Thread.State state = info.getThreadState();
        if (state != Thread.State.RUNNABLE && state != Thread.State.BLOCKED) {
            return true;
        }
        for (StackTraceElement frame : info.getStackTrace()) {
            String at = frame.toString();
            for (String idle : IDLE_FRAMES) {
                if (at.contains(idle)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Frames that mean "waiting for work or for the network", not "doing something".
     */
    private static final String[] IDLE_FRAMES = {
        "java.lang.ref.Reference.waitForReferencePendingList",
        "sun.nio.ch.EPoll.wait",
        "sun.nio.ch.Net.poll",
        "sun.nio.ch.Net.accept",
        "sun.nio.ch.SocketDispatcher.read",
        "java.net.SocketInputStream.socketRead0",
        "java.lang.Thread.sleep",
        "java.lang.Object.wait",
        "sun.awt.windows.WToolkit.eventLoop",
        "sun.misc.Unsafe.park",
        "jdk.internal.misc.Unsafe.park",
        "java.net.PlainSocketImpl.socketAccept",
        "java.net.SocketOutputStream.socketWrite0(Native Method)",
        "java.net.PlainDatagramSocketImpl.receive0",
        "java.lang.Thread.getStackTrace",
        "java.net.PlainSocketImpl.socketConnect",
        "net.contentobjects.jnotify.linux.JNotify_linux.nativeNotifyLoop",
        "de.dal33t.powerfolder.util.net.UDTSocket.recv",
        "sun.nio.ch.ServerSocketChannelImpl.accept0",
        "java.net.DualStackPlainSocketImpl.accept0",
        "sun.nio.ch.WindowsSelectorImpl$SubSelector.poll0",
        "sun.nio.ch.ServerSocketChannelImpl.accept",
        "java.net.TwoStacksPlainDatagramSocketImpl.receive0"};

    private static String detailedObjectState0(Class<?> c, Object o) {
        if (c == Object.class) {
            return "";
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append(detailedObjectState0(c.getSuperclass(), o));

        Field[] fields = c.getDeclaredFields();
        for (Field fld : fields) {
            fld.setAccessible(true);
            buffer.append("; [").append("Field: ").append(fld.getName());
            buffer.append(", toString: ");
            try {
                Object value = fld.get(o);
                buffer.append('(').append(value).append(')');
            } catch (IllegalArgumentException e) {
                buffer.append(e);
            } catch (IllegalAccessException e) {
                buffer.append(e);
            }
            buffer.append(']');
            fld.setAccessible(false);
        }
        return buffer.toString();

    }

    public static String detailedObjectState(Object o) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("Class: ").append(o.getClass().getName());
        buffer.append(detailedObjectState0(o.getClass(), o));
        return buffer.toString();
    }

    private static void showGroupInfo(ThreadGroup group) {
        if (group == null) {
            return;
        }
        Thread threads[] = new Thread[group.activeCount()];
        group.enumerate(threads, false);
        log.fine("");
        log.fine(group + " ########################");

        for (Thread thread : threads) {
            if (thread != null) {
                log.fine('\"' + thread.getName() + "\" - Thread t@" + thread.hashCode());
                log.fine("   java.lang.Thread.State: " + thread.getState());
                dumpStackTrace(thread);
                log.fine("");
            }
        }
        ThreadGroup[] activeGroup = new ThreadGroup[group.activeGroupCount()];
        group.enumerate(activeGroup, false);

        int i = 0;
        while (i < activeGroup.length) {
            showGroupInfo(activeGroup[i]);
            i++;
        }
    }

    private static void dumpStackTrace(Thread t) {
        for (StackTraceElement te : t.getStackTrace()) {
            log.fine("        " + te);
        }
    }

    /**
     * ThreadLocal date formatter.
     */
    private static class MyThreadLocal extends ThreadLocal<DateFormat> {

        protected DateFormat initialValue() {
            return new SimpleDateFormat("dd-MM-yyyy HH:mm");
        }
    }
}

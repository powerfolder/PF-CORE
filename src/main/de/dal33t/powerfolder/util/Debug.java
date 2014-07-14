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
 * $Id: Debug.java 18809 2012-05-09 01:10:58Z tot $
 */
package de.dal33t.powerfolder.util;

import static de.dal33t.powerfolder.disk.FolderSettings.ID;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.message.NodeInformation;
import de.dal33t.powerfolder.net.ConnectionHandler;
import de.dal33t.powerfolder.net.ConnectionQuality;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.DownloadManager;
import de.dal33t.powerfolder.transfer.TransferManager;
import de.dal33t.powerfolder.transfer.Upload;
import de.dal33t.powerfolder.util.compare.FileInfoComparator;
import de.dal33t.powerfolder.util.compare.MemberComparator;
import de.dal33t.powerfolder.util.logging.LoggingManager;

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
        Path logFile = filelistsDir.resolve(
            PathUtils.removeInvalidFilenameChars(folderName) + "/"
                + PathUtils.removeInvalidFilenameChars(memberName)
                + ".list.txt");
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

            long uptimeMinutes = c.getUptime() / 1000 / 60;

            b.append("\nVersion: " + Controller.PROGRAM_VERSION + " ("
                + c.getBuildTime() + ')');
            b.append("\nConfig: " + c.getConfigName());
            b.append("\nCurrent time: " + new Date());
            b.append("\nLocale: " + Locale.getDefault() + " ("
                + Locale.getDefault().getDisplayCountry() + ')');
            b.append("\nUptime: " + uptimeMinutes + " minutes");
            b.append("\nOS: " + System.getProperty("os.name"));
            b.append("\nJava: " + JavaVersion.systemVersion().toString() + " ("
                + System.getProperty("java.vendor") + ')');

            b.append("\nNetworking mode: ");
            b.append(c.getNetworkingMode().name());

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
                // Erase folder ids, keep it secret!
                if (key.indexOf(ID) >= 5) {
                    value = "XXX-erased-XXX";
                }
                // Erase all passwords
                if (key.toLowerCase().contains("password")) {
                    value = "XXX-erased-XXX";
                }
                if (key.toLowerCase().contains("license")) {
                    value = "XXX-erased-XXX";
                }
                b.append("\n   " + key + " = " + value);
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
            b.append("(mee) ");
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
        b.append(", ID: XXX-erased-XXX");
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
            OutputStream fOut = new BufferedOutputStream(Files.newOutputStream(dir.resolve(fileName)));
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
            Path file = LoggingManager.getDebugDir().resolve("nodeinfos/"
                + fileName);
            InputStream fIn = new BufferedInputStream(Files.newInputStream(file));

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
            .getDebugDir().resolve(fileName)))
        {
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
            .getDebugDir().resolve(fileName)))
        {
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
            fOut = new BufferedOutputStream(Files.newOutputStream(file));
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

    public static String dumpCurrentStacktraces(boolean hideIdleThreds) {
        ThreadGroup top = Thread.currentThread().getThreadGroup();
        while (top.getParent() != null) {
            top = top.getParent();
        }
        StringBuilder b = new StringBuilder();
        for (String dumps : getGroupInfo(top, hideIdleThreds)) {
            b.append(dumps);
        }
        return b.toString();
    }

    private static List<String> getGroupInfo(ThreadGroup group,
        boolean hideIdleThreds)
    {
        Thread threads[] = new Thread[group.activeCount()];
        List<String> threadDumps = new LinkedList<String>();
        group.enumerate(threads, false);

        for (Thread thread : threads) {
            if (thread != null) {
                String threadDump = dumpStackTrace(thread, hideIdleThreds);
                if (StringUtils.isBlank(threadDump)) {
                    continue;
                }
                String dump = " " + thread
                    + " --------------------------------------\n";
                dump += threadDump;
                dump += "\n";
                threadDumps.add(dump);
            }
        }
        ThreadGroup[] activeGroup = new ThreadGroup[group.activeGroupCount()];
        group.enumerate(activeGroup, false);

        int i = 0;
        while (i < activeGroup.length) {
            threadDumps.addAll(getGroupInfo(activeGroup[i], hideIdleThreds));
            i++;
        }
        return threadDumps;
    }

    private static String dumpStackTrace(Thread t, boolean hideIdleThreds) {
        StringBuilder b = new StringBuilder();
        for (StackTraceElement te : t.getStackTrace()) {
            if (hideIdleThreds) {
                if (te.toString().contains(
                    "java.net.SocketInputStream.socketRead0"))
                {
                    return null;
                }
                if (te.toString().contains("java.lang.Thread.sleep")) {
                    return null;
                }
                if (te.toString().contains("java.lang.Object.wait")) {
                    return null;
                }
                if (te.toString()
                    .contains("sun.awt.windows.WToolkit.eventLoop"))
                {
                    return null;
                }
                if (te.toString().contains("sun.misc.Unsafe.park")) {
                    return null;
                }
                if (te.toString().contains(
                    "java.net.PlainSocketImpl.socketAccept"))
                {
                    return null;
                }
                if (te.toString().contains(
                    "java.net.SocketOutputStream.socketWrite0(Native Method)"))
                {
                    return null;
                }
                if (te.toString().contains(
                    "java.net.PlainDatagramSocketImpl.receive0"))
                {
                    return null;
                }
                if (te.toString().contains("java.lang.Thread.getStackTrace")) {
                    return null;
                }
                if (te.toString().contains(
                    "java.net.PlainSocketImpl.socketConnect"))
                {
                    return null;
                }
                if (te
                    .toString()
                    .contains(
                        "net.contentobjects.jnotify.linux.JNotify_linux.nativeNotifyLoop"))
                {
                    return null;
                }
                if (te.toString().contains(
                    "de.dal33t.powerfolder.util.net.UDTSocket.recv"))
                {
                    return null;
                }

            }

            b.append("  " + te);
            b.append("\n");
        }
        return b.toString();
    }

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
        Thread threads[] = new Thread[group.activeCount()];
        group.enumerate(threads, false);
        log.fine("");
        log.fine(group + " ########################");

        for (Thread thread : threads) {
            if (thread != null) {
                log.fine(" " + thread
                    + " --------------------------------------");
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
            log.fine("  " + te);
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

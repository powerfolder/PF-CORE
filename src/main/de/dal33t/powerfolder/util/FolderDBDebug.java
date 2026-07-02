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

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Reads a folder database file and writes it to a debug file (human readable).
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class FolderDBDebug {

    /**
     * @param args
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static void main(String[] args) throws IOException,
        ClassNotFoundException
    {
        String fn;
        if (args.length < 1) {
            // throw new IllegalArgumentException(
            // "The first argument has to be the filename of the folder database file");
            fn = "PowerFolder.db";
        } else {
            fn = args[0];
        }

        if (!fn.contains(".")) {
            fn += ".db";
        }
        InputStream fIn = new BufferedInputStream(new FileInputStream(fn));
        ObjectInputStream in = new ObjectInputStream(fIn);
        FileInfo[] files = (FileInfo[]) in.readObject();
        in.close();

        FolderInfo folderInfo = files.length > 0
            ? files[0].getFolderInfo()
            : null;
        String fName = folderInfo != null ? folderInfo.getLocalizedName() : "-unknown-";
        String fId = folderInfo != null ? folderInfo.getId() : "-unknown-";

        // --- Initial summary ---
        long totalSize = 0;
        int activeCount = 0;
        int deletedCount = 0;
        // Group active files by top-level subdirectory (or "." for root-level files)
        Map<String, long[]> dirStats = new LinkedHashMap<>(); // dir -> [count, bytes]
        for (FileInfo fileInfo : files) {
            if (fileInfo.isDeleted()) {
                deletedCount++;
                continue;
            }
            activeCount++;
            totalSize += fileInfo.getSize();
            String rel = fileInfo.getRelativeName();
            int sep = rel.indexOf('/');
            String dir = sep > 0 ? rel.substring(0, sep) : ".";
            dirStats.computeIfAbsent(dir, k -> new long[2]);
            dirStats.get(dir)[0]++;
            dirStats.get(dir)[1] += fileInfo.getSize();
        }
        if (folderInfo != null && folderInfo.isSubFolder()) {
            FolderInfo top = folderInfo.getTopFolder();
            String topName = top != null ? top.getLocalizedName() : "-unknown-";
            String topId   = top != null ? top.getId()            : "-unknown-";
            String location = folderInfo.getLocation() != null
                ? folderInfo.getLocation().getRelativeName() : fName;
            System.out.println("=== FolderDB (SUBFOLDER): " + location + " ===");
            System.out.println("  Top folder    : " + topName + " [" + topId + "]");
        } else {
            System.out.println("=== FolderDB: " + fName + " [" + fId + "] ===");
        }
        System.out.println("  Total entries : " + files.length
            + " (" + activeCount + " active, " + deletedCount + " deleted)");
        System.out.println("  Active size   : " + Format.formatBytesShort(totalSize));
        System.out.println("  Subdirectories: " + dirStats.size());
        System.out.println();
        // Sort by file count descending for quick overview
        dirStats.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
            .forEach(e -> System.out.printf("  %-60s  %6d files  %s%n",
                e.getKey(), e.getValue()[0], Format.formatBytesShort(e.getValue()[1])));
        System.out.println();
        // --- End summary ---

        if (!checkForDupes(files)) {
            System.out.println("OK: DB contains NO dupes.");
        }

        Path f = Paths.get(fn + ".csv");
        // Write filelist to disk
        Path outFile = Debug.writeFileListCSV(f, Arrays.asList(files),
            "FileList of folder " + fName + "/" + fId + ". " + folderInfo);

        System.out.println("Read " + files.length + " files ("
            + Format.formatBytesShort(totalSize) + ") from " + fn
            + ". \nOutput: " + outFile.toRealPath());

        PathUtils.openFile(outFile);
    }

    private static boolean checkForDupes(FileInfo[] list) {
        boolean dupe = false;
        Map<String, FileInfo> lowerCasenames = new HashMap<String, FileInfo>();
        for (FileInfo file : list) {
            if (lowerCasenames
                .containsKey(file.getRelativeName().toLowerCase()))
            {
                dupe = true;
                System.err
                    .println("Detected dupe: " + file.toDetailString() + " of "
                        + lowerCasenames
                            .get(file.getRelativeName().toLowerCase())
                            .toDetailString());
            }
            lowerCasenames.put(file.getRelativeName().toLowerCase(), file);
        }
        return dupe;
    }

}

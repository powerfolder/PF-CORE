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
 * $Id: FolderDBDebug.java 12618 2010-06-16 13:31:33Z tot $
 */
package de.dal33t.powerfolder.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;

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
        System.err.println(in.readObject());
        System.err.println(in.readObject());
        in.close();

        if (!checkForDupes(files)) {
            System.out.println("OK: DB contain NO dupes.");
        }

        FolderInfo folderInfo = files.length > 0
            ? files[0].getFolderInfo()
            : null;
        String fName = folderInfo != null ? folderInfo.getLocalizedName() : "-unknown-";
        String fId = folderInfo != null ? folderInfo.getId() : "-unknown-";
        long totalSize = 0;
        for (FileInfo fileInfo : files) {
            if (fileInfo.isDeleted()) {
                continue;
            }
            totalSize += fileInfo.getSize();
        }
        Path f = Paths.get(fn + ".csv");
        // Write filelist to disk
        Path outFile = Debug.writeFileListCSV(f, Arrays.asList(files),
            "FileList of folder " + fName + "/" + fId);

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

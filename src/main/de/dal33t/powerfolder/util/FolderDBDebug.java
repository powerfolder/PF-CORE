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
package de.dal33t.powerfolder.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
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
        if (args.length < 1) {
            throw new IllegalArgumentException(
                "The first argument has to be the filename of the folder database file");
        }

        InputStream fIn = new BufferedInputStream(new FileInputStream(args[0]));
        ObjectInputStream in = new ObjectInputStream(fIn);
        FileInfo[] files = (FileInfo[]) in.readObject();
        System.err.println(in.readObject());
        System.err.println(in.readObject());

        if (!checkForDupes(files)) {
            System.out.println("OK: DB contain NO dupes.");
        }

        FolderInfo folderInfo = files.length > 0
            ? files[0].getFolderInfo()
            : null;
        String fName = folderInfo != null ? folderInfo.name : "-unknown-";

        long totalSize = 0;
        for (FileInfo fileInfo : files) {
            if (fileInfo.isDeleted()) {
                continue;
            }
            totalSize += fileInfo.getSize();
        }
        File f = new File(args[0] + ".csv");
        // Write filelist to disk
        File outFile = Debug.writeFileListCSV(f, Arrays.asList(files),
            "FileList of folder " + fName);

        System.out.println("Read " + files.length + " files ("
            + Format.formatBytesShort(totalSize) + ") from " + args[0]
            + ". \nOutput: " + outFile.getCanonicalPath());
    }

    private static boolean checkForDupes(FileInfo[] list) {
        boolean dupe = false;
        Map<String, FileInfo> lowerCasenames = new HashMap<String, FileInfo>();
        for (FileInfo file : list) {
            if (lowerCasenames
                .containsKey(file.getRelativeName().toLowerCase()))
            {
                dupe = true;
                System.err.println("Detected dupe: "
                    + file.toDetailString()
                    + " of "
                    + lowerCasenames.get(file.getRelativeName().toLowerCase())
                        .toDetailString());
            }
            lowerCasenames.put(file.getRelativeName().toLowerCase(), file);
        }
        return dupe;
    }

}

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

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;

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

        if (!checkForDupes(files)) {
            System.out.println("OK: DB contain NO dupes.");
        }

        FolderInfo folderInfo = (files.length > 0)
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
        // Write filelist to disk
        File debugFile = new File(Logger.getDebugDir().getAbsolutePath()
            + "/Folder '" + fName + "'.list.txt");
        Debug.writeFileListCSV(Arrays.asList(files), "FileList of folder "
            + fName, debugFile);

        System.out.println("Read " + files.length + " files ("
            + Format.formatBytesShort(totalSize) + ") from " + args[0] + " to "
            + debugFile.getAbsolutePath());
    }

    private static boolean checkForDupes(FileInfo[] list) {
        boolean dupe = false;
        HashSet<String> lowerCasenames = new HashSet<String>();
        for (FileInfo file : list) {
            if (lowerCasenames.contains(file.getName().toLowerCase())) {
                dupe = true;
                System.err.println("Detected dupe: " + file.toDetailString());
            }
            lowerCasenames.add(file.getName().toLowerCase());
        }
        return dupe;
    }

}

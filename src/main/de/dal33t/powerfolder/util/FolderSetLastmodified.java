/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import de.dal33t.powerfolder.light.FileInfo;

/**
 * Reads a folder database and sets the last modified date of the files
 * according to the database IF the size of the file is completely identical.
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class FolderSetLastmodified {
    public static void main(String[] args) throws IOException,
        ClassNotFoundException

    {
        File in = new File(args[0]);
        if (!in.getName().equalsIgnoreCase("PowerFolders")) {
            correct(in);
            correct(new File(in, ".PowerFolder/meta"));
            return;
        } else {
            File[] dirs = in.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
            });
            for (File dir : dirs) {
                correct(dir);
                correct(new File(dir, ".PowerFolder/meta"));
            }
        }
    }

    public static void correct(File localBase) {
        try {

            File dbFile = new File(localBase, ".PowerFolder/.PowerFolder.db");
            InputStream fIn = new BufferedInputStream(new FileInputStream(
                dbFile));
            ObjectInputStream in = new ObjectInputStream(fIn);
            FileInfo[] files = (FileInfo[]) in.readObject();
            int changed = 0;
            int same = 0;
            int total = 0;
            for (FileInfo fileInfo : files) {
                if (fileInfo.isDeleted()) {
                    continue;
                }
                total++;
                File file = new File(localBase, fileInfo.getRelativeName());
                if (!file.exists()) {
                    System.err.println("Skip file not found: " + file);
                    continue;
                }
                if (fileInfo.isFile() && file.length() != fileInfo.getSize()) {
                    System.err.println("Skip file size not identical found: "
                        + file.length() + ". expected: " + fileInfo.getSize()
                        + ": " + file);
                    continue;
                }
                boolean lastModificationSync = DateUtil
                    .equalsFileDateCrossPlattform(file.lastModified(), fileInfo
                        .getModifiedDate().getTime());
                if (!lastModificationSync) {
                    file.setLastModified(fileInfo.getModifiedDate().getTime());
                    changed++;
                } else {
                    same++;
                }
            }
            System.out.println("Finished: " + changed + " lmd changes, " + same
                + " lmd unchanged, " + total + " processed files @ "
                + localBase);
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

}

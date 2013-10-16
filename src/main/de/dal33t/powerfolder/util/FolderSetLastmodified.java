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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

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
        Path in = Paths.get(args[0]);
        if (!in.getFileName().toString().equalsIgnoreCase("PowerFolders")) {
            correct(in);
            correct(in.resolve(".PowerFolder/meta"));
            return;
        } else {
            Filter<Path> filter = new Filter<Path>() {
                @Override
                public boolean accept(Path entry) {
                    return Files.isDirectory(entry);
                }
            };
            try (DirectoryStream<Path> dirs = Files.newDirectoryStream(in, filter)) {
                for (Path dir : dirs) {
                    correct(dir);
                    correct(dir.resolve(".PowerFolder/meta"));
                }
            } catch (IOException ioe) {
                
            }
        }
    }

    public static void correct(Path localBase) {
        try {

            Path dbFile = localBase.resolve(".PowerFolder/.PowerFolder.db");
            InputStream fIn = Files.newInputStream(dbFile);
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
                Path file = localBase.resolve(fileInfo.getRelativeName());
                if (Files.notExists(file)) {
                    System.err.println("Skip file not found: " + file);
                    continue;
                }
                if (fileInfo.isFile() && Files.size(file) != fileInfo.getSize()) {
                    System.err.println("Skip file size not identical found: "
                        + Files.size(file) + ". expected: " + fileInfo.getSize()
                        + ": " + file);
                    continue;
                }
                boolean lastModificationSync = DateUtil
                    .equalsFileDateCrossPlattform(Files.getLastModifiedTime(file).toMillis(), fileInfo
                        .getModifiedDate().getTime());
                if (!lastModificationSync) {
                    Files.setLastModifiedTime(file, FileTime.fromMillis(fileInfo
                        .getModifiedDate().getTime()));
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

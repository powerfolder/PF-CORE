/*
* Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.
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
* $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;

public class AddLicenseHeader {

    /**
     * @param args
     */
    public static void main(String[] args) {
        addLicInfoToDir(Paths.get("."));
    }

    public static void addLicInfoToDir(Path dir) {
        Filter<Path> filter = new Filter<Path>() {
            @Override
            public boolean accept(Path entry) {
                return entry.getFileName().toString().endsWith(".java");
            }
        };

        try (DirectoryStream<Path> javas = Files.newDirectoryStream(dir, filter)) {
            for (Path file : javas) {
                addLicInfo(file);
            }
        } catch (IOException ioe) {
            // TODO:
        }

        filter = new Filter<Path>() {
            @Override
            public boolean accept(Path entry) {
                return Files.isDirectory(entry);
            }
        };

        try (DirectoryStream<Path> subDirs = Files.newDirectoryStream(dir, filter)) {
            for (Path subDir : subDirs) {
                addLicInfoToDir(subDir);
            }
        } catch (IOException ioe) {
            // TODO:
        }
    }

    public static void addLicInfo(Path f) {
        try {
            if (f.toAbsolutePath().toString().contains("\\jwf\\jwf")) {
                System.out.println("Skip: " + f.toRealPath());
                return;
            }
            if (f.toAbsolutePath().toString().contains("org\\jdesktop\\swinghelper")) {
                System.out.println("Skip: " + f.toRealPath());
                return;
            }
            String content = FileUtils.readFileToString(f.toFile(), "UTF-8");
            int i = content.indexOf("package");

//            if (i != 693) {
//                System.out.println("Skip: " + f.getCanonicalPath() + ": " + i);
//                return;
//            }
            boolean dennis = content.contains("@author Dennis");
            if (dennis) {
                System.err.println("Dennis: " + f.toRealPath() + ": " + i);
                content = LIC_INFO_DENNIS + content.substring(i, content.length());
            } else {
                System.out.println("Onlyme: " + f.toRealPath() + ": " + i);
                content = LIC_INFO + content.substring(i, content.length());
            }
//
            // System.out.println(content);
            FileUtils.writeStringToFile(f.toFile(), content, "UTF-8");
            // throw new RuntimeException();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static final String LIC_INFO = "/*\r\n"
        + "* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.\r\n"
        + "*\r\n"
        + "* This file is part of PowerFolder.\r\n"
        + "*\r\n"
        + "* PowerFolder is free software: you can redistribute it and/or modify\r\n"
        + "* it under the terms of the GNU General Public License as published by\r\n"
        + "* the Free Software Foundation.\r\n"
        + "*\r\n"
        + "* PowerFolder is distributed in the hope that it will be useful,\r\n"
        + "* but WITHOUT ANY WARRANTY; without even the implied warranty of\r\n"
        + "* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\r\n"
        + "* GNU General Public License for more details.\r\n"
        + "*\r\n"
        + "* You should have received a copy of the GNU General Public License\r\n"
        + "* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.\r\n"
        + "*\r\n" + "* $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $\r\n" + "*/\r\n";

    private static final String LIC_INFO_DENNIS = "/*\r\n"
        + "* Copyright 2004 - 2008 Christian Sprajc, Dennis Waldherr. All rights reserved.\r\n"
        + "*\r\n"
        + "* This file is part of PowerFolder.\r\n"
        + "*\r\n"
        + "* PowerFolder is free software: you can redistribute it and/or modify\r\n"
        + "* it under the terms of the GNU General Public License as published by\r\n"
        + "* the Free Software Foundation.\r\n"
        + "*\r\n"
        + "* PowerFolder is distributed in the hope that it will be useful,\r\n"
        + "* but WITHOUT ANY WARRANTY; without even the implied warranty of\r\n"
        + "* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\r\n"
        + "* GNU General Public License for more details.\r\n"
        + "*\r\n"
        + "* You should have received a copy of the GNU General Public License\r\n"
        + "* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.\r\n"
        + "*\r\n" + "* $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $\r\n" + "*/\r\n";
}

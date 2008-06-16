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
* $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
*/
package de.dal33t.powerfolder.test.folder;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.test.TestHelper;

/**
 * Test for Trac-#232
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.1 $
 */
public class CheckForDupeFiles extends TestCase {

    public void testDupeFileInfos() throws IOException, ClassNotFoundException {
        System.out.println("CheckForDupeFilesTest.testDupeFileInfos");
        InputStream fIn = new BufferedInputStream(new FileInputStream(
            "src/test-resources/Nice Babes.PowerFolder.db"));
        ObjectInputStream in = new ObjectInputStream(fIn);
        FileInfo[] files = (FileInfo[]) in.readObject();
        in.close();

        assertFalse("Detected duplicate fileinfos in database file",
            checkForDupes(files));
        writeFileInfos(new File(TestHelper.getTestDir(),
            "Database-raw-PowerFolder.db"), files);
        cleanMemberInfos(files);
        assertFalse("Detected duplicate fileinfos in database file",
            checkForDupes(files));
        writeFileInfos(new File(TestHelper.getTestDir(),
            "Database-cleaned-PowerFolder.db"), files);
    }

    private void writeFileInfos(File file, FileInfo[] files)
        throws FileNotFoundException, IOException
    {
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(
            file));
        out.writeObject(files);
        out.close();
    }

    private static boolean checkForDupes(FileInfo[] list) {
        HashSet<String> lowerCasenames = new HashSet<String>();
        List<MemberInfo> instancesMI = new ArrayList<MemberInfo>();
        List<FolderInfo> instancesFI = new ArrayList<FolderInfo>();
        HashSet<String> memberIds = new HashSet<String>();
        boolean dupes = false;
        for (FileInfo file : list) {
            if (lowerCasenames.contains(file.getName().toLowerCase())) {
                System.err.println("Detected dupe: " + file.toDetailString());
                dupes = true;
            }
            boolean instanceFound = false;
            for (MemberInfo info : instancesMI) {
                if (info == file.getModifiedBy()) {
                    instanceFound = true;
                    break;
                }
            }
            if (!instanceFound) {
                instancesMI.add(file.getModifiedBy());
            }
            memberIds.add(file.getModifiedBy().id);

            instanceFound = false;
            for (FolderInfo info : instancesFI) {
                if (info == file.getFolderInfo()) {
                    instanceFound = true;
                    break;
                }
            }
            if (!instanceFound) {
                instancesFI.add(file.getFolderInfo());
            }

            lowerCasenames.add(file.getName().toLowerCase());
        }
        System.out.println("Got " + instancesMI.size()
            + " diffrent memberinfo instances with " + memberIds.size()
            + " diffrent ids. Got " + instancesFI.size()
            + " diffrent folderinfos instances. " + instancesMI);
        return dupes;
    }

    private static void cleanMemberInfos(FileInfo[] list) {
        HashMap<String, MemberInfo> instances = new HashMap<String, MemberInfo>();
        for (FileInfo file : list) {
            MemberInfo fMInfo = file.getModifiedBy();
            MemberInfo dbMInfo = instances.get(fMInfo.id);
            if (dbMInfo == null) {
                instances.put(fMInfo.id, fMInfo);
            } else {
                file.setModifiedInfo(dbMInfo, file.getModifiedDate());
            }
        }
    }
}

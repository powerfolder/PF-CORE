/* $Id: CheckForDupeFilesTest.java,v 1.1 2006/04/22 02:24:35 totmacherr Exp $
 * 
 * Copyright (c) 2006 Riege Software. All rights reserved.
 * Use is subject to license terms.
 */
package de.dal33t.powerfolder.junit.folder;

import java.io.*;
import java.util.HashSet;

import junit.framework.TestCase;
import de.dal33t.powerfolder.light.FileInfo;

/**
 * Test for Trac-#232
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * 
 * @version $Revision: 1.1 $
 */
public class CheckForDupeFilesTest extends TestCase {

    public void testDupeFileInfos() throws IOException, ClassNotFoundException {
        System.out.println("CheckForDupeFilesTest.testDupeFileInfos");
        InputStream fIn = new BufferedInputStream(new FileInputStream(
            "test-resources/PowerFolder-with-dupes.db"));
        ObjectInputStream in = new ObjectInputStream(fIn);
        FileInfo[] files = (FileInfo[]) in.readObject();

        assertFalse("Detected duplicate fileinfos in database file",
            checkForDupes(files));
    }

    private static boolean checkForDupes(FileInfo[] list) {
        HashSet<String> lowerCasenames = new HashSet<String>();
        boolean dupes = false;
        for (FileInfo file : list) {
            if (lowerCasenames.contains(file.getName().toLowerCase())) {
                System.err.println("Detected dupe: " + file.toDetailString());
                dupes = true;
            }
            lowerCasenames.add(file.getName().toLowerCase());
        }
        return dupes;
    }
}

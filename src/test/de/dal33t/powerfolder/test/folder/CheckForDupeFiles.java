/* $Id: CheckForDupeFilesTest.java,v 1.1 2006/04/22 02:24:35 totmacherr Exp $
 */
package de.dal33t.powerfolder.test.folder;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;
import de.dal33t.powerfolder.light.FileInfo;
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
        List<MemberInfo> instances = new ArrayList<MemberInfo>();
        HashSet<String> memberIds = new HashSet<String>();
        boolean dupes = false;
        for (FileInfo file : list) {
            if (lowerCasenames.contains(file.getName().toLowerCase())) {
                System.err.println("Detected dupe: " + file.toDetailString());
                dupes = true;
            }
            boolean instanceFound = false;
            for (MemberInfo info : instances) {
                if (info == file.getModifiedBy()) {
                    instanceFound = true;
                    break;
                }
            }
            if (!instanceFound) {
                instances.add(file.getModifiedBy());
            }
            memberIds.add(file.getModifiedBy().id);
            lowerCasenames.add(file.getName().toLowerCase());
        }
        System.out.println("Got " + instances.size()
            + " diffrent memberinfo instances with " + memberIds.size()
            + " diffrent ids. " + instances);
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

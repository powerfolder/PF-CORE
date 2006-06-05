/* $Id: TestSerialize.java,v 1.2 2006/02/20 01:08:12 totmacherr Exp $
 */
package de.dal33t.powerfolder.test;

import java.io.IOException;
import java.util.Date;

import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.util.ByteSerializer;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.Logger;

/**
 * Tests the serializing perfomance
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class TestSerialize {
    private static final Logger LOG = Logger.getLogger(TestSerialize.class);

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        byte[] raw;
        Message[] testMsgs = generateTestMessages();
        for (int i = 0; i < testMsgs.length; i++) {
            long start = System.currentTimeMillis();
            raw = ByteSerializer.serializeStatic(testMsgs[i], true);
            long took = System.currentTimeMillis() - start;
            LOG.info("Serialize took " + took + "ms (" + raw.length
                + " bytes) of " + testMsgs[i]);
        }
    }

    private static Message[] generateTestMessages() {
        return new Message[]{generateFileChunk(), generateFileChunk(),
            generateFileChunk(), generateFileList(), generateFileList(),
            generateFileList()};
    }

    private static FileChunk generateFileChunk() {
        byte[] data = new byte[10240];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (Math.random() * 255);
        }
        return new FileChunk(generateFileInfo(), 10, data);
    }

    private static FileList generateFileList() {
        FileList list = new FileList();
        list.folder = generateFolderInfo();
        FileInfo[] files = new FileInfo[30000];
        for (int i = 0; i < files.length; i++) {
            files[i] = generateFileInfo();
        }
        list.files = files;
        return list;
    }

    private static FileInfo generateFileInfo() {
        FileInfo fInfo = new FileInfo(generateFolderInfo(), "subdir/"
            + Math.random() + "/and another/test filename.gif");
        fInfo.setSize((long) (Math.random() * 100000));
        fInfo.setModifiedInfo(generateMemberInfo(), new Date());
        return fInfo;
    }

    private static MemberInfo generateMemberInfo() {
        MemberInfo mInfo = new MemberInfo();
        mInfo.nick = "noob";
        mInfo.id = IdGenerator.makeId();
        return mInfo;
    }

    private static FolderInfo generateFolderInfo() {
        return new FolderInfo("TestFolder" + Math.random(), IdGenerator
            .makeId(), false);
    }
}

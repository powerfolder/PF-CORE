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
package de.dal33t.powerfolder.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.disk.DiskItemFilter;
import de.dal33t.powerfolder.light.AccountInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.FileChunk;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.util.ByteSerializer;
import de.dal33t.powerfolder.util.IdGenerator;

/**
 * Tests the serializing perfomance
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public class TestSerialize {

    private static final Logger log = Logger.getLogger(TestSerialize.class
        .getName());

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
            log.info("Serialize took " + took + "ms (" + raw.length
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
        FileInfo[] files = new FileInfo[Constants.FILE_LIST_MAX_FILES_PER_MESSAGE - 1];
        for (int i = 0; i < files.length; i++) {
            files[i] = generateFileInfo();
        }
        FileList list = (FileList) FileList.create4Test(
generateFolderInfo(),
            Arrays.asList(files), new DiskItemFilter())[0];
        return list;
    }

    private static FileInfo generateFileInfo() {
        String fn = "subdir/" + Math.random()
            + "/and another/test filename.gif";

        return FileInfoFactory.unmarshallExistingFile(generateFolderInfo(), fn,
            IdGenerator.makeFileId(), (long) (Math.random() * 100000),
            generateMemberInfo(), generateAccountInfo(), new Date(), 0, null,
            false, null);
    }

    private static MemberInfo generateMemberInfo() {
        return new MemberInfo("noob", IdGenerator.makeId(), null);
    }

    private static AccountInfo generateAccountInfo() {
        return new AccountInfo("noob", IdGenerator.makeId(), null, false);
    }

    private static FolderInfo generateFolderInfo() {
        return new FolderInfo("TestFolder" + Math.random(), IdGenerator
            .makeFolderId());
    }
}

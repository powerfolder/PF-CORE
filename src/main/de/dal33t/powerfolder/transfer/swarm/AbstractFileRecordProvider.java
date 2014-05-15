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
 * $Id: AbstractDownloadManager.java 5151 2008-09-04 21:50:35Z bytekeeper $
 */
package de.dal33t.powerfolder.transfer.swarm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Adler32;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.ProgressListener;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;
import de.dal33t.powerfolder.util.delta.FilePartsRecordBuilder;

/**
 * Abstract {@link FileRecordProvider} which can compute {@link FilePartsRecord}
 * s based on {@link FileInfo}s.
 *
 * @author Dennis "Bytekeeper" Waldherr
 */
public abstract class AbstractFileRecordProvider implements FileRecordProvider {

    private static final Logger log = Logger
        .getLogger(AbstractFileRecordProvider.class.getName());
    private Controller controller;

    /**
     * @param controller
     */
    public AbstractFileRecordProvider(Controller controller) {
        Reject.ifNull(controller, "Controller is null!");
        this.controller = controller;
    }

    public Controller getController() {
        return controller;
    }

    protected FilePartsRecord computeFilePartsRecord(FileInfo fileInfo,
        ProgressListener obs) throws IOException
    {
        assert fileInfo != null;
        long start = System.currentTimeMillis();
        Path f = fileInfo.getDiskFile(controller.getFolderRepository());

        // TODO: Both, the RecordBuilder and the Matcher use "almost"
        // the same algorithms, there should be a shared config.
        // TODO: To select a part size I just took 4Gb as size and
        // wanted the result to be ~512kb.
        // But there should be a more thorough investigation on how to
        // calculate it.
        int partSize = Math
            .max(4096, (int) (Math.pow(Files.size(f), 0.25) * 2048));
        try (InputStream in = Files.newInputStream(f)) {
            FilePartsRecordBuilder b = new FilePartsRecordBuilder(
                new Adler32(), MessageDigest.getInstance("SHA-256"),
                MessageDigest.getInstance("MD5"), partSize);
            int read = 0;
            byte buf[] = new byte[8192];
            long processed = 0, size = Files.size(f);
            while ((read = in.read(buf)) > 0) {
                b.update(buf, 0, read);
                if (obs != null) {
                    obs.progressReached((double) processed / size);
                    processed += read;
                }
            }
            FilePartsRecord fileRecord = b.getRecord();
            long took = System.currentTimeMillis() - start;
            if (log.isLoggable(Level.FINE)) {
                log.fine("Built file parts for " + this + ". took " + took
                    + "ms" + " while processing " + processed + " bytes.");
            }
            return fileRecord;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}

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
 * $Id: MatchCopyWorker.java 11713 2010-03-11 14:37:48Z tot $
 */
package de.dal33t.powerfolder.util.delta;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.ProgressListener;
import de.dal33t.powerfolder.util.Range;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.delta.FilePartsState.PartState;
import de.dal33t.powerfolder.util.logging.Loggable;

public class MatchCopyWorker extends Loggable implements
    Callable<FilePartsState>
{
    private final Path srcFile;
    private final Path dstFile;
    private final FilePartsRecord record;
    private final List<MatchInfo> matchInfoList;

    private RandomAccessFile src;
    private InputStream srcStream;
    private long srcStreamPos;
    private RandomAccessFile dst;
    private final ProgressListener progressObserver;

    public MatchCopyWorker(Path srcFile, Path dstFile, FilePartsRecord record,
        List<MatchInfo> matchInfoList, ProgressListener obs)
    {
        super();
        Reject.noNullElements(srcFile, dstFile, record, matchInfoList);
        this.srcFile = srcFile;
        this.dstFile = dstFile;
        this.record = record;
        this.matchInfoList = matchInfoList;
        this.progressObserver = obs;
    }

    public FilePartsState call() throws Exception {
        try {
            src = new RandomAccessFile(srcFile.toFile(), "r");
        } catch (Exception e) {
            // Fallback in case RAF is not supported
            src = null;
            resetSrcStream();
        }
        try {
            dst = new RandomAccessFile(dstFile.toFile(), "rw");
            try {
                FilePartsState result = new FilePartsState(
                    record.getFileLength());
                int index = 0;
                for (MatchInfo info : matchInfoList) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    if (progressObserver != null) {
                        progressObserver
                            .progressReached(matchInfoList.size() > 0
                                ? (double) index / matchInfoList.size()
                                : 1);
                    }
                    index++;

                    if (src != null) {
                        src.seek(info.getMatchedPosition());
                    } else {
                        if (srcStreamPos > info.getMatchedPosition()) {
                            // Reset stream, we need to start from the beginning
                            resetSrcStream();
                        }
                        srcStream.skip(info.getMatchedPosition());
                        srcStreamPos += info.getMatchedPosition();
                    }
                    long dstPos = info.getMatchedPart().getIndex()
                        * record.getPartLength();
                    dst.seek(dstPos);
                    int rem = (int) Math.min(record.getPartLength(),
                        record.getFileLength() - dstPos);
                    if (src != null) {
                        PathUtils.ncopy(src, dst, rem);
                    } else {
                        PathUtils.ncopy(srcStream, dst, rem);
                    }
                    // The copied data is now AVAILABLE
                    result.setPartState(Range.getRangeByLength(dstPos, rem),
                        PartState.AVAILABLE);
                }
                return result;
            } finally {
                dst.close();
            }
        } finally {
            if (src != null) {
                src.close();
            } else {
                srcStream.close();
            }
        }
    }

    private void resetSrcStream() throws IOException {
        if (srcStream != null) {
            srcStream.close();
        }
        srcStream = Files.newInputStream(srcFile);
        srcStreamPos = 0;
        if (isFine()) {
            logFine("Initialized InputStream instead of RAF for " + srcFile);
        }
    }
}

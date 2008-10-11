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
package de.dal33t.powerfolder.util.delta;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.concurrent.Callable;

import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.ProgressObserver;
import de.dal33t.powerfolder.util.Range;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.delta.FilePartsState.PartState;

public class MatchCopyWorker implements Callable<FilePartsState> {
    private final File srcFile;
    private final File dstFile;
    private final FilePartsRecord record;
    private final List<MatchInfo> matchInfoList;

    private RandomAccessFile src;
    private RandomAccessFile dst;
    private final ProgressObserver progressObserver;

    public MatchCopyWorker(File srcFile, File dstFile, FilePartsRecord record,
        List<MatchInfo> matchInfoList, ProgressObserver obs)
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
        src = new RandomAccessFile(srcFile, "r");
        try {
            dst = new RandomAccessFile(dstFile, "rw");
            try {
                FilePartsState result = new FilePartsState(record
                    .getFileLength());
                int index = 0;
                for (MatchInfo info : matchInfoList) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    if (progressObserver != null) {
                        progressObserver.progressed((double) index
                            / matchInfoList.size());
                    }
                    index++;

                    src.seek(info.getMatchedPosition());
                    long dstPos = info.getMatchedPart().getIndex()
                        * record.getPartLength();
                    dst.seek(dstPos);
                    int rem = (int) Math.min(record.getPartLength(), record
                        .getFileLength()
                        - dstPos);
                    FileUtils.ncopy(src, dst, rem);
                    // The copied data is now AVAILABLE
                    result.setPartState(Range.getRangeByLength(dstPos, rem),
                        PartState.AVAILABLE);
                }
                return result;
            } finally {
                dst.close();
            }
        } finally {
            src.close();
        }
    }
}

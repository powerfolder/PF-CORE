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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import de.dal33t.powerfolder.util.CountedInputStream;
import de.dal33t.powerfolder.util.ProgressObserver;
import de.dal33t.powerfolder.util.Reject;

public class MatchResultWorker implements Callable<List<MatchInfo>> {
    private final FilePartsRecord record;
    private final File inFile;
    private final ProgressObserver progressObserver;

    public MatchResultWorker(FilePartsRecord record, File inFile,
        ProgressObserver obs)
    {
        Reject.noNullElements(record, inFile);
        this.record = record;
        this.inFile = inFile;
        this.progressObserver = obs;
    }

    public List<MatchInfo> call() throws Exception {
        CountedInputStream in = new CountedInputStream(new BufferedInputStream(
            new FileInputStream(inFile)));
        try {
            final long fsize = inFile.length();

            PartInfoMatcher matcher = new PartInfoMatcher(in,
                new RollingAdler32(record.getPartLength()), MessageDigest
                    .getInstance("SHA-256"), record.getInfos());

            List<MatchInfo> matches = new LinkedList<MatchInfo>();
            MatchInfo match = null;
            while ((match = matcher.nextMatch()) != null) {
                if (progressObserver != null) {
                    progressObserver.progressed((double) in.getReadBytes()
                        / fsize);
                }
                matches.add(match);
            }
            return matches;
        } finally {
            in.close();
        }
    }
}

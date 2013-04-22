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
 * $Id: MatchResultWorker.java 11713 2010-03-11 14:37:48Z tot $
 */
package de.dal33t.powerfolder.util.delta;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import de.dal33t.powerfolder.util.CountedInputStream;
import de.dal33t.powerfolder.util.ProgressListener;
import de.dal33t.powerfolder.util.Reject;

public class MatchResultWorker implements Callable<List<MatchInfo>> {
    private final FilePartsRecord record;
    private final Path inFile;
    private final ProgressListener progressListener;

    public MatchResultWorker(FilePartsRecord record, Path inFile,
        ProgressListener obs)
    {
        Reject.noNullElements(record, inFile);
        this.record = record;
        this.inFile = inFile;
        this.progressListener = obs;
    }

    public List<MatchInfo> call() throws Exception {
        CountedInputStream in = new CountedInputStream(Files.newInputStream(inFile));
        try {
            final long fsize = Files.size(inFile);

            PartInfoMatcher matcher = new PartInfoMatcher(in,
                new RollingAdler32(record.getPartLength()), MessageDigest
                    .getInstance("SHA-256"), record.getInfos());

            List<MatchInfo> matches = new LinkedList<MatchInfo>();
            MatchInfo match = null;
            while ((match = matcher.nextMatch()) != null) {
                if (progressListener != null) {
                    progressListener.progressReached(fsize > 0 ? (double) in
                        .getReadBytes()
                        / fsize : 1);
                }
                matches.add(match);
            }
            return matches;
        } finally {
            in.close();
        }
    }
}

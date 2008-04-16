package de.dal33t.powerfolder.util.delta;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import de.dal33t.powerfolder.util.CountedInputStream;
import de.dal33t.powerfolder.util.Reject;

public class MatchResultWorker implements Callable<List<MatchInfo>> {
    private final FilePartsRecord record;
    private final File inFile;


    public MatchResultWorker(FilePartsRecord record, 
        File inFile) {
        Reject.noNullElements(record, inFile);
        this.record = record;
        this.inFile = inFile;
    }


    public List<MatchInfo> call() throws Exception {
        CountedInputStream in = null;
        try {
            in = new CountedInputStream(new BufferedInputStream(new FileInputStream(inFile)));
            final long fsize = inFile.length();

            PartInfoMatcher matcher = new PartInfoMatcher(in,
                new RollingAdler32(record.getPartLength()),
                MessageDigest.getInstance("SHA-256"), record.getInfos());
            
            List<MatchInfo> matches = new LinkedList<MatchInfo>();
            MatchInfo match = null;
            while ((match = matcher.nextMatch()) != null) {
                long dstPos = (match.getMatchedPart().getIndex() * record.getPartLength()); 
                setProgress((int) (in.getReadBytes() * 100.0 / fsize));
                matches.add(match);
            }
            return matches;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    protected void setProgress(int percent) {
    }
}

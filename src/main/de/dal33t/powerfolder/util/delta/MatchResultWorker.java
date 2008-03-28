package de.dal33t.powerfolder.util.delta;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.Callable;

import de.dal33t.powerfolder.util.Reject;

public final class MatchResultWorker implements Callable<List<MatchInfo>> {
    private final FilePartsRecord record;
    private final File inFile;


    public MatchResultWorker(FilePartsRecord record, 
        File inFile) {
        Reject.noNullElements(record, inFile);
        this.record = record;
        this.inFile = inFile;
    }


    public List<MatchInfo> call() throws Exception {

        // First search for matches between the record and the actual file on the disc.
        FileInputStream in = null;
        try {
            in = new FileInputStream(inFile);

            PartInfoMatcher matcher = new PartInfoMatcher(
                new RollingAdler32(record.getPartLength()),
                MessageDigest.getInstance("SHA-256"));
            final long fsize = inFile.length();
            matcher.getProcessedBytes().addValueChangeListener(
                new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        setProgress((int) ((Long) evt
                            .getNewValue()
                            / fsize));
                    }
                });
            return matcher.matchParts(in, record
                .getInfos());
        } finally {
            in.close();
        }
    }

    protected void setProgress(int percent) {
    }
}

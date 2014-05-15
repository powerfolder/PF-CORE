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
 * $Id$
 */
package de.dal33t.powerfolder.transfer;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ConcurrentMap;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.RequestPart;
import de.dal33t.powerfolder.transfer.Transfer.TransferState;
import de.dal33t.powerfolder.util.Range;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;
import de.dal33t.powerfolder.util.delta.FilePartsState.PartState;

/**
 * This download manager will try to download from all available sources.
 *
 * @author Dennis "Bytekeeper" Waldherr
 */
public class MultiSourceDownloadManager extends AbstractDownloadManager {

    private final ConcurrentMap<MemberInfo, Download> downloads = Util
        .createConcurrentHashMap(4);

    private Download pendingPartRecordFrom;

    public static final DownloadManagerFactory factory = new DownloadManagerFactory()
    {
        public DownloadManager createDownloadManager(Controller controller,
            FileInfo file, boolean automatic)
        {
            return new MultiSourceDownloadManager(controller, file, automatic);
        }
    };

    private MultiSourceDownloadManager(Controller controller, FileInfo file,
        boolean automatic)
    {
        super(controller, file, automatic);
    }

    @Override
    protected void addSourceImpl(Download download) {
        assert download != null;
        assert canAddSource(download.getPartner());

        // logFine("Adding source: " + download);

        if (downloads.put(download.getPartner().getInfo(), download) != null) {
            logSevere("Overridden previous download for member: "
                + download.getPartner() + ". " + download);
        }

        // Non-automatic overrides automatic
        if (isRequestedAutomatic() != download.isRequestedAutomatic()) {
            setAutomatic(false);
        }
    }

    public boolean canAddSource(Member member) {
        Reject.ifNull(member, "Member is null");
        return downloads.isEmpty() || Util.useSwarming(getController(), member);
    }

    public Download getSourceFor(Member member) {
        Reject.ifNull(member, "Member is null");
        return downloads.get(member.getInfo());
    }

    public boolean hasSource(Download d) {
        Reject.ifNull(d, "Download is null!");
        return downloads.get(d.getPartner().getInfo()) == d;
    }

    /**
     * @return the completed date of a download. Takes the most recent completed
     *         date of its sources.
     */
    public Date getCompletedDate() {
        if (!downloads.isEmpty()) {
            Date mostRecentCompletion = null;
            for (Download download : downloads.values()) {
                Date completedDate = download.getCompletedDate();
                if (completedDate != null) {
                    if (mostRecentCompletion == null
                        || completedDate.after(mostRecentCompletion))
                    {
                        mostRecentCompletion = completedDate;
                    }
                }
            }
            return mostRecentCompletion;
        }
        return null;
    }

    /*
     * Returns the sources of this manager.
     * @see de.dal33t.powerfolder.transfer.MultiSourceDownload#getSources()
     */
    public Collection<Download> getSources() {
        return Collections.unmodifiableCollection(downloads.values());
    }

    public boolean hasSources() {
        return !downloads.isEmpty();
    }

    @Override
    protected void removeSourceImpl(Download download) {
        assert download != null;
        assert hasSource(download);

        if (downloads.remove(download.getPartner().getInfo()) == null) {

            throw new AssertionError("Removed non-managed download:" + download
                + " " + download.getPartner().getInfo());
        }
        // All pending requests from that download are void.
        if (filePartsState != null) {
            for (RequestPart req : download.getPendingRequests()) {
                filePartsState.setPartState(req.getRange(), PartState.NEEDED);
            }
        }
    }

    @Override
    public String toString() {
        String string = super.toString() + "; sources=" + downloads.values()
            + "; pending requested bytes: ";
        if (filePartsState != null) {
            string += filePartsState.countPartStates(filePartsState.getRange(),
                PartState.PENDING)
                + "; available: "
                + filePartsState.countPartStates(filePartsState.getRange(),
                    PartState.AVAILABLE)
                + "; needed: "
                + filePartsState.countPartStates(filePartsState.getRange(),
                    PartState.NEEDED);
        }
        return string;
    }

    /**
     * Returns an available source for requesting the {@link FilePartsRecord}
     *
     * @param download
     * @return
     */
    protected Download findPartRecordSource(Download download) {
        assert download != null;

        for (Download d : downloads.values()) {
            if (d.isStarted() && !d.isBroken()
                && Util.useDeltaSync(getController(), d))
            {
                download = d;
                break;
            }
        }
        return download;
    }

    protected void requestFilePartsRecord(Download download) {
        assert download == null || Util.useDeltaSync(getController(), download);

        if (pendingPartRecordFrom != null) {
            // logFine("Pending FPR from: " + pendingPartRecordFrom);

            // Check if we really need to do this first
            if (!pendingPartRecordFrom.isBroken()) {
                return;
            }
            logWarning("Source should have been removed: "
                + pendingPartRecordFrom);
            pendingPartRecordFrom = null;
        }
        if (download == null) {
            download = findPartRecordSource(null);
        }

        // logFine("Selected FPR source: " + download);

        if (download != null) {
            assert Util.useDeltaSync(getController(), download);
            if (isFine()) {
                logFine("Requesting Filepartsrecord from " + download);
            }
            setTransferState(TransferState.FILERECORD_REQUEST);
            pendingPartRecordFrom = download;
            pendingPartRecordFrom.requestFilePartsRecord();

            setStarted();
        }
    }

    protected void sendPartRequests() throws BrokenDownloadException {
        if (isFiner()) {
            logFiner("X Sending part requests: "
                + filePartsState.countPartStates(filePartsState.getRange(),
                    PartState.NEEDED));
        }

        setTransferState(TransferState.DOWNLOADING);

        Range range;
        while (true) {
            range = filePartsState.findFirstPart(PartState.NEEDED);
            if (range == null) {
                // File completed, or only pending requests left
                break;
            }
            range = Range.getRangeByLength(range.getStart(), Math.min(
                getController().getTransferManager().getMaxFileChunkSize(),
                range.getLength()));
            // Split requests across sources
            if (findAndRequestDownloadFor(range)) {
                filePartsState.setPartState(range, PartState.PENDING);
            } else {
                break;
            }
        }

        if (isFiner()) {
            logFiner("X Sending part requests over");
        }

        long p = filePartsState.countPartStates(filePartsState.getRange(),
            PartState.PENDING);
        if (p > 0) {
            for (Download d : downloads.values()) {
                if (d.isStarted() && !d.isBroken()) {
                    for (RequestPart rp : d.getPendingRequests()) {
                        p -= rp.getRange().getLength();
                    }
                }
            }
            assert p == 0;
        }
        assert filePartsState.isCompleted()
            || filePartsState.countPartStates(filePartsState.getRange(),
                PartState.PENDING) > 0 || hasNoAvailableSources() : "AVAIL: "
            + filePartsState.countPartStates(filePartsState.getRange(),
                PartState.AVAILABLE)
            + ", NEED : "
            + filePartsState.countPartStates(filePartsState.getRange(),
                PartState.NEEDED)
            + ", PEND : "
            + filePartsState.countPartStates(filePartsState.getRange(),
                PartState.PENDING);
    }

    /**
     * Checks if no sources are available for requests.
     */
    private boolean hasNoAvailableSources() {
        for (Download d : downloads.values()) {
            if (d.isStarted() && !d.isBroken()) {
                return false;
            }
        }
        return true;
    }

    private boolean findAndRequestDownloadFor(Range range)
        throws BrokenDownloadException
    {
        assert range != null;
        if (isFiner()) {
            logFiner("X findAndRequestDownloadFor: " + range);
        }
        for (Download d : downloads.values()) {
            if (!d.isStarted() || d.isBroken()) {
                continue;
            }
            if (d.requestPart(range)) {
                return true;
            }
        }
        return false;
    }
}

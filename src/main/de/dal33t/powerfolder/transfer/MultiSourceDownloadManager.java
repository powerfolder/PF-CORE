package de.dal33t.powerfolder.transfer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang.Validate;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.message.RequestPart;
import de.dal33t.powerfolder.transfer.Transfer.TransferState;
import de.dal33t.powerfolder.util.Range;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.delta.FilePartsRecord;
import de.dal33t.powerfolder.util.delta.FilePartsState;
import de.dal33t.powerfolder.util.delta.FilePartsState.PartState;

/**
 * This download manager will try to download from all available sources.
 * 
 * @author Dennis "Bytekeeper" Waldherr
 */
public class MultiSourceDownloadManager extends AbstractDownloadManager {
    private final ConcurrentMap<MemberInfo, Download> downloads = new ConcurrentHashMap<MemberInfo, Download>();

    private Download pendingPartRecordFrom;

    private boolean usingPartRequests;

    public MultiSourceDownloadManager(Controller controller, FileInfo file,
        boolean automatic) throws IOException
    {
        super(controller, file, automatic);
    }

    public synchronized void addSource(Download download) {
        Validate.notNull(download);
        Validate.isTrue(download.isCompleted()
            || allowsSourceFor(download.getPartner()));

        // log().debug("Adding source: " + download);

        if (downloads.put(download.getPartner().getInfo(), download) != null) {
            log().error(
                "Overridden previous download for member: "
                    + download.getPartner());
        }

        // Non-automatic overrides automatic
        if (isRequestedAutomatic() != download.isRequestedAutomatic()) {
            setAutomatic(false);
        }

        // Was this download restored from database ?
        if (download.isCompleted()) {
            setCompleteOnLoad();
            return;
        }

        // Don't really request data for empty files
        if (getFileInfo().getSize() == 0) {
            log()
                .debug(
                    "Empty file detected, setting transfers completed immediately.");
            setCompleted();
        }

        if (isCompleted()) {
            return;
        }

        if (isUsingPartRequests()
            || Util.usePartRequests(getController(), download))
        {
            usingPartRequests = true;
        }
        
        // At this point we need a "valid" filePartsState
        if (filePartsState == null) {
            filePartsState = new FilePartsState(getFileInfo().getSize());
        }

        Range r = filePartsState == null ? null : filePartsState
            .findFirstPart(PartState.NEEDED);
        if (r != null) {
            download.request(r.getStart());
        } else {
            download.request(0);
        }

        sendPartRequests();

        // log().debug("Now having " + downloads.values().size() + " sources!");
    }

    public boolean allowsSourceFor(Member member) {
        return downloads.isEmpty() || (member.isSupportingPartRequests())
            && isUsingPartRequests();
    }

    public Download getSourceFor(Member member) {
        Validate.notNull(member);
        return downloads.get(member.getInfo());
    }

    /*
     * Returns the sources of this manager.
     * 
     * @see de.dal33t.powerfolder.transfer.MultiSourceDownload#getSources()
     */
    public Collection<Download> getSources() {
        return new ArrayList<Download>(downloads.values());
    }

    public boolean hasSources() {
        return !downloads.isEmpty();
    }

    public synchronized void removeSource(Download download) {
        Validate.notNull(download);

        if (downloads.remove(download.getPartner().getInfo()) == null) {
            log().error("Removed non-managed download:" + download);
        }
        // log().debug("Sources left: " + downloads.values().size());
        // Maybe we're done for, update the tempfile just in case
        if (!hasSources()) {
            updateTempFile();
        }
        if (isUsingPartRequests()) {
            // All pending requests from that download are void.
            if (filePartsState != null) {
                for (RequestPart req : download.getPendingRequests()) {
                    filePartsState.setPartState(req.getRange(),
                        PartState.NEEDED);
                }
            }
            if (pendingPartRecordFrom == download) {
                pendingPartRecordFrom = null;
                requestFilePartsRecord(null);
            }
        }
        sendPartRequests();
    }

    public void setBroken() {
        setBroken(TransferProblem.BROKEN_DOWNLOAD, "");
    }

    @Override
    public String toString() {
        return super.toString() + "; #sources=" + downloads.values().size();
    }

    /**
     * Returns an available source for requesting the {@link FilePartsRecord}
     * 
     * @param download
     * @return
     */
    protected Download findPartRecordSource(Download download) {
        for (Download d : downloads.values()) {
            if (d.isStarted() && !d.isBroken() && d.usePartialTransfers()) {
                download = d;
                break;
            }
        }
        return download;
    }

    @Override
    protected Collection<Download> getDownloads() {
        return downloads.values();
    }

    protected void requestFilePartsRecord(Download download) {
        // log().debug("Requesting record: " + isUsingPartRequests());
        if (!isUsingPartRequests()) {
            return;
        }

        if (pendingPartRecordFrom != null) {
            // log().debug("Pending FPR from: " + pendingPartRecordFrom);

            // Check if we really need to do this first
            if (!pendingPartRecordFrom.isBroken()) {
                return;
            }
            log().error(
                "Source should have been removed: " + pendingPartRecordFrom);
            pendingPartRecordFrom = null;
        }
        if (download == null) {
            download = findPartRecordSource(null);
        }

        // log().debug("Selected FPR source: " + download);

        if (download != null) {
            log().debug("Requesting Filepartsrecord from " + download);
            setTransferState(TransferState.FILERECORD_REQUEST);
            pendingPartRecordFrom = download;
            pendingPartRecordFrom.requestFilePartsRecord();
        }

        setStarted();
    }

    protected synchronized void sendPartRequests() {
        if (isDone()) {
            return;
        }
        // If we aren't allowed to send requests, just don't do it
        if (!isUsingPartRequests()) {
            return;
        }
        // We also won't request while waiting for part state initialization
        if (filePartsState == null) {
            return;
        }

        // log().debug("Sending part requests!");

        setTransferState(TransferState.DOWNLOADING);
        // log().debug("Sending requests!");

        Range range;
        while (true) {
            range = filePartsState.findFirstPart(PartState.NEEDED);
            if (range == null) {
                // File completed, or only pending requests left
                return;
            }
            range = Range.getRangeByLength(range.getStart(), Math.min(
                TransferManager.MAX_CHUNK_SIZE, range.getLength()));
            // Split requests across sources
            if (findAndRequestDownloadFor(range)) {
                filePartsState.setPartState(range, PartState.PENDING);
            } else {
                break;
            }
        }

        setStarted();
    }

    private boolean findAndRequestDownloadFor(Range range) {
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

    private boolean isUsingPartRequests() {
        return usingPartRequests;
    }
}

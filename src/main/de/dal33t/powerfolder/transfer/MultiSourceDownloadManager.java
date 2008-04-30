package de.dal33t.powerfolder.transfer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
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
    private final ConcurrentMap<MemberInfo, Download> downloads = new ConcurrentHashMap<MemberInfo, Download>();

    private Download pendingPartRecordFrom;

    private boolean usingPartRequests;

    public MultiSourceDownloadManager(Controller controller, FileInfo file,
        boolean automatic) throws IOException
    {
        super(controller, file, automatic);
    }

    @Override
    protected void addSourceImpl(Download download) {

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

        if (isUsingPartRequests()
            || Util.usePartRequests(getController(), download))
        {
            usingPartRequests = true;
        }
    }

    public boolean allowsSourceFor(Member member) {
        Reject.ifNull(member, "Member is null");
        return downloads.isEmpty() || (member.isSupportingPartRequests()
            && isUsingPartRequests()
            && Util.allowSwarming(getController(), member.isOnLAN()));
    }

    public Download getSourceFor(Member member) {
        Reject.ifNull(member, "Member is null");
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

    @Override
    protected void removeSourceImpl(Download download) {
        Reject.ifNull(download, "Download is null");

        if (downloads.remove(download.getPartner().getInfo()) == null) {
            log().error("Removed non-managed download:" + download + " " + download.getPartner().getInfo());
            throw new RuntimeException(downloads.toString());
        }
        // log().debug("Sources left: " + downloads.values().size());
        // Maybe we're done for, update the tempfile just in case
        if (!hasSources()) {
            updateTempFile();
            return;
        }

        if (isUsingPartRequests()) {
            // All pending requests from that download are void.
            if (filePartsState != null) {
                for (RequestPart req : download.getPendingRequests()) {
                    filePartsState.setPartState(req.getRange(),
                        PartState.NEEDED);
                }
            }
        }
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
            if (d.isStarted() && !d.isBroken() && d.usePartialTransfers()
                && Util.allowDeltaSync(getController(), d.getPartner().isOnLAN())) {
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

    protected void sendPartRequests() {
        // log().debug("Sending part requests: " +
        // filePartsState.countPartStates(filePartsState.getRange(),
        // PartState.NEEDED));

        setTransferState(TransferState.DOWNLOADING);

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
    }

    private boolean findAndRequestDownloadFor(Range range) {
        for (Download d : downloads.values()) {
            if (!d.isStarted() || d.isBroken()) {
                continue;
            }
            if (d.requestPart(range)) {
                if (d.getPendingRequests().isEmpty()) {
                    throw new AssertionError("Pending list is emtpy!");
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isUsingPartRequests() {
        return usingPartRequests;
    }

    @Override
    protected boolean isUsingDeltaSync() {
        for (Download d: downloads.values()) {
            if (d.isBroken() || d.isCompleted()) {
                continue;
            }
            if (Util.allowDeltaSync(getController(), d.getPartner().isOnLAN())) {
                return true;
            }
        }
        return false;
    }
}

package de.dal33t.powerfolder.transfer.swarm;

import java.io.IOException;
import java.util.Collection;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.transfer.Download;
import de.dal33t.powerfolder.transfer.TransferProblem;

public interface DownloadControl {
    /**
     * Call this after construction. Otherwise download might not have tempfile
     * ready. Does not prepare tempfile if completed.
     *
     * @param completed
     *            if this download is already completed.
     * @throws IOException
     */
    void init(boolean completed) throws IOException;

    /**
     * Returns the download belonging to the given member.
     *
     * @param member
     *            the download of the member or null if there isn't one
     * @return
     */
    Download getSourceFor(Member member);

    /**
     * Returns a collection containing all sources of this swarm. Any changes to
     * the returned collection are <b>not</b> reflected in the actual list.
     *
     * @return
     */
    Collection<Download> getSources();

    /**
     * Called when a download stops being available as a source.
     *
     * @param download
     */
    void removeSource(Download download);

    /**
     * Called when a new download source is available. If the given download is
     * completed, this manager should set itself to completed as well and not
     * transfer anything.
     *
     * @param download
     * @return true if the download was actually requested from the remote side.
     *         false if not.
     */
    boolean addSource(Download download);

    /**
     * Returns true if adding a source using that member is allowed.
     *
     * @param member
     * @return
     */
    boolean canAddSource(Member member);

    /**
     * Called if the download should be aborted.
     */
    void abort();

    /**
     * Aborts the download and deletes any temporary file used.
     */
    void abortAndCleanup();

    /**
     * Breaks all existing downloads in this manager and sets it to broken.
     *
     * @param string
     */
    void setBroken(TransferProblem problem, String details);
}

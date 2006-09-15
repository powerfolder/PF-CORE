package de.dal33t.powerfolder.event;

/**
 * TODO maybe should return an enum with: OVERWRITE, SKIP, CANCEL,
 * OVERWRITE_ALL, SKIPP_ALL. if the event has a boolean that indicates that
 * there are more files queued to restore.
 */
public interface RecycleBinConfirmationHandler {
    public boolean confirmOverwriteOnRestore(
        RecycleBinConfirmEvent recycleBinConfirmEvent);
}

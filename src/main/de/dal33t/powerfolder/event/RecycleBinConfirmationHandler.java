package de.dal33t.powerfolder.event;


public interface RecycleBinConfirmationHandler {
    public boolean confirmOverwriteOnRestore(RecycleBinConfirmEvent recycleBinConfirmEvent);
}

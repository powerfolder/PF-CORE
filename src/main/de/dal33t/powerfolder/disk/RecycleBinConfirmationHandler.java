package de.dal33t.powerfolder.disk;


public interface RecycleBinConfirmationHandler {
    public boolean confirmOverwriteOnRestore(RecycleBinConfirmEvent recycleBinConfirmEvent);
}

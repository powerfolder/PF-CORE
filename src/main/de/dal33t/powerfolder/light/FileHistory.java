package de.dal33t.powerfolder.light;

import de.dal33t.powerfolder.util.Reject;

public abstract class FileHistory implements Iterable<VersionedFile> {

    /**
     * @return the FileInfo with the most recent version.
     */
    public abstract VersionedFile getFile();

    /**
     * Adds a new version to the history and replaces the most recent file.
     * 
     * @param newFileInfo
     *            the file version to add
     * @return a new FileHistory with the given fileInfo as the most recent
     *         version
     */
    public abstract FileHistory addVersion(VersionedFile newFileInfo);

    /**
     * Tests if the given FileHistory has a version conflict with this one. Two
     * histories are in conflict if their most recent file is different but at
     * some point in their history they have at least one file in common.
     * 
     * @param other
     * @return
     */
    public boolean hasConflictWith(FileHistory other) {
        Reject.ifNull(other, "other is null!");
        if (getFile().equals(other.getFile())) {
            return false;
        }
        return getCommonAncestor(other) != null;
    }

    /**
     * Returns the newest file that is shared by
     * 
     * @param other
     * @return
     */
    public abstract VersionedFile getCommonAncestor(FileHistory other);

}
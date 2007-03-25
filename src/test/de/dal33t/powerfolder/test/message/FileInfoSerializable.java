package de.dal33t.powerfolder.test.message;


import java.io.Serializable;
import java.util.Date;

/**
 * File information of a local or remote file
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.33 $
 */
public class FileInfoSerializable implements Serializable {
    private static final long serialVersionUID = 100L;

    /** The filename (including the path from the base of the folder) */
    public String fileName;

    /** The size of the file */
    public Long size;

    /** modified info */
    public String modifiedBy;
    /** modified in folder on date */
    public Date lastModifiedDate;

    /** Version number of this file */
    public int version;

    /** the deleted flag */
    public boolean deleted;

    /** the folder */
    public String folderInfo;

    // Serialization optimization *********************************************

}
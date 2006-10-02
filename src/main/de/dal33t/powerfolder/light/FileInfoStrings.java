package de.dal33t.powerfolder.light;

/**
 * A object, that holds some cost-intense strings about a fileinfo.
 * 
 * @author <a href="mailto:sprajc@riege.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public class FileInfoStrings {
    private String fileNameOnly;
    private String lowerCaseName;
    private String locationInFolder;

    public String getFileNameOnly() {
        return fileNameOnly;
    }

    public void setFileNameOnly(String fileNameOnly) {
        this.fileNameOnly = fileNameOnly;
    }

    public String getLocationInFolder() {
        return locationInFolder;
    }

    public void setLocationInFolder(String locationInFolder) {
        this.locationInFolder = locationInFolder;
    }

    public String getLowerCaseName() {
        return lowerCaseName;
    }

    public void setLowerCaseName(String lowerCaseName) {
        this.lowerCaseName = lowerCaseName;
    }
}

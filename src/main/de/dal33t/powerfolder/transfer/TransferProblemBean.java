package de.dal33t.powerfolder.transfer;

import de.dal33t.powerfolder.light.FileInfo;

import java.util.Date;

/**
 * Represents an actual transfer problem bean.
 */
public class TransferProblemBean {

    /**
     * The file info.
     */
    private final FileInfo fileInfo;

    /**
     * The date it occurred.
     */
    private final Date date;

    /**
     * Details of the problem.
     */
    private final String problemDetail;

    /**
     * Constructor
     *
     * @param fileInfo
     * @param date
     * @param problemDetail
     */
    public TransferProblemBean(FileInfo fileInfo, Date date, String problemDetail) {
        this.fileInfo = fileInfo;
        this.date = date;
        this.problemDetail = problemDetail;
    }

    /**
     * Returns the problem detail
     *
     * @return the problem detail
     */
    public String getProblemDetail() {
        return problemDetail;
    }

    /**
     * Returns the file info
     *
     * @return the file info
     */
    public FileInfo getFileInfo() {
        return fileInfo;
    }

    /**
     * Returns the date
     *
     * @return the date
     */
    public Date getDate() {
        return date;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        TransferProblemBean that = (TransferProblemBean) obj;

        if (date != null ? !date.equals(that.date) : that.date != null) {
            return false;
        }
        if (fileInfo != null ? !fileInfo.equals(that.fileInfo) : that.fileInfo != null) {
            return false;
        }
        if (problemDetail != null ? !problemDetail.equals(that.problemDetail) : that.problemDetail != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = fileInfo != null ? fileInfo.hashCode() : 0;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (problemDetail != null ? problemDetail.hashCode() : 0);
        return result;
    }
}

/*
 * Copyright 2004 - 2009 Christian Sprajc. All rights reserved.
 *
 * This file is part of PowerFolder.
 *
 * PowerFolder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation.
 *
 * PowerFolder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
 *
 * $Id: AddLicenseHeader.java 4282 2008-06-16 03:25:09Z tot $
 */
package de.dal33t.powerfolder.disk.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * An converter, that translates {@link FileInfo} object into a
 * {@link PreparedStatement} and constructs a {@link FileInfo} object from a
 * {@link ResultSet}
 * 
 * @author sprajc
 */
public class FileInfoSQLConverter {
    static final String FIELDNAME_FOLDER_ID = "folderId";
    static final String FIELDNAME_FILE_NAME_LOWER_CASE = "fileNameLower";
    static final String FIELDNAME_MODIFIED_BY_NODE_ID = "modifiedByNodeId";

    private static final Logger LOG = Logger
        .getLogger(FileInfoSQLConverter.class.getName());

    private FileInfoSQLConverter() {
    }

    /**
     * Sets the values of the {@link FileInfo} to the {@link PreparedStatement}.
     * The following order is used:
     * <p>
     * 1 = fileName
     * <p>
     * 2 = fileName (lower case)
     * <p>
     * 3 = size
     * <p>
     * 4 = node id of modifier
     * <p>
     * 5 = last modification time in milliseconds.
     * <p>
     * 6 = version
     * <p>
     * 7 = deleted flag
     * <p>
     * 8 = folder id
     * 
     * @param fInfo
     *            the {@link FileInfo}
     * @param ps
     *            the {@link PreparedStatement} to set the values to.
     * @return the {@link PreparedStatement}
     * @throws SQLException
     */
    public static PreparedStatement set(FileInfo fInfo, PreparedStatement ps)
        throws SQLException
    {
        Reject.ifNull(fInfo, "FileInfo is null");
        Reject.ifNull(ps, "Prepared statement is null");

        int i = 2;
        ps.setString(i++, fInfo.getName());
        ps.setString(i++, fInfo.getName().toLowerCase());
        ps.setLong(i++, fInfo.getSize());
        ps.setString(i++, fInfo.getModifiedBy() != null
            ? fInfo.getModifiedBy().id
            : null);
        ps.setLong(i++, fInfo.getModifiedDate().getTime());
        ps.setLong(i++, fInfo.getVersion());
        ps.setBoolean(i++, fInfo.isDeleted());
        ps.setString(i++, fInfo.getFolderInfo() != null
            ? fInfo.getFolderInfo().id
            : null);

        return ps;
    }

    /**
     * Retrieves a {@link FileInfo} from the given {@link ResultSet} by using
     * the {@link Controller} to resolve {@link FolderInfo} and
     * {@link MemberInfo} objects.
     * 
     * @param controller
     *            the controller to retrieve {@link FolderInfo} and
     *            {@link MemberInfo} from. Can be left null.
     * @param rs
     *            the {@link ResultSet}
     * @return the {@link FileInfo} object.
     * @throws SQLException
     */
    public static FileInfo get(Controller controller, ResultSet rs)
        throws SQLException
    {
        Reject.ifNull(rs, "ResultSet is null");

        String fileName = rs.getString(FileInfo.PROPERTYNAME_FILE_NAME);
        String folderId = rs.getString(FIELDNAME_FOLDER_ID);
        FolderInfo foInfo = null;
        // Try to retrieve from repo
        // TODO Speed this UP!
        if (controller != null) {
            for (Folder folder : controller.getFolderRepository()
                .getFoldersAsCollection())
            {
                if (folder.getId().equals(folderId)) {
                    foInfo = folder.getInfo();
                    break;
                }
            }
        }
        if (foInfo == null) {
            foInfo = new FolderInfo("<unknown>", folderId);
            LOG.log(Level.WARNING,
                "Unable to retrieve folder from controller. ID: " + folderId
                    + " file: " + fileName);
        }

        long size = rs.getLong(FileInfo.PROPERTYNAME_SIZE);
        int version = rs.getInt(FileInfo.PROPERTYNAME_VERSION);
        boolean deleted = rs.getBoolean(FileInfo.PROPERTYNAME_DELETED);

        String modifiedByNodeId = rs.getString(FIELDNAME_MODIFIED_BY_NODE_ID);
        Member modifiedBy = controller != null ? controller.getNodeManager()
            .getNode(modifiedByNodeId) : null;
        MemberInfo modifiedByInfo;
        if (modifiedBy != null) {
            modifiedByInfo = modifiedBy.getInfo();
        } else {
            LOG.warning("Unable to retrieve modifier from controller. ID: "
                + modifiedByNodeId + " file: " + foInfo + "/" + fileName);
            modifiedByInfo = new MemberInfo("<unknonw>", modifiedByNodeId, null);
        }
        long modifiedTime = rs
            .getLong(FileInfo.PROPERTYNAME_LAST_MODIFIED_DATE);
        Date modDate = new Date(modifiedTime);

        if (deleted) {
            return FileInfo.unmarshallDelectedFile(foInfo, fileName,
                modifiedByInfo, modDate, version);
        } else {
            return FileInfo.unmarshallExistingFile(foInfo, fileName, size,
                modifiedByInfo, modDate, version);
        }
    }
}

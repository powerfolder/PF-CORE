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

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.util.Reject;

/**
 * An converter, that translates {@link FileInfo} object into a lucene
 * {@link Document} and back.
 * 
 * @author sprajc
 */
public class FileInfoDocumentConverter {
    static final String FIELDNAME_FOLDER_ID = "folderId";
    static final String FIELDNAME_FILE_NAME_LOWER_CASE = "fileNameLower";
    static final String FIELDNAME_MODIFIED_BY_NODE_ID = "modifiedByNodeId";

    private static final Logger LOG = Logger
        .getLogger(FileInfoDocumentConverter.class.getName());

    private FileInfoDocumentConverter() {
    }

    /**
     * Converts a FileInfo into a lucene Document which is ready to be written
     * into the index
     * 
     * @param fInfo
     *            the FileInfo to conver to
     * @return the Lucene Document
     */
    public static final Document convertToDocument(FileInfo fInfo) {
        Reject.ifNull(fInfo, "FileInfo is null");
        fInfo.validate();
        Document doc = new Document();
        doc.add(new Field(FileInfo.PROPERTYNAME_FILE_NAME, fInfo.getName(),
            Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(FIELDNAME_FILE_NAME_LOWER_CASE, fInfo.getName()
            .toLowerCase(), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(FileInfo.PROPERTYNAME_SIZE, String.valueOf(fInfo
            .getSize()), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(FileInfo.PROPERTYNAME_VERSION, String.valueOf(fInfo
            .getVersion()), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(FileInfo.PROPERTYNAME_DELETED, String.valueOf(fInfo
            .isDeleted()), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(FileInfo.PROPERTYNAME_LAST_MODIFIED_DATE, ""
            + fInfo.getModifiedDate().getTime(), Field.Store.YES,
            Field.Index.NOT_ANALYZED));

        // Referenced objects. Store ID only
        doc
            .add(new Field(FIELDNAME_MODIFIED_BY_NODE_ID,
                fInfo.getModifiedBy().id, Field.Store.YES,
                Field.Index.NOT_ANALYZED));
        doc.add(new Field(FIELDNAME_FOLDER_ID, fInfo.getFolderInfo().id,
            Field.Store.YES, Field.Index.NOT_ANALYZED));

        return doc;
    }

    /**
     * Converts a lucene {@link Document} into a {@link FileInfo} utilizing the
     * controller to retrieve references object like {@link FolderInfo} and
     * {@link MemberInfo}
     * 
     * @param controller
     *            the controller to retrieve referenced objects from (optional).
     * @param doc
     *            the Lucene {@link Document}
     * @return the FileInfo object.
     */
    public static final FileInfo convertToFileInfo(Controller controller,
        Document doc)
    {
        Reject.ifNull(doc, "Document is null");
        String fileName = doc.get(FileInfo.PROPERTYNAME_FILE_NAME);
        String folderId = doc.get(FIELDNAME_FOLDER_ID);
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
        FileInfo fInfo = new FileInfo(foInfo, fileName);

        fInfo.setSize(Long.valueOf(doc.get(FileInfo.PROPERTYNAME_SIZE)));
        fInfo.setVersion(Integer
            .valueOf(doc.get(FileInfo.PROPERTYNAME_VERSION)));
        fInfo.setDeleted(Boolean
            .valueOf(doc.get(FileInfo.PROPERTYNAME_DELETED)));

        String modifiedByNodeId = doc.get(FIELDNAME_MODIFIED_BY_NODE_ID);
        Member modifiedBy = controller != null ? controller.getNodeManager()
            .getNode(modifiedByNodeId) : null;
        MemberInfo modifiedByInfo;
        if (modifiedBy != null) {
            modifiedByInfo = modifiedBy.getInfo();
        } else {
            LOG.warning("Unable to retrieve modifier from controller. ID: "
                + modifiedByNodeId + " file: " + fInfo.getFolderInfo() + "/"
                + fInfo.getName());
            modifiedByInfo = new MemberInfo("<unknonw>", modifiedByNodeId, null);
        }
        long modifiedTime = Long.valueOf(doc
            .get(FileInfo.PROPERTYNAME_LAST_MODIFIED_DATE));
        fInfo.setModifiedInfo(modifiedByInfo, new Date(modifiedTime));

        return fInfo;
    }
}

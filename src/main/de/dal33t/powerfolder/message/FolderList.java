/*
 * Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
 * $Id$
 */
package de.dal33t.powerfolder.message;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.util.ByteSerializer;
import de.dal33t.powerfolder.util.Convert;
import de.dal33t.powerfolder.util.FileUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StreamUtils;
import de.dal33t.powerfolder.util.Util;

/**
 * List of available folders
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
public class FolderList extends Message {
    private static final long serialVersionUID = 101L;
    private static Logger LOG = Logger.getLogger(FolderList.class.getName());

    /** List of public folders. LEFT for backward compatibility */
    public FolderInfo[] folders = new FolderInfo[0];

    /** Secret folders, Folder IDs are encrypted with magic Id */
    public FolderInfo[] secretFolders;

    /**
     * Boolean to indicate that the source also has joined matching meta
     * folders.
     */
    public boolean joinedMetaFolders;

    public FolderList() {
        // Serialisation constructor
    }

    /**
     * Constructor which splits up public and secret folder into own array.
     * Folder Ids of secret folders will be encrypted with magic Id sent by
     * remote node
     * 
     * @param allFolders
     * @param remoteMagicId
     *            the magic id which was sent by the remote side
     */
    public FolderList(Collection<FolderInfo> allFolders, String remoteMagicId) {
        Reject.ifBlank(remoteMagicId, "Remote magic id is blank");
        // Split folderlist into secret and public list
        // Encrypt secret folder ids with magic id
        List<FolderInfo> secretFos = new ArrayList<FolderInfo>(allFolders
            .size());
        for (FolderInfo folderInfo : allFolders) {
            // Send secret folder infos if magic id is not empty
            // Clone folderinfo
            String secureId = folderInfo.calculateSecureId(remoteMagicId);
            // Set Id to secure Id
            FolderInfo secretFolder = new FolderInfo(folderInfo.getName(),
                secureId);
            // Secret folder, encrypt folder id with magic id
            secretFos.add(secretFolder);
        }
        this.secretFolders = new FolderInfo[secretFos.size()];
        this.joinedMetaFolders = Feature.META_FOLDER.isEnabled();
        secretFos.toArray(secretFolders);
    }

    public boolean contains(FolderInfo foInfo, String magicId) {
        String secureId = foInfo.calculateSecureId(magicId);
        for (FolderInfo folder : secretFolders) {
            if (folder.id.equals(secureId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param member
     *            the {@link Member} to store this {@link FolderList} relative
     *            to.
     * @return true if this {@link FolderList} could be stored.
     * @see #load(Member)
     */
    public synchronized boolean store(Member member) {
        String idPath = new String(Util.encodeHex(Util.md5(member.getId()
            .getBytes(Convert.UTF8))));
        File file = new File(Controller.getMiscFilesLocation(), member
            .getController().getConfigName()
            + "/nodes/" + idPath + ".FolderList");
        return store(file);
    }

    /**
     * @param file
     *            the file to store this {@link FolderList} in.
     * @return true if this {@link FolderList} could be stored to that file.
     *@see #load(File)
     */
    public synchronized boolean store(File file) {
        Reject.ifNull(file, "File");
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try {
            byte[] buf = ByteSerializer.serializeStatic(this, false);
            FileUtils.copyFromStreamToFile(new ByteArrayInputStream(buf), file);
            return true;
        } catch (Exception e) {
            LOG.warning("Unable to store to " + file + ". " + e + ". " + this);
            return false;
        }
    }

    /**
     * @param member
     *            the {@link Member} to load a {@link FolderList} previously
     *            stored with {@link #store(Member)} from
     * @return the loaded {@link FolderList} or null if failed or not existing.
     */
    public static FolderList load(Member member) {
        String idPath = new String(Util.encodeHex(Util.md5(member.getId()
            .getBytes(Convert.UTF8))));
        File file = new File(Controller.getMiscFilesLocation(), "nodes/"
            + idPath + ".FolderList");
        return load(file);
    }

    /**
     * @param file
     *            the file to load a {@link FolderList} previously stored with
     *            {@link #store(File)} from
     * @return the loaded {@link FolderList} or null if failed or not existing.
     */
    public static FolderList load(File file) {
        Reject.ifNull(file, "File");
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            byte[] buf = StreamUtils.readIntoByteArray(in);
            return (FolderList) ByteSerializer.deserializeStatic(buf, false);
        } catch (Exception e) {
            LOG.warning("Unable to load to " + file + ". " + e);
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (joinedMetaFolders ? 1231 : 1237);
        result = prime * result + Arrays.hashCode(secretFolders);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FolderList other = (FolderList) obj;
        if (joinedMetaFolders != other.joinedMetaFolders)
            return false;
        if (!Arrays.equals(secretFolders, other.secretFolders))
            return false;
        return true;
    }

    public String toString() {
        return "FolderList: " + secretFolders.length + " folders";
    }
}
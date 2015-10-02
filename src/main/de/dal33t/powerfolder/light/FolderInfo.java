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
package de.dal33t.powerfolder.light;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.protocol.FolderInfoProto;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.intern.FolderInfoInternalizer;
import de.dal33t.powerfolder.util.intern.Internalizer;

/**
 * A Folder hash info
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FolderInfo implements Serializable, Cloneable, D2DObject {
    private static final long serialVersionUID = 102L;
    private static final Internalizer<FolderInfo> INTERNALIZER = new FolderInfoInternalizer();

    public static final String PROPERTYNAME_ID = "id";
    public static final String PROPERTYNAME_NAME = "name";

    @Index(name="IDX_FOLDER_NAME")
    private String name;
    @Id
    public String id;

    /**
     * The cached hash info.
     */
    private transient int hash;

    private FolderInfo() {
        // NOP - for Hibernate
    }

    public FolderInfo(Folder folder) {
        name = folder.getName();
        id = folder.getId();
        hash = hashCode0();
    }

    public FolderInfo(String name, String id) {
        this.name = name;
        this.id = id;
        hash = hashCode0();
    }

    /**
     * PFS-1129: Account relative backup folder. Name must be unique.
     *
     * @param name
     * @param aInfo
     */
    public FolderInfo(String name, AccountInfo aInfo) {
        this(name, "PB-" + aInfo.getOID() + "-" + name);
    }

    /** FolderInfo
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    public
    FolderInfo(AbstractMessage mesg)
    {
      initFromD2D(mesg);
    }

    public boolean isMetaFolder() {
        // #1548: Convert this into boolean flag?
        return id != null && id.startsWith(Constants.METAFOLDER_ID_PREFIX);
    }

    /**
     * @return the {@link FolderInfo} of the PARENT folder if this is a meta
     *         folder.
     */
    public FolderInfo getParentFolderInfo() {
        if (!isMetaFolder()) {
            Logger.getLogger(FolderInfo.class.getName()).log(
                Level.WARNING,
                "Not required to retrieve parent folder info on non-meta folder: "
                    + this, new RuntimeException("from here"));
            return this;
        }
        try {
            int i = id.indexOf(Constants.METAFOLDER_ID_PREFIX);
            String folderId = id.substring(i
                + Constants.METAFOLDER_ID_PREFIX.length());
            i = name.indexOf(Constants.METAFOLDER_ID_PREFIX);
            String folderName = name.substring(i
                + Constants.METAFOLDER_ID_PREFIX.length());
            return new FolderInfo(folderName, folderId).intern();
        } catch (Exception e) {
            Logger.getLogger(FolderInfo.class.getName()).log(Level.WARNING,
                "Unable to get parent folder info for meta-folder: " + this, e);
            return this;
        }
    }

    /**
     * @return the meta-folder info for this folder
     */
    public FolderInfo getMetaFolderInfo() {
        if (isMetaFolder()) {
            Logger.getLogger(FolderInfo.class.getName()).log(Level.WARNING,
                "Tried to retrieve meta-folder for meta-folder: " + this,
                new RuntimeException("from here"));
            return this;
        }
        return new FolderInfo(Constants.METAFOLDER_ID_PREFIX + name,
            Constants.METAFOLDER_ID_PREFIX + id);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the joined folder, or null if folder is not joined
     *
     * @param controller
     * @return the folder
     */
    public Folder getFolder(Controller controller) {
        return controller.getFolderRepository().getFolder(this);
    }

    // Security ****************************************************************

    /**
     * Calculates the secure Id for this folder with magicid from remote
     *
     * @param magicId
     * @return the secure Id for this folder with magicid from remote
     */
    public String calculateSecureId(String magicId) {
        // Do the magic...
        try {
            byte[] mId = magicId.getBytes("UTF-8");
            byte[] fId = id.getBytes("UTF-8");
            byte[] hexId = new byte[mId.length * 2 + fId.length];

            // Build secure ID base: [MAGIC_ID][FOLDER_ID][MAGIC_ID]
            System.arraycopy(mId, 0, hexId, 0, mId.length);
            System.arraycopy(fId, 0, hexId, mId.length - 1, fId.length);
            System.arraycopy(mId, 0, hexId, mId.length + fId.length - 2,
                mId.length);
            return new String(Util.encodeHex(Util.md5(hexId)));
        } catch (UnsupportedEncodingException e) {
            throw (IllegalStateException) new IllegalStateException(
                "Fatal problem: UTF-8 encoding not found").initCause(e);
        }
    }

    /*
     * General
     */

    @Override
    public int hashCode() {
        if (hash == 0) {
            // Oh! Default value. Better recalculate hashcode cache
            hash = hashCode0();
        }
        return hash;
    }

    private int hashCode0() {
        return (id == null) ? 0 : id.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof FolderInfo) {
            FolderInfo otherInfo = (FolderInfo) other;
            return Util.equals(this.id, otherInfo.id);
        }

        return false;
    }

    public FolderInfo intern(boolean force) {
        if (force) {
            return INTERNALIZER.rename(this);
        } else {
            return intern();
        }
    }

    public FolderInfo intern() {
        return INTERNALIZER.intern(this);
    }

    // used for sorting ignores case
    public int compareTo(Object other) {
        FolderInfo otherFolderInfo = (FolderInfo) other;
        return name.compareToIgnoreCase(otherFolderInfo.name);
    }

    @Override
    public String toString() {
        return "Folder '" + name + '\'';
    }

    // Serialization optimization *********************************************

    private static final long extVersionUID = 100L;

    public static FolderInfo readExt(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        FolderInfo folderInfo = new FolderInfo();
        folderInfo.readExternal(in);
        return folderInfo;
    }

    public void readExternal(ObjectInput in) throws IOException,
        ClassNotFoundException
    {
        long extUID = in.readLong();
        if (extUID != extVersionUID) {
            throw new InvalidClassException(this.getClass().getName(),
                "Unable to read. extVersionUID(steam): " + extUID
                    + ", expected: " + extVersionUID);
        }
        id = in.readUTF();
        name = in.readUTF();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(extVersionUID);
        out.writeUTF(id);
        out.writeUTF(name);
    }

    public String getLocalizedName() {
        return name
            .replace(Constants.ZYNCRO_GROUP_TOKEN.trim(),
                Translation.get("general.group"))
            .replace(Constants.ZYNCRO_DEPARTMENT_TOKEN.trim(),
                Translation.get("general.department"))
            .replace(Constants.ZYNCRO_COMPANY_TOKEN.trim(),
                Translation.get("general.company"))
            .replace(Constants.FOLDER_PUBLIC_SHARED_FILES,
                Translation.get("general.public_shared_files"))
            .replace(Constants.FOLDER_PERSONAL_FILES,
                Translation.get("general.personal_files"))
            .replace(Constants.MAIL_ATTACHMENT_FOLDER,
                Translation.get("mail_attachment_folder_name"));
    }

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void
    initFromD2D(AbstractMessage mesg)
    {
      if(mesg instanceof FolderInfoProto.FolderInfo)
        {
          FolderInfoProto.FolderInfo finfo = (FolderInfoProto.FolderInfo)mesg;

          this.name = finfo.getName();
          this.id   = finfo.getId();
          this.hash = hashCode0();
        }
    }

    /** toD2DMessage
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage
    toD2D()
    {
      FolderInfoProto.FolderInfo.Builder builder = FolderInfoProto.FolderInfo.newBuilder();

      builder.setClassName("FolderInfo");
      builder.setName(this.name);
      builder.setId(this.id);

      return builder.build();
    }
}

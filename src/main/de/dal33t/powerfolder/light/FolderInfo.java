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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import de.dal33t.powerfolder.util.StackDump;
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

import static de.dal33t.powerfolder.light.FolderInfoFactory.lookupInstance;
import static de.dal33t.powerfolder.light.FolderInfoFactory.unmarshallExistingTopFolder;

/**
 * A Folder hash info
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.9 $
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FolderInfo implements Serializable, Cloneable, D2DObject {
    private static final Logger LOG = Logger.getLogger(FolderInfo.class.getName());
    private static final long serialVersionUID = 102L;
    private static final Internalizer<FolderInfo> INTERNALIZER = new FolderInfoInternalizer();

    public static final String PROPERTYNAME_ID = "id";
    public static final String PROPERTYNAME_NAME = "name";
    public static final String PROPERTYNAME_VERSION = "version";
    public static final String PROPERTYNAME_PARENT = "parent";

    @Index(name="IDX_FOLDER_NAME")
    private String name;
    @Id
    public String id;

    /**
     * PFC-3136: Version number of this folder
     */
    @Transient
    private int version;

    /**
     * PF-1790: The location of this folder in another folder. null if top level
     */
    @Transient
    private DirectoryInfo location;

    /**
     * The cached hash info.
     */
    private transient int hash;

    private FolderInfo()
    {
        // NOP - for Hibernate
    }

    // TODO
    @Deprecated
    public FolderInfo(String name, String id) {
        this.name = name;
        this.id = id;
        hash = hashCode0();
    }

    FolderInfo(String name, String id, int version, DirectoryInfo location) {
        this.name = name;
        this.id = id;
        this.version = version;
        this.location = location;
        hash = hashCode0();
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

    public boolean isLookupInstance() {
        return version < 0;
    }

    /**
     * @return the lookup {@link FolderInfo} of the PARENT folder if this is a meta
     *         folder.
     */
    public FolderInfo lookupParentFolderInfo() {
        if (!isMetaFolder()) {
            LOG.log(Level.WARNING,
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
            return lookupInstance(folderId, name);
        } catch (Exception e) {
            LOG.log(Level.WARNING,
                "Unable to get parent folder info for meta-folder: " + this, e);
            return this;
        }
    }

    /**
     * @return the meta-folder info for this folder
     */
    public FolderInfo getMetaFolderInfo() {
        return unmarshallExistingTopFolder(
                Constants.METAFOLDER_ID_PREFIX + id,
                Constants.METAFOLDER_ID_PREFIX + name,
                version);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        if (isLookupInstance()) {
            // Safeguard, never hand out -1
            return 0;
        }
        return version;
    }

    public DirectoryInfo getLocation() {
        return location;
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
     * Calculates the secure Id for this folder with magicid from remote. Deprecated and security is handled differently.
     *
     * @param magicId
     * @return the secure Id for this folder with magicid from remote
     */
    @Deprecated
    public String calculateSecureId(String magicId) {
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
            throw new IllegalStateException(
                "Fatal problem: UTF-8 encoding not found", e);
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

    public FolderInfo intern() {
        return intern(false);
    }

    public FolderInfo intern(boolean force) {
        if (force) {
            return INTERNALIZER.rename(this);
        } else {
            return INTERNALIZER.intern(this);
        }
    }

    // used for sorting ignores case
    public int compareTo(Object other) {
        FolderInfo otherFolderInfo = (FolderInfo) other;
        return name.compareToIgnoreCase(otherFolderInfo.name);
    }

    @Override
    public String toString() {
        if (isLookupInstance()) {
            return "Folder " + name + '/' + id + '/' + "L";
        }
        return "Folder " + name + '/' + id + '/' + version + (location != null ? "<-" + location : "");
    }

    // Serialization optimization *********************************************

    private static final long extVersionUID = 101L;

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
        if (extUID != 100L && extUID != extVersionUID) {
            throw new InvalidClassException(this.getClass().getName(),
                "Unable to read. extVersionUID(steam): " + extUID
                    + ", expected: " + extVersionUID);
        }
        id = in.readUTF();
        name = in.readUTF();
        if (extUID == 100L) {
            return;
        }
        version = in.readInt();
        if (in.readBoolean()) {
            location = (DirectoryInfo) FileInfoFactory.readExt(in);
        }
        LOG.log(Level.INFO,this + ": readExternal " + extUID, new StackDump());
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        writeExternal(out, true);
    }

    public void writeExternal(ObjectOutput out, boolean includeVersionAndParent) throws IOException {
        boolean requiresNewProtocol = version > 0 || location != null;
        if (includeVersionAndParent) {
            includeVersionAndParent = requiresNewProtocol;
        } else if (requiresNewProtocol) {
            LOG.log(Level.WARNING,
                    this + ": writeExternal would require new protocol, using backward compatibility.", new StackDump());
        }
        if (includeVersionAndParent) {
            out.writeLong(extVersionUID);
        } else {
            // Use old protocol
            out.writeLong(100L);
        }
        out.writeUTF(id);
        out.writeUTF(name);
        if (!includeVersionAndParent) {
            return;
        }
        LOG.log(Level.INFO, this + ": writeExternal ? " + includeVersionAndParent, new StackDump());
        out.writeInt(version);
        if (location != null) {
            out.writeBoolean(true);
            location.writeExternal(out);
        } else {
            out.writeBoolean(false);
        }
    }

    public String getLocalizedName() {
        if (name == null) {
            return null;
        }
        return name
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

    /** toD2D
     * Convert to D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @return Converted D2D message
     **/

    @Override
    public AbstractMessage
    toD2D()
    {
      FolderInfoProto.FolderInfo.Builder builder = FolderInfoProto.FolderInfo.newBuilder();

      builder.setClazzName(this.getClass().getSimpleName());
      builder.setName(this.name);
      builder.setId(this.id);

      return builder.build();
    }

    // Writing / Loading *****************************************************

    /**
     * Saves this FolderInfo to the given file.
     *
     * @param file
     */
    boolean save(Path file) {
        if (Files.notExists(file.getParent())) {
            return false;
        }
        try (ObjectOutputStream oout = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(file)))) {
            oout.writeObject(this);
        } catch (Exception e) {
            LOG.warning(this + ": Unable to store FolderInfo to " + file + ". " + e);
        }
        return true;
    }

    static FolderInfo load(Path file) {
        if (Files.notExists(file)) {
            return null;
        }
        try (InputStream fin = Files.newInputStream(file)) {
            ObjectInputStream oin = new ObjectInputStream(
                    new BufferedInputStream(fin));
            FolderInfo folderInfo = (FolderInfo) oin.readObject();
            return folderInfo;
        } catch (Exception e) {
            LOG.warning("Unable to read FolderInfo from " + file + ". " + e);
        }
        return null;
    }
}

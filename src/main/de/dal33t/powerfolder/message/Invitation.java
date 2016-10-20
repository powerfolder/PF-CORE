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
 * $Id: Invitation.java 20495 2012-12-08 14:15:00Z glasgow $
 */
package de.dal33t.powerfolder.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.persistence.*;

import org.hibernate.annotations.Index;
import org.hibernate.annotations.Type;

import com.google.protobuf.AbstractMessage;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.light.MemberInfo;
import de.dal33t.powerfolder.light.ServerInfo;
import de.dal33t.powerfolder.protocol.FolderInfoProto;
import de.dal33t.powerfolder.protocol.InvitationProto;
import de.dal33t.powerfolder.protocol.MemberInfoProto;
import de.dal33t.powerfolder.security.FolderPermission;
import de.dal33t.powerfolder.util.IdGenerator;
import de.dal33t.powerfolder.util.PathUtils;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.StringUtils;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.os.OSUtil;
import de.dal33t.powerfolder.util.os.Win32.WinUtils;

/**
 * An invitation to a folder or an invitation of a user.
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * 
 * Adapted the class to be stored to a database using hibernate.
 * 
 * @author <a href="mailto:krickl@powerfolder.com">Maximilian Krickl</a>
 */
@Entity
public class Invitation extends FolderRelatedMessage
  implements D2DObject
{
    private static final long serialVersionUID = 101L;

    public static final String PROPERTY_OID = "oid";
    public static final String PROPERTY_INVITOR = "invitor";
    public static final String PROPERTY_INVITATION_TEXT = "invitationText";
    public static final String PROPERTY_SUGGESTED_SYNC_PROFILE_CONFIG = "suggestedSyncProfileConfig";
    public static final String PROPERTY_SUGGESTED_LOCAL_BASE_PATH = "suggestedLocalBasePath";
    public static final String PROPERTY_RELATIVE = "relative";
    public static final String PROPERTY_PERMISSION = "permission";
    public static final String PROPERTY_SERVER = "server";
    public static final String PROPERTY_SENDER = "sender";
    public static final String PROPERTY_RECIPIENT = "recipient";

    // Since 9.1
    @Id
    private String oid;

    @ManyToOne
    @JoinColumn(name = "nodeInfo_id")
    private MemberInfo invitor;
    @Column(length = 2048)
    private String invitationText;
    private String suggestedSyncProfileConfig;

    @Column(length = 2048)
    private String suggestedLocalBasePath;
    @Enumerated(EnumType.STRING)
    private PathType relative;
    @Type(type = "permissionType")
    private FolderPermission permission;

    /**
     * PFS-2455
     * @since 11.0
     */
    @ManyToOne
    @JoinColumn(name = "serviceInfo_id")
    private ServerInfo server;

    /**
     * PFS-2008: replaces {@link #username}
     * NOTE: sender should be annotated with a @Column(length = 512) as it could contain
     *       an e-mail address {@link de.dal33t.powerfolder.security.Account#emails}
     *       and those can be up to 512 char long. BUT this would lead to an index of
     *       size > 900 which is a hard limit in MS SQL databases.
     * @since 11.2
     */
    @Index(name = "IDX_INV_SENDER")
    private String sender;

    /**
     * PFS-2008: replaces {@link #inviteeUsername}
     * NOTE: recipient should be annotated with a @Column(length = 512) as it could contain
     *       an e-mail address {@link de.dal33t.powerfolder.security.Account#emails}
     *       and those can be up to 512 char long. BUT this would lead to an index of
     *       size > 900 which is a hard limit in MS SQL databases.
     * @since 11.2
     */
    @Index(name = "IDX_INV_RECIPIENT")
    private String recipient;

    /**
     * PFS-2008
     * @since 6.0
     * @deprecated as of v11.1, replaced by {@link #sender}
     */
    @Deprecated
    @Transient
    private String username;
    /**
     * PFS-2008
     * @since 9.1
     * @deprecated as of v11.1, replaced by {@link #recipient}
     */
    @Deprecated
    @Transient
    private String inviteeUsername;

    /**
     * Constructor
     *
     * @param permission The permission to the folder of this invitation
     */
    public Invitation(FolderPermission permission) {
        oid = IdGenerator.makeId();
        this.permission = permission;
        this.folder = permission.getFolder();
    }

    private Invitation() {
        // NOP - Hibernate
    }

    public String getOID() {
        return oid;
    }

    public void setSuggestedSyncProfile(SyncProfile suggestedSyncProfile) {
        suggestedSyncProfileConfig = suggestedSyncProfile.getFieldList();
    }

    /**
     * Sets the suggested local base. Parses to get relative paths from apps dir
     * and PowerFolder local base. For subdirs of the PowerFolder base directory
     * and of the apps dir, the relative part of the location is extracted so
     * that the receiver can locate local to his computer's environment.
     *
     * @param controller
     * @param suggestedLocalBase
     */
    public void setSuggestedLocalBase(Controller controller,
        Path suggestedLocalBase)
    {
        Reject.ifNull(suggestedLocalBase, "File is null");
        String folderBase = controller.getFolderRepository()
            .getFoldersBasedirString();
        String appsDir = getAppsDir();
        String userHomeDir = getUserHomeDir();
        if (OSUtil.isWindowsSystem() && appsDir != null
            && suggestedLocalBase.toAbsolutePath().startsWith(appsDir))
        {
            String filePath = suggestedLocalBase.toAbsolutePath().toString();
            suggestedLocalBasePath = filePath.substring(appsDir.length());

            // Remove any leading file separators.
            while (suggestedLocalBasePath.startsWith(suggestedLocalBase.getFileSystem().getSeparator())) {
                suggestedLocalBasePath = suggestedLocalBasePath.substring(1);
            }
            relative = PathType.RELATIVE_APP_DATA;
        } else if (folderBase != null
            && suggestedLocalBase.toAbsolutePath().startsWith(folderBase))
        {
            String filePath = suggestedLocalBase.toAbsolutePath().toString();
            String baseDirPath = controller.getFolderRepository()
                .getFoldersBasedirString();
            suggestedLocalBasePath = filePath.substring(baseDirPath.length());

            // Remove any leading file separators.
            while (suggestedLocalBasePath.startsWith(suggestedLocalBase.getFileSystem().getSeparator())) {
                suggestedLocalBasePath = suggestedLocalBasePath.substring(1);
            }
            relative = PathType.RELATIVE_PF_BASE;
        } else if (userHomeDir != null
            && suggestedLocalBase.toAbsolutePath().startsWith(userHomeDir))
        {
            String filePath = suggestedLocalBase.toAbsolutePath().toString();
            suggestedLocalBasePath = filePath.substring(userHomeDir.length());

            // Remove any leading file separators.
            while (suggestedLocalBasePath.startsWith(suggestedLocalBase.getFileSystem().getSeparator())) {
                suggestedLocalBasePath = suggestedLocalBasePath.substring(1);
            }
            relative = PathType.RELATIVE_USER_HOME;
        } else {
            suggestedLocalBasePath = suggestedLocalBase.toAbsolutePath().toString();
            relative = PathType.ABSOLUTE;
        }
    }

    /**
     * Get the suggested local base. Uses 'relative' to adjust for the local
     * environment.
     *
     * @param controller
     * @return the suggestion path on the local computer
     */
    public Path getSuggestedLocalBase(Controller controller) {

        if (suggestedLocalBasePath == null) {
            return controller.getFolderRepository()
                .getFoldersBasedir().resolve(
                PathUtils.removeInvalidFilenameChars(folder.getName()));
        }

        if (OSUtil.isLinux() || OSUtil.isMacOS()) {
            suggestedLocalBasePath = Util.replace(suggestedLocalBasePath, "\\",
                FileSystems.getDefault().getSeparator());
        } else {
            suggestedLocalBasePath = Util.replace(suggestedLocalBasePath, "/",
                FileSystems.getDefault().getSeparator());
        }

        if (relative == PathType.RELATIVE_APP_DATA) {
            return Paths.get(getAppsDir(), suggestedLocalBasePath);
        } else if (relative == PathType.RELATIVE_PF_BASE) {
            Path powerFolderBaseDir = controller.getFolderRepository()
                .getFoldersBasedir();
            return powerFolderBaseDir.resolve(suggestedLocalBasePath);
        } else if (relative == PathType.RELATIVE_USER_HOME) {
            return Paths.get(getUserHomeDir(), suggestedLocalBasePath);
        } else {
            return Paths.get(suggestedLocalBasePath);
        }
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public ServerInfo getServer() {
        return server;
    }

    public void setServer(ServerInfo server) {
        this.server = server;
    }

    public MemberInfo getInvitor() {
        return invitor;
    }

    public void setInvitor(MemberInfo invitor) {
        this.invitor = invitor;
    }

    public String getInvitationText() {
        return invitationText;
    }

    public void setInvitationText(String invitationText) {
        this.invitationText = invitationText;
    }

    public FolderPermission getPermission() {
        return permission;
    }

    public void setPermission(FolderPermission permission) {
        if (permission != null) {
            this.folder = permission.getFolder();
        }
        this.permission = permission;
    }

    public SyncProfile getSuggestedSyncProfile() {
        if (suggestedSyncProfileConfig == null) {
            // For backward compatibility.
            return SyncProfile.AUTOMATIC_SYNCHRONIZATION;
        }
        return SyncProfile
            .getSyncProfileByFieldList(suggestedSyncProfileConfig);
    }

    private static String getAppsDir() {
        if (OSUtil.isWindowsSystem()) {
            return WinUtils.getAppDataCurrentUser();
        }

        // Loading a Windows invitation on a Mac/Unix box:
        // no APPDIR, so set to somewhere safe.
        return getUserHomeDir();
    }

    private static String getUserHomeDir() {
        return System.getProperty("user.home");
    }

    private static enum PathType {
        /** suggestedLocalBase is absolute. */
        ABSOLUTE,
        /** suggestedLocalBase is relative to apps directory. */
        RELATIVE_APP_DATA,
        /** suggestedLocalBase is relative to PowerFolder base directory. */
        RELATIVE_PF_BASE,
        /** suggestedLocalBase is relative to user home directory. */
        RELATIVE_USER_HOME
    }

    @Override
    public String toString() {
        return "Invitation to " + folder + " from " + invitor;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + (oid == null ? 0 : oid.hashCode());
        result = prime * result
            + (invitationText == null ? 0 : invitationText.hashCode());
        result = prime * result + (invitor == null ? 0 : invitor.hashCode());
        result = prime * result
            + (permission == null ? 0 : permission.hashCode());
        result = prime * result + (username == null ? 0 : username.hashCode());
        result = prime * result
            + (inviteeUsername == null ? 0 : inviteeUsername.hashCode());
        result = prime
            * result
            + (suggestedLocalBasePath == null ? 0 : suggestedLocalBasePath
                .hashCode());
        result = prime
            * result
            + (suggestedSyncProfileConfig == null
                ? 0
                : suggestedSyncProfileConfig.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Invitation other = (Invitation) obj;
        if (oid == null) {
            if (other.oid != null) {
                return false;
            }
        }else if (!oid.equals(other.oid)) {
            return false;
        }
        if (invitationText == null) {
            if (other.invitationText != null) {
                return false;
            }
        } else if (!invitationText.equals(other.invitationText)) {
            return false;
        }
        if (invitor == null) {
            if (other.invitor != null) {
                return false;
            }
        } else if (!invitor.equals(other.invitor)) {
            return false;
        }
        if (username == null) {
            if (other.username != null) {
                return false;
            }
        } else if (!username.equals(other.username)) {
            return false;
        }
        if (inviteeUsername == null) {
            if (other.inviteeUsername != null) {
                return false;
            }
        } else if (!inviteeUsername.equals(other.inviteeUsername)) {
            return false;
        }
        if (permission == null) {
            if (other.permission != null) {
                return false;
            }
        } else if (!permission.equals(other.permission)) {
            return false;
        }
        if (relative != other.relative) {
            return false;
        }
        if (suggestedLocalBasePath == null) {
            if (other.suggestedLocalBasePath != null) {
                return false;
            }
        } else if (!suggestedLocalBasePath.equals(other.suggestedLocalBasePath)) {
            return false;
        }
        if (suggestedSyncProfileConfig == null) {
            if (other.suggestedSyncProfileConfig != null) {
                return false;
            }
        } else if (!suggestedSyncProfileConfig
            .equals(other.suggestedSyncProfileConfig)) {
            return false;
        }
        return true;
    }

    // +++ D2D (un)marshalling +++

    /** initFromD2DMessage
     * Init from D2D message
     * @author Christoph Kappel <kappel@powerfolder.com>
     * @param  mesg  Message to use data from
     **/

    @Override
    public void
    initFromD2D(AbstractMessage mesg)
    {
      if(mesg instanceof InvitationProto.Invitation)
        {
          InvitationProto.Invitation proto = (InvitationProto.Invitation)mesg;

          this.invitor = new MemberInfo(proto.getInvitor());
          this.invitationText = proto.getInvitationText();
          this.username = proto.getUsername();
          this.oid = proto.getOid();
          this.inviteeUsername = proto.getInviteeUsername();
          this.folder = new FolderInfo(proto.getFolder());
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
      InvitationProto.Invitation.Builder builder =
        InvitationProto.Invitation.newBuilder();

      builder.setClazzName("Invitation");
      builder.setInvitor(
        (MemberInfoProto.MemberInfo)this.invitor.toD2D());
      builder.setInvitationText(this.invitationText);
      builder.setUsername(this.username);
      builder.setOid(this.oid);
      builder.setInviteeUsername(this.inviteeUsername);
      builder.setFolder(
        (FolderInfoProto.FolderInfo)this.folder.toD2D());

      return builder.build();
    }

    // Backward compatability for deprecated/replaced fields.

    public void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.sender    = this.username;
        this.recipient = this.inviteeUsername;
    }

    public void writeObject(ObjectOutputStream out) throws IOException {
        this.inviteeUsername = this.recipient;
        this.username        = this.sender;
        out.defaultWriteObject();
    }

    /**
     * @deprecated Since 11.1 use {@link #getSender()}
     */
    @Deprecated
    // Return the user name if not blank, else the invitor nick.
    public String getInvitorUsername() {
        if (StringUtils.isBlank(username)) {
            if (invitor == null) {
                return "";
            }
            return invitor.getNick();
        }
        return username;
    }

    /**
     * @deprecated Since 11.1 use {@link #setSender(String)}
     */
    @Deprecated
    public void setInvitorUsername(String username) {
        this.username = username;
    }

    /**
     * @deprecated Since 11.1 use {@link #getRecipient()}
     */
    @Deprecated
    public String getInviteeUsername() {
        return inviteeUsername;
    }

    /**
     * @deprecated Since 11.1 use {@link #setRecipient(String)}
     */
    @Deprecated
    public void setInviteeUsername(String username) {
        this.inviteeUsername = username;
    }
}

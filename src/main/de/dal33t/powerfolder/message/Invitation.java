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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

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
 * A Invitation to a folder
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.5 $
 */
public class Invitation extends FolderRelatedMessage
  implements D2DObject
{
    private static final long serialVersionUID = 101L;

    /** suggestedLocalBase is absolute. */
    private static final int ABSOLUTE = 0;

    /** suggestedLocalBase is relative to apps directory. */
    private static final int RELATIVE_APP_DATA = 1;

    /** suggestedLocalBase is relative to PowerFolder base directory. */
    private static final int RELATIVE_PF_BASE = 2;

    /** suggestedLocalBase is relative to user home directory. */
    private static final int RELATIVE_USER_HOME = 3;
    
    public static final String ACCOUNT_INVITATION_ID_PREFIX = "AI_";

    private MemberInfo invitor;
    // For backward compatibility to pre 3.1.2 versions.
    private File suggestedLocalBase;
    private String invitationText;
    private String suggestedSyncProfileConfig;
    private String suggestedLocalBasePath;
    private int relative;
    private FolderPermission permission;

    // Since 4.0.1:
    private long size;
    private int filesCount;

    // Since 6.0: invitorUsername
    private String username;

    // Since 9.1
    private String oid;
    private String inviteeUsername;

    // Since 11.0: PFS-2455
    private ServerInfo server;

    /**
     * Constructor
     *
     * @param folder
     * @param invitor
     */
    public Invitation(FolderInfo folder, MemberInfo invitor) {
        this.folder = folder;
        this.invitor = invitor;
        oid = IdGenerator.makeId();
    }

    public String getOID() {
        return oid;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public int getFilesCount() {
        return filesCount;
    }

    public void setFilesCount(int filesCount) {
        this.filesCount = filesCount;
    }

    public void setInvitor(MemberInfo invitor) {
        this.invitor = invitor;
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
        try {
            this.suggestedLocalBase = suggestedLocalBase.toFile();
        } catch (Exception e) {
            Logger.getLogger(Invitation.class.getName()).fine(
                "Unable to set suggested path: " + suggestedLocalBase + ". "
                    + e);
        }
        String folderBase = controller.getFolderRepository()
            .getFoldersBasedirString();
        String appsDir = getAppsDir();
        String userHomeDir = getUserHomeDir();
        if (OSUtil.isWindowsSystem() && appsDir != null
            && suggestedLocalBase.toAbsolutePath().toString().startsWith(appsDir))
        {
            String filePath = suggestedLocalBase.toAbsolutePath().toString();
            suggestedLocalBasePath = filePath.substring(appsDir.length());

            // Remove any leading file separators.
            while (suggestedLocalBasePath.startsWith(suggestedLocalBase.getFileSystem().getSeparator())) {
                suggestedLocalBasePath = suggestedLocalBasePath.substring(1);
            }
            relative = RELATIVE_APP_DATA;
        } else if (folderBase != null
            && suggestedLocalBase.toAbsolutePath().toString().startsWith(folderBase))
        {
            String filePath = suggestedLocalBase.toAbsolutePath().toString();
            String baseDirPath = controller.getFolderRepository()
                .getFoldersBasedirString();
            suggestedLocalBasePath = filePath.substring(baseDirPath.length());

            // Remove any leading file separators.
            while (suggestedLocalBasePath.startsWith(suggestedLocalBase.getFileSystem().getSeparator())) {
                suggestedLocalBasePath = suggestedLocalBasePath.substring(1);
            }
            relative = RELATIVE_PF_BASE;
        } else if (userHomeDir != null
            && suggestedLocalBase.toAbsolutePath().toString().startsWith(userHomeDir))
        {
            String filePath = suggestedLocalBase.toAbsolutePath().toString();
            suggestedLocalBasePath = filePath.substring(userHomeDir.length());

            // Remove any leading file separators.
            while (suggestedLocalBasePath.startsWith(suggestedLocalBase.getFileSystem().getSeparator())) {
                suggestedLocalBasePath = suggestedLocalBasePath.substring(1);
            }
            relative = RELATIVE_USER_HOME;
        } else {
            suggestedLocalBasePath = suggestedLocalBase.toAbsolutePath().toString();
            relative = ABSOLUTE;
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
                Paths.get("").getFileSystem().getSeparator());
        } else {
            suggestedLocalBasePath = Util.replace(suggestedLocalBasePath, "/",
                Paths.get("").getFileSystem().getSeparator());
        }

        if (relative == RELATIVE_APP_DATA) {
            return Paths.get(getAppsDir(), suggestedLocalBasePath);
        } else if (relative == RELATIVE_PF_BASE) {
            Path powerFolderBaseDir = controller.getFolderRepository()
                .getFoldersBasedir();
            return powerFolderBaseDir.resolve(suggestedLocalBasePath);
        } else if (relative == RELATIVE_USER_HOME) {
            return Paths.get(getUserHomeDir(), suggestedLocalBasePath);
        } else {
            return Paths.get(suggestedLocalBasePath);
        }
    }

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

    public void setInvitorUsername(String username) {
        this.username = username;
    }

    public ServerInfo getServer() {
        return server;
    }

    public void setServer(ServerInfo server) {
        this.server = server;
    }

    public void setInviteeUsername(String username) {
        this.inviteeUsername = username;
    }

    public String getInviteeUsername() {
        return inviteeUsername;
    }

    public MemberInfo getInvitor() {
        return invitor;
    }

    public String getInvitationText() {
        return invitationText;
    }

    public void setInvitationText(String invitationText) {
        this.invitationText = invitationText;
    }

    public int getRelative() {
        return relative;
    }

    public FolderPermission getPermission() {
        return permission;
    }

    public void setPermission(FolderPermission permission) {
        this.permission = permission;
    }

    public boolean isFolderInvitation() {
        return StringUtils.isNotBlank(folder.getId())
            && !folder.getId().startsWith(ACCOUNT_INVITATION_ID_PREFIX);
    }

    public boolean isAccountInvitation() {
        return StringUtils.isBlank(folder.getId())
            || folder.getId().startsWith(ACCOUNT_INVITATION_ID_PREFIX);
    }

    public SyncProfile getSuggestedSyncProfile() {
        if (suggestedSyncProfileConfig == null) {
            // For backward compatibility.
            return SyncProfile.AUTOMATIC_SYNCHRONIZATION;
        }
        return SyncProfile
            .getSyncProfileByFieldList(suggestedSyncProfileConfig);
    }

    @Override
    public String toString() {
        return "Invitation to " + folder + " from " + invitor;
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
        result = prime * result + relative;
        result = prime * result + (username == null ? 0 : username.hashCode());
        result = prime * result
            + (inviteeUsername == null ? 0 : inviteeUsername.hashCode());
        result = prime
            * result
            + (suggestedLocalBase == null ? 0 : suggestedLocalBase.hashCode());
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
        if (suggestedLocalBase == null) {
            if (other.suggestedLocalBase != null) {
                return false;
            }
        } else if (!suggestedLocalBase.equals(other.suggestedLocalBase)) {
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
          this.size = proto.getSize();
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
      builder.setSize(this.size);
      builder.setUsername(this.username);
      builder.setOid(this.oid);
      builder.setInviteeUsername(this.inviteeUsername);
      builder.setFolder(
        (FolderInfoProto.FolderInfo)this.folder.toD2D());

      return builder.build();
    }
}

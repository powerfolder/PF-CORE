/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.light;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.Constants;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Feature;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.InterruptedSubFolderIndex;
import de.dal33t.powerfolder.protocol.FolderInfoProto;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.TagUtil;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.intern.FolderInfoInternalizer;
import de.dal33t.powerfolder.util.intern.Internalizer;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static de.dal33t.powerfolder.light.FolderInfoFactory.lookupInstance;
import static de.dal33t.powerfolder.light.FolderInfoFactory.unmarshallExistingTopFolder;
import static de.dal33t.powerfolder.util.StringUtils.isNotBlank;

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
    /*
     * WARNING: Changing this value causes SIGNIFICANT problems and is virtually never the right
     * move: every serialized FolderInfo becomes unreadable (InvalidClassException) - all stored
     * folder databases (.PowerFolder/db, FileInfos embed their FolderInfo), the on-disk FolderInfo
     * files and any wire message from nodes still running the old value. The result is a full
     * rescan and loss of all file metadata (versions, modifiers, tags) on every folder.
     *
     * Adding a field is a serialization-COMPATIBLE change under the same UID: readObject defaults
     * it via GetField, older nodes simply ignore it (PFS-5306 kept 102 for the tags field for
     * exactly this reason). Wire-format evolution is versioned separately via extVersionUID.
     */
    private static final long serialVersionUID = 102L;
    private static final Internalizer<FolderInfo> INTERNALIZER = new FolderInfoInternalizer();

    public static final String PROPERTYNAME_ID = "id";
    public static final String PROPERTYNAME_NAME = "name";
    public static final String PROPERTYNAME_VERSION = "version";
    public static final String PROPERTYNAME_TOP_FOLDER = "topFolder";
    public static final String PROPERTYNAME_TOP_PATH = "topPath";
    public static final String PROPERTYNAME_INHERITS_PERMISSIONS = "inheritsPermissions";
    public static final String PROPERTYNAME_TAGS = "tags";

    @Index(name="IDX_FOLDER_NAME")
    private String name;
    @Id
    public String id;

    /**
     * PFC-3136: Version number of this folder
     */
    private int version;

    @ManyToOne
    @JoinColumn(name = "topFolderInfo_id")
    private FolderInfo topFolder;
    @Column(name = "topPath", length = 1024)
    private String topPath;

    /**
     * PFC-3543: Whether this (sub)folder inherits permissions from its top folder.
     * {@code true} (the default) means normal inheritance; {@code false} interrupts
     * it so that only explicit permissions set on this folder apply. Top folders
     * always inherit. Only honored when the interruption feature is enabled (see
     * {@link de.dal33t.powerfolder.Feature#FOLDER_PERMISSION_INHERITANCE_INTERRUPTION}).
     */
    @Column(name = "inheritsPermissions", nullable = false)
    private boolean inheritsPermissions = true;

    /**
     * PFS-5306: Tags of this folder as JSON array string (e.g. ["Projekt","2026"]),
     * same encoding as {@code FileInfo#tags}. {@code null} when untagged.
     */
    @Column(name = "tags", length = 2047)
    private String tags;

    /**
     * The cached hash info.
     */
    private transient int hash;

    private FolderInfo() {
        // NOP - for Hibernate
    }

    FolderInfo(String name, String id, int version, DirectoryInfo parent) {
        this(name, id, version, parent, null, true);
    }

    /**
     * PFS-5306: FolderInfo is immutable - tags and the inheritance flag are set at construction
     * time only. Changing them requires a new, version-bumped instance via
     * {@link FolderInfoFactory#changeTags(FolderInfo, String)} respectively
     * {@link FolderInfoFactory#changeInheritsPermissions(FolderInfo, boolean)}.
     *
     * @param tags                the tags as JSON array string, {@code null} when untagged
     * @param inheritsPermissions {@code false} to interrupt permission inheritance (PFC-3543)
     */
    FolderInfo(String name, String id, int version, DirectoryInfo parent, String tags,
        boolean inheritsPermissions)
    {
        this.name = name;
        this.id = id;
        this.version = version;
        this.tags = tags;
        this.inheritsPermissions = inheritsPermissions;
        setParent(parent);
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
     * @return the lookup {@link FolderInfo} of the CONTENT folder if this is a meta
     *         folder.
     */
    public FolderInfo lookupContentFolderInfo() {
        if (!isMetaFolder()) {
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
                "Unable to get content folder info for meta-folder: " + this, e);
            return this;
        }
    }

    /**
     * @return the meta-folder info for this folder
     */
    public FolderInfo getMetaFolderInfo() {
        if (isMetaFolder()) {
            return this;
        }
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

    private void setParent(DirectoryInfo parent) {
        if (parent != null) {
            Reject.ifNull(parent.getRelativeName(), "Parent relative name / path must not be null");
            this.topFolder = parent.getFolderInfo();
            this.topPath = parent.getRelativeName();
        } else {
            this.topFolder = null;
            this.topPath = null;
        }
    }

    /**
     * @return the parent under which the folder is located.
     * e.g. if the structure is "subdir/is/here/subfolder" this would return "subdir/is/here"
     */
    public DirectoryInfo getParent() {
        if (topFolder == null) {
            return null;
        }
        return FileInfoFactory.lookupDirectory(topFolder, topPath != null ? topPath : "");
    }

    public String getTopPath() {
        return topPath;
    }

    /**
     * @return the location under which the folder is located.
     * e.g. if the structure is "subdir/is/here/subfolder" this would return "subdir/is/here/subfolder"
     */
    public DirectoryInfo getLocation() {
        String path = locationPath();
        return path == null ? null : FileInfoFactory.lookupDirectory(topFolder, path);
    }

    /**
     * PFC-3543: The same location as {@link #getLocation()}, but as the plain path - no
     * {@link DirectoryInfo} is built. Whoever only compares paths must use this one: the barrier
     * resolution asks every interrupted subfolder of the system per call, and building a
     * DirectoryInfo per barrier and per call was the single most expensive thing in a scan of a
     * migrated server (visible in a thread dump as Pattern.match, from the message the FileInfo
     * constructor used to format eagerly).
     * <p>
     * Composed on every call - one string, no state: {@link FolderInfo} stays immutable.
     *
     * @return the location path in top-folder coordinates, or {@code null} for a top folder
     */
    public String locationPath() {
        if (topFolder == null) {
            return null;
        }
        return isNotBlank(topPath) ? topPath + '/' + name : name;
    }

    /**
     * PFC-3576: human-readable location for admin lists. For a subfolder this is the top folder's
     * localized name plus the relative location ("TopFolder/sub/path/leaf"), so admins can tell which
     * top folder a subfolder belongs to; for a top folder it is just the localized (leaf) name.
     */
    public String getDisplayPath() {
        if (!isSubFolder() || topFolder == null) {
            return getLocalizedName();
        }
        DirectoryInfo location = getLocation();
        String relative = location != null ? location.getRelativeName() : name;
        return topFolder.getLocalizedName() + "/" + relative;
    }

    public FolderInfo getTopFolder() {
        return topFolder;
    }

    public boolean isTopFolder() {
        return topFolder == null;
    }

    public boolean isSubFolder() {
        return topFolder != null;
    }

    public boolean inheritsPermissions() {
        // Top folders are always the root of inheritance. When the feature is
        // disabled the interruption is never honored, so the folder inherits.
        if (isTopFolder() || Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.isDisabled()) {
            return true;
        }
        return inheritsPermissions;
    }

    /**
     * PFS-5510: From the given candidates, the innermost subfolder that encloses the addressed
     * location ({@code folder} + {@code relativeName}), or {@code null} if none does. Works purely on
     * the {@link FolderInfo} structures (no mounting required): a candidate encloses the location if
     * it is a subfolder of the same top folder and its location path is a prefix of (or equal to) the
     * addressed path. Accepts a top folder or a subfolder as {@code folder} - a subfolder's path is
     * translated to top-folder coordinates first.
     *
     * PFC-3543: A subfolder whose permission inheritance is interrupted is a barrier the resolution does
     * not look past - it is considered even when it is not among {@code candidates}, which is the normal
     * case for an account that holds no permission on it. Those barriers come from
     * {@link InterruptedSubFolderIndex#barriers()}, so every caller honors the interruption without
     * having to know about it. With the feature disabled that lookup is a single volatile read of an
     * empty array and this method allocates nothing at all.
     *
     * @param candidates   subfolders to consider (e.g. an account's or group's permitted folders)
     * @param folder       the addressed folder (top folder or shared subfolder)
     * @param relativeName the addressed path relative to {@code folder}, may be blank
     * @return the innermost enclosing subfolder from {@code candidates}, or {@code null}
     */
    /**
     * @param folders the folders to map, may be null or empty
     * @return their IDs in iteration order, as the DAOs that take an ID array expect them, never null
     */
    public static String[] ids(Collection<FolderInfo> folders) {
        if (folders == null || folders.isEmpty()) {
            return new String[0];
        }
        String[] ids = new String[folders.size()];
        int i = 0;
        for (FolderInfo foInfo : folders) {
            if (i == ids.length) {
                break;
            }
            ids[i++] = foInfo != null ? foInfo.id : null;
        }
        return ids;
    }

    public static FolderInfo findEnclosingSubFolder(Collection<FolderInfo> candidates, FolderInfo folder,
                                                    String relativeName) {
        return findEnclosingSubFolder(candidates, InterruptedSubFolderIndex.barriersOf(topOf(folder)),
            folder, relativeName);
    }

    /**
     * PFC-3543: Like {@link #findEnclosingSubFolder(Collection, FolderInfo, String)}, but with an
     * explicitly supplied set of barriers instead of the currently interrupted subfolders. Only for
     * tests that need a deterministic barrier set - production code uses the three-argument variant,
     * which looks the barriers up itself.
     * <p>
     * The innermost enclosing subfolder of BOTH sets wins, so no further arbitration is needed: a
     * barrier beats a permitted subfolder further up (access is decoupled there), and a permitted
     * subfolder below a barrier beats the barrier (access was granted past it). A tie - the same
     * subfolder in both sets - goes to {@code candidates}, so the instance carried by the permission is
     * returned.
     *
     * @param candidates   subfolders to consider (e.g. an account's or group's permitted folders)
     * @param barriers     the subfolders acting as barriers, may be {@code null} or empty. NOT modified
     * @param folder       the addressed folder (top folder or shared subfolder)
     * @param relativeName the addressed path relative to {@code folder}, may be blank
     * @return the innermost enclosing subfolder of either set, or {@code null}
     */
    static FolderInfo findEnclosingSubFolder(Collection<FolderInfo> candidates, FolderInfo[] barriers,
                                             FolderInfo folder, String relativeName) {
        if (folder == null) {
            return null;
        }
        boolean hasBarriers = barriers != null && barriers.length > 0;
        if (candidates == null && !hasBarriers) {
            return null;
        }
        String path = addressedPath(folder, relativeName);
        if (path == null) {
            return null;
        }
        FolderInfo top = topOf(folder);
        FolderInfo innermost = findInnermostEnclosing(candidates, top, path, null);
        // Barriers only win on a strictly deeper match, so a tie goes to the candidates.
        return hasBarriers ? findInnermostEnclosing(barriers, top, path, innermost) : innermost;
    }

    /**
     * PFC-3543: The innermost subfolder with INTERRUPTED permission inheritance that encloses the
     * addressed location, or {@code null} if none does.
     * <p>
     * Such a subfolder keeps its content in its OWN database (PFC-3565) - the rows were migrated out of
     * the top folder when the inheritance was interrupted. Anything that READS content for an addressed
     * path therefore has to go through it instead of through the top folder, no matter how the caller
     * reached the path. Use {@link #relativeNameIn(FolderInfo, FolderInfo, String)} to translate the
     * addressed path into that subfolder's coordinates.
     * <p>
     * Allocation-free when nothing is interrupted, which is the common case.
     *
     * @param folder       the addressed folder (top folder or shared subfolder)
     * @param relativeName the addressed path relative to {@code folder}, may be blank
     * @return the innermost enclosing interrupted subfolder, or {@code null}
     */
    public static FolderInfo findEnclosingInterruptedSubFolder(FolderInfo folder, String relativeName) {
        FolderInfo[] barriers = InterruptedSubFolderIndex.barriersOf(topOf(folder));
        return barriers.length == 0 ? null : findEnclosingSubFolder(null, barriers, folder, relativeName);
    }

    /**
     * PFC-3543: The addressed location expressed relative to one of its enclosing subfolders - the path
     * to use when reading from that subfolder instead of from {@code folder}.
     *
     * @param subFolder    an enclosing subfolder, e.g. from
     *                     {@link #findEnclosingInterruptedSubFolder(FolderInfo, String)}
     * @param folder       the addressed folder (top folder or shared subfolder)
     * @param relativeName the addressed path relative to {@code folder}, may be blank
     * @return the path relative to {@code subFolder}, blank when it IS the addressed location, or
     *         {@code null} if {@code subFolder} does not enclose the location
     */
    public static String relativeNameIn(FolderInfo subFolder, FolderInfo folder, String relativeName) {
        if (subFolder == null || folder == null || !subFolder.isSubFolder()) {
            return null;
        }
        String base = subFolder.locationPath();
        String path = addressedPath(folder, relativeName);
        if (base == null || path == null) {
            return null;
        }
        if (path.equals(base)) {
            return "";
        }
        return path.startsWith(base + "/") ? path.substring(base.length() + 1) : null;
    }

    /** The top folder an addressed folder belongs to - itself when it is one. {@code null} stays null. */
    private static FolderInfo topOf(FolderInfo folder) {
        if (folder == null) {
            return null;
        }
        return folder.isSubFolder() ? folder.getTopFolder() : folder;
    }

    /**
     * The addressed path in TOP FOLDER coordinates: a subfolder's own location is prepended, so all
     * comparisons happen in one coordinate system.
     *
     * @return the path, or {@code null} if a subfolder does not know its location
     */
    private static String addressedPath(FolderInfo folder, String relativeName) {
        String path = relativeName != null ? relativeName : "";
        if (!folder.isSubFolder()) {
            return path;
        }
        String base = folder.locationPath();
        if (base == null) {
            return null;
        }
        return path.isEmpty() ? base : base + "/" + path;
    }

    /**
     * The innermost subfolder of {@code candidates} that encloses {@code path}, or {@code current} if
     * none is deeper than it.
     *
     * @param candidates the subfolders to consider, may be {@code null} or empty
     * @param top        the top folder the path is relative to
     * @param path       the addressed path in top-folder coordinates
     * @param current    the best match so far, may be {@code null}
     */
    private static FolderInfo findInnermostEnclosing(Collection<FolderInfo> candidates, FolderInfo top, String path,
                                                     FolderInfo current) {
        if (candidates == null || candidates.isEmpty()) {
            return current;
        }
        FolderInfo innermost = current;
        int innermostLength = enclosingPathLength(current, top, path);
        for (FolderInfo candidate : candidates) {
            int length = enclosingPathLength(candidate, top, path);
            if (length > innermostLength) {
                innermost = candidate;
                innermostLength = length;
            }
        }
        return innermost;
    }

    /**
     * Array variant of {@link #findInnermostEnclosing(Collection, FolderInfo, String, FolderInfo)} for
     * the barrier snapshot: an indexed loop, so the resolution allocates nothing - not even an iterator -
     * on the web hot path.
     */
    private static FolderInfo findInnermostEnclosing(FolderInfo[] candidates, FolderInfo top, String path,
                                                     FolderInfo current) {
        FolderInfo innermost = current;
        int innermostLength = enclosingPathLength(current, top, path);
        for (int i = 0; i < candidates.length; i++) {
            int length = enclosingPathLength(candidates[i], top, path);
            if (length > innermostLength) {
                innermost = candidates[i];
                innermostLength = length;
            }
        }
        return innermost;
    }

    /**
     * How deep the given subfolder sits if it encloses {@code path}, measured as the length of its
     * location path so a deeper match always compares greater. Matching is segment-exact, so a sibling
     * whose name is a string prefix (e.g. "documents" vs "doc") never matches.
     *
     * @param candidate the subfolder to check, may be {@code null} or a top folder
     * @param top       the top folder the path is relative to
     * @param path      the addressed path in top-folder coordinates
     * @return the location path length, or {@code -1} if the candidate does not enclose the path
     */
    private static int enclosingPathLength(FolderInfo candidate, FolderInfo top, String path) {
        if (candidate == null || !candidate.isSubFolder() || !top.equals(candidate.getTopFolder())) {
            return -1;
        }
        String candidatePath = candidate.locationPath();
        if (candidatePath == null) {
            return -1;
        }
        if (path.equals(candidatePath) || path.startsWith(candidatePath + "/")) {
            return candidatePath.length();
        }
        return -1;
    }

    /**
     * The stored inheritance flag, ignoring the feature gate and top-folder rule.
     * Package-private accessor for {@link FolderInfoFactory} (no-op detection).
     */
    boolean storedInheritsPermissions() {
        return inheritsPermissions;
    }

    /**
     * PFS-5306: the tags as raw JSON array string, {@code null} when untagged.
     */
    public String getTags() {
        return tags;
    }

    /**
     * PFS-5306: the parsed tags. Never {@code null}, empty when untagged.
     */
    public List<String> getTagsList() {
        return TagUtil.parse(tags);
    }

    /**
     * The stored raw tags value, {@code null} when untagged.
     * Package-private accessor for {@link FolderInfoFactory} (no-op detection).
     */
    String storedTags() {
        return tags;
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
        String prefix = "";
        if (topFolder != null) {
            prefix += "(" + topFolder.name;
            if (isNotBlank(topPath)) {
                prefix += "/" + topPath;
            }
            prefix += ")/";
        }
        return (isSubFolder() ? "Sub" : "") + "Folder " + prefix + name + '/' + id + '/' + version;
    }

    // Serialization optimization *********************************************

    // PFC-3543: bumped 101 -> 102 to carry the inheritsPermissions flag.
    // PFS-5306: bumped 102 -> 103 to carry the tags.
    // Protocol 100 = id+name, 101 = +version+parent, 102 = +inheritsPermissions, 103 = +tags.
    private static final long extVersionUID = 103L;

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
        if (extUID != 100L && extUID != 101L && extUID != 102L && extUID != extVersionUID) {
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
            DirectoryInfo parent = (DirectoryInfo) FileInfoFactory.readExt(in);
            setParent(parent);
        } else {
            setParent(null);
        }
        // PFC-3543: protocol 102+ carries the inheritsPermissions flag; older
        // streams (100/101) default to inheriting.
        if (extUID >= 102L) {
            inheritsPermissions = in.readBoolean();
        }
        // PFS-5306: protocol 103+ carries the tags; older streams have none.
        if (extUID >= 103L) {
            if (in.readBoolean()) {
                tags = in.readUTF();
            }
        }
        // LOG.log(Level.INFO,this + ": readExternal " + extUID, new StackDump());
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        writeExternal(out, true);
    }

    public void writeExternal(ObjectOutput out, boolean includeVersionAndParent) throws IOException {
        // PFC-3543: standalone callers that do not negotiate the inheritsPermissions
        // protocol must not emit it (safe default: the peer treats the folder as
        // inheriting). Only FolderListExt talking to a >= 115 peer passes true.
        writeExternal(out, includeVersionAndParent, false);
    }

    public void writeExternal(ObjectOutput out, boolean includeVersionAndParent,
        boolean includeInheritsPermissions) throws IOException {
        // PFS-5306: standalone callers that do not negotiate the tags protocol must
        // not emit them (safe default: the peer sees the folder as untagged). Only
        // FolderListExt talking to a >= 116 peer passes true.
        writeExternal(out, includeVersionAndParent, includeInheritsPermissions, false);
    }

    public void writeExternal(ObjectOutput out, boolean includeVersionAndParent,
        boolean includeInheritsPermissions, boolean includeTags) throws IOException {

        // Pick the highest FolderInfo protocol version we may write. We escalate
        // only as far as (a) this folder actually needs and (b) the peer negotiated,
        // so older peers keep receiving a format they understand:
        //   100 = id + name
        //   101 = + version + parent folder (subfolder support)
        //   102 = + inheritsPermissions flag (PFC-3543)
        //   103 = + tags (PFS-5306)
        boolean writeVersionAndParent = includeVersionAndParent
            && (version > 0 || topFolder != null);
        boolean writeInheritsPermissions = writeVersionAndParent
            && includeInheritsPermissions
            && Feature.FOLDER_PERMISSION_INHERITANCE_INTERRUPTION.isEnabled()
            && !inheritsPermissions;
        boolean writeTags = writeVersionAndParent && includeTags && tags != null;

        long protocolVersion = 100L;
        if (writeTags) {
            protocolVersion = extVersionUID; // 103
        } else if (writeInheritsPermissions) {
            protocolVersion = 102L;
        } else if (writeVersionAndParent) {
            protocolVersion = 101L;
        }

        // Version 100: id + name (always written)
        out.writeLong(protocolVersion);
        out.writeUTF(id);
        out.writeUTF(name);
        if (protocolVersion == 100L) {
            return;
        }

        // Version 101: version + parent folder
        out.writeInt(version);
        if (topPath != null) {
            out.writeBoolean(true);
            getParent().writeExternal(out);
        } else {
            out.writeBoolean(false);
        }

        // Version 102: inheritsPermissions flag. Must be written whenever the
        // stream announces >= 102, even if only the tags forced the escalation.
        if (protocolVersion >= 102L) {
            out.writeBoolean(inheritsPermissions);
        }

        // Version 103: tags (PFS-5306), boolean-prefixed.
        if (protocolVersion >= 103L) {
            out.writeBoolean(tags != null);
            if (tags != null) {
                out.writeUTF(tags);
            }
        }
    }

    /**
     * PFC-3543: Custom deserialization so that instances written before the
     * "inheritsPermissions" field existed default to inheriting ({@code true})
     * instead of the boolean default ({@code false}). Keep in sync with the
     * serialized instance fields if new fields are added.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField serialFields = in.readFields();
        name = (String) serialFields.get(PROPERTYNAME_NAME, null);
        id = (String) serialFields.get(PROPERTYNAME_ID, null);
        version = serialFields.get(PROPERTYNAME_VERSION, 0);
        topFolder = (FolderInfo) serialFields.get(PROPERTYNAME_TOP_FOLDER, null);
        topPath = (String) serialFields.get(PROPERTYNAME_TOP_PATH, null);
        inheritsPermissions = serialFields.get(PROPERTYNAME_INHERITS_PERMISSIONS, true);
        // PFS-5306: absent in old streams -> untagged.
        tags = (String) serialFields.get(PROPERTYNAME_TAGS, null);
        hash = hashCode0();
    }

    public String getLocalizedName() {
        if (name == null) {
            return null;
        }
        if (name.contains(Constants.FOLDER_PERSONAL_FILES)
                || name.contains(Constants.MAIL_ATTACHMENT_FOLDER)) {
            return name
                    .replace(Constants.FOLDER_PERSONAL_FILES,
                            Translation.get("general.personal_files"))
                    .replace(Constants.MAIL_ATTACHMENT_FOLDER,
                            Translation.get("mail_attachment_folder_name"));
        }
        return name;
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
          // TODO PFC-3543: once powerfolder-protobuf-*.jar is regenerated from the
          // updated FolderInfoProto.proto (field 4), read the flag here. The proto
          // field is the inverted "interruptInheritance" (proto3 bool defaults to
          // false = inherits), so bridge it back:
          // this.inheritsPermissions = !finfo.getInterruptInheritance();
          // TODO PFS-5306: once powerfolder-protobuf-*.jar is regenerated from the
          // updated FolderInfoProto.proto (field 5), read the tags here. proto3
          // string defaults to "" (= untagged), so bridge it back to null:
          // this.tags = finfo.getTags().isEmpty() ? null : finfo.getTags();
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
      // TODO PFC-3543: once powerfolder-protobuf-*.jar is regenerated from the
      // updated FolderInfoProto.proto (field 4), write the flag here (inverted,
      // so proto3 default false = inherits):
      // builder.setInterruptInheritance(!this.inheritsPermissions);
      // TODO PFS-5306: once powerfolder-protobuf-*.jar is regenerated from the
      // updated FolderInfoProto.proto (field 5), write the tags here (proto3
      // string default "" = untagged):
      // if (this.tags != null) { builder.setTags(this.tags); }

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
            // Most frequent way a FolderInfo fails to persist, and it used to return silently -
            // indistinguishable from a successful save. Folder.updateInfo reports the consequence.
            LOG.fine(this + ": Unable to store FolderInfo, directory does not exist: " + file.getParent());
            return false;
        }
        try (ObjectOutputStream oout = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(file)))) {
            oout.writeObject(this);
        } catch (Exception e) {
            LOG.warning(this + ": Unable to store FolderInfo to " + file + ". " + e);
            return false;
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
            return (FolderInfo) oin.readObject();
        } catch (Exception e) {
            LOG.warning("Unable to read FolderInfo from " + file + ". " + e);
        }
        return null;
    }
}

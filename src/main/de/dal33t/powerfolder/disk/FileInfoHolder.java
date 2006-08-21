package de.dal33t.powerfolder.disk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;

/**
 * Holds a FileInfo for each Member of the Folder
 * 
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 */
public class FileInfoHolder {
    private FileInfo fileInfo;
    private Folder folder;
    boolean fileInfoIsMyOwn;
    /** For each member the fileInfo. key = member, value = FileInfo */
    private Map<Member, FileInfo> memberHasFileInfoMap;

    /**
     * the availability of this file (number of users that have the highest not
     * deleted version of this file
     */
    private int availability;

    /**
     * creates a FileInfoHolder and reads the fileData from the FileInfo and
     * addes the first relationship between Member and FileInfo (member has
     * file)
     */
    public FileInfoHolder(Folder folder, Member member, FileInfo fileInfo) {
        this.fileInfo = fileInfo;
        this.folder = folder;
        fileInfoIsMyOwn = member.isMySelf();

        memberHasFileInfoMap = Collections
            .synchronizedMap(new HashMap<Member, FileInfo>(2));
        memberHasFileInfoMap.put(member, fileInfo);
        availability = 1;
    }

    /** returns the FileInfo-mation about the file at this member */
    public FileInfo getFileInfo(Member member) {
        if (memberHasFileInfoMap.containsKey(member)) {
            return memberHasFileInfoMap.get(member);
        }
        throw new IllegalArgumentException("not has file " + member);
    }

    /**
     * removes file for this member if there
     * 
     * @return true if empty as result of removal
     */
    public boolean removeFileOfMember(Member member) {
        if (memberHasFileInfoMap.containsKey(member)) {
            memberHasFileInfoMap.remove(member);
        }
        return memberHasFileInfoMap.isEmpty();
    }

    /**
     * returns true it this members has this file and the file is not remotely
     * deleted.
     */
    public boolean hasFile(Member member) {
        if (memberHasFileInfoMap.containsKey(member)) {
            FileInfo fileInfo = memberHasFileInfoMap.get(member);
            return !fileInfo.isDeleted();
        }
        return false;
    }

    /** used to replace in converted to meta FileInfo (Mp3/Image) */
    void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public void put(Member member, FileInfo fileInfo) {
        memberHasFileInfoMap.put(member, fileInfo);
        if (fileInfoIsMyOwn) { // do not overwite myself
            calcAvailability();
            return;
        }
        if (member.isMySelf()) { // use myself as info
            this.fileInfo = fileInfo;
            fileInfoIsMyOwn = true;
            calcAvailability();
            return;
        }
        if (!fileInfo.completelyIdentical(this.fileInfo)) {
            if (fileInfo.getVersion() > this.fileInfo.getVersion()) {
                this.fileInfo = fileInfo;
            } else {
                if (fileInfo.getSize() != this.fileInfo.getSize()
                    || !fileInfo.getModifiedBy().equals(
                        this.fileInfo.getModifiedBy())
                    || fileInfo.getModifiedDate().equals(
                        this.fileInfo.getModifiedDate()))
                {
                    // versions equal but filesize or modifier/date has
                    // changed! use the most recent one
                    if (fileInfo.isNewerThan(this.fileInfo)) {
                        this.fileInfo = fileInfo;
                    }
                }
            }
        }
        calcAvailability();
    }

    /**
     * @return Returns the filename.
     */
    public String getFilename() {
        return fileInfo.getFilenameOnly();
    }

    /**
     * @return Returns the folder.
     */
    public Folder getFolder() {
        return folder;
    }

    /**
     * @return Returns the path (== LocationInFolder).
     */
    public String getPath() {
        return fileInfo.getLocationInFolder();
    }

    private void calcAvailability() {
        Iterator<FileInfo> fileInfos = memberHasFileInfoMap.values().iterator();
        int tmpAvailability = 0;
        int newestVersion = getNewestVersion();
        while (fileInfos.hasNext()) {
            FileInfo fileInfo = fileInfos.next();
            if (newestVersion == fileInfo.getVersion() && !fileInfo.isDeleted())
            {
                tmpAvailability++;
            }
        }
        availability = tmpAvailability;
    }

    private int getNewestVersion() {
        Iterator<FileInfo> fileInfos = memberHasFileInfoMap.values().iterator();
        int tmpHighestVersion = -1;
        while (fileInfos.hasNext()) {
            FileInfo fileInfo = fileInfos.next();
            // TODO SCHAATSER Check: Is this correct? maybe the newest version
            // IS the deleted one? Scenario:
            // initalfile(ver:0)->modified(ver:1)->deleted by
            // user(ver:2). The latest, deleted version of the file is the
            // newest version of the file.

            // Schaatser: Maybe the name of the method is wrong. It is used to
            // calcAvailability() of the file. And since deleted files are not
            // available the result of this method is what we want. So
            // the name should be "getNewestAvailableVersion" ?
            if (fileInfo.isDeleted()) {
                continue;
            }
            tmpHighestVersion = Math.max(tmpHighestVersion, fileInfo
                .getVersion());
        }
        return tmpHighestVersion;
    }

    /** returns the number of complete files of the latest version in the network */
    public int getAvailability() {
        return availability;
    }

    /**
     * returns a list of Members that have the file
     */
    public List<Member> getSources() {
        int newestVersion = getNewestVersion();
        Iterator<Member> members = memberHasFileInfoMap.keySet().iterator();
        List<Member> sources = new ArrayList<Member>();
        while (members.hasNext()) {
            Member member = members.next();
            FileInfo fileInfo = memberHasFileInfoMap.get(member);
            if (fileInfo.getVersion() == newestVersion && !fileInfo.isDeleted())
            {
                sources.add(member);
            }
        }
        return sources;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    /**
     * valid if at least one connected member has a not deleted version or
     * member with deleted version is myself
     */
    public boolean isValid() {
        Iterator members = memberHasFileInfoMap.keySet().iterator();
        while (members.hasNext()) {
            Member member = (Member) members.next();
            if (member.isConnected() || member.isMySelf()) {
                FileInfo fileInfo = memberHasFileInfoMap.get(member);
                if (fileInfo.isDeleted()) {
                    if (member.isMySelf()) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }
}

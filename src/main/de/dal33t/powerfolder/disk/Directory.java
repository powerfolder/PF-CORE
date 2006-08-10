package de.dal33t.powerfolder.disk;

import java.io.File;
import java.util.*;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.util.BuildStrings;
import de.dal33t.powerfolder.util.FileCopier;
import de.dal33t.powerfolder.util.Translation;

/**
 * Represents a directory of files. No actual disk access from this file, build
 * from list of FileInfos Holds the SubDirectories (may contain Files and
 * Subdirectories themselfs) and Files (FileInfos)
 * 
 * @author <a href="mailto:schaatser@powerfolder.com">Jan van Oosterom </a>
 * @version $Revision: 1.43 $
 */
public class Directory implements Comparable, MutableTreeNode {
    /**
     * The files (FileInfoHolder s) in this Directory key = fileInfo value =
     * FileInfoHolder
     */
    private Map<FileInfo, FileInfoHolder> fileInfoHolderMap = new HashMap<FileInfo, FileInfoHolder>();
    /** key = dir name, value = Directory* */
    private Map<String, Directory> subDirectoriesMap = Collections
        .synchronizedMap(new HashMap<String, Directory>());
    /**
     * the path to this directory (including its name, excluding the localbase
     * (see Folder)
     */
    private String path;
    /** the name of this directory */
    private String name;
    /** The root Folder which this Directory belongs to */
    private Folder rootFolder;
    /** The parent Directory (may be null, if no parent!) */
    private Directory parent;
    /** The TreeNode that displayes this Directory in the Tree */
    //private DefaultMutableTreeNode treeNode;

    /**
     * @param name
     *            The name of this folder
     * @param path
     *            The path to this folder
     */
    public Directory(Directory parent, String name, String path, Folder root) {
        this.parent = parent;
        this.name = name;
        this.path = path;
        this.rootFolder = root;
    }

    public boolean isBlackListed() {
        return rootFolder.isInBlacklist(getFilesRecursive());
    }

    /** returns a File object to the diretory in the filesystem */
    public File getFile() {
        return new File(rootFolder.getLocalBase(), path);
    }

    /** returns the Directory with this name, creates it if not exists yet */
    public Directory getCreateSubDirectory(String name) {
        if (subDirectoriesMap.containsKey(name)) {
            return subDirectoriesMap.get(name);
        }
        Directory sub = new Directory(this, name, path + "/" + name, rootFolder);

        final File newFileName = new File(getFile(), name);
        if (!newFileName.exists()) {
            newFileName.mkdir();
        }
        return sub;
    }

    public boolean hasParent() {
        return parent != null;
    }

    public Directory getParentDirectory() {
        return parent;
    }

    /** notify this Directory that a file is added */
    public void add(File file) {
        FileInfo fileInfo = new FileInfo(rootFolder, file);
        rootFolder.scanNewFile(fileInfo);
    }

    /** copy a file from this source to this Directory */
    public boolean copyFileFrom(final File file, final FileCopier fileCopier) {
        if (file.exists() && file.canRead()) {
            final File newFile = new File(getFile(), file.getName());
            if (file.equals(newFile)) {
                //cannot copy file onto itself
                return false;
            }
            fileCopier.add(file, newFile, this);

            if (!fileCopier.isStarted()) {
                Runnable runner = new Runnable() {
                    public void run() {
                        fileCopier.start();
                    }
                };
                Thread thread = new Thread(runner);
                thread.start();
                // wait for start else more are started if more files are
                // added using this method
                while (!fileCopier.isStarted()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {

                    }
                }
            }
            return true;
        }
        return false;

    }

    public boolean removeFilesOfMember(Member member) {
        boolean removed = false;
        synchronized (fileInfoHolderMap) {
            Iterator fileInfoHolders = fileInfoHolderMap.values().iterator();
            List toRemove = new LinkedList();
            while (fileInfoHolders.hasNext()) {
                FileInfoHolder holder = (FileInfoHolder) fileInfoHolders.next();
                boolean empty = holder.removeFileOfMember(member);
                if (empty) {
                    toRemove.add(holder.getFileInfo());
                }
            }
            removed = toRemove.size() > 0;
            for (int i = 0; i < toRemove.size(); i++) {
                fileInfoHolderMap.remove(toRemove);
            }
        }

        synchronized (subDirectoriesMap) {
            Set<String> dirnames = subDirectoriesMap.keySet();
            for (Iterator it = dirnames.iterator(); it.hasNext();) {
                Directory dir = subDirectoriesMap.get(it.next());
                boolean dirRemoved = dir.removeFilesOfMember(member);
                removed = removed || dirRemoved;
            }
        }
        return removed;
    }

    /**  */
    public FileInfoHolder getFileInfoHolder(FileInfo fileInfo) {
        if (fileInfoHolderMap.containsKey(fileInfo)) {
            return fileInfoHolderMap.get(fileInfo);
        }
        String path = fileInfo.getLocationInFolder();
        String dirName;
        String rest;
        int index = path.indexOf("/");
        if (index == -1) {
            dirName = path;
            rest = "";
        } else {
            dirName = path.substring(0, index);
            rest = path.substring(index + 1, path.length());
        }
        if (dirName.length() == 0) {
            return null;
        }
        if (subDirectoriesMap.containsKey(dirName)) {
            Directory dir = subDirectoriesMap.get(dirName);
            if (rest.equals("")) {
                return dir.getFileInfoHolder(fileInfo);
            }
            return dir.getFileInfoHolder(fileInfo, rest);
        }
        return null; // should not happen but this saves a crash (white
        // screen)
        // throw new IllegalStateException("dir not found: " + dirName + " | "
        // + fileInfo.getName());
    }

    private FileInfoHolder getFileInfoHolder(FileInfo fileInfo, String restPath)
    {
        String dirName;
        String rest;
        int index = restPath.indexOf("/");
        if (index == -1) {
            dirName = restPath;
            rest = "";
        } else {
            dirName = restPath.substring(0, index);
            rest = restPath.substring(index + 1, restPath.length());
        }
        if (subDirectoriesMap.containsKey(dirName)) {
            Directory dir = subDirectoriesMap.get(dirName);
            if (rest.equals("")) {
                return dir.getFileInfoHolder(fileInfo);
            }
            return dir.getFileInfoHolder(fileInfo, rest);
        }
        throw new IllegalStateException("dir not found: " + dirName + " | "
            + fileInfo.getName());
    }

    /** */
    public Directory getSubDirectory(String dirName) {        
        String tmpDirName;
        String rest;
        int index = dirName.indexOf("/");
        if (index == -1) {
            tmpDirName = dirName;
            rest = "";
        } else {
            tmpDirName = dirName.substring(0, index);
            rest = dirName.substring(index + 1, dirName.length());
        }
        if (subDirectoriesMap.containsKey(tmpDirName)) {
            Directory dir = subDirectoriesMap.get(tmpDirName);
            if (rest.equals("")) {
                return dir;
            } 
            return dir.getSubDirectory(rest);
        } 
        throw new IllegalStateException("dir " +dirName + " not found");        
    }
    
    
    
    /**
     * get the files in this dir (not the files in the subs)
     * 
     * @see #getFilesRecursive()
     */
    public List<FileInfo> getFiles() {
        List<FileInfo> files = Collections.synchronizedList(new ArrayList());
        Iterator<FileInfo> fileInfos = fileInfoHolderMap.keySet().iterator();
        while (fileInfos.hasNext()) {
            FileInfo fileInfo = fileInfos.next();
            if (fileInfo.diskFileExists(rootFolder.getController())) {
                files.add(fileInfo);
            }
        }
        return files;
    }

    /**
     * returns only valid files. (valid if at least one member has a not deleted
     * version or member with deleted version is myself)
     */
    public List<FileInfo> getFilesRecursive() {
        List files = Collections.synchronizedList(new ArrayList());
        Iterator<FileInfoHolder> fileInfoHolders = fileInfoHolderMap.values()
            .iterator();
        while (fileInfoHolders.hasNext()) {
            FileInfoHolder holder = fileInfoHolders.next();
            if (holder.isValid()) {
                files.add(holder.getFileInfo());
            }
        }
        Iterator<Directory> subs = subDirectoriesMap.values().iterator();
        while (subs.hasNext()) {
            Directory subDir = subs.next();
            files.addAll(subDir.getFilesRecursive());
        }
        return files;
    }

    /**
     * @return the list of subdirectories in this directory
     */
    public List<Directory> listSubDirectories() {
        List list = new ArrayList(subDirectoriesMap.values());
        Collections.sort(list);
        return list;
    }

    public int countSubDirectories() {
        return subDirectoriesMap.size();
    }

    /**
     * @return true if this is the same object or the paths are equal
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (other instanceof Directory) {
            Directory otherDirectory = (Directory) other;
            if (rootFolder != null) {
                if (rootFolder != otherDirectory.rootFolder) {
                    return false;
                }
            }
            return otherDirectory.path.equals(path);
        }
        return false;
    }

    /** used for sorting, ignores case * */
    public int compareTo(Object other) {
        if (!(other instanceof Directory)) {
            return -1;
        }
        if (other == this) {
            return 0;
        }
        return path.compareToIgnoreCase(((Directory) other).path);
    }

    /** check if file with this name already exists in this Directory */
    public boolean contains(File file) {
        String name = file.getName();
        for (FileInfo fileInfo : fileInfoHolderMap.keySet()) {
            if (name.equals(fileInfo.getFilenameOnly())) {
                return true;
            }
        }
        return false;
    }

    /** does this Direcory allready has this exact file on disk */
    public boolean alreadyHasFileOnDisk(File file) {
        File toCheck = new File(getFile(), file.getName());
        return toCheck.exists();
    }
    
    /**
     * Adds a FileInfo to this Directory
     * 
     * @param fileInfo
     *            the file to add to this Directory
     */
    private void addFile(Member member, FileInfo fileInfo) {
        if (fileInfoHolderMap.containsKey(fileInfo)) { // already there
            FileInfoHolder fileInfoHolder = fileInfoHolderMap.get(fileInfo);
            fileInfoHolder.put(member, fileInfo);
        } else { // new
            FileInfoHolder fileInfoHolder = new FileInfoHolder(rootFolder,
                member, fileInfo);
            fileInfoHolderMap.put(fileInfo, fileInfoHolder);
        }
    }

    public void addAll(Member member, FileInfo[] fileInfos) {
        for (int i = 0; i < fileInfos.length; i++) {
            add(member, fileInfos[i]);
        }
    }

    /**
     * Answers if all files in this dir and in subdirs are expected.
     */
    public boolean isExpected(FolderRepository folderRepository) {
        Iterator fileInfoHolders = fileInfoHolderMap.values().iterator();
        while (fileInfoHolders.hasNext()) {
            if (!((FileInfoHolder) fileInfoHolders.next()).getFileInfo()
                .isExpected(folderRepository))
            {
                return false;
            }
        }
        synchronized (subDirectoriesMap) {
            Iterator it = subDirectoriesMap.values().iterator();
            while (it.hasNext()) {
                Directory dir = (Directory) it.next();
                if (!dir.isExpected(folderRepository)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @return true if all filesInfos and filesInfos in subdirectories are
     *         deleted
     */
    public boolean isDeleted() {
        if (fileInfoHolderMap.isEmpty() && subDirectoriesMap.isEmpty()) {
            return false;
        }
        Iterator fileInfoHolders = fileInfoHolderMap.values().iterator();
        while (fileInfoHolders.hasNext()) {
            if (!((FileInfoHolder) fileInfoHolders.next()).getFileInfo()
                .isDeleted())
            {
                return false; // one file not deleted
            }
        }
        synchronized (subDirectoriesMap) {
            Iterator it = subDirectoriesMap.values().iterator();
            while (it.hasNext()) {
                Directory dir = (Directory) it.next();
                if (!dir.isDeleted()) {
                    return false;
                }
            }
        }
        return true; // this dir is deleted
    }

    /** returns a list of all valid FileInfo s, so not the remotely deleted */
    public List<FileInfo> getValidFiles() {
        List<FileInfo> files = Collections
            .synchronizedList(new ArrayList<FileInfo>());
        Iterator<FileInfoHolder> fileInfoHolders = fileInfoHolderMap.values()
            .iterator();
        while (fileInfoHolders.hasNext()) {
            FileInfoHolder holder = fileInfoHolders.next();
            if (holder.isValid()) {
                files.add(holder.getFileInfo());
            }
        }
        return files;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }

    /**
     * @return Returns the rootFolder.
     */
    public Folder getRootFolder() {
        return rootFolder;
    }

    public void copyListsFrom(Directory other) {
        fileInfoHolderMap = other.fileInfoHolderMap;
        subDirectoriesMap = other.subDirectoriesMap;
    }

    /**
     * @return Returns the path.
     */
    public String getPath() {
        return path;
    }

    /**
     * helper code to fill build the Directory with FileInfos in the correct sub
     * Directories
     * 
     * @param listOfFiles
     *            The files to add to this Diretory
     * @param folder
     *            The Folder that holds this directory
     */
    static Directory buildDirsRecursive(Member member, FileInfo[] listOfFiles,
        Folder folder)
    {
        Directory root = new Directory(null, Translation
            .getTranslation("general.files"), "", folder);
        for (int i = 0; i < listOfFiles.length; i++) {
            root.add(member, listOfFiles[i]);
        }
        return root;
    }

    /** add a file recursive to this or correct sub Directory */
    void add(Member member, FileInfo file) {
        String path = file.getLocationInFolder();
        if (path.equals("")) {
            addFile(member, file);
        } else {
            String dirName;
            String rest;
            int index = path.indexOf("/");
            if (index == -1) {
                dirName = path;
                rest = "";
            } else {
                dirName = path.substring(0, index);
                rest = path.substring(index + 1, path.length());
            }
            if (subDirectoriesMap.containsKey(dirName)) {
                Directory dir = subDirectoriesMap.get(dirName);
                dir.add(member, file, rest);
                // TODO fire change ?
            } else {
                Directory dir = new Directory(this, dirName, dirName,
                    rootFolder);
                subDirectoriesMap.put(dirName, dir);
                dir.add(member, file, rest);
                // TODO fire change ?
            }
        }
    }

    private void add(Member member, FileInfo file, String restPath) {
        if (restPath.equals("")) {
            addFile(member, file);
        } else {
            String dirName;
            String rest;
            int index = restPath.indexOf("/");
            if (index == -1) {
                dirName = restPath;
                rest = "";
            } else {
                dirName = restPath.substring(0, index);
                rest = restPath.substring(index + 1, restPath.length());
            }
            if (subDirectoriesMap.containsKey(dirName)) {
                Directory dir = subDirectoriesMap.get(dirName);
                dir.add(member, file, rest);
                // TODO fire Change?
            } else {
                Directory dir = new Directory(this, dirName, path + "/"
                    + dirName, rootFolder);
                subDirectoriesMap.put(dirName, dir);
                dir.add(member, file, rest);
                // TODO fire change ?
            }
        }
    }

    public String toAscii() {
        BuildStrings str = new BuildStrings();
        str.append(rootFolder.getName());
        str.append("\n");
        return toAscii(str, 0).toString();
    }

    private BuildStrings toAscii(BuildStrings str, int depth) {
        int newdepth = depth + 1;
        String tabs = getTabs(depth);
        Iterator it = subDirectoriesMap.values().iterator();
        while (it.hasNext()) {
            str.append(tabs);
            str.append("<DIR>");
            Directory sub = (Directory) it.next();
            str.append(sub.name);            
            str.append("\n");
            sub.toAscii(str, newdepth);
        }
        tabs = getTabs(newdepth);
        it = fileInfoHolderMap.values().iterator();
        while (it.hasNext()) {
            str.append(tabs);
            str.append("<FILE>");
            FileInfo file = ((FileInfoHolder) it.next()).getFileInfo();
            str.append(file.getFilenameOnly());
            str.append("\n");
        }
        return str;
    }

    private static final String tabChars = "--";

    private static final String getTabs(int number) {
        String str = "";
        for (int i = 0; i < number; i++) {
            str += (tabChars);
        }
        return str;
    }
    
    public Directory[] getDirectoryPath() {
        List<Directory> path = getTreeNodePath();
        Directory[] pathArray = new Directory[path.size()];
        int index = path.size()-1;
        for (Directory directory : path) {
            pathArray[index--] = directory;
        }
        return pathArray;
    }
    
    // TreeNode
    /**
     * NOTE: this is a reversed list! deepest path item first. First path
     * element is this Directory 2nd is parent etc.
     */
    public List getTreeNodePath() {
        Directory dir = this;
        List treeNodes = new LinkedList();
        treeNodes.add(dir);
        while (dir.hasParent()) {
            dir = dir.getParentDirectory();
            if (dir.hasParent()) {// do not include the root "files"
                treeNodes.add(dir);
            }
        }
        return treeNodes;
    }

    public Enumeration children() {
        return new MyChildrenEnum(listSubDirectories());

    }

    public boolean getAllowsChildren() {
        return true;
    }

    public TreeNode getChildAt(int childIndex) {
        return listSubDirectories().get(childIndex);

    }

    public int getChildCount() {
        return listSubDirectories().size();
    }

    public int getIndex(TreeNode treeNode) {
        if (!(treeNode instanceof Directory)) {
            return -1;
        }
        return listSubDirectories().indexOf(treeNode);
    }

    public boolean isLeaf() {
        return listSubDirectories().size() != 0;
    }

    public TreeNode getParent() {
        if (parent == null) {
            return rootFolder.getTreeNode();
        }
        return parent;
    }

    private class MyChildrenEnum implements Enumeration {
        private List childs;
        private int index = -1;

        public MyChildrenEnum(List childs) {
            this.childs = childs;
        }

        public boolean hasMoreElements() {
            return index < childs.size() - 1;
        }

        public Object nextElement() {
            return childs.get(++index);
        }
    }

    // not mutable ignore this
    public void insert(MutableTreeNode child, int index) {
    }

    public void remove(int index) {
    }

    public void remove(MutableTreeNode node) {
    }

    public void removeFromParent() {
    }

    public void setParent(MutableTreeNode newParent) {
    }

    public void setUserObject(Object object) {
    }

}
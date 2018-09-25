package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.StatusCode;
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.d2d.NodeEvent;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.dao.FileInfoCriteria;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FileInfoFactory;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.protocol.FileListRequestProto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public class D2DFileListRequest extends D2DRequestMessage {

    private String folderId;
    private Collection<String> fileIds;
    private boolean recursive;

    public D2DFileListRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public D2DFileListRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public String getFolderId() {
        return folderId;
    }

    public Collection<String> getFileIds() {
        return fileIds;
    }

    public void setFileIds(Collection<String> fileIds) {
        this.fileIds = fileIds;
    }

    public boolean isRecursive() {
        return recursive;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof FileListRequestProto.FileListRequest) {
            FileListRequestProto.FileListRequest proto = (FileListRequestProto.FileListRequest) message;
            this.requestCode = proto.getRequestCode();
            this.folderId = proto.getFolderId();
            this.fileIds = new CopyOnWriteArrayList<>();
            this.fileIds.addAll(proto.getFileIdsList());
            this.recursive = proto.getRecursive();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        FileListRequestProto.FileListRequest.Builder builder = FileListRequestProto.FileListRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.getRequestCode());
        if (this.folderId != null) builder.setFolderId(this.folderId);
        if (this.fileIds != null) {
            for (String fileId : this.fileIds) {
                builder.addFileIds(fileId);
            }
        }
        builder.setRecursive(this.recursive);
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.folderId != null;
    }

    @Override
    public NodeEvent getNodeEvent() {
        return NodeEvent.FILE_LIST_REQUEST;
    }

    /**
     * @api.status 200 OK
     * @api.status 400 BAD_REQUEST
     * @api.status 403 FORBIDDEN
     */
    @Override
    public void handle(Member node) {
        if (!this.isValid()) {
            node.sendMessagesAsynchron(new FileListReply(this.requestCode, StatusCode.FORBIDDEN, null));
            return;
        }
        // Get folder
        if (this.folderId == null) {
            node.sendMessagesAsynchron(new FileListReply(this.requestCode, StatusCode.BAD_REQUEST, null));
            return;
        }
        FolderInfo folderInfo = new FolderInfo("", this.folderId);
        Folder folder = node.getController().getFolderRepository().getFolder(folderInfo);
        if (folder == null) {
            node.sendMessagesAsynchron(new FileListReply(this.requestCode, StatusCode.BAD_REQUEST, null));
            return;
        }
        if (!folder.hasReadPermission(node)) {
            node.sendMessagesAsynchron(new FileListReply(this.requestCode, StatusCode.BAD_REQUEST, null));
            return;
        }
        folder.waitForScan();
        Collection<FileInfo> fileInfos;
        // If no file IDs are provided, send all file infos of the folder
        if (this.fileIds == null || this.fileIds.size() == 0) {
            // Set criteria
            FileInfoCriteria fileInfoCriteria = new FileInfoCriteria();
            fileInfoCriteria.addConnectedAndMyself(folder);
            if (this.recursive) {
                fileInfoCriteria.setRecursive(true);
            }
            fileInfos = folder.getDAO().findFilesFast(fileInfoCriteria);
        } else {
            // If file IDs are provided fetch the file infos fo those IDs
            fileInfos = new ArrayList<>();
            for (String fileId: this.fileIds) {
                FileInfo fileInfo = folder.getDAO().find(FileInfoFactory.lookupInstance(folderInfo, fileId), node.getController().getMySelf().getId());
                if (fileInfo != null) {
                    fileInfos.add(fileInfo);
                }
                // If the file info is a directory info, also add all file infos inside this directory
                if (fileInfo instanceof DirectoryInfo) {
                    FileInfoCriteria fileInfoCriteria = new FileInfoCriteria();
                    fileInfoCriteria.addConnectedAndMyself(folder);
                    fileInfoCriteria.setPath(fileInfo.getRelativeName());
                    // Only search recursive if explicitly requested
                    if (this.recursive) {
                        fileInfoCriteria.setRecursive(true);
                    }
                    fileInfos.addAll(folder.getDAO().findFilesFast(fileInfoCriteria));
                }
            }
        }
        node.sendMessagesAsynchron(new FileListReply(this.requestCode, StatusCode.OK, fileInfos));
    }

}

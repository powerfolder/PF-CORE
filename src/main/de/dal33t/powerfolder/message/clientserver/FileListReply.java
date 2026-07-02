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
 */
package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.StatusCode;
import de.dal33t.powerfolder.d2d.D2DEvent;
import de.dal33t.powerfolder.d2d.D2DReplyFromServer;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.d2d.NodeEvent;
import de.dal33t.powerfolder.light.DirectoryInfo;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.light.FolderInfo;
import de.dal33t.powerfolder.message.FileList;
import de.dal33t.powerfolder.protocol.DirectoryInfoProto;
import de.dal33t.powerfolder.protocol.FileInfoProto;
import de.dal33t.powerfolder.protocol.FileListReplyProto;
import de.dal33t.powerfolder.protocol.ShareLinkInfoProto;

import java.util.Collection;

public class FileListReply extends D2DReplyMessage implements D2DEvent {

    private Collection<FileInfo> fileInfos;

    public FileListReply() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public FileListReply(AbstractMessage message) {
        initFromD2D(message);
    }

    public FileListReply(String replyCode, StatusCode replyStatusCode, Collection<FileInfo> fileInfos) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.fileInfos = fileInfos;
    }

    public Collection<FileInfo> getFileInfos() {
        return fileInfos;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof FileListReplyProto.FileListReply) {
            FileListReplyProto.FileListReply proto = (FileListReplyProto.FileListReply) message;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = StatusCode.getEnum(proto.getReplyStatusCode());
            for (FileInfoProto.FileInfo fileInfo : proto.getFileInfosList()) {
                if (fileInfo.getClazzName().equals("DirectoryInfo")) {
                    this.fileInfos.add(new DirectoryInfo(fileInfo));
                } else {
                    this.fileInfos.add(new FileInfo(fileInfo));
                }
            }
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        FileListReplyProto.FileListReply.Builder builder = FileListReplyProto.FileListReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        builder.setReplyStatusCode(this.replyStatusCode.getCode());
        if (this.fileInfos != null) {
            for (FileInfo fileInfo : this.fileInfos) {
                builder.addFileInfos((FileInfoProto.FileInfo) fileInfo.toD2D());
            }
        }
        return builder.build();
    }

    public NodeEvent getNodeEvent() {
        return NodeEvent.FILE_LIST_REPLY;
    }

}

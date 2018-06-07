package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DReplyMessage;
import de.dal33t.powerfolder.protocol.FileSearchReplyProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;

public class FileSearchReply extends D2DReplyMessage {

    private String[] fileIds;

    public FileSearchReply() {
    }

    public FileSearchReply(String replyCode, ReplyStatusCode replyStatusCode) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
    }

    public FileSearchReply(String replyCode, ReplyStatusCode replyStatusCode, String[] fileIds) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.fileIds = fileIds;
    }

    public String[] getFileIds() {
        return fileIds;
    }

    public void setFileIds(String[] fileIds) {
        this.fileIds = fileIds;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public FileSearchReply(AbstractMessage message) {
        initFromD2D(message);
    }


    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof FileSearchReplyProto.FileSearchReply) {
            FileSearchReplyProto.FileSearchReply proto = (FileSearchReplyProto.FileSearchReply) message;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        FileSearchReplyProto.FileSearchReply.Builder builder = FileSearchReplyProto.FileSearchReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null) builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode) this.replyStatusCode.toD2D());
        if (this.fileIds != null) {
            for (String fileId : this.fileIds) {
                builder.addFileIds(fileId);
            }
        }
        return builder.build();
    }

}

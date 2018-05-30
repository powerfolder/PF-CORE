package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.light.FileInfo;
import de.dal33t.powerfolder.protocol.FileInfoProto;
import de.dal33t.powerfolder.protocol.ThumbnailRequestProto;

public class ThumbnailRequest extends D2DRequestMessage {

    private FileInfo fileInfo;

    public ThumbnailRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public ThumbnailRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof ThumbnailRequestProto.ThumbnailRequest) {
            ThumbnailRequestProto.ThumbnailRequest proto = (ThumbnailRequestProto.ThumbnailRequest) message;
            this.requestCode = proto.getRequestCode();
            this.fileInfo = new FileInfo(proto.getFileInfo());
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        ThumbnailRequestProto.ThumbnailRequest.Builder builder = ThumbnailRequestProto.ThumbnailRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.getRequestCode());
        if (this.fileInfo != null) builder.setFileInfo((FileInfoProto.FileInfo) this.fileInfo.toD2D());
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.fileInfo != null;
    }

}

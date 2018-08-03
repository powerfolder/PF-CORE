package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.protocol.GroupSearchRequestProto;

public class GroupSearchRequest extends D2DRequestMessage {

    private String keyword;

    public GroupSearchRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public GroupSearchRequest(AbstractMessage message) {
        initFromD2D(message);
    }


    public String getKeyword() {
        return keyword;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof GroupSearchRequestProto.GroupSearchRequest) {
            GroupSearchRequestProto.GroupSearchRequest proto = (GroupSearchRequestProto.GroupSearchRequest) message;
            this.requestCode = proto.getRequestCode();
            this.keyword = proto.getKeyword();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        GroupSearchRequestProto.GroupSearchRequest.Builder builder = GroupSearchRequestProto.GroupSearchRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.keyword != null) builder.setKeyword(this.keyword);
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.keyword != null;
    }

}

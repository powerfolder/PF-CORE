package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.CertificateRequestProto;

public class CertificateRequest extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;
    private String nodeId;

    /**
     * Serialization constructor
     */
    public CertificateRequest() {
    }

    public CertificateRequest(String nodeId) {
        this.setNodeId(nodeId);
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public CertificateRequest(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof CertificateRequestProto.CertificateRequest) {
            CertificateRequestProto.CertificateRequest proto = (CertificateRequestProto.CertificateRequest)mesg;
            this.setNodeId(proto.getNodeId());
        }
    }
    
    /** toD2D
     * Convert to D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @return Converted D2D message
     **/
    
    @Override
    public AbstractMessage toD2D() {
        CertificateRequestProto.CertificateRequest.Builder builder = CertificateRequestProto.CertificateRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setNodeId(this.getNodeId());
        return builder.build();
    }
}

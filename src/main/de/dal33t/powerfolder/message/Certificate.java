package de.dal33t.powerfolder.message;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.protocol.CertificateProto;

public class Certificate extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;
    private String nodeId;
    private String certificate;

    /**
     * Serialization constructor
     */
    public Certificate() {
    }

    public Certificate(String nodeId, String certificate) {
        this.setNodeId(nodeId);
        this.setCertificate(certificate);
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public Certificate(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof CertificateProto.Certificate) {
            CertificateProto.Certificate proto = (CertificateProto.Certificate)mesg;
            this.setNodeId(proto.getNodeId());
            this.setCertificate(proto.getCertificate());
        }
    }
    
    /** toD2D
     * Convert to D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @return Converted D2D message
     **/
    
    @Override
    public AbstractMessage toD2D() {
        CertificateProto.Certificate.Builder builder = CertificateProto.Certificate.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        builder.setNodeId(this.getNodeId());
        builder.setCertificate(this.getCertificate());
        return builder.build();
    }
}

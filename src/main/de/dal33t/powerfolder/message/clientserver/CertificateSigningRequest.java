package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DRequestMessage;
import de.dal33t.powerfolder.d2d.D2DRequestToServer;
import de.dal33t.powerfolder.d2d.NodeEvent;
import de.dal33t.powerfolder.protocol.CertificateSigningRequestProto;

public class CertificateSigningRequest extends D2DRequestMessage implements D2DRequestToServer {

    private String certificateSigningRequest;

    public CertificateSigningRequest() {
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    public CertificateSigningRequest(AbstractMessage message) {
        initFromD2D(message);
    }

    public String getCertificateSigningRequest() {
        return certificateSigningRequest;
    }

    /**
     * Init from D2D message
     *
     * @param message Message to use data from
     **/
    @Override
    public void initFromD2D(AbstractMessage message) {
        if (message instanceof CertificateSigningRequestProto.CertificateSigningRequest) {
            CertificateSigningRequestProto.CertificateSigningRequest proto = (CertificateSigningRequestProto.CertificateSigningRequest) message;
            this.requestCode = proto.getRequestCode();
            this.certificateSigningRequest = proto.getCertificateSigningRequest();
        }
    }

    /**
     * Convert to D2D message
     *
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        CertificateSigningRequestProto.CertificateSigningRequest.Builder builder = CertificateSigningRequestProto.CertificateSigningRequest.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.requestCode != null) builder.setRequestCode(this.requestCode);
        if (this.certificateSigningRequest != null) builder.setCertificateSigningRequest(this.certificateSigningRequest);
        return builder.build();
    }

    @Override
    public boolean isValid() {
        return super.isValid() && this.certificateSigningRequest != null;
    }

    @Override
    public NodeEvent getNodeEvent() {
        return NodeEvent.CERTIFICATE_SIGNING_REQUEST;
    }

}

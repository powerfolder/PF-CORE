package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.CertificateSigningRequestProto;

public class CertificateSigningRequest extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private String requestCode;
    private String certificateSigningRequest;

    /**
     * Serialization constructor
     */
    public CertificateSigningRequest() {
    }

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public String getCertificateSigningRequest() {
        return certificateSigningRequest;
    }

    public void setCertificateSigningRequest(String certificateSigningRequest) {
        this.certificateSigningRequest = certificateSigningRequest;
    }

    /**
     * Init from D2D message
     * @param mesg Message to use data from
     **/
    public CertificateSigningRequest(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof CertificateSigningRequestProto.CertificateSigningRequest) {
            CertificateSigningRequestProto.CertificateSigningRequest proto = (CertificateSigningRequestProto.CertificateSigningRequest)mesg;
            this.requestCode = proto.getRequestCode();
            this.setCertificateSigningRequest(proto.getCertificateSigningRequest());
        }
    }
    
    /** toD2D
     * Convert to D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
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
}

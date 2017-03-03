package de.dal33t.powerfolder.message.clientserver;

import com.google.protobuf.AbstractMessage;
import de.dal33t.powerfolder.d2d.D2DObject;
import de.dal33t.powerfolder.message.Message;
import de.dal33t.powerfolder.protocol.CertificateSigningReplyProto;
import de.dal33t.powerfolder.protocol.ReplyStatusCodeProto;

public class CertificateSigningReply extends Message implements D2DObject {
    private static final long serialVersionUID = 100L;

    private String replyCode;
    private ReplyStatusCode replyStatusCode;
    private String certificate;

    /**
     * Serialization constructor
     */
    public CertificateSigningReply() {
    }

    public CertificateSigningReply(String replyCode, ReplyStatusCode replyStatusCode, String certificate) {
        this.replyCode = replyCode;
        this.replyStatusCode = replyStatusCode;
        this.certificate = certificate;
    }

    public String getReplyCode() {
        return replyCode;
    }

    public void setReplyCode(String replyCode) {
        this.replyCode = replyCode;
    }

    public ReplyStatusCode getReplyStatusCode() {
        return replyStatusCode;
    }

    public void setReplyStatusCode(ReplyStatusCode replyStatusCode) {
        this.replyStatusCode = replyStatusCode;
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
    public CertificateSigningReply(AbstractMessage mesg) {
        initFromD2D(mesg);
    }

    /**
     * toD2D
     * Convert to D2D message
     *
     * @return Converted D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     **/
    @Override
    public void initFromD2D(AbstractMessage mesg) {
        if(mesg instanceof CertificateSigningReplyProto.CertificateSigningReply) {
            CertificateSigningReplyProto.CertificateSigningReply proto = (CertificateSigningReplyProto.CertificateSigningReply)mesg;
            this.replyCode = proto.getReplyCode();
            this.replyStatusCode = new ReplyStatusCode(proto.getReplyStatusCode());
            this.certificate = proto.getCertificate();
        }
    }
    
    /** toD2D
     * Convert to D2D message
     * @author Christian Oberdörfer <oberdoerfer@powerfolder.com>
     * @return Converted D2D message
     **/
    @Override
    public AbstractMessage toD2D() {
        CertificateSigningReplyProto.CertificateSigningReply.Builder builder = CertificateSigningReplyProto.CertificateSigningReply.newBuilder();
        builder.setClazzName(this.getClass().getSimpleName());
        if (this.replyCode != null) builder.setReplyCode(this.replyCode);
        if (this.replyStatusCode != null) builder.setReplyStatusCode((ReplyStatusCodeProto.ReplyStatusCode)this.replyStatusCode.toD2D());
        if (this.certificate != null) builder.setCertificate(this.certificate);
        return builder.build();
    }
}

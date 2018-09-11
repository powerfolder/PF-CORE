package de.dal33t.powerfolder.d2d;

import de.dal33t.powerfolder.message.Identity;
import de.dal33t.powerfolder.message.IdentityReply;
import de.dal33t.powerfolder.message.clientserver.AccountInfoRequest;
import de.dal33t.powerfolder.message.clientserver.LoginRequest;

public enum NodeEvent {

    IDENTITY,
    IDENTITY_REPLY,
    LOGIN_REQUEST,
    ACCOUNT_INFO_REQUEST;

    public static NodeEvent getEnum(D2DObject object) {
        if (object instanceof Identity) {
            return IDENTITY;
        } else if (object instanceof IdentityReply) {
            return IDENTITY_REPLY;
        } else if (object instanceof LoginRequest) {
            return LOGIN_REQUEST;
        } else if (object instanceof AccountInfoRequest) {
            return ACCOUNT_INFO_REQUEST;
        }
        return IDENTITY;
    }

}
